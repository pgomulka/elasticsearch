/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.plugins.scanners;

import org.elasticsearch.sp.api.analysis.Analyzer;
import org.elasticsearch.sp.api.analysis.TokenFilterFactory;
import org.elasticsearch.sp.api.analysis.TokenizerFactory;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InterfaceScanner extends ClassVisitor {

    private Set<Class<?>> ANALYSIS_INTERFACES = Set.of(TokenFilterFactory.class, TokenizerFactory.class, Analyzer.class);

    Map<String, List<String>> classHierarchy = new HashMap<>();
    Set<String> analysisDirectImplFound = new HashSet<>();
    public InterfaceScanner(ClassLoader pluginClassLoader) {
        super(Opcodes.ASM9);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        Set<String> interfacesSet = Set.of(interfaces);

        classHierarchy.computeIfAbsent(name, (n)-> new ArrayList<>());
        classHierarchy.get(name).add(superName);
        classHierarchy.get(name).addAll(interfacesSet);



        super.visit(version, access, name, signature, superName, interfaces);
    }

    public Map<String, List<String>> getClassHierarchy() {
        return classHierarchy;
    }
}
