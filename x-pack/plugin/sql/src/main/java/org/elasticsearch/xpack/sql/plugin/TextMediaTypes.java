/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.sql.plugin;

import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.MediaType;
import org.elasticsearch.rest.RestRequest;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class TextMediaTypes implements MediaType {
    private TextMediaTypes() {
    }
    /**
     * Content type depending on the request.
     * Might be used by some formatters (like CSV) to specify certain metadata like
     * whether the header is returned or not.
     */
    public abstract String responseContentType(RestRequest request);

    protected String formatParameters(RestRequest request) {
        Map<String, String> parameters = request.getParsedAccept() != null ?
            request.getParsedAccept().getParameters() : Collections.emptyMap();

        String joined = parameters.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(";"));
        return joined.isEmpty() ? "" : ";" + joined;
    }

    public static TextMediaTypes VND_PLAIN_TEXT = new TextMediaTypes() {
        @Override
        public String responseContentType(RestRequest request) {
            String parameters = formatParameters(request);
            return "text/vnd.elasticsearch+plain" + parameters;
        }

        @Override
        public String queryParameter() {
            return "vnd_txt";
        }

        @Override
        public Set<HeaderValue> headerValues() {
            return Set.of(
                new HeaderValue("text/vnd.elasticsearch+plain",
                    Map.of("header", "present|absent", COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN)));


        }
    };

    public static TextMediaTypes VND_CSV = new TextMediaTypes() {

        @Override
        public String responseContentType(RestRequest request) {
            String parameters = formatParameters(request);
            return "text/vnd.elasticsearch+csv" + parameters;
        }

        @Override
        public String queryParameter() {
            return "vnd_csv";
        }

        @Override
        public Set<HeaderValue> headerValues() {
            return Set.of(
                new HeaderValue("text/vnd.elasticsearch+csv",
                    Map.of("header", "present|absent", "delimiter", ".+", COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN)));

        }
    };

    public static TextMediaTypes VND_TSV = new TextMediaTypes() {
        @Override
        public String responseContentType(RestRequest request) {
            String parameters = formatParameters(request);
            return "text/vnd.elasticsearch+tab-separated-values" + parameters;
        }

        @Override
        public String queryParameter() {
            return "vnd_tsv";
        }

        @Override
        public Set<HeaderValue> headerValues() {
            return Set.of(new HeaderValue("text/vnd.elasticsearch+tab-separated-values",
                Map.of("header", "present|absent", COMPATIBLE_WITH_PARAMETER_NAME, VERSION_PATTERN)));
        }
    };

    public static TextMediaTypes PLAIN_TEXT = new TextMediaTypes() {
        @Override
        public String responseContentType(RestRequest request) {
            return "text/plain";
        }

        @Override
        public String queryParameter() {
            return "txt";
        }

        @Override
        public Set<HeaderValue> headerValues() {
            return Set.of(
                new HeaderValue("text/plain",
                    Map.of("header", "present|absent")));

        }
    };

    public static TextMediaTypes CSV = new TextMediaTypes() {
        private static final String URL_PARAM_HEADER = "header";
        private static final String PARAM_HEADER_ABSENT = "absent";
        private static final String PARAM_HEADER_PRESENT = "present";

        @Override
        public String responseContentType(RestRequest request) {
            return "text/csv"+"; charset=utf-8; " +
                URL_PARAM_HEADER + "=" + (hasHeader(request) ? PARAM_HEADER_PRESENT : PARAM_HEADER_ABSENT);
        }

        @Override
        public String queryParameter() {
            return "csv";
        }

        @Override
        public Set<HeaderValue> headerValues() {
            return Set.of(
                new HeaderValue("text/csv",
                    Map.of("header", "present|absent", "delimiter", ".+"))// more detailed parsing is in TextFormat.CSV#delimiter
            );
        }

        boolean hasHeader(RestRequest request) {
            String header = request.param(URL_PARAM_HEADER);
            if (header == null) {
                List<String> values = request.getAllHeaderValues("Accept");
                if (values != null) {
                    // header values are separated by `;` so try breaking it down
                    for (String value : values) {
                        String[] params = Strings.tokenizeToStringArray(value, ";");
                        for (String param : params) {
                            if (param.toLowerCase(Locale.ROOT).equals(URL_PARAM_HEADER + "=" + PARAM_HEADER_ABSENT)) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            } else {
                return !header.toLowerCase(Locale.ROOT).equals(PARAM_HEADER_ABSENT);
            }
        }
    };

    public static TextMediaTypes TSV = new TextMediaTypes() {
        @Override
        public String responseContentType(RestRequest request) {
            return "text/tab-separated-values"+ "; charset=utf-8";
        }

        @Override
        public String queryParameter() {
            return "tsv";
        }

        @Override
        public Set<HeaderValue> headerValues() {
            return Set.of(
                new HeaderValue("text/tab-separated-values",
                    Map.of("header", "present|absent")));
        }
    };


}
