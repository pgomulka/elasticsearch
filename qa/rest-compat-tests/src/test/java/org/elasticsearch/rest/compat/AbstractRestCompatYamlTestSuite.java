package org.elasticsearch.rest.compat;

import com.carrotsearch.randomizedtesting.annotations.Name;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;
import org.elasticsearch.test.rest.yaml.ESClientYamlSuiteTestCase;
import org.elasticsearch.test.rest.yaml.section.ExecutableSection;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class AbstractRestCompatYamlTestSuite extends ESClientYamlSuiteTestCase {

    static final Set<String> BLACKLISTED = Set.of(
        "/plugins/repository-gcs",
        "/plugins/discovery-gce",
        "/plugins/repository-azure",
        "/plugins/discovery-ec2",
        "/plugins/repository-s3"
    );

    protected AbstractRestCompatYamlTestSuite(@Name("yaml") ClientYamlTestCandidate testCandidate) {
        super(testCandidate);
    }

    protected String getApiSpecRelativePath() {
        String[] splits = System.getProperty("restCompatAPIRoot").split(File.separator);
        return "/" + splits[splits.length - 1];
    }

    static Iterable<Object[]> getParameters(String category) throws Exception {
        String[] splits = System.getProperty("restCompatTestRoot").split(File.separator);
        String testRoot = "/" + splits[splits.length - 1] + "/" + category;
        Set<String> testRoots = new HashSet<>();
        Path fullTestRoot = PathUtils.get(ESClientYamlSuiteTestCase.class.getResource(testRoot).toURI());

        if (Files.isDirectory(fullTestRoot)) {
            Files.walk(fullTestRoot).forEach(file -> {
                if (file.toString().endsWith(".yml")) {

                    String parent = file.getParent().getParent().toString();
                    String relativePath = parent.substring(parent.indexOf(testRoot));

                    if (BLACKLISTED.stream().anyMatch(p -> file.toString().contains(p))) {
                        //TODO: use a logger
                        System.out.println("* * * Skipping " + file + " due to blacklist");
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
                    //TODO: mutate tests here to allow for deprecations
                    tests.add(objectArray);
                }
            }
        });
        return tests;
    }
}
