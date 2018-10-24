/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.upgrade;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.CheckedConsumer;
import org.elasticsearch.common.io.stream.NamedWriteableRegistry;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.IndexScopedSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsFilter;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.env.Environment;
import org.elasticsearch.env.NodeEnvironment;
import org.elasticsearch.indices.IndexTemplateMissingException;
import org.elasticsearch.plugins.ActionPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.protocol.xpack.migration.UpgradeActionRequired;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestHandler;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.xpack.core.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.core.template.TemplateUtils;
import org.elasticsearch.xpack.core.upgrade.actions.IndexUpgradeAction;
import org.elasticsearch.xpack.core.upgrade.actions.IndexUpgradeInfoAction;
import org.elasticsearch.xpack.core.watcher.client.WatcherClient;
import org.elasticsearch.xpack.core.watcher.support.WatcherIndexTemplateRegistryField;
import org.elasticsearch.xpack.core.watcher.transport.actions.service.WatcherServiceRequest;
import org.elasticsearch.xpack.core.watcher.transport.actions.service.WatcherServiceRequestBuilder;
import org.elasticsearch.xpack.core.watcher.transport.actions.stats.WatcherStatsRequestBuilder;
import org.elasticsearch.xpack.upgrade.actions.TransportIndexUpgradeAction;
import org.elasticsearch.xpack.upgrade.actions.TransportIndexUpgradeInfoAction;
import org.elasticsearch.xpack.upgrade.rest.RestIndexUpgradeAction;
import org.elasticsearch.xpack.upgrade.rest.RestIndexUpgradeInfoAction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Upgrade extends Plugin implements ActionPlugin {

    public static final Version UPGRADE_INTRODUCED = Version.CURRENT.minimumCompatibilityVersion();

    private final Settings settings;
    private final List<BiFunction<Client, ClusterService, IndexUpgradeCheck>> upgradeCheckFactories;

    public Upgrade(Settings settings) {
        this.settings = settings;
        this.upgradeCheckFactories = new ArrayList<>();
        this.upgradeCheckFactories.add(getWatchesIndexUpgradeCheckFactory(settings));
        this.upgradeCheckFactories.add(getTriggeredWatchesIndexUpgradeCheckFactory(settings));
        this.upgradeCheckFactories.add(getSecurityUpgradeCheckFactory(settings));
    }
    // this is the required index.format setting for 6.0 services (watcher and security) to start up
    // this index setting is set by the upgrade API or automatically when a 6.0 index template is created
    private static final int EXPECTED_INDEX_FORMAT_VERSION = 6;
    /**
     * Checks the format of an internal index and returns true if the index is up to date or false if upgrade is required
     */
    public static boolean checkInternalIndexFormat(IndexMetaData indexMetaData) {
        return indexMetaData.getSettings().getAsInt(IndexMetaData.INDEX_FORMAT_SETTING.getKey(), 0) == EXPECTED_INDEX_FORMAT_VERSION;
    }
    public static final String[] MAPPING_TYPE_NAME_ARRAY = { /*NativeUsersStore.USER_DOC_TYPE,
        NativeUsersStore.RESERVED_USER_DOC_TYPE, NativeUsersStore.NEW_INDEX_TYPE ,*/RoleDescriptor.ROLE_TYPE,  };

    static BiFunction<Client, ClusterService, IndexUpgradeCheck> getSecurityUpgradeCheckFactory(Settings settings) {
        return (internalClient, clusterService) ->
            new IndexUpgradeCheck<Void>("security",
                settings,
                indexMetaData -> {
                    if (".security".equals(indexMetaData.getIndex().getName())
                        || indexMetaData.getAliases().containsKey(".security")) {

                        if (checkInternalIndexFormat(indexMetaData)) {
                            return UpgradeActionRequired.UP_TO_DATE;
                        } else {
                            return UpgradeActionRequired.UPGRADE;
                        }
                    } else {
                        return UpgradeActionRequired.NOT_APPLICABLE;
                    }
                }, internalClient,
                clusterService,
                /*IndexLifecycleManager.*/MAPPING_TYPE_NAME_ARRAY,
                new Script(ScriptType.INLINE, "painless",
                    "ctx._source.type = ctx._type;\n" +
                        "if (!ctx._type.equals(\"doc\")) {\n" +
                        "   ctx._id = ctx._type + \"-\" + ctx._id;\n" +
                        "}\n" +
                        "ctx._type = \"doc\";",
                    new HashMap<>()),
                voidListener -> preSecurityUpgrade(internalClient, voidListener),
                (success, listener) -> listener.onResponse(null));
    }

    public static final int UPRADE_VERSION = 6;

    public static int NEW_INDEX_VERSION = /*IndexUpgradeCheck.*/UPRADE_VERSION;

    public static final String SECURITY_INDEX_NAME = ".security";
    public static final String SECURITY_TEMPLATE_NAME = "security-index-template";
    public static final String NEW_SECURITY_TEMPLATE_NAME = "security-index-template-v6";
    public static final String NEW_SECURITY_INDEX_NAME = SECURITY_INDEX_NAME + "-" + /*IndexLifecycleManager.*/NEW_INDEX_VERSION;


    public static final String TEMPLATE_VERSION_PATTERN =
        Pattern.quote("${security.template.version}");

    private static void preSecurityUpgrade(Client client, ActionListener<Void> listener) {
        // put the new security template
        final byte[] templateBytes = TemplateUtils.loadTemplate("/" + NEW_SECURITY_TEMPLATE_NAME + ".json",
            Version.CURRENT.toString(), /*IndexLifecycleManager.*/TEMPLATE_VERSION_PATTERN).getBytes(StandardCharsets.UTF_8);
        client.admin().indices().preparePutTemplate(NEW_SECURITY_TEMPLATE_NAME)
            .setSource(templateBytes, XContentType.JSON).execute(new ActionListener<AcknowledgedResponse>() {
            @Override
            public void onResponse(AcknowledgedResponse putIndexTemplateResponse) {
                if (putIndexTemplateResponse.isAcknowledged()) {
                    listener.onResponse(null);
                } else {
                    listener.onFailure(new IllegalStateException("unable to create new security template"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                listener.onFailure(e);
            }
        });
    }

    static BiFunction<Client, ClusterService, IndexUpgradeCheck> getWatchesIndexUpgradeCheckFactory(Settings settings) {
        return (internalClient, clusterService) ->
            new IndexUpgradeCheck<Boolean>("watches",
                settings,
                indexMetaData -> {
                    if (indexOrAliasExists(indexMetaData, ".watches")) {
                        if (checkInternalIndexFormat(indexMetaData)) {
                            return UpgradeActionRequired.UP_TO_DATE;
                        } else {
                            return UpgradeActionRequired.UPGRADE;
                        }
                    } else {
                        return UpgradeActionRequired.NOT_APPLICABLE;
                    }
                }, internalClient,
                clusterService,
                new String[]{"watch"},
                new Script(ScriptType.INLINE, "painless", "ctx._type = \"doc\";\n" +
                    "if (ctx._source.containsKey(\"_status\") && !ctx._source.containsKey(\"status\")  ) {\n" +
                    "  ctx._source.status = ctx._source.remove(\"_status\");\n" +
                    "}",
                    new HashMap<>()),
                booleanActionListener -> preWatchesIndexUpgrade(internalClient, booleanActionListener),
                (shouldStartWatcher, listener) -> postWatchesIndexUpgrade(internalClient, shouldStartWatcher, listener)
            );
    }

    public static final String INDEX_NAME = ".triggered_watches";

    static BiFunction<Client, ClusterService, IndexUpgradeCheck> getTriggeredWatchesIndexUpgradeCheckFactory(Settings settings) {
        return (internalClient, clusterService) ->
            new IndexUpgradeCheck<Boolean>("triggered-watches",
                settings,
                indexMetaData -> {
                    if (indexOrAliasExists(indexMetaData, /*TriggeredWatchStore.*/INDEX_NAME)) {
                        if (checkInternalIndexFormat(indexMetaData)) {
                            return UpgradeActionRequired.UP_TO_DATE;
                        } else {
                            return UpgradeActionRequired.UPGRADE;
                        }
                    } else {
                        return UpgradeActionRequired.NOT_APPLICABLE;
                    }
                }, internalClient,
                clusterService,
                new String[]{"triggered-watch"},
                new Script(ScriptType.INLINE, "painless", "ctx._type = \"doc\";\n", new HashMap<>()),
                booleanActionListener -> preTriggeredWatchesIndexUpgrade(internalClient, booleanActionListener),
                (shouldStartWatcher, listener) -> postWatchesIndexUpgrade(internalClient, shouldStartWatcher, listener)
            );
    }

    private static boolean indexOrAliasExists(IndexMetaData indexMetaData, String name) {
        return name.equals(indexMetaData.getIndex().getName()) || indexMetaData.getAliases().containsKey(name);
    }

    static void preTriggeredWatchesIndexUpgrade(Client client, ActionListener<Boolean> listener) {
        new WatcherClient(client).prepareWatcherStats().execute(ActionListener.wrap(
            stats -> {
                if (stats.watcherMetaData().manuallyStopped()) {
                    preTriggeredWatchesIndexUpgrade(client, listener, false);
                } else {
                    new WatcherClient(client).prepareWatchService().stop().execute(ActionListener.wrap(
                        watcherServiceResponse -> {
                            if (watcherServiceResponse.isAcknowledged()) {
                                preTriggeredWatchesIndexUpgrade(client, listener, true);
                            } else {
                                listener.onFailure(new IllegalStateException("unable to stop watcher service"));
                            }

                        },
                        listener::onFailure));
                }
            },
            listener::onFailure));
    }

    private static void preTriggeredWatchesIndexUpgrade(final Client client, final ActionListener<Boolean> listener,
                                                        final boolean restart) {
        final String legacyTriggeredWatchesTemplateName = "triggered_watches";

        ActionListener<AcknowledgedResponse> returnToCallerListener =
            deleteIndexTemplateListener(legacyTriggeredWatchesTemplateName, listener, () -> listener.onResponse(restart));

        // step 2, after put new .triggered_watches template: delete triggered_watches index template, then return to caller
        ActionListener<AcknowledgedResponse> putTriggeredWatchesListener =
            putIndexTemplateListener(WatcherIndexTemplateRegistryField.TRIGGERED_TEMPLATE_NAME, listener,
                () -> client.admin().indices().prepareDeleteTemplate(legacyTriggeredWatchesTemplateName)
                    .execute(returnToCallerListener));

        // step 1, put new .triggered_watches template
        final byte[] triggeredWatchesTemplate = TemplateUtils.loadTemplate("/triggered-watches.json",
            WatcherIndexTemplateRegistryField.INDEX_TEMPLATE_VERSION,
            Pattern.quote("${xpack.watcher.template.version}")).getBytes(StandardCharsets.UTF_8);

        client.admin().indices().preparePutTemplate(WatcherIndexTemplateRegistryField.TRIGGERED_TEMPLATE_NAME)
            .setSource(triggeredWatchesTemplate, XContentType.JSON).execute(putTriggeredWatchesListener);
    }

    static void preWatchesIndexUpgrade(Client client, ActionListener<Boolean> listener) {
        new WatcherClient(client).prepareWatcherStats().execute(ActionListener.wrap(
            stats -> {
                if (stats.watcherMetaData().manuallyStopped()) {
                    preWatchesIndexUpgrade(client, listener, false);
                } else {
                    new WatcherClient(client).prepareWatchService().stop().execute(ActionListener.wrap(
                        watcherServiceResponse -> {
                            if (watcherServiceResponse.isAcknowledged()) {
                                preWatchesIndexUpgrade(client, listener, true);
                            } else {
                                listener.onFailure(new IllegalStateException("unable to stop watcher service"));
                            }
                        },
                        listener::onFailure));
                }
            },
            listener::onFailure));
    }

    private static void preWatchesIndexUpgrade(final Client client, final ActionListener<Boolean> listener, final boolean restart) {
        final String legacyWatchesTemplateName = "watches";
        ActionListener<AcknowledgedResponse> returnToCallerListener =
            deleteIndexTemplateListener(legacyWatchesTemplateName, listener, () -> listener.onResponse(restart));

        // step 3, after put new .watches template: delete watches index template, then return to caller
        ActionListener<AcknowledgedResponse> putTriggeredWatchesListener =
            putIndexTemplateListener(WatcherIndexTemplateRegistryField.TRIGGERED_TEMPLATE_NAME, listener,
                () -> client.admin().indices().prepareDeleteTemplate(legacyWatchesTemplateName)
                    .execute(returnToCallerListener));

        // step 2, after delete watch history templates: put new .watches template
        final byte[] watchesTemplate = TemplateUtils.loadTemplate("/watches.json",
            WatcherIndexTemplateRegistryField.INDEX_TEMPLATE_VERSION,
            Pattern.quote("${xpack.watcher.template.version}")).getBytes(StandardCharsets.UTF_8);

        ActionListener<AcknowledgedResponse> putTriggeredWatchesTemplateListener = deleteIndexTemplateListener("watch_history_*",
            listener,
            () -> client.admin().indices().preparePutTemplate(WatcherIndexTemplateRegistryField.WATCHES_TEMPLATE_NAME)
                .setSource(watchesTemplate, XContentType.JSON)
                .execute(putTriggeredWatchesListener));

        // step 1, delete watch history index templates
        client.admin().indices().prepareDeleteTemplate("watch_history_*").execute(putTriggeredWatchesTemplateListener);
    }

    static void postWatchesIndexUpgrade(Client client, boolean shouldStartWatcher,
                                        ActionListener<TransportResponse.Empty> listener) {
        if (shouldStartWatcher) {
            // Start the watcher service
            new WatcherClient(client).watcherService(new WatcherServiceRequest().start(), ActionListener.wrap(
                r -> listener.onResponse(TransportResponse.Empty.INSTANCE), listener::onFailure
            ));
        } else {
            listener.onResponse(TransportResponse.Empty.INSTANCE);
        }
    }

    private static ActionListener<AcknowledgedResponse> putIndexTemplateListener(String name, ActionListener<Boolean> listener,
                                                                                     Runnable runnable) {
        return ActionListener.wrap(r -> {
            if (r.isAcknowledged()) {
                runnable.run();
            } else {
                listener.onFailure(new ElasticsearchException("Putting [{}] template was not acknowledged", name));
            }
        }, listener::onFailure);
    }

    private static ActionListener<AcknowledgedResponse> deleteIndexTemplateListener(String name, ActionListener<Boolean> listener,
                                                                                           Runnable runnable) {
        return ActionListener.wrap(r -> {
            if (r.isAcknowledged()) {
                runnable.run();
            } else {
                listener.onFailure(new ElasticsearchException("Deleting [{}] template was not acknowledged", name));
            }
        }, e -> {
            // if we tried to delete a template, but it was not there, we can continue as usual, no need
            // to stop the upgrade
            if (e instanceof IndexTemplateMissingException) {
                runnable.run();
            } else {
                listener.onFailure(e);
            }
        });
    }
    
    @Override
    public Collection<Object> createComponents(Client client, ClusterService clusterService, ThreadPool threadPool,
                                               ResourceWatcherService resourceWatcherService, ScriptService scriptService,
                                               NamedXContentRegistry xContentRegistry, Environment environment,
                                               NodeEnvironment nodeEnvironment, NamedWriteableRegistry namedWriteableRegistry) {
        List<IndexUpgradeCheck> upgradeChecks = new ArrayList<>(upgradeCheckFactories.size());
        for (BiFunction<Client, ClusterService, IndexUpgradeCheck> checkFactory : upgradeCheckFactories) {
            upgradeChecks.add(checkFactory.apply(client, clusterService));
        }
        return Collections.singletonList(new IndexUpgradeService(settings, Collections.unmodifiableList(upgradeChecks)));
    }

    @Override
    public List<ActionHandler<? extends ActionRequest, ? extends ActionResponse>> getActions() {
        return Arrays.asList(
                new ActionHandler<>(IndexUpgradeInfoAction.INSTANCE, TransportIndexUpgradeInfoAction.class),
                new ActionHandler<>(IndexUpgradeAction.INSTANCE, TransportIndexUpgradeAction.class)
        );
    }

    @Override
    public List<RestHandler> getRestHandlers(Settings settings, RestController restController, ClusterSettings clusterSettings,
                                             IndexScopedSettings indexScopedSettings, SettingsFilter settingsFilter,
                                             IndexNameExpressionResolver indexNameExpressionResolver,
                                             Supplier<DiscoveryNodes> nodesInCluster) {
        return Arrays.asList(
                new RestIndexUpgradeInfoAction(settings, restController),
                new RestIndexUpgradeAction(settings, restController)
        );
    }

}
