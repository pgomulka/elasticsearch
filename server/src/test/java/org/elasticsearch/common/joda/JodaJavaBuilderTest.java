package org.elasticsearch.common.joda;


import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.core.IsEqual.equalTo;

public class JodaJavaBuilderTest extends ESTestCase {

    public void testJoda() {
        JodaJavaBuilder jodaJavaBuilder = new JodaJavaBuilder();
        jodaJavaBuilder.appendPattern("YYYY-MM-dd");
        String javaPattern = jodaJavaBuilder.getJavaPattern();
        assertThat(javaPattern, equalTo("yyyy-MM-dd"));
    }

}
