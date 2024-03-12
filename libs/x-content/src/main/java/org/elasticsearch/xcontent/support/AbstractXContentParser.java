/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.xcontent.support;

import org.elasticsearch.core.Booleans;
import org.elasticsearch.core.RestApiVersion;
import org.elasticsearch.xcontent.DeprecationHandler;
import org.elasticsearch.xcontent.NamedXContentRegistry;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;

public abstract class AbstractXContentParser extends  AbstractMapListParser implements XContentParser {

    // Currently this is not a setting that can be changed and is a policy
    // that relates to how parsing of things like "boost" are done across
    // the whole of Elasticsearch (eg if String "1.0" is a valid float).
    // The idea behind keeping it as a constant is that we can track
    // references to this policy decision throughout the codebase and find
    // and change any code that needs to apply an alternative policy.
    public static final boolean DEFAULT_NUMBER_COERCE_POLICY = true;

    private static void checkCoerceString(boolean coerce, Class<? extends Number> clazz) {
        if (coerce == false) {
            // Need to throw type IllegalArgumentException as current catch logic in
            // NumberFieldMapper.parseCreateField relies on this for "malformed" value detection
            throw new IllegalArgumentException(clazz.getSimpleName() + " value passed as String");
        }
    }

    private final NamedXContentRegistry xContentRegistry;
    private final DeprecationHandler deprecationHandler;
    private final RestApiVersion restApiVersion;

    public AbstractXContentParser(
        NamedXContentRegistry xContentRegistry,
        DeprecationHandler deprecationHandler,
        RestApiVersion restApiVersion
    ) {
        this.xContentRegistry = xContentRegistry;
        this.deprecationHandler = deprecationHandler;
        this.restApiVersion = restApiVersion;
    }

    public AbstractXContentParser(NamedXContentRegistry xContentRegistry, DeprecationHandler deprecationHandler) {
        this(xContentRegistry, deprecationHandler, RestApiVersion.current());
    }

    // The 3rd party parsers we rely on are known to silently truncate fractions: see
    // http://fasterxml.github.io/jackson-core/javadoc/2.3.0/com/fasterxml/jackson/core/JsonParser.html#getShortValue()
    // If this behaviour is flagged as undesirable and any truncation occurs
    // then this method is called to trigger the"malformed" handling logic
    void ensureNumberConversion(boolean coerce, long result, Class<? extends Number> clazz) throws IOException {
        if (coerce == false) {
            double fullVal = doDoubleValue();
            if (result != fullVal) {
                // Need to throw type IllegalArgumentException as current catch
                // logic in NumberFieldMapper.parseCreateField relies on this
                // for "malformed" value detection
                throw new IllegalArgumentException(fullVal + " cannot be converted to " + clazz.getSimpleName() + " without data loss");
            }
        }
    }

    @Override
    public boolean isBooleanValue() throws IOException {
        return switch (currentToken()) {
            case VALUE_BOOLEAN -> true;
            case VALUE_STRING -> Booleans.isBoolean(textCharacters(), textOffset(), textLength());
            default -> false;
        };
    }

    @Override
    public boolean booleanValue() throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            return Booleans.parseBoolean(textCharacters(), textOffset(), textLength(), false /* irrelevant */);
        }
        return doBooleanValue();
    }

    protected abstract boolean doBooleanValue() throws IOException;

    @Override
    public short shortValue() throws IOException {
        return shortValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public short shortValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Short.class);

            double doubleValue = Double.parseDouble(text());

            if (doubleValue < Short.MIN_VALUE || doubleValue > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Value [" + text() + "] is out of range for a short");
            }

            return (short) doubleValue;
        }
        short result = doShortValue();
        ensureNumberConversion(coerce, result, Short.class);
        return result;
    }

    protected abstract short doShortValue() throws IOException;

    @Override
    public int intValue() throws IOException {
        return intValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public int intValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Integer.class);
            double doubleValue = Double.parseDouble(text());

            if (doubleValue < Integer.MIN_VALUE || doubleValue > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Value [" + text() + "] is out of range for an integer");
            }

            return (int) doubleValue;
        }
        int result = doIntValue();
        ensureNumberConversion(coerce, result, Integer.class);
        return result;
    }

    protected abstract int doIntValue() throws IOException;

    private static BigInteger LONG_MAX_VALUE_AS_BIGINTEGER = BigInteger.valueOf(Long.MAX_VALUE);
    private static BigInteger LONG_MIN_VALUE_AS_BIGINTEGER = BigInteger.valueOf(Long.MIN_VALUE);
    // weak bounds on the BigDecimal representation to allow for coercion
    private static BigDecimal BIGDECIMAL_GREATER_THAN_LONG_MAX_VALUE = BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE);
    private static BigDecimal BIGDECIMAL_LESS_THAN_LONG_MIN_VALUE = BigDecimal.valueOf(Long.MIN_VALUE).subtract(BigDecimal.ONE);

    /** Return the long that {@code stringValue} stores or throws an exception if the
     *  stored value cannot be converted to a long that stores the exact same
     *  value and {@code coerce} is false. */
    private static long toLong(String stringValue, boolean coerce) {
        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException e) {
            // we will try again with BigDecimal
        }

        final BigInteger bigIntegerValue;
        try {
            final BigDecimal bigDecimalValue = new BigDecimal(stringValue);
            if (bigDecimalValue.compareTo(BIGDECIMAL_GREATER_THAN_LONG_MAX_VALUE) >= 0
                || bigDecimalValue.compareTo(BIGDECIMAL_LESS_THAN_LONG_MIN_VALUE) <= 0) {
                throw new IllegalArgumentException("Value [" + stringValue + "] is out of range for a long");
            }
            bigIntegerValue = coerce ? bigDecimalValue.toBigInteger() : bigDecimalValue.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Value [" + stringValue + "] has a decimal part");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("For input string: \"" + stringValue + "\"");
        }

        if (bigIntegerValue.compareTo(LONG_MAX_VALUE_AS_BIGINTEGER) > 0 || bigIntegerValue.compareTo(LONG_MIN_VALUE_AS_BIGINTEGER) < 0) {
            throw new IllegalArgumentException("Value [" + stringValue + "] is out of range for a long");
        }

        assert bigIntegerValue.longValueExact() <= Long.MAX_VALUE; // asserting that no ArithmeticException is thrown
        return bigIntegerValue.longValue();
    }

    @Override
    public long longValue() throws IOException {
        return longValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public long longValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Long.class);
            return toLong(text(), coerce);
        }
        long result = doLongValue();
        ensureNumberConversion(coerce, result, Long.class);
        return result;
    }

    protected abstract long doLongValue() throws IOException;

    @Override
    public float floatValue() throws IOException {
        return floatValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public float floatValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Float.class);
            return Float.parseFloat(text());
        }
        return doFloatValue();
    }

    protected abstract float doFloatValue() throws IOException;

    @Override
    public double doubleValue() throws IOException {
        return doubleValue(DEFAULT_NUMBER_COERCE_POLICY);
    }

    @Override
    public double doubleValue(boolean coerce) throws IOException {
        Token token = currentToken();
        if (token == Token.VALUE_STRING) {
            checkCoerceString(coerce, Double.class);
            return Double.parseDouble(text());
        }
        return doDoubleValue();
    }

    protected abstract double doDoubleValue() throws IOException;

    @Override
    public final String textOrNull() throws IOException {
        if (currentToken() == Token.VALUE_NULL) {
            return null;
        }
        return text();
    }

    @Override
    public CharBuffer charBufferOrNull() throws IOException {
        if (currentToken() == Token.VALUE_NULL) {
            return null;
        }
        return charBuffer();
    }



    @Override
    public <T> T namedObject(Class<T> categoryClass, String name, Object context) throws IOException {
        return xContentRegistry.parseNamedObject(categoryClass, name, this, context);
    }

    @Override
    public NamedXContentRegistry getXContentRegistry() {
        return xContentRegistry;
    }

    @Override
    public abstract boolean isClosed();

    @Override
    public RestApiVersion getRestApiVersion() {
        return restApiVersion;
    }

    @Override
    public DeprecationHandler getDeprecationHandler() {
        return deprecationHandler;
    }
}
