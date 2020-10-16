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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class MediaTypeRegistry {

    private final Map<String, MediaType> formatToMediaType;
    private final Map<String, MediaType> typeWithSubtypeToMediaType;
    private final Map<String, Map<String, Pattern>> parametersMap;

    public MediaTypeRegistry(List<MediaTypeDefinition> definitions) {
        Map<String, MediaType> formatToMediaType = new HashMap<>();
        Map<String, MediaType> typeWithSubtypeToMediaType = new HashMap<>();
        Map<String, Map<String, Pattern>> parametersMap = new HashMap<>();
        definitions.forEach(definition -> {
            if (definition.format() != null) {
                formatToMediaType.put(definition.format(), definition.getMediaType());
            }
            typeWithSubtypeToMediaType.put(definition.getMediaTypeString(), definition.getMediaType());
            Map<String, String> uncompiledParams = definition.getMediaTypeParameters();
            Map<String, Pattern> parametersForMediaType = new HashMap<>(uncompiledParams.size());
            for (Entry<String, String> params : uncompiledParams.entrySet()) {
                String parameterName = params.getKey().toLowerCase(Locale.ROOT);
                String parameterRegex = params.getValue();
                Pattern pattern = Pattern.compile(parameterRegex, Pattern.CASE_INSENSITIVE);
                parametersForMediaType.put(parameterName, pattern);
            }
            parametersMap.put(definition.getMediaTypeString(), parametersForMediaType);
        });

        this.formatToMediaType = Map.copyOf(formatToMediaType);
        this.typeWithSubtypeToMediaType = Map.copyOf(typeWithSubtypeToMediaType);
        this.parametersMap = Map.copyOf(parametersMap);
    }

    public MediaType formatToMediaType(String format) {
        return formatToMediaType.get(format);
    }

    public MediaType typeWithSubtypeToMediaType(String typeWithSubtype) {
        return typeWithSubtypeToMediaType.get(typeWithSubtype);
    }

    public Map<String, Pattern> parametersFor(String typeWithSubtype) {
        return parametersMap.get(typeWithSubtype);
    }

}
