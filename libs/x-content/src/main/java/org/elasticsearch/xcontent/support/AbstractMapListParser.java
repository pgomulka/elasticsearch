/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.xcontent.support;

import org.elasticsearch.core.CheckedFunction;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.xcontent.XContentParseException;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public abstract class AbstractMapListParser implements XContentParser {
    @Override
    public Map<String, Object> map() throws IOException {
        return readMapSafe(this, SIMPLE_MAP_FACTORY);
    }

    @Override
    public Map<String, Object> mapOrdered() throws IOException {
        return readMapSafe(this, ORDERED_MAP_FACTORY);
    }

    @Override
    public Map<String, String> mapStrings() throws IOException {
        return map(HashMap::new, XContentParser::text);
    }

    @Override
    public <T> Map<String, T> map(Supplier<Map<String, T>> mapFactory, CheckedFunction<XContentParser, T, IOException> mapValueParser)
        throws IOException {
        final Map<String, T> map = mapFactory.get();
        String fieldName = findNonEmptyMapStart(this);
        if (fieldName == null) {
            return map;
        }
        assert currentToken() == Token.FIELD_NAME : "Expected field name but saw [" + currentToken() + "]";
        do {
            nextToken();
            T value = mapValueParser.apply(this);
            map.put(fieldName, value);
        } while ((fieldName = nextFieldName()) != null);
        return map;
    }

    @Override
    public List<Object> list() throws IOException {
        skipToListStart(this);
        return readListUnsafe(this, SIMPLE_MAP_FACTORY);
    }

    @Override
    public List<Object> listOrderedMap() throws IOException {
        skipToListStart(this);
        return readListUnsafe(this, ORDERED_MAP_FACTORY);
    }

    private static final Supplier<Map<String, Object>> SIMPLE_MAP_FACTORY = HashMap::new;

    private static final Supplier<Map<String, Object>> ORDERED_MAP_FACTORY = LinkedHashMap::new;

    private static Map<String, Object> readMapSafe(XContentParser parser, Supplier<Map<String, Object>> mapFactory) throws IOException {
        final Map<String, Object> map = mapFactory.get();
        final String firstKey = findNonEmptyMapStart(parser);
        return firstKey == null ? map : readMapEntries(parser, mapFactory, map, firstKey);
    }

    private static Map<String, Object> readMapEntries(
        XContentParser parser,
        Supplier<Map<String, Object>> mapFactory,
        Map<String, Object> map,
        String currentFieldName
    ) throws IOException {
        do {
            Object value = readValueUnsafe(parser.nextToken(), parser, mapFactory);
            map.put(currentFieldName, value);
        } while ((currentFieldName = parser.nextFieldName()) != null);
        return map;
    }

    /**
     * Checks if the next current token in the supplied parser is a map start for a non-empty map.
     * Skips to the next token if the parser does not yet have a current token (i.e. {@link #currentToken()} returns {@code null}) and then
     * checks it.
     *
     * @return the first key in the map if a non-empty map start is found
     */
    @Nullable
    private static String findNonEmptyMapStart(XContentParser parser) throws IOException {
        Token token = parser.currentToken();
        if (token == null) {
            token = parser.nextToken();
        }
        if (token == Token.START_OBJECT) {
            return parser.nextFieldName();
        }
        return token == Token.FIELD_NAME ? parser.currentName() : null;
    }

    // Skips the current parser to the next array start. Assumes that the parser is either positioned before an array field's name token or
    // on the start array token.
    private static void skipToListStart(XContentParser parser) throws IOException {
        Token token = parser.currentToken();
        if (token == null) {
            token = parser.nextToken();
        }
        if (token == Token.FIELD_NAME) {
            token = parser.nextToken();
        }
        if (token != Token.START_ARRAY) {
            throw new XContentParseException(
                parser.getTokenLocation(),
                "Failed to parse list:  expecting " + Token.START_ARRAY + " but got " + token
            );
        }
    }

    // read a list without bounds checks, assuming the the current parser is always on an array start
    private static List<Object> readListUnsafe(XContentParser parser, Supplier<Map<String, Object>> mapFactory) throws IOException {
        assert parser.currentToken() == Token.START_ARRAY;
        ArrayList<Object> list = new ArrayList<>();
        for (Token token = parser.nextToken(); token != null && token != Token.END_ARRAY; token = parser.nextToken()) {
            list.add(readValueUnsafe(token, parser, mapFactory));
        }
        return list;
    }

    public static Object readValue(XContentParser parser, Supplier<Map<String, Object>> mapFactory) throws IOException {
        return readValueUnsafe(parser.currentToken(), parser, mapFactory);
    }

    /**
     * Reads next value from the parser that is assumed to be at the given current token without any additional checks.
     *
     * @param currentToken current token that the parser is at
     * @param parser       parser to read from
     * @param mapFactory   map factory to use for reading objects
     */
    private static Object readValueUnsafe(Token currentToken, XContentParser parser, Supplier<Map<String, Object>> mapFactory)
        throws IOException {
        assert currentToken == parser.currentToken()
            : "Supplied current token [" + currentToken + "] is different from actual parser current token [" + parser.currentToken() + "]";
        switch (currentToken) {
            case VALUE_STRING:
                return parser.text();
            case VALUE_NUMBER:
                return parser.numberValue();
            case VALUE_BOOLEAN:
                return parser.booleanValue();
            case START_OBJECT: {
                final Map<String, Object> map = mapFactory.get();
                final String nextFieldName = parser.nextFieldName();
                return nextFieldName == null ? map : readMapEntries(parser, mapFactory, map, nextFieldName);
            }
            case START_ARRAY:
                return readListUnsafe(parser, mapFactory);
            case VALUE_EMBEDDED_OBJECT:
                return parser.binaryValue();
            case VALUE_NULL:
            default:
                return null;
        }
    }
}
