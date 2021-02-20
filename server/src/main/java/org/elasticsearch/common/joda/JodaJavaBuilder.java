package org.elasticsearch.common.joda;

import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class JodaJavaBuilder/* extends DateTimeFormatterBuilder*/ {
    List<StringBuilder> patterns = new ArrayList<>();
    {
        patterns.add(new StringBuilder());
    }

    public JodaJavaBuilder appendPattern(String pattern) {
        CustomDateTimeFormat.parsePatternTo(this, pattern);
        return this;
    }

    public String getJavaPattern() {
        StringJoiner result = new StringJoiner("||");
        for (StringBuilder pattern : patterns) {
            result.add(pattern.toString());
        }
        return result.toString();
    }


    private JodaJavaBuilder appendToPattern(String text) {
        for (StringBuilder pattern : patterns) {
            pattern.append(text);
        }
        return this;
    }

    private StringBuilder multiply(String text, int times) {
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<times;i++){
            sb.append(text);
        }
        return sb;
    }

    private JodaJavaBuilder split(String ... toAdd) {
        List<StringBuilder> newPatterns = new ArrayList<>();
        for (StringBuilder pattern : patterns) {
            for (String suff : toAdd) {
                StringBuilder newBuilder = new StringBuilder(pattern);
                newBuilder.append(suff);
                newPatterns.add(newBuilder);
            }
        }
        patterns = newPatterns;
        return this;
    }

    //-----------------------------------------------------------------------
    /**
     * Instructs the printer to emit a specific character, and the parser to
     * expect it. The parser is case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendLiteral(char c) {
       return appendToPattern(""+c);
    }

    /**
     * Instructs the printer to emit specific text, and the parser to expect
     * it. The parser is case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     * @throws IllegalArgumentException if text is null
     */
    public JodaJavaBuilder appendLiteral(String text) {
        return appendToPattern(text);
    }

    /**
     * Appends the print/parse of a fractional second.
     * <p>
     * This reliably handles the case where fractional digits are being handled
     * beyond a visible decimal point. The digits parsed will always be treated
     * as the most significant (numerically largest) digits.
     * Thus '23' will be parsed as 230 milliseconds.
     * This method does not print or parse the decimal point itself.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to print or parse
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendFractionOfSecond(int minDigits, int maxDigits) {
        StringBuilder sb = multiply("S", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric secondOfMinute field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendSecondOfMinute(int minDigits) {
        StringBuilder sb = multiply("s", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric minuteOfHour field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendMinuteOfHour(int minDigits) {
        StringBuilder sb = multiply("m", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric hourOfDay field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendHourOfDay(int minDigits) {
        StringBuilder sb = multiply("H", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric clockhourOfDay field.
     *
     * @param minDigits minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendClockhourOfDay(int minDigits) {
        StringBuilder sb = multiply("k", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric hourOfHalfday field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendHourOfHalfday(int minDigits) {
        StringBuilder sb = multiply("K", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric clockhourOfHalfday field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendClockhourOfHalfday(int minDigits) {
        StringBuilder sb = multiply("h", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric dayOfWeek field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendDayOfWeek(int minDigits) {
        StringBuilder sb = multiply("e", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric dayOfMonth field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendDayOfMonth(int minDigits) {
        ArrayList<String> list = new ArrayList<>();
        if(minDigits == 1){
            list.add("d");
        }else if (minDigits == 2){
            list.add("d");
            list.add("dd");
        }else{
            list.add("d");
            list.add("dd");
            StringBuilder sb = new StringBuilder();
            for(int i=3;i<=minDigits;i++){
                sb.append("0");
                list.add("'"+sb+"'dd");
            }
        }
        split(list.toArray(new String[]{}));
        return this;
    }

    /**
     * Instructs the printer to emit a numeric dayOfYear field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendDayOfYear(int minDigits) {
        StringBuilder sb = multiply("D", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric weekOfWeekyear field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendWeekOfWeekyear(int minDigits) {
        StringBuilder sb = multiply("w", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric weekyear field.
     *
     * @param minDigits  minimum number of digits to <i>print</i>
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendWeekyear(int minDigits, int maxDigits) {
        StringBuilder sb = multiply("Y", minDigits);
        return appendToPattern(sb.toString());
    }

    /**
     * Instructs the printer to emit a numeric monthOfYear field.
     *
     * @param minDigits  minimum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendMonthOfYear(int minDigits) {
        return appendToPattern("MM");
    }

    /**
     * Instructs the printer to emit a numeric year field.
     *
     * @param minDigits  minimum number of digits to <i>print</i>
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendYear(int minDigits, int maxDigits) {
        StringBuilder sb = multiply("u", minDigits);
        return appendToPattern(sb.toString());
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
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendTwoDigitYear(int pivot) {
        return appendToPattern("uu");
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
     * @return this JodaJavaBuilder, for chaining
     * @since 1.1
     */
    public JodaJavaBuilder appendTwoDigitYear(int pivot, boolean lenientParse) {
        return appendToPattern("uu");
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
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendTwoDigitWeekyear(int pivot) {
       return appendToPattern("YY");
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
     * @return this JodaJavaBuilder, for chaining
     * @since 1.1
     */
    public JodaJavaBuilder appendTwoDigitWeekyear(int pivot, boolean lenientParse) {
        return appendToPattern("YY");
    }

    /**
     * Instructs the printer to emit a numeric yearOfEra field.
     *
     * @param minDigits  minimum number of digits to <i>print</i>
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendYearOfEra(int minDigits, int maxDigits) {
        StringBuilder sb = multiply("y", minDigits);
        return appendToPattern(sb.toString());

    }

    /**
     * Instructs the printer to emit a numeric century of era field.
     *
     * @param minDigits  minimum number of digits to print
     * @param maxDigits  maximum number of digits to <i>parse</i>, or the estimated
     * maximum number of digits to print
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendCenturyOfEra(int minDigits, int maxDigits) {
        throw new UnsupportedOperationException("Century of era not supported");
    }

    /**
     * Instructs the printer to emit a locale-specific AM/PM text, and the
     * parser to expect it. The parser is case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendHalfdayOfDayText() {
        return appendToPattern("a");
    }

    /**
     * Instructs the printer to emit a locale-specific dayOfWeek text. The
     * parser will accept a long or short dayOfWeek text, case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendDayOfWeekText() {
        return split("cccc","ccc");
    }

    /**
     * Instructs the printer to emit a short locale-specific dayOfWeek
     * text. The parser will accept a long or short dayOfWeek text,
     * case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendDayOfWeekShortText() {
        return split("cccc","ccc");
    }

    /**
     * Instructs the printer to emit a short locale-specific monthOfYear
     * text. The parser will accept a long or short monthOfYear text,
     * case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendMonthOfYearText() {
        return appendToPattern("MMMM");
    }

    /**
     * Instructs the printer to emit a locale-specific monthOfYear text. The
     * parser will accept a long or short monthOfYear text, case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendMonthOfYearShortText() {
        return appendToPattern("MMM");
    }

    /**
     * Instructs the printer to emit a locale-specific era text (BC/AD), and
     * the parser to expect it. The parser is case-insensitive.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendEraText() {
        return appendToPattern("G");
    }

    /**
     * Instructs the printer to emit a locale-specific time zone name.
     * Using this method prevents parsing, because time zone names are not unique.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendTimeZoneName() {
        return appendToPattern("zzzz");
    }

    /**
     * Instructs the printer to emit a short locale-specific time zone name.
     * Using this method prevents parsing, because time zone names are not unique.
     * See {@link #appendTimeZoneShortName(Map)}.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendTimeZoneShortName() {
        return appendToPattern("zzz");
    }

    /**
     * Instructs the printer to emit a short locale-specific time zone
     * name, providing a lookup for parsing.
     * Time zone names are not unique, thus the API forces you to supply the lookup.
     * The names are searched in the order of the map, thus it is strongly recommended
     * to use a {@code LinkedHashMap} or similar.
     *
     * @param parseLookup  the table of names, null to use the {@link DateTimeUtils#getDefaultTimeZoneNames() default names}
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendTimeZoneShortName(Map<String, DateTimeZone> parseLookup) {
        return appendToPattern("zzz");
    }

    /**
     * Instructs the printer to emit the identifier of the time zone.
     * From version 2.0, this field can be parsed.
     *
     * @return this JodaJavaBuilder, for chaining
     */
    public JodaJavaBuilder appendTimeZoneId() {
        return appendToPattern("VV");
    }


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
     * @return this JodaJavaBuilder, for chaining
     * @since 2.0
     */
    public JodaJavaBuilder appendTimeZoneOffset(
        String zeroOffsetPrintText, String zeroOffsetParseText, boolean showSeparators,
        int minFields, int maxFields) {
        if(showSeparators){
            return split("XXXXX","XXX","X"); //+01:02:03(XXXXX) or +01:02 (XX) or +01(X)
        }
        return split("XXXX","X");//+010203(XXXX) or +0102(X allows this) or +01 (X)
    }
}
