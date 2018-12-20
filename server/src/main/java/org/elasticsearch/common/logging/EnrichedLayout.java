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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;

import java.nio.charset.Charset;

@Plugin(name = "SampleLayout", category = "Core", elementType = "layout", printObject = true)
public class EnrichedLayout extends AbstractStringLayout {

    protected EnrichedLayout(boolean locationInfo, boolean properties, boolean complete,
                           Charset charset) {
        super(charset);
    }

    @PluginFactory
    public static EnrichedLayout createLayout(@PluginAttribute("locationInfo") boolean locationInfo,
                                            @PluginAttribute("properties") boolean properties,
                                            @PluginAttribute("complete") boolean complete,
                                            @PluginAttribute(value = "charset", defaultString = "UTF-8") Charset charset) {
        return new EnrichedLayout(locationInfo, properties, complete, charset);
    }

    @Override
    public String toSerializable(LogEvent event) {
        return null;
    }
}
