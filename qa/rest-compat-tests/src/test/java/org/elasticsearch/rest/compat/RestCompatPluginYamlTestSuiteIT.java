package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import com.carrotsearch.randomizedtesting.annotations.ParametersFactory;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

/**
 * Runs the REST compatibility tests for all of the plugins.
 */
public class RestCompatPluginYamlTestSuiteIT extends AbstractRestCompatYamlTestSuite {

    //TODO: ensure plugins are installed.

    public RestCompatPluginYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    @ParametersFactory
    public static Iterable<Object[]> parameters() throws Exception {
        return AbstractRestCompatYamlTestSuite.getOldTests("plugins");
    }
}
