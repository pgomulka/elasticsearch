package org.elasticsearch.common.joda;


import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.core.IsEqual.equalTo;

public class JodaJavaBuilderTest extends ESTestCase {

    public void testJoda() {
//        assertThat(convertPattern("YYYY-MM-dd"), equalTo("yyyy-MM-dd"));
//        assertThat(convertPattern("yyyy-MM-dd"), equalTo("uuuu-MM-dd"));
//        assertThat(convertPattern("YY-MM-dd"), equalTo("uu-MM-dd"));//joda calls year anyway
//        assertThat(convertPattern("yy-MM-dd"), equalTo("uu-MM-dd"));
//        assertThat(convertPattern("yyyy-MM-dd'T'"), equalTo("uuuu-MM-dd'T'"));
//        assertThat(convertPattern("yyyy-MM-dd'T'yy'something'yy-MM-dd"), equalTo("uuuu-MM-dd'T'uu'something'uu-MM-dd"));

    }


    public void testSplit(){
        assertThat(convertPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZ"), equalTo("uuuu-MM-dd'T'hh:mm:ss.SSSXXXX||uuuu-MM-dd'T'hh:mm:ss.SSSX"));
        assertThat(convertPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZ"),
            equalTo("uuuu-MM-dd'T'hh:mm:ss.SSSXXXXX||uuuu-MM-dd'T'hh:mm:ss.SSSXXX||uuuu-MM-dd'T'hh:mm:ss.SSSX"));
        assertThat(convertPattern("yyyy-MM-dd'T'hh:mm:ss.SSSZZZ"), equalTo("uuuu-MM-dd'T'hh:mm:ss.SSSVV"));

        assertThat(convertPattern("yyyy-MM-dd'T'Zyyyy-MM-dd'T'"), equalTo("uuuu-MM-dd'T'XXXXuuuu-MM-dd'T'||uuuu-MM-dd'T'Xuuuu-MM-dd'T'"));

        assertThat(convertPattern("yyyyZMMZ"), equalTo("uuuuXXXXMMXXXX||uuuuXXXXMMX||uuuuXMMXXXX||uuuuXMMX"));


    }

    private String convertPattern(String jodaPattern) {
        JodaJavaBuilder jodaJavaBuilder = new JodaJavaBuilder();

        jodaJavaBuilder.appendPattern(jodaPattern);
        return jodaJavaBuilder.getJavaPattern();
    }

}
