package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.test.rest.yaml.section.Assertion;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

class AbstractRestCompatYamlTestSuite extends ESClientYamlSuiteTestCase {

    //TODO: support new command line flag to enforce tests from a specific module, plugin, or core, add this to the reproduce line
    //-Dtests.rest.suite.compat.root=[plugin-name | module-name | core]

    enum SkipReason {
        REQUIRES_ADDITIONAL_SETUP("requires additional setup");

        private final String display;

        public String getDisplay() {
            return display;
        }

        SkipReason(String display) {
            this.display = display;
        }
    }

    //TODO: figure out the additional steps and try to get this list down to zero.
    static final Map<String, SkipReason> SKIP = Map.of(
        "/plugins/repository-gcs", SkipReason.REQUIRES_ADDITIONAL_SETUP,
        "/plugins/discovery-gce", SkipReason.REQUIRES_ADDITIONAL_SETUP,
        "/plugins/repository-azure", SkipReason.REQUIRES_ADDITIONAL_SETUP,
        "/plugins/discovery-ec2", SkipReason.REQUIRES_ADDITIONAL_SETUP,
        "/plugins/repository-s3", SkipReason.REQUIRES_ADDITIONAL_SETUP,
        "/modules/repository-url", SkipReason.REQUIRES_ADDITIONAL_SETUP
    );

    protected AbstractRestCompatYamlTestSuite(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    protected String getApiSpecRelativePath() {
        String[] splits = System.getProperty("restCompatAPIRoot").split(File.separator);
        return "/" + splits[splits.length - 1];
    }

    /**
     * <p>
     * Finds the tests the prior version. Gradle checked out the prior version and copied the REST tests to "restCompatTestRoot" dir.
     * This method finds those old tests to feed into {@link ESClientYamlSuiteTestCase} so that the old REST tests are run against the
     * current server with compatibility mode flag sent. Some the old tests may not parse correctly into executable tests and are listed
     * locally in the SKIP variable.
     * </p><p>
     * Returns a list of parsed and executable tests. Once the tests are parsed and represented by Java code some manipulation is possible.
     * </p>
     * Specially the following manipulations occur:
     * <ul>
     * <li>Inject the compatibly header</li>
     * <li>Ignore warnings (these will be tested elsewhere)</li>
     * <li>Override tests (to allow for different assertions)</li>
     * </ul>
     */
    static Iterable<Object[]> getOldTests(String category) throws Exception {
        String[] splits = System.getProperty("restCompatTestRoot").split(File.separator);
        String testRoot = "/" + splits[splits.length - 1] + "/" + category;
        Set<String> testRoots = new HashSet<>();
        Path fullTestRoot = PathUtils.get(ESClientYamlSuiteTestCase.class.getResource(testRoot).toURI());

        if (Files.isDirectory(fullTestRoot)) {
            Files.walk(fullTestRoot).forEach(file -> {
                if (file.toString().endsWith(".yml")) {
                    String parent = file.getParent().getParent().toString();
                    String relativePath = parent.substring(parent.indexOf(testRoot));
                    Optional<Map.Entry<String, SkipReason>> match =
                        SKIP.entrySet().stream().filter(e -> file.toString().contains(e.getKey())).findFirst();
                    if (match.isPresent()) {
                        //TODO: use a logger
                        System.out.println("* * * Skipping " + file + " [" + match.get().getValue().getDisplay() + "]");
                    } else {
                        testRoots.add(relativePath);
                    }
                }
            });
        }

        List<Object[]> tests = new ArrayList<>(100);
        Iterable<Object[]> candidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, testRoots);
        candidates.forEach(objectArray -> {
            for (Object o : objectArray) {
                if (o instanceof ClientYamlTestCandidate) {
                    ClientYamlTestCandidate testCandidate = (ClientYamlTestCandidate) o;
                    //disable checking for warning headers, we know that many of the tests will have deprecation and compatibility warnings.
                    //the deprecation and compatibility warnings should be explicitly tested via the REST tests from this version.
                    testCandidate.getTestSection().getExecutableSections().stream().filter(s -> s instanceof DoSection).forEach(ds -> {
                        DoSection doSection = (DoSection) ds;
                        doSection.checkWarningHeaders(false);
                        //TODO: use real header
                        doSection.getApiCallSection().addHeaders(Collections.singletonMap("compatible-with", "v7"));
                    });
                    //--tests "org.elasticsearch.rest.compat.RestCompatOldModulesYamlTestSuiteIT.test {yaml=painless/71_context_api/Action to list contexts}"
                    if (testCandidate.getApi().equals("painless") && testCandidate.getName().equals("71_context_api")) {
                        testCandidate.getTestSection().getExecutableSections().stream().filter(s -> s instanceof Assertion).forEach(as -> {
                            Assertion assertionSection = (Assertion) as;
                            //TODO: parse this out from the overrides
                            if (assertionSection.getField().equals("contexts.22")) {
                                assertionSection.setExpectedOverrideValue("terms_set");
                            }
                        });
                    }
                    tests.add(objectArray);
                }
            }
        });
        return tests;
    }
}
