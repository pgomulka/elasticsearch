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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class MediaTypeDefinition {
    private final String typeWithSubtype;
    private final MediaType mediaType;
    private final String format;
    private final Map<String, Pattern> parameters;

    public MediaTypeDefinition(String typeWithSubtype, MediaType mediaType, String format, Map<String, String> parameters) {

        this.typeWithSubtype = typeWithSubtype;
        this.mediaType = mediaType;
        this.format = format;
        Map<String, Pattern> parametersForMediaType = new HashMap<>(parameters.size());
        for (Map.Entry<String, String> params : parameters.entrySet()) {
            String parameterName = params.getKey().toLowerCase(Locale.ROOT);
            String parameterRegex = params.getValue();
            Pattern pattern = Pattern.compile(parameterRegex, Pattern.CASE_INSENSITIVE);
            parametersForMediaType.put(parameterName, pattern);
        }
        this.parameters = parametersForMediaType;
    }


    public static MediaTypeDefinition of(String typeWithSubtype, MediaType mediaType, String format, Map<String,String> parameters) {
        return new MediaTypeDefinition(typeWithSubtype, mediaType, format, parameters);
    }

    public String getTypeWithSubtype() {
        return typeWithSubtype;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public String getFormat() {
        return format;
    }

    public Map<String, Pattern> getParameters() {
        return parameters;
    }
}
