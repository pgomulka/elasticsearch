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
// ./gradlew ':qa:version-api:integTestRunner' -Dtests.rest.suite='get/10_basic,index/10_with_id' -Dtests.timestamp=$(date +%S) --info
// ./gradlew ':qa:version-api:integTestRunner' -Dtests.rest.suite='mapper_size' -Dtests.timestamp=$(date +%S) --info
// ./gradlew ':qa:version-api:integTestRunner' --tests "org.elasticsearch.version.api.VersionApiClientYamlTestSuiteIT.test {yaml=/10_basic/Test percolator basics via rest}" -Dtests.timestamp=$(date +%S) --info
public class VersionApiClientYamlTestSuiteIT extends ESClientYamlSuiteTestCase {


    //TODO: remove this ... we shouldn't need this..just here while building this out.
    static final Set<String> BLACKLISTED = Set.of(
        "repository-s3" //TODO: figure out why this breaks running a single tests .. i assume some assert or exception is thrown from here.
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

        List<Object[]> tests = new ArrayList<>();
        String testRoot = System.getProperty("versionApiTestRoot");

        add(Paths.get(testRoot, "rest-api-spec", "src", "main", "resources", "rest-api-spec", "test"), tests);
        addWithChildren(Paths.get(testRoot, "modules"), tests);
        addWithChildren(Paths.get(testRoot, "plugins"), tests);

        //TODO: support REST tests defined in THIS project too !
        if (tests.isEmpty()) {
            fail("No test found to run !");
        }
        return tests;
    }

    private static void addWithChildren(Path parent, List<Object[]> tests) throws Exception {
        for (File f : Objects.requireNonNull(parent.toFile().listFiles())) {
            Path root = parent.resolve(Paths.get(f.getName(), "src", "test", "resources", "rest-api-spec", "test"));
            add(root, tests);
        }
    }

    private static void add(Path root, List<Object[]> tests) throws Exception {
        if (BLACKLISTED.stream().anyMatch(p -> root.toString().contains(p))) {
            System.out.println("%%%%%%%%%%%%%%%%%% Skipping due to blacklist " + root.toString());
            return;
        }
        System.out.println("************* Finding tests from: " + root);
        Iterable<Object[]> foundTests = ESClientYamlSuiteTestCase.createParameters(ExecutableSection.XCONTENT_REGISTRY, root, false);
        foundTests.forEach(objectArray -> {
            for (Object o : objectArray) {
                System.out.println("** -->" + o);
            }
        });
        foundTests.forEach(tests::add);
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
        getAllDoSections().forEach(d -> d.getApiCallSection().addHeaders(Collections.singletonMap("compatible-with", "v7")));
    }

    private void handleTypeWarnings() {
        if (EXPECT_TYPE_WARNINGS.contains(getTestCandidate().getTestPath())) {
            List<DoSection> doSections = getDoSectionsByParam("type");
            doSections.forEach(d -> d.addExpectedWarningHeader("foobarbear"));
        }
    }
}

