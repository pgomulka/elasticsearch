package org.elasticsearch.common.joda;

import org.elasticsearch.test.ESTestCase;

import static org.hamcrest.Matchers.equalTo;

public class JodaDeprecationPatternsTest extends ESTestCase {

    public void testMigrate() {
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy"), equalTo("uuuu"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'T'yyyy"), equalTo("uuuu'T'uuuu"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'T"), equalTo("uuuu'T"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'T'yyy'T"), equalTo("uuuu'T'uuu'T"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy'y'"), equalTo("uuuu'y'"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyy'y'yy'y"), equalTo("uuu'y'uu'y"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy-MM-dd"), equalTo("uuuu-MM-dd"));
        assertThat(JodaDeprecationPatterns.migratePattern("yyyy-MM-dd'T'HH:mm:ss"), equalTo("uuuu-MM-dd'T'HH:mm:ss"));

        assertThat(JodaDeprecationPatterns.migratePattern("YYYY"), equalTo("yyyy"));
        assertThat(JodaDeprecationPatterns.migratePattern("YYYY-MM-dd"), equalTo("yyyy-MM-dd"));
        assertThat(JodaDeprecationPatterns.migratePattern("xxxx-ww"), equalTo("YYYY-ww"));
    }

    public void testMigrateFailure() {
        expectThrows(IllegalArgumentException.class, () -> JodaDeprecationPatterns.migratePattern("C"));

    }

}
