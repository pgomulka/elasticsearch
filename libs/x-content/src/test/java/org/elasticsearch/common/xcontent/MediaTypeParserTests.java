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

import org.elasticsearch.test.ESTestCase;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class MediaTypeParserTests extends ESTestCase {
    MediaTypeRegistry mediaTypeRegistry = new MediaTypeRegistry(XContentType.MEDIA_TYPE_DEFINITIONS);
    MediaTypeParser<XContentType> mediaTypeParser = new MediaTypeParser<>(mediaTypeRegistry);

    public void testJsonWithParameters() throws Exception {
        String mediaType = "application/vnd.elasticsearch+json";
        assertThat(mediaTypeParser.parseMediaType(mediaType).getParameters(),
            equalTo(Collections.emptyMap()));
        assertThat(mediaTypeParser.parseMediaType(mediaType + ";").getParameters(),
            equalTo(Collections.emptyMap()));
        assertThat(mediaTypeParser.parseMediaType(mediaType + "; charset=UTF-8").getParameters(),
            equalTo(Map.of("charset", "utf-8")));
        assertThat(mediaTypeParser.parseMediaType(mediaType + "; compatible-with=123;charset=UTF-8").getParameters(),
            equalTo(Map.of("charset", "utf-8", "compatible-with", "123")));
    }

    public void testWhiteSpaceInTypeSubtype() {
        String mediaType = " application/vnd.elasticsearch+json ";
        assertThat(mediaTypeParser.parseMediaType(mediaType).getMediaType(),
            equalTo(XContentType.JSON));

        assertThat(mediaTypeParser.parseMediaType(mediaType + "; compatible-with=123; charset=UTF-8").getParameters(),
            equalTo(Map.of("charset", "utf-8", "compatible-with", "123")));
        assertThat(mediaTypeParser.parseMediaType(mediaType + "; compatible-with=123;\n charset=UTF-8").getParameters(),
            equalTo(Map.of("charset", "utf-8", "compatible-with", "123")));

        mediaType = " application / json ";
        assertThat(mediaTypeParser.parseMediaType(mediaType),
            is(nullValue()));
    }

    public void testInvalidParameters() {
        String mediaType = "application/vnd.elasticsearch+json";
        assertThat(mediaTypeParser.parseMediaType(mediaType + "; charset=unknown") ,
            is(nullValue()));
        assertThat(mediaTypeParser.parseMediaType(mediaType + "; keyvalueNoEqualsSign"),
            is(nullValue()));
        assertThat(mediaTypeParser.parseMediaType(mediaType + "; key = value"),
            is(nullValue()));
        assertThat(mediaTypeParser.parseMediaType(mediaType + "; key=") ,
            is(nullValue()));
    }

    public void testVersionParsing() {
        byte version = (byte) Math.abs(randomByte());
        assertThat(mediaTypeParser.parseVersion("application/vnd.elasticsearch+json;compatible-with=" + version),
            equalTo(version));
        assertThat(mediaTypeParser.parseVersion("application/json"),
            nullValue());


        assertThat(mediaTypeParser.parseVersion("APPLICATION/VND.ELASTICSEARCH+JSON;COMPATIBLE-WITH=" + version),
            equalTo(version));
        assertThat(mediaTypeParser.parseVersion("APPLICATION/JSON"),
            nullValue());

        assertThat(mediaTypeParser.parseVersion("application/json;compatible-with=" + version + ".0"),
            is(nullValue()));
    }

    public void testUnrecognizedParameter() {
        assertThat(mediaTypeParser.parseVersion("application/json; sth=123"),
            is(nullValue()));    }

}
