/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.elasticsearch.common.Strings;

/**
 * Holds context for building Mapper objects from their Builders
 */
public class MapperBuilderContext {

    /**
     * The root context, to be used when building a tree of mappers
     */
    public static final MapperBuilderContext ROOT = new MapperBuilderContext(null, null);

    // TODO remove this
    public static MapperBuilderContext forPath(ContentPath path) {
        return forPath(path,null);
    }
    // TODO remove this
    public static MapperBuilderContext forPath(ContentPath path, String indexName) {
        String p = path.pathAsText("");
        if (p.endsWith(".")) {
            p = p.substring(0, p.length() - 1);
        }
        return new MapperBuilderContext(p, indexName);
    }

    private final String path;
    private final String indexName;

    private MapperBuilderContext(String path, String indexName) {
        this.path = path;
        this.indexName = indexName;
    }

    /**
     * Creates a new MapperBuilderContext that is a child of this context
     * @param name the name of the child context
     * @return a new MapperBuilderContext with this context as its parent
     */
    public MapperBuilderContext createChildContext(String name) {
        return new MapperBuilderContext(buildFullName(name), this.indexName);
    }

    /**
     * Builds the full name of the field, taking into account parent objects
     */
    public final String buildFullName(String name) {
        if (Strings.isEmpty(path)) {
            return name;
        }
        return path + "." + name;
    }

    public String getIndexName() {
        return indexName;
    }

    public MapperBuilderContext withIndexName(String indexName) {
        return new MapperBuilderContext(this.path, indexName);
    }
}
