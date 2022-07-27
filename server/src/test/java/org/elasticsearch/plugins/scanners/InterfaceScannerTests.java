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
import java.util.List;
import java.util.Map;

public class InterfaceScannerTests extends ESTestCase {


    public void testDirectImplements() throws IOException {
        InterfaceScanner scanner = performScan("org/elasticsearch/plugins/scanners/TestClass.class"            );
        Map<String, List<String>> classHierarchy = scanner.getClassHierarchy();

        assertThat(classHierarchy, Matchers.allOf(
            Matchers.hasEntry("org/elasticsearch/plugins/scanners/TestClass",
                List.of("java/lang/Object", "org/elasticsearch/sp/api/analysis/TokenFilterFactory"))
        ));
    }

    public void testTransitiveImplements() throws IOException {
        InterfaceScanner scanner = performScan("org/elasticsearch/plugins/scanners/TestClassWithTransitiveImplements.class",
            "org/elasticsearch/plugins/scanners/TransitiveInterface.class"
        );
        Map<String, List<String>> classHierarchy = scanner.getClassHierarchy();

        assertThat(classHierarchy, Matchers.allOf(
            Matchers.hasEntry("org/elasticsearch/plugins/scanners/TestClassWithTransitiveImplements",
                List.of("java/lang/Object", "org/elasticsearch/plugins/scanners/TransitiveInterface") ),
            Matchers.hasEntry("org/elasticsearch/plugins/scanners/TransitiveInterface",
                List.of( "java/lang/Object","org/elasticsearch/sp/api/analysis/TokenFilterFactory"))
        ));
    }

    public void testTransitiveImplementsWithMultipleInterfaces(){

    }


    public void testAbstractClassImplements() throws IOException{
        InterfaceScanner scanner = performScan("org/elasticsearch/plugins/scanners/TestClassWithHierarchy.class",
            "org/elasticsearch/plugins/scanners/AbstractTestTokenFilter.class"
        );
        Map<String, List<String>> classHierarchy = scanner.getClassHierarchy();

        assertThat(classHierarchy, Matchers.allOf(
            Matchers.hasEntry("org/elasticsearch/plugins/scanners/TestClassWithHierarchy",
                List.of("java/lang/Object", "org/elasticsearch/plugins/scanners/AbstractTestTokenFilter") ),
            Matchers.hasEntry("org/elasticsearch/plugins/scanners/AbstractTestTokenFilter",
                List.of("java/lang/Object", "org/elasticsearch/sp/api/analysis/TokenFilterFactory"))
        ));
    }

    public void testAbstractClassTransitiveImplements(){

    }

    public void testAbstractClassWithMultipleInterfaces(){

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
