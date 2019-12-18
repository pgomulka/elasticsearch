package org.elasticsearch.version.api;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.apache.http.HttpHost;
import org.elasticsearch.Version;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ClientYamlTestClient;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.test.rest.yaml.restspec.ClientYamlSuiteRestSpec;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

// ./gradlew :qa:version-api:integTestRunner  -Dtests.timestamp=$(date +%S) --info
// ./gradlew ':qa:version-api:integTestRunner' --tests "org.elasticsearch.version.api.VersionApiClientYamlTestSuiteIT.test {yaml=ingest/80_foreach/Test foreach Processor}"
public class VersionApiClientYamlTestSuiteIT extends ESClientYamlSuiteTestCase {


    //TODO: remove this ... we shouldn't need this..just here while building this out.
    static final Set<String> BLACKLISTED = Set.of(
        "repository-s3" //TODO: figure out why this breaks running a single tests .. i assume some assert or exception is thrown from here.
    );

    static boolean USE_WHITE_LIST = true;
    //TODO: remove this ... we shouldn't need this..just here while building this out.
    static final Set<String> WHITELISTED = Set.of(
        "rest-api-spec",
        "ingest-common",
        "ingest-geoip"
    );

    //These are test names from the the last minor
    static final Set<String> EXPECT_TYPE_WARNINGS = Set.of(
        "ingest/80_foreach/Test foreach Processor"
    );

    public VersionApiClientYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        //TODO: support a warning if indivisual tests are requested.
        //TODO: support a JVM arg to allow picking just a single group, e.g. which index/10_basic ?

        List<Object[]> tests = new ArrayList<>();
        //TODO: DRY THIS .. the first one uses a slightly different path , the second two are the same
        Path root1 = Paths.get(System.getProperty("versionApiTestRoot"), "rest-api-spec", "src", "main", "resources", "rest-api-spec", "test");

        if (BLACKLISTED.stream().anyMatch(p -> root1.toString().contains(p))) {
            System.out.println("%%%%%%%%%%%%%%%%%% Skipping due to blacklist " + root1.toString());

        } else if (USE_WHITE_LIST && WHITELISTED.stream().anyMatch(p -> root1.toString().contains(p)) || USE_WHITE_LIST == false) {
            System.out.println("************* Finding tests from: " + root1);
            Iterable<Object[]> foundTests = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, root1, false);

            foundTests.forEach(objectArray -> {
                for (Object o : objectArray) {
                    System.out.println("** --> " + o);
                }
            });
            foundTests.forEach(tests::add);
        }

        Path groupRoot = Paths.get(System.getProperty("versionApiTestRoot"), "modules");
        for (File f : Objects.requireNonNull(groupRoot.toFile().listFiles())) {
            Path pathToResources = Paths.get("src", "test", "resources", "rest-api-spec", "test");
            Path root = groupRoot.resolve(Paths.get(f.getName()).resolve(pathToResources));
            if (BLACKLISTED.stream().anyMatch(p -> root.toString().contains(p))) {
                System.out.println("%%%%%%%%%%%%%%%%%% Skipping due to blacklist " + root.toString());
                continue;
            } else if (USE_WHITE_LIST && WHITELISTED.stream().anyMatch(p -> root.toString().contains(p)) || USE_WHITE_LIST == false) {
                System.out.println("************* Finding tests from: " + root);

                Iterable<Object[]> foundTests = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, root, false);
                foundTests.forEach(objectArray -> {
                    for (Object o : objectArray) {
                        System.out.println("** -->" + o);
                    }
                });
                foundTests.forEach(tests::add);
            }
        }

        groupRoot = Paths.get(System.getProperty("versionApiTestRoot"), "plugins");
        for (File f : Objects.requireNonNull(groupRoot.toFile().listFiles())) {
            Path pathToResources = Paths.get("src", "test", "resources", "rest-api-spec", "test");
            Path root = groupRoot.resolve(Paths.get(f.getName()).resolve(pathToResources));
            if (BLACKLISTED.stream().anyMatch(p -> root.toString().contains(p))) {
                System.out.println("%%%%%%%%%%%%%%%%%% Skipping due to blacklist " + root.toString());
                continue;
            } else if (USE_WHITE_LIST && WHITELISTED.stream().anyMatch(p -> root.toString().contains(p)) || USE_WHITE_LIST == false) {

                System.out.println("************* Finding tests from: " + root);
                Iterable<Object[]> foundTests = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, root, false);
                foundTests.forEach(objectArray -> {
                    for (Object o : objectArray) {
                        System.out.println("** -->" + o);
                    }
                });
                foundTests.forEach(tests::add);
            }
        }

        //TODO: support REST tests defined in THIS project too !

        return tests;
    }


    @Override
    protected ClientYamlTestClient initClientYamlTestClient(ClientYamlSuiteRestSpec restSpec, RestClient restClient, List<HttpHost> hosts, Version esVersion, Version masterVersion) {
        return super.initClientYamlTestClient(restSpec, restClient, hosts, esVersion, masterVersion);
    }

    @Override
    public void overrideDoSection() {
        addVersionHeader();
        handleTypeWarnings();
    }

    private void addVersionHeader() {
        //TODO: figure exactly what this header should be ...
        getAllDoSections().forEach(d -> d.getApiCallSection().addHeaders(Collections.singletonMap(Version.COMPATIBLE_HEADER, Version.COMPATIBLE_VERSION)));
    }

    private void handleTypeWarnings() {
        if (EXPECT_TYPE_WARNINGS.contains(getTestCandidate().getTestPath())) {
            List<DoSection> doSections = getDoSectionsByParam("type");
            doSections.forEach(d -> d.addExpectedWarningHeader("foobarbear"));
        }
    }
}

