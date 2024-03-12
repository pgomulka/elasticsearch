/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 * Copyright Elasticsearch B.V. All rights reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch B.V. and its suppliers, if any.
 * The intellectual and technical concepts contained herein
 * are proprietary to Elasticsearch B.V. and its suppliers and
 * may be covered by U.S. and Foreign Patents, patents in
 * process, and are protected by trade secret or copyright
 * law.  Dissemination of this information or reproduction of
 * this material is strictly forbidden unless prior written
 * permission is obtained from Elasticsearch B.V.
 */

package org.elasticsearch.benchmark.metering;

import org.elasticsearch.core.RestApiVersion;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentLocation;
import org.elasticsearch.xcontent.XContentParser;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xcontent.support.AbstractMapListParser;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is an XContentParser that is performing metering.
 * the metering is taking into account field names and field values.
 * The structure, format is ignored.
 * <p>
 * Decimals (int, long) are all metered with same value (long bit size)
 * Floating points (float, double) are metered with double bit size
 * field names are metered with length * char bit size
 * text fields are length * char bit size
 */
public class MeteringLengthParser extends AbstractMapListParser implements XContentParser {
    private final XContentParser delegate;
    private final AtomicLong counter;

    /**
     * A function to accumulate total size of the document
     * based on the JSONS's token type it will add size for the type.
     * This method is called by nextToken method which is only called once per each token.
     */
    private void charge(Token token) throws IOException {
        if (token != null) {
            var sizeInBits = switch (token) {
                case FIELD_NAME, VALUE_STRING -> calculateTextLength();
                case VALUE_EMBEDDED_OBJECT -> calculateBase64Length(binaryValue().length) * Character.SIZE;
                case VALUE_NUMBER -> Long.SIZE;
                case VALUE_BOOLEAN -> 1;
                default -> 0;
            };
            counter.addAndGet(sizeInBits);
        }
    }

    private int calculateTextLength() throws IOException {
        char[] charArray = textCharacters();
        int byteLength = 0;
        int limit = textOffset() + textLength();

        for (int i = textOffset(); i < limit;) {
            int codePoint = Character.codePointAt(charArray, i, limit);
            int charCount = Character.charCount(codePoint);
            i += charCount;

            if (codePoint <= 0x7F) {
                byteLength++; // ASCII characters require one byte
            } else if (codePoint <= 0x7FF) {
                byteLength += 2; // Two bytes for characters in the range U+0080 to U+07FF
            } else if (codePoint <= 0xFFFF) {
                byteLength += 3; // Three bytes for characters in the range U+0800 to U+FFFF
            } else {
                byteLength += 4; // Four bytes for supplementary characters
            }
        }

        return byteLength * 8;
    }

    public MeteringLengthParser(XContentParser xContentParser, AtomicLong counter) {
        // super(xContentParser.getXContentRegistry(), xContentParser.getDeprecationHandler(), xContentParser.getRestApiVersion());
        this.delegate = xContentParser;
        this.counter = counter;
    }

    protected XContentParser delegate() {
        return delegate;
    }

    @Override
    public Token nextToken() throws IOException {
        Token token = delegate().nextToken();
        charge(token);
        return token;
    }

    /**
     * when a parser is defined to use this method (field has a ignored=true) we still want to charge for those.
     * The parsing should iterate all the token as if the field was not ignored.
     */
    @Override
    public void skipChildren() throws IOException {
        // reimplementing the ParserMinimalBase#skipChildren()
        Token currentToken = currentToken();
        if (currentToken != Token.START_OBJECT && currentToken != Token.START_ARRAY) {
            return;
        }
        int open = 1;
        while (true) {
            Token t = nextToken();
            if (t == null) {
                return;
            }
            switch (t) {
                case START_OBJECT:
                case START_ARRAY:
                    ++open;
                    break;
                case END_OBJECT:
                case END_ARRAY:
                    if (--open == 0) {
                        return;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private long calculateBase64Length(int n) {
        /*
        this calculates the length of a base64 (padded) encoded string for a given byte array length N
        the calculation is as follows:
        every 3 bytes of byte array are encoded in 4characters blocks
        if blocks are not fully populated by a byte array (byte array length is not divisible by 3) the remaining is padded with =
        Therefore we need a ceil (n/3)
        (n+2)/3 is an equivalent of ceil(n/3)
         */
        return (n + 2) / 3 * 4;
    }

    @Override
    public XContentType contentType() {
        return delegate().contentType();
    }

    @Override
    public void allowDuplicateKeys(boolean allowDuplicateKeys) {
        delegate().allowDuplicateKeys(allowDuplicateKeys);
    }

    @Override
    public Token currentToken() {
        return delegate().currentToken();
    }

    @Override
    public String currentName() throws IOException {
        return delegate().currentName();
    }

    @Override
    public String text() throws IOException {
        return delegate().text();
    }

    @Override
    public String textOrNull() throws IOException {
        return delegate().textOrNull();
    }

    @Override
    public CharBuffer charBufferOrNull() throws IOException {
        return delegate().charBufferOrNull();
    }

    @Override
    public CharBuffer charBuffer() throws IOException {
        return delegate().charBuffer();
    }

    @Override
    public Object objectText() throws IOException {
        return delegate().objectText();
    }

    @Override
    public Object objectBytes() throws IOException {
        return delegate().objectBytes();
    }

    @Override
    public boolean hasTextCharacters() {
        return delegate().hasTextCharacters();
    }

    @Override
    public char[] textCharacters() throws IOException {
        return delegate().textCharacters();
    }

    @Override
    public int textLength() throws IOException {
        return delegate().textLength();
    }

    @Override
    public int textOffset() throws IOException {
        return delegate().textOffset();
    }

    @Override
    public Number numberValue() throws IOException {
        return delegate().numberValue();
    }

    @Override
    public NumberType numberType() throws IOException {
        return delegate().numberType();
    }

    @Override
    public short shortValue(boolean coerce) throws IOException {
        return delegate().shortValue(coerce);
    }

    @Override
    public int intValue(boolean coerce) throws IOException {
        return delegate().intValue(coerce);
    }

    @Override
    public long longValue(boolean coerce) throws IOException {
        return delegate().longValue(coerce);
    }

    @Override
    public float floatValue(boolean coerce) throws IOException {
        return delegate().floatValue(coerce);
    }

    @Override
    public double doubleValue(boolean coerce) throws IOException {
        return delegate().doubleValue(coerce);
    }

    @Override
    public short shortValue() throws IOException {
        return delegate().shortValue();
    }

    @Override
    public int intValue() throws IOException {
        return delegate().intValue();
    }

    @Override
    public long longValue() throws IOException {
        return delegate().longValue();
    }

    @Override
    public float floatValue() throws IOException {
        return delegate().floatValue();
    }

    @Override
    public double doubleValue() throws IOException {
        return delegate().doubleValue();
    }

    @Override
    public boolean isBooleanValue() throws IOException {
        return delegate().isBooleanValue();
    }

    @Override
    public boolean booleanValue() throws IOException {
        return delegate().booleanValue();
    }

    @Override
    public byte[] binaryValue() throws IOException {
        return delegate().binaryValue();
    }

    @Override
    public XContentLocation getTokenLocation() {
        return delegate().getTokenLocation();
    }

    @Override
    public <T> T namedObject(Class<T> categoryClass, String name, Object context) throws IOException {
        return delegate().namedObject(categoryClass, name, context);
    }

    @Override
    public NamedXContentRegistry getXContentRegistry() {
        return delegate().getXContentRegistry();
    }

    @Override
    public boolean isClosed() {
        return delegate().isClosed();
    }

    @Override
    public void close() throws IOException {
        var closeable = delegate();
        if (closeable != null) {
            closeable.close();
        }
    }

    @Override
    public RestApiVersion getRestApiVersion() {
        return delegate().getRestApiVersion();
    }

    @Override
    public DeprecationHandler getDeprecationHandler() {
        return delegate().getDeprecationHandler();
    }

}
