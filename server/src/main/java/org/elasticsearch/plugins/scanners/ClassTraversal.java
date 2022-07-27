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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassTraversal {
    private final Map<Class<?>,  Map<String, Tuple<String,ClassLoader>>> analysisInterfaceToNameComponentClassnameMap = new HashMap<>();
    private final Map<String, String> classNameToAnnotatedName;
    private final Map<String, List<String>> classHierarchy;
    private Set<String> ANALYSIS_INTERFACES = Set.of(TokenFilterFactory.class, TokenizerFactory.class, Analyzer.class).stream()
        .map(c -> c.getCanonicalName().replace(".","/")).collect(Collectors.toSet());


    public ClassTraversal(Map<String, String> classNameToAnnotatedName, Map<String, List<String>> classHierarchy) {
        this.classNameToAnnotatedName = classNameToAnnotatedName;
        this.classHierarchy = classHierarchy;
    }

    public Map<Class<?>, Map<String, Tuple<String, ClassLoader>>> getResult(ClassLoader pluginClassLoader) {

        for (String annotatedClassName : classNameToAnnotatedName.keySet()) {
            Class<?> stablePluginInterface = getStablePluginINterface(annotatedClassName);


            analysisInterfaceToNameComponentClassnameMap.computeIfAbsent(stablePluginInterface, (n)->new HashMap<>());
            analysisInterfaceToNameComponentClassnameMap.get(stablePluginInterface)
                .put(classNameToAnnotatedName.get(annotatedClassName), Tuple.tuple(annotatedClassName.replace("/","."), pluginClassLoader));

        }

        return analysisInterfaceToNameComponentClassnameMap;
    }



    private Class<?> getStablePluginINterface(String annotatedClassName) {
        List<String> precedessors = classHierarchy.get(annotatedClassName);
        if (precedessors == null) {
            return  null;
        }
        String stableInterface = getStableInterface(precedessors);
        if(stableInterface != null && stableInterface.equals("java/lang/Object")) {
            return  null;
        }
        if(stableInterface!=null){
            try {
                return Class.forName(stableInterface.replace("/","."));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            for (String precedessor : precedessors) {

                Class<?> stablePluginINterface = getStablePluginINterface(precedessor);
                if(stablePluginINterface != null){
                     return stablePluginINterface;
                }
            }
        }
        return  null;
    }

    private String getStableInterface(List<String> precedessors) {
        for (String precedessor : precedessors) {
            if(ANALYSIS_INTERFACES.contains(precedessor)){
                return precedessor;
            }
        }
        return null;
    }
}
