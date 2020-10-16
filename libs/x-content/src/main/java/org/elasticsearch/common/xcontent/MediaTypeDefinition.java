/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.xcontent;

import org.elasticsearch.common.Nullable;

import java.util.Map;
import java.util.Objects;

public class MediaTypeDefinition {

    private final String format;
    private final String mediaTypeString;
    private final Map<String, String> mediaTypeParameters;
    private final MediaType mediaType;

    public MediaTypeDefinition(String mediaTypeString, MediaType mediaType, Map<String, String> mediaTypeParameters) {
        this(null, mediaTypeString, mediaType, mediaTypeParameters);
    }

    public MediaTypeDefinition(MediaType mediaType, Map<String, String> mediaTypeParameters) {
        this(mediaType.format(), mediaType.typeWithSubtype(), mediaType, mediaTypeParameters);
    }

    private MediaTypeDefinition(String format, String mediaTypeString, MediaType mediaType, Map<String, String> mediaTypeParameters) {
        this.format = format;
        this.mediaTypeString = Objects.requireNonNull(mediaTypeString);
        this.mediaType = Objects.requireNonNull(mediaType);
        this.mediaTypeParameters = Objects.requireNonNull(mediaTypeParameters);
    }

    @Nullable
    public String format() {
        return format;
    }

    public String getMediaTypeString() {
        return mediaTypeString;
    }

    public Map<String, String> getMediaTypeParameters() {
        return mediaTypeParameters;
    }

    public MediaType getMediaType() {
        return mediaType;
    }
}
