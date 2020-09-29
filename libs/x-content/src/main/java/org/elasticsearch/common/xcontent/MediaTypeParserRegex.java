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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaTypeParserRegex<T extends MediaType> {
    private final Map<String, T> formatToMediaType;
    private final Map<String, T> typeWithSubtypeToMediaType;
    private final Map<String, Map<String, Pattern>> parametersMap;

    public MediaTypeParserRegex(Map<String, T> formatToMediaType, Map<String, T> typeWithSubtypeToMediaType,
                                Map<String, Map<String, Pattern>> parametersMap) {
        this.formatToMediaType = Map.copyOf(formatToMediaType);
        this.typeWithSubtypeToMediaType = Map.copyOf(typeWithSubtypeToMediaType);
        this.parametersMap = Map.copyOf(parametersMap);
    }

    public T fromMediaType(String mediaType) {
        ParsedMediaType parsedMediaType = parseMediaType(mediaType);
        return parsedMediaType != null ? parsedMediaType.getMediaType() : null;
    }

    public T fromFormat(String format) {
        if (format == null) {
            return null;
        }
        return formatToMediaType.get(format.toLowerCase(Locale.ROOT));
    }

    static String token = "[a-zA-z0-9!#$%&'*+\\-\\.\\^_`|~]+";
    static String quotedString = "\"((?:\t| |!|[\\x23-\\x5B]|[\\x5D-\\x7E]|[\\x80-\\xFF]|\\\\|\\\\\")*)\"";
    static String parameter = token + "=" + "(" + token + "|" + quotedString + ")";
    private static final Pattern COMPATIBLE_API_HEADER_PATTERN = Pattern.compile(
        "(" + token + "/" + token + ")((\\s*;\\s*" + parameter + ")*$|$)", Pattern.CASE_INSENSITIVE);

    /**
     * parsing media type that follows https://tools.ietf.org/html/rfc7231#section-3.1.1.1
     *
     * @param headerValue a header value from Accept or Content-Type
     * @return a parsed media-type
     * //todo pg write a benchmark and consider using a regex
     */
    public ParsedMediaType parseMediaType(String headerValue) {
        if (headerValue != null) {
            Matcher matcher = COMPATIBLE_API_HEADER_PATTERN.matcher(headerValue.toLowerCase(Locale.ROOT).trim());
            if (matcher.find()) {
                String typeWithSubtype = matcher.group(1);
                T xContentType = typeWithSubtypeToMediaType.get(typeWithSubtype);

                if (xContentType != null) {

                    String params = matcher.group(2);
                    Map<String, String> parameters = new HashMap<>();
                    if (params != null && params.length() > 0) {

                        String[] split = params.trim().split(";");

                        for (String s : split) {

                            String paramPair = s.trim();
                            if (paramPair.length() > 0) {

                                String[] keyValue = paramPair.split("=");
                                String key = keyValue[0];
                                String value = keyValue[1];
                                if (isValidParameter(typeWithSubtype, key, value) == false) {
                                    return null;
                                }
                                parameters.put(key, value);

                            }
                        }
                    }
                    return new ParsedMediaType(xContentType, parameters);

                }
            }

        }
        return null;
    }

    /*
                String[] split = headerValue.toLowerCase(Locale.ROOT).split(";");

            String[] typeSubtype = split[0].trim().toLowerCase(Locale.ROOT)
                .split("/");
            if (typeSubtype.length == 2) {

                String type = typeSubtype[0];
                String subtype = typeSubtype[1];
                String typeWithSubtype = type + "/" + subtype;
                T xContentType = typeWithSubtypeToMediaType.get(typeWithSubtype);
                if (xContentType != null) {
                    Map<String, String> parameters = new HashMap<>();
                    for (int i = 1; i < split.length; i++) {
                        //spaces are allowed between parameters, but not between '=' sign
                        String[] keyValueParam = split[i].trim().split("=");
                        if (keyValueParam.length != 2 || hasSpaces(keyValueParam[0]) || hasSpaces(keyValueParam[1])) {
                            return null;
                        }
                        String parameterName = keyValueParam[0].toLowerCase(Locale.ROOT);
                        String parameterValue = keyValueParam[1].toLowerCase(Locale.ROOT);
                        if (isValidParameter(typeWithSubtype, parameterName, parameterValue) == false) {
                            return null;
                        }
                        parameters.put(parameterName, parameterValue);
                    }
                    return new ParsedMediaType(xContentType, parameters);
                }

     */

    private boolean isValidParameter(String typeWithSubtype, String parameterName, String parameterValue) {
        if (parametersMap.containsKey(typeWithSubtype)) {
            Map<String, Pattern> parameters = parametersMap.get(typeWithSubtype);
            if (parameters.containsKey(parameterName)) {
                Pattern regex = parameters.get(parameterName);
                return regex.matcher(parameterValue).matches();
            }
        }
        return false;
    }

    private boolean hasSpaces(String s) {
        return s.trim().equals(s) == false;
    }

    /**
     * A media type object that contains all the information provided on a Content-Type or Accept header
     */
    public class ParsedMediaType {
        private final Map<String, String> parameters;
        private final T mediaType;

        public ParsedMediaType(T mediaType, Map<String, String> parameters) {
            this.parameters = parameters;
            this.mediaType = mediaType;
        }

        public T getMediaType() {
            return mediaType;
        }

        public Map<String, String> getParameters() {
            return parameters;
        }
    }

    public static class Builder<T extends MediaType> {
        private final Map<String, T> formatToMediaType = new HashMap<>();
        private final Map<String, T> typeWithSubtypeToMediaType = new HashMap<>();
        private final Map<String, Map<String, Pattern>> parametersMap = new HashMap<>();

        public Builder<T> withMediaTypeAndParams(String alternativeMediaType, T mediaType, Map<String, Pattern> paramNameAndValueRegex) {
            typeWithSubtypeToMediaType.put(alternativeMediaType.toLowerCase(Locale.ROOT), mediaType);
            formatToMediaType.put(mediaType.format(), mediaType);

            Map<String, Pattern> parametersForMediaType = new HashMap<>(paramNameAndValueRegex.size());
            for (Map.Entry<String, Pattern> params : paramNameAndValueRegex.entrySet()) {
                String parameterName = params.getKey().toLowerCase(Locale.ROOT);
                Pattern parameterRegex = params.getValue();
                Pattern pattern = Pattern.compile(parameterRegex.pattern(), Pattern.CASE_INSENSITIVE);
                parametersForMediaType.put(parameterName, pattern);
            }
            parametersMap.put(alternativeMediaType, parametersForMediaType);

            return this;
        }

        public Builder<T> copyFromMediaTypeParser(MediaTypeParserRegex<? extends T> mediaTypeParser) {
            formatToMediaType.putAll(mediaTypeParser.formatToMediaType);
            typeWithSubtypeToMediaType.putAll(mediaTypeParser.typeWithSubtypeToMediaType);
            parametersMap.putAll(mediaTypeParser.parametersMap);
            return this;
        }

        public MediaTypeParserRegex<T> build() {
            return new MediaTypeParserRegex<>(formatToMediaType, typeWithSubtypeToMediaType, parametersMap);
        }
    }
}
