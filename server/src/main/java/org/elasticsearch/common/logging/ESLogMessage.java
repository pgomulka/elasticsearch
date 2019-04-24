/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.logging;

import org.apache.logging.log4j.message.ParameterizedMessage;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A base class for custom log4j logger messages. Carries additional fields which will populate JSON fields in logs.
 */
public abstract class ESLogMessage extends ParameterizedMessage {
    private final Map<String, String> fields;

    public ESLogMessage(Map<String, String> fields, String messagePattern, Object... args) {
        super(messagePattern, args);
        this.fields = fields;
    }

    public String getValueFor(String key) {
        return fields.get(key);
    }

    public static String inQuotes(String s) {
        if(s == null)
            return inQuotes("");
        return "\"" + s + "\"";
    }

    public static String inQuotes(Object s) {
        if(s == null)
            return inQuotes("");
        return inQuotes(s.toString());
    }

    public static String asJsonArray(Stream<String> stream) {
        return "[" + stream
            .map(ESLogMessage::inQuotes)
            .collect(Collectors.joining(", ")) + "]";
    }
}
