/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugins;

import org.elasticsearch.core.Tuple;
import org.elasticsearch.sp.api.analysis.TokenFilterFactory;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.Matchers;
import org.objectweb.asm.ClassReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class AnnotationScannerTests extends ESTestCase {

    public void testClass() throws IOException {
        Path path = Path.of(TestClass.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath(), "org/elasticsearch/plugins/TestClass.class");
        FileInputStream fileInputStream = new FileInputStream(path.toFile());
        Map<Class<?>, Map<String, Tuple<String, ClassLoader>>> analysisInterfaceToNameComponentClassnameMap = new HashMap<>();
        ClassReader cr = new ClassReader(fileInputStream);
        AnnotationScanner classVisitor = new AnnotationScanner(getClass().getClassLoader(), analysisInterfaceToNameComponentClassnameMap);
        cr.accept(classVisitor, 0);

        assertThat(analysisInterfaceToNameComponentClassnameMap,
            Matchers.hasEntry(TokenFilterFactory.class, Map.of("fff", Tuple.tuple(getClass().getClassLoader(),"org.elasticsearch.plugins.TestClass"))));
    }

}

