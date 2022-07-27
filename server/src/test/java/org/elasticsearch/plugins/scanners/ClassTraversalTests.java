/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugins.scanners;

import junit.framework.TestCase;

import org.elasticsearch.core.Tuple;
import org.elasticsearch.sp.api.analysis.TokenFilterFactory;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.Matchers;
import org.objectweb.asm.ClassReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;


public class ClassTraversalTests extends ESTestCase {

    public void testTransitiveImplements() throws IOException {
        InterfaceScanner scanner = performScan("org/elasticsearch/plugins/scanners/TestClassWithHierarchy.class",
            "org/elasticsearch/plugins/scanners/AbstractTestTokenFilter.class"
        );
        AnnotationScanner classVisitor = performAnnotationScan("org/elasticsearch/plugins/scanners/TestClassWithHierarchy.class");

        Map<String, List<String>> classHierarchy = scanner.getClassHierarchy();
        Map<String, String> classNameToAnnotatedName = classVisitor.getClassNameToAnnotatedName();

        ClassTraversal classTraversal = new ClassTraversal( classNameToAnnotatedName, classHierarchy);
        ClassLoader classLoader = this.getClass().getClassLoader();
        Map<Class<?>, Map<String, Tuple<String, ClassLoader>>> result = classTraversal.getResult(classLoader);

        Map<Class<?>, Map<String, Tuple<String, ClassLoader>>> testClassWithHierarchy = Map.of(TokenFilterFactory.class,
            Map.of("TestClassWithHierarchy",  Tuple.tuple("org.elasticsearch.plugins.scanners.TestClassWithHierarchy", classLoader)));
        assertThat(result, Matchers.equalTo(testClassWithHierarchy));


    }
    private AnnotationScanner performAnnotationScan( String className) throws IOException {
        String mainPath = TestClass.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();
        Path path = Path.of(mainPath, className);
        FileInputStream fileInputStream = new FileInputStream(path.toFile());
        ClassReader cr = new ClassReader(fileInputStream);
        AnnotationScanner classVisitor = new AnnotationScanner(getClass().getClassLoader());
        cr.accept(classVisitor, 0);
        return classVisitor;
    }
    private InterfaceScanner performScan( String ... classNames) throws IOException {
        String mainPath = TestClass.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();
        InterfaceScanner classVisitor = new InterfaceScanner(getClass().getClassLoader());


        for (String className : classNames) {
            Path path = Path.of(mainPath, className);
            FileInputStream fileInputStream = new FileInputStream(path.toFile());
            ClassReader cr = new ClassReader(fileInputStream);
            cr.accept(classVisitor, 0);
        }

        return classVisitor;
    }
}
