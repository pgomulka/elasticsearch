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

package org.elasticsearch.common.time;

import org.elasticsearch.common.Strings;

import java.text.ParsePosition;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class JavaDateFormatter implements DateFormatter {

    // base fields which should be used for default parsing, when we round up for date math
    private static final Map<TemporalField, Long> ROUND_UP_BASE_FIELDS = new HashMap<>(6);
    {
        ROUND_UP_BASE_FIELDS.put(ChronoField.MONTH_OF_YEAR, 1L);
        ROUND_UP_BASE_FIELDS.put(ChronoField.DAY_OF_MONTH, 1L);
        ROUND_UP_BASE_FIELDS.put(ChronoField.HOUR_OF_DAY, 23L);
        ROUND_UP_BASE_FIELDS.put(ChronoField.MINUTE_OF_HOUR, 59L);
        ROUND_UP_BASE_FIELDS.put(ChronoField.SECOND_OF_MINUTE, 59L);
        ROUND_UP_BASE_FIELDS.put(ChronoField.NANO_OF_SECOND, 999_999_999L);
    }

    private final String format;
    private final DateTimeFormatter printer;
    private final List<DateTimeFormatter> parsers;
    private final DateTimeFormatter roundupParser;

    JavaDateFormatter(String format, DateTimeFormatter printer, DateTimeFormatter... parsers) {
        this(format, printer, builder -> ROUND_UP_BASE_FIELDS.forEach(builder::parseDefaulting), parsers);
    }

    JavaDateFormatter(String format, DateTimeFormatter printer, Consumer<DateTimeFormatterBuilder> roundupParserConsumer,
                              DateTimeFormatter... parsers) {
        if (printer == null) {
            throw new IllegalArgumentException("printer may not be null");
        }
        long distinctZones = Arrays.stream(parsers).map(DateTimeFormatter::getZone).distinct().count();
        if (distinctZones > 1) {
            throw new IllegalArgumentException("formatters must have the same time zone");
        }
        long distinctLocales = Arrays.stream(parsers).map(DateTimeFormatter::getLocale).distinct().count();
        if (distinctLocales > 1) {
            throw new IllegalArgumentException("formatters must have the same locale");
        }
        this.printer = printer;
        this.format = format;

        if (parsers.length == 0) {
            this.parsers = Arrays.asList(printer);
        } else {
            this.parsers = Arrays.asList(parsers);
        }

        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        if (format.contains("||") == false) {
            builder.append(firstParser());
        }
        roundupParserConsumer.accept(builder);
        DateTimeFormatter roundupFormatter = builder.toFormatter(firstParser().getLocale());
        if (printer.getZone() != null) {
            roundupFormatter = roundupFormatter.withZone(printer.getZone());
        }
        this.roundupParser = roundupFormatter;
    }

    private DateTimeFormatter firstParser() {
        return this.parsers.get(0);
    }

    DateTimeFormatter getRoundupParser() {
        return roundupParser;
    }

    DateTimeFormatter getPrinter() {
        return printer;
    }

    @Override
    public TemporalAccessor parse(String input) {
        if (Strings.isNullOrEmpty(input)) {
            throw new IllegalArgumentException("cannot parse empty date");
        }

        try {
            return doParse(input);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("failed to parse date field [" + input + "] with format [" + format + "]", e);
        }
    }

    private TemporalAccessor doParse(String input) {
        if (parsers.size() > 1) {
            for (DateTimeFormatter formatter : parsers) {
                if (tryParseUnresolved(formatter, input) == true) {
                    return formatter.parse(input);
                }
            }
        }
        return firstParser().parse(input);
    }

    /**
     * Attempt parsing the input without throwing exception. This is needed because java-time requires ordering on optional (composite)
     * patterns. Joda does not suffer from this.
     * https://bugs.openjdk.java.net/browse/JDK-8188771
     *
     * @param input An arbitrary string resembling the string representation of a date or time
     * @return true if parsing was successful, false if parsing failed
     */
    private boolean tryParseUnresolved(DateTimeFormatter formatter, String input) {
        try {
            ParsePosition pp = new ParsePosition(0);
            formatter.parseUnresolved(input, pp);
            int len = input.length();
            if (pp.getErrorIndex() == -1 && pp.getIndex() == len) {
                return true;
            }
        } catch (RuntimeException ex) {
            // should not happen, but ignore if it does
        }
        return false;
    }

    @Override
    public DateFormatter withZone(ZoneId zoneId) {
        // shortcurt to not create new objects unnecessarily
        if (zoneId.equals(firstParser().getZone())) {
            return this;
        }

        return new JavaDateFormatter(format, printer.withZone(zoneId),
            parsers.stream().map(p->p.withZone(zoneId)).toArray(size->new DateTimeFormatter[size]));
    }

    @Override
    public DateFormatter withLocale(Locale locale) {
        //TODO not sure that we can keep that shortcut, and the one above
        // shortcurt to not create new objects unnecessarily
        if (locale.equals(firstParser().getLocale())) {
            return this;
        }

        return new JavaDateFormatter(format, printer.withLocale(locale),
            parsers.stream().map(p->p.withLocale(locale)).toArray(size->new DateTimeFormatter[size]));
    }

    @Override
    public String format(TemporalAccessor accessor) {
        return printer.format(DateFormatters.from(accessor));
    }

    @Override
    public String pattern() {
        return format;
    }

    @Override
    public Locale locale() {
        return this.printer.getLocale();
    }

    @Override
    public ZoneId zone() {
        return this.printer.getZone();
    }

    @Override
    public DateMathParser toDateMathParser() {
        return new JavaDateMathParser(format, this, getRoundupParser());
    }

    @Override
    public int hashCode() {
        return Objects.hash(locale(), printer.getZone(), format);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass().equals(this.getClass()) == false) {
            return false;
        }
        JavaDateFormatter other = (JavaDateFormatter) obj;

        return Objects.equals(format, other.format) &&
               Objects.equals(locale(), other.locale()) &&
               Objects.equals(this.printer.getZone(), other.printer.getZone());
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "format[%s] locale[%s]", format, locale());
    }

    public Collection<DateTimeFormatter> getParsers() {
        return parsers;
    }
}
