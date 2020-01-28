package org.elasticsearch.common.joda;

import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.util.Map;
import java.util.regex.Pattern;

public class JodaJavaBuilder extends DateTimeFormatterBuilder {
    StringBuilder pattern = new StringBuilder();
//yyyy-MM-dd'#T' '#something' yy-MM-dd
    public DateTimeFormatterBuilder appendPattern(String pattern) {
        String pattern1 = pattern.replaceAll("'([^']+)'", "'#$1'");
        return super.appendPattern(pattern1);
    }

    public String getJavaPattern() {
        String s = pattern.toString();
        return s.replaceAll("#","");
    }

    //-----------------------------------------------------------------------
    /**
     * Instructs the printer to emit a specific character, and the parser to
     * expect it. The parser is case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendLiteral(char c) {
       return appendX(""+c);
    }

    /**
     * Instructs the printer to emit specific text, and the parser to expect
     * it. The parser is case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     * @throws IllegalArgumentException if text is null
     */
    public DateTimeFormatterBuilder appendLiteral(String text) {
        return appendX("'"+text+"'");
    }

//    /**
//     * Instructs the printer to emit a field value as a decimal number, and the
//     * parser to expect an unsigned decimal number.
//     *
//     * @param fieldType  type of field to append
//     * @param minDigits  minimum number of digits to <i>print</i>
//     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
//     * maximum number of digits to print
//     * @return this DateTimeFormatterBuilder, for chaining
//     * @throws IllegalArgumentException if field type is null
//     */
//    public DateTimeFormatterBuilder appendDecimal(
//        DateTimeFieldType fieldType, int minDigits, int maxDigits) {
//        pattern.append("'"+text+"'");
//        return this;
//        if (fieldType == null) {
//            throw new IllegalArgumentException("Field type must not be null");
//        }
//        if (maxDigits < minDigits) {
//            maxDigits = minDigits;
//        }
//        if (minDigits < 0 || maxDigits <= 0) {
//            throw new IllegalArgumentException();
//        }
//        if (minDigits <= 1) {
//            return append0(new UnpaddedNumber(fieldType, maxDigits, false));
//        } else {
//            return append0(new PaddedNumber(fieldType, maxDigits, false, minDigits));
//        }
//    }

    /**
     * Instructs the printer to emit a field value as a fixed-width decimal
     * number (smaller numbers will be left-padded with zeros), and the parser
     * to expect an unsigned decimal number with the same fixed width.
     *
     * @param fieldType  type of field to append
     * @param numDigits  the exact number of digits to parse or print, except if
     * printed value requires more digits
     * @return this DateTimeFormatterBuilder, for chaining
     * @throws IllegalArgumentException if field type is null or if <code>numDigits <= 0</code>
     * @since 1.5
     */
//    public DateTimeFormatterBuilder appendFixedDecimal(
//        DateTimeFieldType fieldType, int numDigits) {
//        if (fieldType == null) {
//            throw new IllegalArgumentException("Field type must not be null");
//        }
//        if (numDigits <= 0) {
//            throw new IllegalArgumentException("Illegal number of digits: " + numDigits);
//        }
//        return append0(new FixedNumber(fieldType, numDigits, false));
//    }

    /**
     * Instructs the printer to emit a field value as a decimal number, and the
     * parser to expect a signed decimal number.
     *
     * @param fieldType  type of field to append
     * @param minDigits  minimum number of digits to <i>print</i>
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     * @throws IllegalArgumentException if field type is null
     */
//    public DateTimeFormatterBuilder appendSignedDecimal(
//        DateTimeFieldType fieldType, int minDigits, int maxDigits) {
//        if (fieldType == null) {
//            throw new IllegalArgumentException("Field type must not be null");
//        }
//        if (maxDigits < minDigits) {
//            maxDigits = minDigits;
//        }
//        if (minDigits < 0 || maxDigits <= 0) {
//            throw new IllegalArgumentException();
//        }
//        if (minDigits <= 1) {
//            return append0(new UnpaddedNumber(fieldType, maxDigits, true));
//        } else {
//            return append0(new PaddedNumber(fieldType, maxDigits, true, minDigits));
//        }
//    }

    /**
     * Instructs the printer to emit a field value as a fixed-width decimal
     * number (smaller numbers will be left-padded with zeros), and the parser
     * to expect an signed decimal number with the same fixed width.
     *
     * @param fieldType  type of field to append
     * @param numDigits  the exact number of digits to parse or print, except if
     * printed value requires more digits
     * @return this DateTimeFormatterBuilder, for chaining
     * @throws IllegalArgumentException if field type is null or if <code>numDigits <= 0</code>
     * @since 1.5
     */
//    public DateTimeFormatterBuilder appendFixedSignedDecimal(
//        DateTimeFieldType fieldType, int numDigits) {
//        if (fieldType == null) {
//            throw new IllegalArgumentException("Field type must not be null");
//        }
//        if (numDigits <= 0) {
//            throw new IllegalArgumentException("Illegal number of digits: " + numDigits);
//        }
//        return append0(new FixedNumber(fieldType, numDigits, true));
//    }

    /**
     * Instructs the printer to emit a field value as text, and the
     * parser to expect text.
     *
     * @param fieldType  type of field to append
     * @return this DateTimeFormatterBuilder, for chaining
     * @throws IllegalArgumentException if field type is null
     */
//    public DateTimeFormatterBuilder appendText(DateTimeFieldType fieldType) {
//        if (fieldType == null) {
//            throw new IllegalArgumentException("Field type must not be null");
//        }
//        return append0(new TextField(fieldType, false));
//    }

    /**
     * Instructs the printer to emit a field value as short text, and the
     * parser to expect text.
     *
     * @param fieldType  type of field to append
     * @return this DateTimeFormatterBuilder, for chaining
     * @throws IllegalArgumentException if field type is null
     */
//    public DateTimeFormatterBuilder appendShortText(DateTimeFieldType fieldType) {
//        if (fieldType == null) {
//            throw new IllegalArgumentException("Field type must not be null");
//        }
//        return append0(new TextField(fieldType, true));
//    }

    /**
     * Instructs the printer to emit a remainder of time as a decimal fraction,
     * without decimal point. For example, if the field is specified as
     * minuteOfHour and the time is 12:30:45, the value printed is 75. A
     * decimal point is implied, so the fraction is 0.75, or three-quarters of
     * a minute.
     *
     * @param fieldType  type of field to append
     * @param minDigits  minimum number of digits to print.
     * @param maxDigits  maximum number of digits to print or parse.
     * @return this DateTimeFormatterBuilder, for chaining
     * @throws IllegalArgumentException if field type is null
     */
//    public DateTimeFormatterBuilder appendFraction(
//        DateTimeFieldType fieldType, int minDigits, int maxDigits) {
//        if (fieldType == null) {
//            throw new IllegalArgumentException("Field type must not be null");
//        }
//        if (maxDigits < minDigits) {
//            maxDigits = minDigits;
//        }
//        if (minDigits < 0 || maxDigits <= 0) {
//            throw new IllegalArgumentException();
//        }
//        return append0(new Fraction(fieldType, minDigits, maxDigits));
//    }

    /**
     * Appends the print/parse of a fractional second.
     * <p>
     * This reliably handles the case where fractional digits are being handled
     * beyond a visible decimal point. The digits parsed will always be treated
     * as the most significant (numerically largest) digits.
     * Thus '23' will be parsed as 230 milliseconds.
     * Contrast this behaviour to {@link #appendMillisOfSecond}.
     * This method does not print or parse the decimal point itself.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to print or parse
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendFractionOfSecond(int minDigits, int maxDigits) {
        return multiply("S",minDigits);
    }

    /**
     * Appends the print/parse of a fractional minute.
     * <p>
     * This reliably handles the case where fractional digits are being handled
     * beyond a visible decimal point. The digits parsed will always be treated
     * as the most significant (numerically largest) digits.
     * Thus '23' will be parsed as 0.23 minutes (converted to milliseconds).
     * This method does not print or parse the decimal point itself.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to print or parse
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendFractionOfMinute(int minDigits, int maxDigits) {
        return appendFraction(DateTimeFieldType.minuteOfDay(), minDigits, maxDigits);
    }

    /**
     * Appends the print/parse of a fractional hour.
     * <p>
     * This reliably handles the case where fractional digits are being handled
     * beyond a visible decimal point. The digits parsed will always be treated
     * as the most significant (numerically largest) digits.
     * Thus '23' will be parsed as 0.23 hours (converted to milliseconds).
     * This method does not print or parse the decimal point itself.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to print or parse
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendFractionOfHour(int minDigits, int maxDigits) {
        return appendFraction(DateTimeFieldType.hourOfDay(), minDigits, maxDigits);
    }

    /**
     * Appends the print/parse of a fractional day.
     * <p>
     * This reliably handles the case where fractional digits are being handled
     * beyond a visible decimal point. The digits parsed will always be treated
     * as the most significant (numerically largest) digits.
     * Thus '23' will be parsed as 0.23 days (converted to milliseconds).
     * This method does not print or parse the decimal point itself.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to print or parse
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendFractionOfDay(int minDigits, int maxDigits) {
        return appendFraction(DateTimeFieldType.dayOfYear(), minDigits, maxDigits);
    }

    /**
     * Instructs the printer to emit a numeric millisOfSecond field.
     * <p>
     * This method will append a field that prints a three digit value.
     * During parsing the value that is parsed is assumed to be three digits.
     * If less than three digits are present then they will be counted as the
     * smallest parts of the millisecond. This is probably not what you want
     * if you are using the field as a fraction. Instead, a fractional
     * millisecond should be produced using {@link #appendFractionOfSecond}.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendMillisOfSecond(int minDigits) {
        return appendDecimal(DateTimeFieldType.millisOfSecond(), minDigits, 3);
    }

    /**
     * Instructs the printer to emit a numeric millisOfDay field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendMillisOfDay(int minDigits) {
        return appendDecimal(DateTimeFieldType.millisOfDay(), minDigits, 8);
    }

    /**
     * Instructs the printer to emit a numeric secondOfMinute field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendSecondOfMinute(int minDigits) {
        return multiply("s",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric secondOfDay field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendSecondOfDay(int minDigits) {
        return appendDecimal(DateTimeFieldType.secondOfDay(), minDigits, 5);
    }

    /**
     * Instructs the printer to emit a numeric minuteOfHour field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendMinuteOfHour(int minDigits) {
        return multiply("m",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric minuteOfDay field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendMinuteOfDay(int minDigits) {
        return appendDecimal(DateTimeFieldType.minuteOfDay(), minDigits, 4);
    }

    /**
     * Instructs the printer to emit a numeric hourOfDay field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendHourOfDay(int minDigits) {
        return multiply("H",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric clockhourOfDay field.
     *
     * @param minDigits minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendClockhourOfDay(int minDigits) {
        return multiply("k",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric hourOfHalfday field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendHourOfHalfday(int minDigits) {
        return multiply("K",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric clockhourOfHalfday field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendClockhourOfHalfday(int minDigits) {
        return multiply("h",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric dayOfWeek field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendDayOfWeek(int minDigits) {
        return multiply("e",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric dayOfMonth field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendDayOfMonth(int minDigits) {
        return multiply("d",minDigits);
    }

    private DateTimeFormatterBuilder multiply(String d, int minDigits) {
        for(int i=0;i<minDigits;i++)
            pattern.append(d);
        return this;
    }

    /**
     * Instructs the printer to emit a numeric dayOfYear field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendDayOfYear(int minDigits) {
        return multiply("D",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric weekOfWeekyear field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendWeekOfWeekyear(int minDigits) {
        return multiply("w",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric weekyear field.
     *
     * @param minDigits  minimum number of digits to <i>print</i>
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendWeekyear(int minDigits, int maxDigits) {
        return multiply("Y",maxDigits);
    }

    private DateTimeFormatterBuilder appendX(String y) {
        pattern.append(y);
        return this;
    }

    /**
     * Instructs the printer to emit a numeric monthOfYear field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendMonthOfYear(int minDigits) {
        return appendX("MM");
    }

    /**
     * Instructs the printer to emit a numeric year field.
     *
     * @param minDigits  minimum number of digits to <i>print</i>
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendYear(int minDigits, int maxDigits) {
        return multiply("u",minDigits);
    }

    /**
     * Instructs the printer to emit a numeric year field which always prints
     * and parses two digits. A pivot year is used during parsing to determine
     * the range of supported years as <code>(pivot - 50) .. (pivot + 49)</code>.
     *
     * <pre>
     * pivot   supported range   00 is   20 is   40 is   60 is   80 is
     * ---------------------------------------------------------------
     * 1950      1900..1999      1900    1920    1940    1960    1980
     * 1975      1925..2024      2000    2020    1940    1960    1980
     * 2000      1950..2049      2000    2020    2040    1960    1980
     * 2025      1975..2074      2000    2020    2040    2060    1980
     * 2050      2000..2099      2000    2020    2040    2060    2080
     * </pre>
     *
     * @param pivot  pivot year to use when parsing
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendTwoDigitYear(int pivot) {
        return appendX("uu");
    }

    /**
     * Instructs the printer to emit a numeric year field which always prints
     * two digits. A pivot year is used during parsing to determine the range
     * of supported years as <code>(pivot - 50) .. (pivot + 49)</code>. If
     * parse is instructed to be lenient and the digit count is not two, it is
     * treated as an absolute year. With lenient parsing, specifying a positive
     * or negative sign before the year also makes it absolute.
     *
     * @param pivot  pivot year to use when parsing
     * @param lenientParse  when true, if digit count is not two, it is treated
     * as an absolute year
     * @return this DateTimeFormatterBuilder, for chaining
     * @since 1.1
     */
    public DateTimeFormatterBuilder appendTwoDigitYear(int pivot, boolean lenientParse) {
        return appendX("uu");
    }

    /**
     * Instructs the printer to emit a numeric weekyear field which always prints
     * and parses two digits. A pivot year is used during parsing to determine
     * the range of supported years as <code>(pivot - 50) .. (pivot + 49)</code>.
     *
     * <pre>
     * pivot   supported range   00 is   20 is   40 is   60 is   80 is
     * ---------------------------------------------------------------
     * 1950      1900..1999      1900    1920    1940    1960    1980
     * 1975      1925..2024      2000    2020    1940    1960    1980
     * 2000      1950..2049      2000    2020    2040    1960    1980
     * 2025      1975..2074      2000    2020    2040    2060    1980
     * 2050      2000..2099      2000    2020    2040    2060    2080
     * </pre>
     *
     * @param pivot  pivot weekyear to use when parsing
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendTwoDigitWeekyear(int pivot) {
       return appendX("YY");
    }

    /**
     * Instructs the printer to emit a numeric weekyear field which always prints
     * two digits. A pivot year is used during parsing to determine the range
     * of supported years as <code>(pivot - 50) .. (pivot + 49)</code>. If
     * parse is instructed to be lenient and the digit count is not two, it is
     * treated as an absolute weekyear. With lenient parsing, specifying a positive
     * or negative sign before the weekyear also makes it absolute.
     *
     * @param pivot  pivot weekyear to use when parsing
     * @param lenientParse  when true, if digit count is not two, it is treated
     * as an absolute weekyear
     * @return this DateTimeFormatterBuilder, for chaining
     * @since 1.1
     */
    public DateTimeFormatterBuilder appendTwoDigitWeekyear(int pivot, boolean lenientParse) {
        return appendX("YY");
    }

    /**
     * Instructs the printer to emit a numeric yearOfEra field.
     *
     * @param minDigits  minimum number of digits to <i>print</i>
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendYearOfEra(int minDigits, int maxDigits) {
        return multiply("y",minDigits);

    }

    /**
     * Instructs the printer to emit a numeric year of century field.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendYearOfCentury(int minDigits, int maxDigits) {
        return appendDecimal(DateTimeFieldType.yearOfCentury(), minDigits, maxDigits);
    }

    /**
     * Instructs the printer to emit a numeric century of era field.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendCenturyOfEra(int minDigits, int maxDigits) {
        throw new UnsupportedOperationException("Century of era not supported");
    }

    /**
     * Instructs the printer to emit a locale-specific AM/PM text, and the
     * parser to expect it. The parser is case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendHalfdayOfDayText() {
        return appendX("a");
    }

    /**
     * Instructs the printer to emit a locale-specific dayOfWeek text. The
     * parser will accept a long or short dayOfWeek text, case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendDayOfWeekText() {
        return multiply("e",4);
    }

    /**
     * Instructs the printer to emit a short locale-specific dayOfWeek
     * text. The parser will accept a long or short dayOfWeek text,
     * case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendDayOfWeekShortText() {
        return multiply("e",3);
    }

    /**
     * Instructs the printer to emit a short locale-specific monthOfYear
     * text. The parser will accept a long or short monthOfYear text,
     * case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendMonthOfYearText() {
        return appendX("MMMM");
    }

    /**
     * Instructs the printer to emit a locale-specific monthOfYear text. The
     * parser will accept a long or short monthOfYear text, case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendMonthOfYearShortText() {
        return appendX("MMM");
    }

    /**
     * Instructs the printer to emit a locale-specific era text (BC/AD), and
     * the parser to expect it. The parser is case-insensitive.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendEraText() {
        return appendX("G");
    }

    /**
     * Instructs the printer to emit a locale-specific time zone name.
     * Using this method prevents parsing, because time zone names are not unique.
     * See {@link #appendTimeZoneName(Map)}.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendTimeZoneName() {
        return appendX("zzzz");
    }

    /**
     * Instructs the printer to emit a locale-specific time zone name, providing a lookup for parsing.
     * Time zone names are not unique, thus the API forces you to supply the lookup.
     * The names are searched in the order of the map, thus it is strongly recommended
     * to use a {@code LinkedHashMap} or similar.
     *
     * @param parseLookup  the table of names, not null
     * @return this DateTimeFormatterBuilder, for chaining
     */
//    public DateTimeFormatterBuilder appendTimeZoneName(Map<String, DateTimeZone> parseLookup) {
//        TimeZoneName pp = new TimeZoneName(TimeZoneName.LONG_NAME, parseLookup);
//        return append0(pp, pp);
//    }

    /**
     * Instructs the printer to emit a short locale-specific time zone name.
     * Using this method prevents parsing, because time zone names are not unique.
     * See {@link #appendTimeZoneShortName(Map)}.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendTimeZoneShortName() {
        return appendX("zzz");
    }

    /**
     * Instructs the printer to emit a short locale-specific time zone
     * name, providing a lookup for parsing.
     * Time zone names are not unique, thus the API forces you to supply the lookup.
     * The names are searched in the order of the map, thus it is strongly recommended
     * to use a {@code LinkedHashMap} or similar.
     *
     * @param parseLookup  the table of names, null to use the {@link DateTimeUtils#getDefaultTimeZoneNames() default names}
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendTimeZoneShortName(Map<String, DateTimeZone> parseLookup) {
        return appendX("zzz");

    }

    /**
     * Instructs the printer to emit the identifier of the time zone.
     * From version 2.0, this field can be parsed.
     *
     * @return this DateTimeFormatterBuilder, for chaining
     */
    public DateTimeFormatterBuilder appendTimeZoneId() {
        return appendX("V");
    }

    /**
     * Instructs the printer to emit text and numbers to display time zone
     * offset from UTC. A parser will use the parsed time zone offset to adjust
     * the datetime.
     * <p>
     * If zero offset text is supplied, then it will be printed when the zone is zero.
     * During parsing, either the zero offset text, or the offset will be parsed.
     *
     * @param zeroOffsetText  the text to use if time zone offset is zero. If
     * null, offset is always shown.
     * @param showSeparators  if true, prints ':' separator before minute and
     * second field and prints '.' separator before fraction field.
     * @param minFields  minimum number of fields to print, stopping when no
     * more precision is required. 1=hours, 2=minutes, 3=seconds, 4=fraction
     * @param maxFields  maximum number of fields to print
     * @return this DateTimeFormatterBuilder, for chaining
     */
//    public DateTimeFormatterBuilder appendTimeZoneOffset(
//        String zeroOffsetText, boolean showSeparators,
//        int minFields, int maxFields) {
//        if()
//        return appendPattern()
//    }

    /**
     * Instructs the printer to emit text and numbers to display time zone
     * offset from UTC. A parser will use the parsed time zone offset to adjust
     * the datetime.
     * <p>
     * If zero offset print text is supplied, then it will be printed when the zone is zero.
     * If zero offset parse text is supplied, then either it or the offset will be parsed.
     *
     * @param zeroOffsetPrintText  the text to print if time zone offset is zero. If
     * null, offset is always shown.
     * @param zeroOffsetParseText  the text to optionally parse to indicate that the time
     * zone offset is zero. If null, then always use the offset.
     * @param showSeparators  if true, prints ':' separator before minute and
     * second field and prints '.' separator before fraction field.
     * @param minFields  minimum number of fields to print, stopping when no
     * more precision is required. 1=hours, 2=minutes, 3=seconds, 4=fraction
     * @param maxFields  maximum number of fields to print
     * @return this DateTimeFormatterBuilder, for chaining
     * @since 2.0
     */
    public DateTimeFormatterBuilder appendTimeZoneOffset(
        String zeroOffsetPrintText, String zeroOffsetParseText, boolean showSeparators,
        int minFields, int maxFields) {
        if(showSeparators){
            return multiply("X",2);
        }
        return multiply("X",3);
    }
}
