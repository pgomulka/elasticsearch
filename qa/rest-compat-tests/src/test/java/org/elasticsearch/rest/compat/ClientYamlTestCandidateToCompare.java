package org.elasticsearch.rest.compat;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.test.rest.yaml.ClientYamlTestCandidate;

import java.util.Objects;

/**
 * Enough information about the ClientYamlTestCandidate to uniquely identify the test. This is used to identify how a local test can
 * override  a compatibility test.
 */
public class ClientYamlTestCandidateToCompare {
    private final String api;
    private final String name;
    private final String testSectionName;
    private final String source;
    private final String parent;

    public ClientYamlTestCandidateToCompare(ClientYamlTestCandidate candidate) {
        //for this path, api = source.parent.api or source.api
        String api = candidate.getApi();
        String[] parts = api.split("\\.");
        assert parts.length == 2 || parts.length == 3;
        this.source = parts[0];
        if (parts.length == 3) { //source.parent.api
            this.parent = parts[1];
            this.api = parts[2];
        } else { //source.api
            this.parent = null;
            this.api = parts[1];
        }
        this.name = candidate.getName();
        this.testSectionName = candidate.getTestSection().getName();
    }

    public ClientYamlTestCandidateToCompare(ClientYamlTestCandidate candidate, String source, @Nullable String parent) {
        this.api = candidate.getApi();
        this.name = candidate.getName();
        this.testSectionName = candidate.getTestSection().getName();
        this.source = source;
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientYamlTestCandidateToCompare compare = (ClientYamlTestCandidateToCompare) o;
        return Objects.equals(api, compare.api) &&
            Objects.equals(name, compare.name) &&
            Objects.equals(testSectionName, compare.testSectionName) &&
            Objects.equals(source, compare.source) &&
            Objects.equals(parent, compare.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(api, name, testSectionName, source, parent);
    }

    @Override
    public String toString() {
        return "TestCandidate4Compare{" +
            "api='" + api + '\'' +
            ", name='" + name + '\'' +
            ", testSectionName='" + testSectionName + '\'' +
            ", source='" + source + '\'' +
            ", parent='" + parent + '\'' +
            '}';
    }
}
