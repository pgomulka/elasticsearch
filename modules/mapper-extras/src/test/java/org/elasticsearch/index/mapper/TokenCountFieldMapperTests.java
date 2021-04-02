/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CannedTokenStream;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AnalyzerScope;
import org.elasticsearch.index.analysis.IndexAnalyzers;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.plugins.Plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

/**
 * Test for {@link TokenCountFieldMapper}.
 */
public class TokenCountFieldMapperTests extends MapperTestCase {

    @Override
    protected Collection<Plugin> getPlugins() {
        return Collections.singletonList(new MapperExtrasPlugin());
    }

    @Override
    protected void minimalMapping(XContentBuilder b) throws IOException {
        b.field("type", "token_count").field("analyzer", "keyword");
    }

    @Override
    protected Object getSampleValueForDocument() {
        return "some words";
    }

    @Override
    protected Object getSampleValueForQuery() {
        return 1;
    }

    @Override
    protected void registerParameters(ParameterChecker checker) throws IOException {
        checker.registerConflictCheck("index", b -> b.field("index", false));
        checker.registerConflictCheck("store", b -> b.field("store", true));
        checker.registerConflictCheck("doc_values", b -> b.field("doc_values", false));
        checker.registerConflictCheck("null_value", b -> b.field("null_value", 1));
        checker.registerConflictCheck("enable_position_increments", b -> b.field("enable_position_increments", false));
        checker.registerUpdateCheck(
            this::minimalMapping,
            b -> b.field("type", "token_count").field("analyzer", "standard"),
            m -> {
                TokenCountFieldMapper tcfm = (TokenCountFieldMapper) m;
                assertThat(tcfm.analyzer(), equalTo("standard"));
            });
    }

    @Override
    protected IndexAnalyzers createIndexAnalyzers(IndexSettings indexSettings) {
        NamedAnalyzer dflt = new NamedAnalyzer(
            "default",
            AnalyzerScope.INDEX,
            new StandardAnalyzer()
        );
        NamedAnalyzer standard = new NamedAnalyzer("standard", AnalyzerScope.INDEX, new StandardAnalyzer());
        NamedAnalyzer keyword = new NamedAnalyzer("keyword", AnalyzerScope.INDEX, new KeywordAnalyzer());
        Map<String, NamedAnalyzer> analyzers = new HashMap<>();
        analyzers.put("default", dflt);
        analyzers.put("standard", standard);
        analyzers.put("keyword", keyword);
        return new IndexAnalyzers(analyzers, Collections.emptyMap(), Collections.emptyMap());
    }

    @Override
    protected String typeName() {
        return "token_count";
    }

    /**
     *  When position increments are counted, we're looking to make sure that we:
        - don't count tokens without an increment
        - count normal tokens with one increment
        - count funny tokens with more than one increment
        - count the final token increments on the rare token streams that have them
     */
    public void testCountPositionsWithIncrements() throws IOException {
        Analyzer analyzer = createMockAnalyzer();
        assertThat(TokenCountFieldMapper.countPositions(analyzer, "", "", true), equalTo(7));
    }

    /**
     *  When position increments are not counted (only positions are counted), we're looking to make sure that we:
        - don't count tokens without an increment
        - count normal tokens with one increment
        - count funny tokens with more than one increment as only one
        - don't count the final token increments on the rare token streams that have them
     */
    public void testCountPositionsWithoutIncrements() throws IOException {
        Analyzer analyzer = createMockAnalyzer();
        assertThat(TokenCountFieldMapper.countPositions(analyzer, "", "", false), equalTo(2));
    }

    private Analyzer createMockAnalyzer() {
        Token t1 = new Token();      // Token without an increment
        t1.setPositionIncrement(0);
        Token t2 = new Token();
        t2.setPositionIncrement(1);  // Normal token with one increment
        Token t3 = new Token();
        t2.setPositionIncrement(2);  // Funny token with more than one increment
        int finalTokenIncrement = 4; // Final token increment
        Token[] tokens = new Token[] {t1, t2, t3};
        Collections.shuffle(Arrays.asList(tokens), random());
        final TokenStream tokenStream = new CannedTokenStream(finalTokenIncrement, 0, tokens);
        // TODO: we have no CannedAnalyzer?
        return new Analyzer() {
            @Override
            public TokenStreamComponents createComponents(String fieldName) {
                return new TokenStreamComponents(new MockTokenizer(), tokenStream);
            }
        };
    }

    public void testParseNullValue() throws Exception {
        DocumentMapper mapper = createIndexWithTokenCountField();
        ParseContext.Document doc = parseDocument(mapper, createDocument(null));
        assertNull(doc.getField("test.tc"));
    }

    public void testParseEmptyValue() throws Exception {
        DocumentMapper mapper = createIndexWithTokenCountField();
        ParseContext.Document doc = parseDocument(mapper, createDocument(""));
        assertEquals(0, doc.getField("test.tc").numericValue());
    }

    public void testParseNotNullValue() throws Exception {
        DocumentMapper mapper = createIndexWithTokenCountField();
        ParseContext.Document doc = parseDocument(mapper, createDocument("three tokens string"));
        assertEquals(3, doc.getField("test.tc").numericValue());
    }

    private DocumentMapper createIndexWithTokenCountField() throws IOException {
        return createDocumentMapper(mapping(b -> {
            b.startObject("test");
            {
                b.field("type", "text");
                b.startObject("fields");
                {
                    b.startObject("tc");
                    {
                        b.field("type", "token_count");
                        b.field("analyzer", "standard");
                    }
                    b.endObject();
                }
                b.endObject();
            }
            b.endObject();
        }));
    }

    private SourceToParse createDocument(String fieldValue) throws Exception {
        return source(b -> b.field("test", fieldValue));
    }

    private ParseContext.Document parseDocument(DocumentMapper mapper, SourceToParse request) {
        return mapper.parse(request)
            .docs().stream().findFirst().orElseThrow(() -> new IllegalStateException("Test object not parsed"));
    }

    @Override
    protected String generateRandomInputValue(MappedFieldType ft) {
        int words = between(1, 1000);
        StringBuilder b = new StringBuilder(words * 5);
        b.append(randomAlphaOfLength(4));
        for (int w = 1; w < words; w++) {
            b.append(' ').append(randomAlphaOfLength(4));
        }
        return b.toString();
    }

    @Override
    protected void randomFetchTestFieldConfig(XContentBuilder b) throws IOException {
        b.field("type", "token_count").field("analyzer", "standard");
    }
}
