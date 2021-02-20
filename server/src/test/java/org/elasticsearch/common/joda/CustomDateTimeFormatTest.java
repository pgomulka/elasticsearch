package org.elasticsearch.common.joda;

import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;

public class CustomDateTimeFormatTest extends ESTestCase {

    public void testTextTokens() {
        assertThat(parseToken("-"), equalTo("'-"));
        assertThat(parseToken("-'W'"), equalTo("'-'W'"));
        assertThat(parseToken("-'''"), equalTo("'-''"));
    }

    public void testMultipleTokens() {
        int[] indexRef = new int[1];
        indexRef[0] = 0;
        String pattern = "-'W'-e";
        String token = CustomDateTimeFormat.parseToken(pattern, indexRef);
        assertThat(token, equalTo("'-'W'-"));
        indexRef[0]++;

         token = CustomDateTimeFormat.parseToken(pattern, indexRef);
        assertThat(token, equalTo("e"));
    }

    private String parseToken(String pattern) {
        int[] indexRef = new int[1];
        indexRef[0] = 0;
        return CustomDateTimeFormat.parseToken(pattern, indexRef);
    }
}
