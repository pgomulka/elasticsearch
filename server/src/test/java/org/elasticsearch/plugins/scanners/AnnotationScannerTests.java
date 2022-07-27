/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugins.scanners;

import org.elasticsearch.test.ESTestCase;
import org.hamcrest.Matchers;
import org.objectweb.asm.ClassReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class AnnotationScannerTests extends ESTestCase {

    public void testClass() throws IOException {
        AnnotationScanner classVisitor = performScan("org/elasticsearch/plugins/scanners/TestClass.class");

        assertThat(classVisitor.getClassNameToAnnotatedName(),
            Matchers.hasEntry("org/elasticsearch/plugins/scanners/TestClass", "TestClass"));
    }

    public void testClassWithInterfaceHierarchy() throws IOException {
        AnnotationScanner classVisitor = performScan("org/elasticsearch/plugins/scanners/TestClassWithHierarchy.class");

        assertThat(classVisitor.getClassNameToAnnotatedName(),
            Matchers.hasEntry("org/elasticsearch/plugins/scanners/TestClassWithHierarchy", "TestClassWithHierarchy"));
    }

    private AnnotationScanner performScan( String className) throws IOException {
        String mainPath = TestClass.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();
        Path path = Path.of(mainPath, className);
        FileInputStream fileInputStream = new FileInputStream(path.toFile());
        ClassReader cr = new ClassReader(fileInputStream);
        AnnotationScanner classVisitor = new AnnotationScanner(getClass().getClassLoader());
        cr.accept(classVisitor, 0);
        return classVisitor;
    }
}

