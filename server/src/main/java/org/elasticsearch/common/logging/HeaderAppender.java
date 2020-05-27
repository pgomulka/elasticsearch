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

package org.elasticsearch.common.logging;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.elasticsearch.common.util.concurrent.ThreadContext;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Plugin(name = "HeaderAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
//@Plugin(name = "ESJsonLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class HeaderAppender extends AbstractAppender {
    /**
     * This is set once by the {@code Node} constructor, but it uses {@link CopyOnWriteArraySet} to ensure that tests can run in parallel.
     * <p>
     * Integration tests will create separate nodes within the same classloader, thus leading to a shared, {@code static} state.
     * In order for all tests to appropriately be handled, this must be able to remember <em>all</em> {@link ThreadContext}s that it is
     * given in a thread safe manner.
     * <p>
     * For actual usage, multiple nodes do not share the same JVM and therefore this will only be set once in practice.
     */
    static final CopyOnWriteArraySet<ThreadContext> THREAD_CONTEXT = new CopyOnWriteArraySet<>();
    /**
     * Set the {@link ThreadContext} used to add warning headers to network responses.
     * <p>
     * This is expected to <em>only</em> be invoked by the {@code Node}'s constructor (therefore once outside of tests).
     *
     * @param threadContext The thread context owned by the {@code ThreadPool} (and implicitly a {@code Node})
     * @throws IllegalStateException if this {@code threadContext} has already been set
     */
    public static void setThreadContext(ThreadContext threadContext) {
        Objects.requireNonNull(threadContext, "Cannot register a null ThreadContext");

        // add returning false means it _did_ have it already
        if (THREAD_CONTEXT.add(threadContext) == false) {
            throw new IllegalStateException("Double-setting ThreadContext not allowed!");
        }
    }

    /**
     * Remove the {@link ThreadContext} used to add warning headers to network responses.
     * <p>
     * This is expected to <em>only</em> be invoked by the {@code Node}'s {@code close} method (therefore once outside of tests).
     *
     * @param threadContext The thread context owned by the {@code ThreadPool} (and implicitly a {@code Node})
     * @throws IllegalStateException if this {@code threadContext} is unknown (and presumably already unset before)
     */
    public static void removeThreadContext(ThreadContext threadContext) {
        assert threadContext != null;

        // remove returning false means it did not have it already
        if (THREAD_CONTEXT.remove(threadContext) == false) {
            throw new IllegalStateException("Removing unknown ThreadContext not allowed!");
        }
    }

    protected HeaderAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, HeaderWarningLayout.createLayout(StandardCharsets.UTF_8));
    }

    @Override
    public void append(LogEvent event) {
        String message = event.getMessage().getFormattedMessage();
        addWarning(THREAD_CONTEXT, message);
    }

    public static void addWarning(Set<ThreadContext> threadContexts, String warningHeaderValue) {
        final Iterator<ThreadContext> iterator = threadContexts.iterator();
        if (iterator.hasNext()) {

//            assert HeaderWarningLayout.WARNING_HEADER_PATTERN.matcher(warningHeaderValue).matches();
//            assert HeaderWarningLayout.extractWarningValueFromWarningHeader(warningHeaderValue, false)
//                .equals(HeaderWarningLayout.escapeAndEncode(formattedMessage));
            while (iterator.hasNext()) {
                try {
                    final ThreadContext next = iterator.next();
                    next.addResponseHeader("Warning", warningHeaderValue);
                } catch (final IllegalStateException e) {
                    // ignored; it should be removed shortly
                }
            }
        }
    }
}
