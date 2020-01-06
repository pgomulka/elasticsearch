package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;

public class RestCompatYamlTestSuiteIT extends ESClientYamlSuiteTestCase {

    protected RestCompatYamlTestSuiteIT(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }
}
