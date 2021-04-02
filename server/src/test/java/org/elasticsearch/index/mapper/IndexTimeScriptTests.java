/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.elasticsearch.common.Strings;
import org.elasticsearch.script.DoubleFieldScript;
import org.elasticsearch.script.LongFieldScript;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptContext;
import org.elasticsearch.search.lookup.SearchLookup;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class IndexTimeScriptTests extends MapperServiceTestCase {

    public void testSimpleFieldReference() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "text").endObject();
            b.startObject("message_length");
            b.field("type", "long");
            b.field("script", "message_length");
            b.endObject();
        }));

        ParsedDocument doc = mapper.parse(source(b -> b.field("message", "this is some text")));
        IndexableField[] lengthFields = doc.rootDoc().getFields("message_length");
        assertEquals(2, lengthFields.length);
        assertEquals("LongPoint <message_length:17>", lengthFields[0].toString());
        assertEquals("docValuesType=SORTED_NUMERIC<message_length:17>", lengthFields[1].toString());
    }

    public void testDocAccess() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("long_field").field("type", "long").endObject();
            b.startObject("long_field_plus_two");
            b.field("type", "long");
            b.field("script", "long_field_plus_two");
            b.endObject();
        }));

        ParsedDocument doc = mapper.parse(source(b -> b.field("long_field", 4)));
        assertEquals(doc.rootDoc().getField("long_field_plus_two").numericValue(), 6L);
    }

    public void testDoublesAccess() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("double_field").field("type", "double").endObject();
            b.startObject("double_field_plus_two");
            b.field("type", "double");
            b.field("script", "double_field_plus_two");
            b.endObject();
        }));

        ParsedDocument doc = mapper.parse(source(b -> b.field("double_field", 4.5)));
        assertEquals(doc.rootDoc().getField("double_field_plus_two").numericValue(), 6.5);
    }

    public void testSerialization() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "text").endObject();
            b.startObject("message_length");
            b.field("type", "long");
            b.field("script", "message_length");
            b.endObject();
        }));
        assertEquals(
            "{\"_doc\":{\"properties\":{\"message\":{\"type\":\"text\"}," +
                "\"message_length\":{\"type\":\"long\",\"script\":{\"source\":\"message_length\",\"lang\":\"painless\"}}}}}",
            Strings.toString(mapper.mapping()));
    }

    public void testCrossReferences() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "text").endObject();
            b.startObject("message_length_plus_two");
            b.field("type", "long");
            b.field("script", "message_length_plus_two");
            b.endObject();
            b.startObject("message_length");
            b.field("type", "long");
            b.field("script", "message_length");
            b.endObject();
            b.startObject("message_length_plus_four");
            b.field("type", "double");
            b.field("script", "message_length_plus_two_plus_two");
            b.endObject();
        }));
        ParsedDocument doc = mapper.parse(source(b -> b.field("message", "this is a message")));
        assertEquals(doc.rootDoc().getField("message_length_plus_two").numericValue(), 19L);
        assertEquals(doc.rootDoc().getField("message_length").numericValue(), 17L);
        assertEquals(doc.rootDoc().getField("message_length_plus_four").numericValue(), 21d);
    }

    public void testCannotIndexDirectlyIntoScriptMapper() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "text").endObject();
            b.startObject("message_length");
            b.field("type", "long");
            b.field("script", "length");
            b.endObject();
        }));

        Exception e = expectThrows(MapperParsingException.class, () -> mapper.parse(source(b -> {
            b.field("message", "foo");
            b.field("message_length", 3);
        })));
        assertEquals("failed to parse field [message_length] of type [long] in document with id '1'. Preview of field's value: '3'",
            e.getMessage());
        Throwable original = e.getCause();
        assertEquals("Cannot index data directly into a field with a [script] parameter", original.getMessage());
    }

    public void testLoopDetection() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("field1").field("type", "long").field("script", "field2_plus_two").endObject();
            b.startObject("field2").field("type", "long").field("script", "field1_plus_two").endObject();
        }));

        Exception e = expectThrows(MapperParsingException.class, () -> mapper.parse(source(b -> {})));
        assertEquals("Error executing script on field [field1]", e.getMessage());

        Throwable root = e.getCause();
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertThat(root.getMessage(), containsString("Loop in field resolution detected"));
        // Can be either field1->field2->field1 or field2->field1->field2 because
        // post-phase executor order is not deterministic
        assertThat(root.getMessage(), containsString("field1->field2"));
    }

    public void testStoredScriptsNotPermitted() {
        Exception e = expectThrows(MapperParsingException.class, () -> createDocumentMapper(fieldMapping(b -> {
            b.field("type", "long");
            b.startObject("script").field("id", "foo").endObject();
        })));
        assertThat(e.getMessage(), equalTo("Failed to parse mapping [_doc]: stored scripts are not supported on field [field]"));
    }

    public void testCannotReferToRuntimeFields() throws IOException {
        DocumentMapper mapper = createDocumentMapper(topMapping(b -> {
            b.startObject("runtime");
            b.startObject("runtime-field").field("type", "long").endObject();
            b.endObject();
            b.startObject("properties");
            b.startObject("index-field").field("type", "long").field("script", "refer-to-runtime").endObject();
            b.endObject();
        }));

        Exception e = expectThrows(MapperParsingException.class, () -> mapper.parse(source(b -> {})));
        assertEquals("Error executing script on field [index-field]", e.getMessage());
        assertEquals("No field found for [runtime-field] in mapping", e.getCause().getMessage());
    }

    public void testScriptErrorParameterRequiresScript() {
        Exception e = expectThrows(MapperParsingException.class, () -> createDocumentMapper(fieldMapping(b -> {
            b.field("type", "long");
            b.field("on_script_error", "ignore");
        })));
        assertThat(e.getMessage(),
            equalTo("Failed to parse mapping [_doc]: Field [on_script_error] requires field [script] to be configured"));
    }

    public void testIgnoreScriptErrors() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "keyword").endObject();
            b.startObject("message_length");
            {
                b.field("type", "long");
                b.field("script", "message_length");
            }
            b.endObject();
            b.startObject("message_error");
            {
                b.field("type", "long");
                b.field("script", "throws");
                b.field("on_script_error", "ignore");
            }
            b.endObject();
        }));

        ParsedDocument doc = mapper.parse(source(b -> b.field("message", "this is some text")));
        assertThat(doc.rootDoc().getFields("message_length"), arrayWithSize(2));
        assertThat(doc.rootDoc().getFields("message_error"), arrayWithSize(0));
        assertThat(doc.rootDoc().getField("_ignored").stringValue(), equalTo("message_error"));
    }

    public void testRejectScriptErrors() throws IOException {
        DocumentMapper mapper = createDocumentMapper(mapping(b -> {
            b.startObject("message").field("type", "keyword").endObject();
            b.startObject("message_length");
            {
                b.field("type", "long");
                b.field("script", "message_length");
            }
            b.endObject();
            b.startObject("message_error");
            {
                b.field("type", "long");
                b.field("script", "throws");
            }
            b.endObject();
        }));

        Exception e = expectThrows(MapperParsingException.class, () -> mapper.parse(source(b -> b.field("message", "foo"))));
        assertThat(e.getMessage(), equalTo("Error executing script on field [message_error]"));
        assertThat(e.getCause().getMessage(), equalTo("Oops!"));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T compileScript(Script script, ScriptContext<T> context) {
        if (context.factoryClazz == LongFieldScript.Factory.class) {
            return (T) (LongFieldScript.Factory) (n, p, l) -> ctx -> new TestLongFieldScript(
                n, p, l, ctx, getLongScript(script.getIdOrCode())
            );
        }
        if (context.factoryClazz == DoubleFieldScript.Factory.class) {
            return (T) (DoubleFieldScript.Factory) (n, p, l) -> ctx -> new TestDoubleFieldScript(
                n, p, l, ctx, getDoubleScript(script.getIdOrCode())
            );
        }
        throw new IllegalArgumentException("Unknown factory type " + context.factoryClazz + " for code " + script.getIdOrCode());
    }

    private static Consumer<TestLongFieldScript> getLongScript(String name) {
        if ("refer-to-runtime".equals(name)) {
            return s -> {
                s.emitValue((long) s.getDoc().get("runtime-field").get(0));
            };
        }
        if ("throws".equals(name)) {
            return s -> { throw new RuntimeException("Oops!"); };
        }
        if (name.endsWith("_length")) {
            String field = name.substring(0, name.lastIndexOf("_length"));
            return s -> {
                for (Object v : s.extractValuesFromSource(field)) {
                    s.emitValue(Objects.toString(v).length());
                }
            };
        }
        if (name.endsWith("_plus_two")) {
            String field = name.substring(0, name.lastIndexOf("_plus_two"));
            return s -> {
                long input = (long) s.getDoc().get(field).get(0);
                s.emitValue(input + 2);
            };
        }
        throw new UnsupportedOperationException("Unknown script [" + name + "]");
    }

    private static Consumer<TestDoubleFieldScript> getDoubleScript(String name) {
        if (name.endsWith("_plus_two")) {
            String field = name.substring(0, name.lastIndexOf("_plus_two"));
            return s -> {
                Number input = (Number) s.getDoc().get(field).get(0);
                s.emitValue(input.doubleValue() + 2);
            };
        }
        throw new UnsupportedOperationException("Unknown script [" + name + "]");
    }

    private static class TestLongFieldScript extends LongFieldScript {

        final Consumer<TestLongFieldScript> executor;

        TestLongFieldScript(
            String fieldName,
            Map<String, Object> params,
            SearchLookup searchLookup,
            LeafReaderContext ctx,
            Consumer<TestLongFieldScript> executor
        ) {
            super(fieldName, params, searchLookup, ctx);
            this.executor = executor;
        }

        @Override
        public void execute() {
            executor.accept(this);
        }

        public void emitValue(long v) {
            super.emit(v);
        }

        public List<Object> extractValuesFromSource(String path) {
            return super.extractFromSource(path);
        }
    }

    private static class TestDoubleFieldScript extends DoubleFieldScript {

        final Consumer<TestDoubleFieldScript> executor;

        TestDoubleFieldScript(
            String fieldName,
            Map<String, Object> params,
            SearchLookup searchLookup,
            LeafReaderContext ctx,
            Consumer<TestDoubleFieldScript> executor
        ) {
            super(fieldName, params, searchLookup, ctx);
            this.executor = executor;
        }

        @Override
        public void execute() {
            executor.accept(this);
        }

        public void emitValue(double v) {
            super.emit(v);
        }

        public List<Object> extractValuesFromSource(String path) {
            return super.extractFromSource(path);
        }
    }

}
