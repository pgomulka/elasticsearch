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

public interface IMediaTypeParser<T extends MediaType> {



    public T fromMediaType(String mediaType);

    public T fromFormat(String format) ;

    /**
     * parsing media type that follows https://tools.ietf.org/html/rfc7231#section-3.1.1.1
     *
     * @param headerValue a header value from Accept or Content-Type
     * @return a parsed media-type
     */
    public ParsedMediaType<T> parseMediaType(String headerValue) ;



    /**
     * A media type object that contains all the information provided on a Content-Type or Accept header
     */
    public class ParsedMediaType<T extends MediaType> {
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


}
