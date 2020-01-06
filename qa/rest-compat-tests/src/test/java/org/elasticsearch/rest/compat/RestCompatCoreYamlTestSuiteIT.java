package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;

/**
 * Runs the REST compatibility tests for all of the core tests from rest-api-spec.
 */
public class RestCompatCoreYamlTestSuiteIT extends AbstractRestCompatYamlTestSuite {

    public RestCompatCoreYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return AbstractRestCompatYamlTestSuite.getParameters("rest-api-spec");
    }
}
