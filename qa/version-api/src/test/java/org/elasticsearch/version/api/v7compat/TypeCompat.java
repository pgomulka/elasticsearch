package org.elasticsearch.version.api.v7compat;

import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.version.api.VersionApiClientYamlTestSuiteIT;

import java.util.List;
import java.util.Set;

/**
 * Handle version v7 API compatibility. This class can be removed when the current major version == 9;
 */
public class TypeCompat {
    private TypeCompat(){}

    //These tests from 7.x are expected to emit warnings about types
    static final Set<String> EXPECT_TYPE_WARNINGS = Set.of(
        "ingest/80_foreach/Test foreach Processor"
    );

    //TODO: handle the compat warnings

    public static void handleTypeCompatibility(ClientYamlTestCandidate testCandidate){
        //TODO: meaningful assertion here if running version 9
        allowTypeWarnings(testCandidate);
    }

    /**
     * Adds the expected type warning header so that the
     */
    private static void allowTypeWarnings(ClientYamlTestCandidate testCandidate) {
        if (EXPECT_TYPE_WARNINGS.contains(testCandidate.getTestPath())) {
            List<DoSection> doSections = VersionApiClientYamlTestSuiteIT.getDoSectionsByParam(testCandidate, "type");
            doSections.forEach(d -> d.addExpectedWarningHeader("foobarbear"));
        }
    }

}
