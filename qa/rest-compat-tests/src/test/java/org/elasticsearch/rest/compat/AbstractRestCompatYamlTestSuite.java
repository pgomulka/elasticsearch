package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.test.rest.yaml.section.DoSection;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

class AbstractRestCompatYamlTestSuite extends ESClientYamlSuiteTestCase {

    static boolean foo = true;
    //TODO: maybe ? support new command line flag to enforce tests from a specific module, plugin, or core, add this to the reproduce line
    //-Dtests.rest.suite.compat.parent=[plugin-name | module-name]

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
     * Finds the tests from the prior version. Gradle checked out the prior version and copied the REST tests to "restCompatTestRoot" dir.
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
     * </ul>
     * TODO: something about overrides
     */
    static Iterable<Object[]> getPriorVersionTests(String source) throws Exception {
        assert "plugins".equals(source) || "modules".equals(source) || "rest-api-spec".equals(source);
        String[] splits = System.getProperty("restCompatTestRoot").split(File.separator);
        String classPathRoot = "/" + splits[splits.length - 1] + "/" + source;
        Set<String> testsClassPaths = new HashSet<>();
        Path absoluteTestsPath = PathUtils.get(ESClientYamlSuiteTestCase.class.getResource(classPathRoot).toURI());
        if (Files.isDirectory(absoluteTestsPath)) {
            Files.walk(absoluteTestsPath).forEach(file -> {
                if (file.toString().endsWith(".yml")) {
                    String testClassPath = Path.of(classPathRoot).resolve(absoluteTestsPath.relativize(file.getParent().getParent())).toString().replace("\\", "/");
                    Optional<Map.Entry<String, SkipReason>> match =
                        SKIP.entrySet().stream().filter(e -> file.toString().contains(e.getKey())).findFirst();
                    if (match.isPresent()) {
                        //TODO: use a logger
                        System.out.println("* * * Skipping " + file + " [" + match.get().getValue().getDisplay() + "]");
                    } else {
                        testsClassPaths.add(testClassPath);
                    }
                }
            });
        }
        Set<ClientYamlTestCandidateToCompare> testOverrides = getTestsOverrides();
        List<Object[]> tests = new ArrayList<>(100);
        for (String testsClassPath : testsClassPaths) {
            final String parent = ("modules".equals(source) || "plugins".equals(source)) ? testsClassPath.split("/")[3] : null;
            Iterable<Object[]> candidates = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, testsClassPath);
            //TODO: use StreamSupport
            candidates.forEach(objectArray -> {
                for (Object o : objectArray) {
                    List<ClientYamlTestCandidate> testCandidates = new ArrayList<>(objectArray.length);
                    if (o instanceof ClientYamlTestCandidate) {
                        ClientYamlTestCandidate testCandidate = (ClientYamlTestCandidate) o;
                        ClientYamlTestCandidateToCompare currentCandidate4Compare = new ClientYamlTestCandidateToCompare(testCandidate, source, parent);
                        if (testOverrides.contains(currentCandidate4Compare) == false) {
                            testCandidates.add(testCandidate);
                            //disable checking for warning headers, we know that many of the tests will have deprecation and compatibility warnings.
                            //the deprecation and compatibility warnings should be explicitly tested via the REST tests from this version.
                            testCandidate.getTestSection().getExecutableSections().stream().filter(s -> s instanceof DoSection).forEach(ds -> {
                                DoSection doSection = (DoSection) ds;
                                doSection.checkWarningHeaders(false);
                                //TODO: use real header
                                doSection.getApiCallSection().addHeaders(Collections.singletonMap("compatible-with", "v7"));
                            });
                        } else {
                            //TODO: use a logger
                            System.out.println("* * * Skipping test [" + currentCandidate4Compare + "]");
                        }
                        if (testCandidates.isEmpty() == false) {
                            tests.add(testCandidates.toArray());
                        }
                    }
                }
            });
        }
        //TODO: what happens when a single test is requested via the modules, but the module is overriden ? (maybe keep all of the skipped tests locally and output them with a helpful hint here if empty
        return tests;
    }

    private static Set<ClientYamlTestCandidateToCompare> getTestsOverrides() throws Exception {
        Iterable<Object[]> candidates = ESClientYamlSuiteTestCase.createParameters();
        Set<ClientYamlTestCandidateToCompare> testOverrides = new HashSet<>(100);
        StreamSupport.stream(candidates.spliterator(), false)
            .flatMap(Arrays::stream).forEach(o -> testOverrides.add(new ClientYamlTestCandidateToCompare((ClientYamlTestCandidate) o)));
        return testOverrides;
    }
}
