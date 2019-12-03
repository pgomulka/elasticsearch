package org.elasticsearch.common.joda;

import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;

public class JodaDeprecationPatternsTest extends ESTestCase {

    public void testMigrate_y() {
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy"), equalTo("uuuu"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'T'yyyy"), equalTo("uuuu'T'uuuu"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'T"), equalTo("uuuu'T"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'T'yyy'T"), equalTo("uuuu'T'uuu'T"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'y'"), equalTo("uuuu'y'"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyy'y'yy'y"), equalTo("uuu'y'uu'y"));
    }

    public void testMigrate_Y() {
        assertThat(JodaDeprecationPatterns.migratePattern("YYYY"), equalTo("yyyy"));
        assertThat(JodaDeprecationPatterns.migratePattern("xxxx-ww"), equalTo("YYYY-ww"));
    }


}
