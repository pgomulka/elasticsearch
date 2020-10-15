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

package org.elasticsearch.plugins;

import org.elasticsearch.Version;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.MediaTypeRegistry;


/**
 * An extension point for Compatible API plugin implementation.
 */
public interface RestCompatibilityPlugin {
    /**
     * Returns a version which was requested on Accept and Content-Type headers
     *
     * @param acceptHeader      - a media-type value from Accept header
     * @param contentTypeHeader - a media-type value from Content-Type header
     * @param hasContent        - a flag indicating if a request has content
     * @return a requested Compatible API Version
     */
    Version getCompatibleVersion(@Nullable String acceptHeader, @Nullable String contentTypeHeader, boolean hasContent, MediaTypeRegistry mediaTypeRegistry);

}
