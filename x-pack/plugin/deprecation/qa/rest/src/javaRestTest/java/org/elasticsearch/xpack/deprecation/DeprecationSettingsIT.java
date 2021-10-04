/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.deprecation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.Header;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.HeaderWarning;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.hamcrest.Matcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.elasticsearch.test.hamcrest.RegexMatcher.matches;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

/**
 * moving the _cluster/settings test with deprecated setting to separate testcase because it was affecting other tests
 */
public class DeprecationSettingsIT extends ESRestTestCase {
    /**
     * Same as <code>DeprecationIndexingAppender#DEPRECATION_MESSAGES_DATA_STREAM</code>, but that class isn't visible from here.
     */
    private static final String DATA_STREAM_NAME = ".logs-deprecation.elasticsearch-default";


    /**
     * Check that configuring deprecation settings causes a warning to be added to the
     * response headers.
     */
    public void testDeprecatedSettingsReturnWarnings() throws Exception {
        XContentBuilder builder = JsonXContent.contentBuilder()
            .startObject()
            .startObject("transient")
            .field(
                TestDeprecationHeaderRestAction.TEST_DEPRECATED_SETTING_TRUE1.getKey(),
                TestDeprecationHeaderRestAction.TEST_DEPRECATED_SETTING_TRUE1.getDefault(Settings.EMPTY) == false
            )
            .field(
                TestDeprecationHeaderRestAction.TEST_DEPRECATED_SETTING_TRUE2.getKey(),
                TestDeprecationHeaderRestAction.TEST_DEPRECATED_SETTING_TRUE2.getDefault(Settings.EMPTY) == false
            )
            // There should be no warning for this field
            .field(
                TestDeprecationHeaderRestAction.TEST_NOT_DEPRECATED_SETTING.getKey(),
                TestDeprecationHeaderRestAction.TEST_NOT_DEPRECATED_SETTING.getDefault(Settings.EMPTY) == false
            )
            .endObject()
            .endObject();

        final Request request = new Request("PUT", "_cluster/settings");
        ///
        request.setJsonEntity(Strings.toString(builder));
        final Response response = client().performRequest(request);

        final List<String> deprecatedWarnings = getWarningHeaders(response.getHeaders());
        final List<Matcher<String>> headerMatchers = new ArrayList<>(2);

        for (Setting<Boolean> setting : List.of(
            TestDeprecationHeaderRestAction.TEST_DEPRECATED_SETTING_TRUE1,
            TestDeprecationHeaderRestAction.TEST_DEPRECATED_SETTING_TRUE2
        )) {
            headerMatchers.add(
                equalTo(
                    "["
                        + setting.getKey()
                        + "] setting was deprecated in Elasticsearch and will be removed in a future release! "
                        + "See the breaking changes documentation for the next major version."
                )
            );
        }

        assertThat(deprecatedWarnings, hasSize(headerMatchers.size()));
        for (final String deprecatedWarning : deprecatedWarnings) {
            assertThat(
                "Header does not conform to expected pattern",
                deprecatedWarning,
                matches(HeaderWarning.WARNING_HEADER_PATTERN.pattern())
            );
        }

        final List<String> actualWarningValues = deprecatedWarnings.stream()
            .map(s -> HeaderWarning.extractWarningValueFromWarningHeader(s, true))
            .collect(Collectors.toList());
        for (Matcher<String> headerMatcher : headerMatchers) {
            assertThat(actualWarningValues, hasItem(headerMatcher));
        }

        assertBusy(() -> {
            try{
                client().performRequest(new Request("POST", "/" + DATA_STREAM_NAME + "/_refresh?ignore_unavailable=true"));
                Response getResponse = client().performRequest(new Request("GET", "/" + DATA_STREAM_NAME + "/_search"));
                assertOK(getResponse);

                ObjectMapper mapper = new ObjectMapper();
                final JsonNode jsonNode = mapper.readTree(response.getEntity().getContent());

                final int hits = jsonNode.at("/hits/total/value").intValue();
                assertThat(hits, equalTo(2));
            }catch (Exception e) {
                throw new AssertionError(e);
            }

        }, 30, TimeUnit.SECONDS);
    }

    private List<String> getWarningHeaders(Header[] headers) {
        List<String> warnings = new ArrayList<>();

        for (Header header : headers) {
            if (header.getName().equals("Warning")) {
                warnings.add(header.getValue());
            }
        }

        return warnings;
    }

}
