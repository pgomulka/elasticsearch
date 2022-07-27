/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugins.scanners;

import org.elasticsearch.core.Tuple;
import org.elasticsearch.sp.api.analysis.Analyzer;
import org.elasticsearch.sp.api.analysis.TokenFilterFactory;
import org.elasticsearch.sp.api.analysis.TokenizerFactory;
import org.elasticsearch.sp.api.analysis.annotations.Factory;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AnnotationScanner extends ClassVisitor {
    private final ClassLoader pluginClassLoader;
//    private final Map<Class<?>, Map<String, Tuple<String, ClassLoader>>> analysisInterfaceToNameComponentClassnameMap;

    private Map<String,String> classNameToAnnotatedName = new HashMap<>();
    String classNameLocal;
    Class<?> analysisInterface;

    public AnnotationScanner(ClassLoader pluginClassLoader) {
        super(Opcodes.ASM9);
        this.pluginClassLoader = pluginClassLoader;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.classNameLocal = name;

//        Set<String> interfacesSet = Set.of(interfaces);
//        for (Class<?> analysisInterface :
//            Set.of(TokenFilterFactory.class, TokenizerFactory.class, Analyzer.class)) {
//            if (interfacesSet.contains(analysisInterface.getCanonicalName().replace('.', '/'))) {
//                this.analysisInterface = analysisInterface; // should we allow for more?
//                break;
//            }
//        }

        super.visit(version, access, name, signature, superName, interfaces);
    }


    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        if (isFactoryAnnotation(descriptor)) {
            return new FactoryAnnotationVisitor();
        }
        return super.visitAnnotation(descriptor, visible);
    }

    @Override
    public void visitEnd() {
        this.classNameLocal = null;
    }

    private boolean isFactoryAnnotation(String descriptor) {
        return descriptor.equals("L" + Factory.class.getCanonicalName().replace('.', '/') + ";");
    }

    class FactoryAnnotationVisitor extends AnnotationVisitor {

        FactoryAnnotationVisitor() {
            super(Opcodes.ASM8);
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            classNameToAnnotatedName.put(classNameLocal, value.toString());
        }
    }

    public Map<String, String> getClassNameToAnnotatedName() {
        return classNameToAnnotatedName;
    }
}
