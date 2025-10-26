/*
MIT License

Copyright (c) 2016-2023, Openkoda CDX Sp. z o.o. Sp. K. <openkoda.com>

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice
shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR
A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

package com.openkoda.core.helper;

import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * Provides static date and time formatting, conversion, and arithmetic utilities for java.util.Date and java.time.LocalDateTime.
 * <p>
 * This helper class centralizes date formatting operations with locale-aware methods,
 * primarily using English locale for consistent formatting across the application.
 * All date arithmetic methods operate on the current system time.
 * </p>
 * <p>
 * Thread-Safety: This class allocates SimpleDateFormat instances per method call to avoid
 * thread-safety issues with shared mutable formatters. All static methods are safe for
 * concurrent use without external synchronization.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * String formatted = DatesHelper.formatDateTimeLocaleEN(new Date());
 * LocalDateTime future = DatesHelper.getDatePlusMonthsFromCurrent(3);
 * </pre>
 * </p>
 *
 * @author Arkadiusz Drysch (adrysch@stratoflow.com)
 * @author OpenKoda Team
 * @version 1.7.1
 * @since 1.7.1
 */
@Component("dates")
public class DatesHelper {

    /**
     * Formats a java.util.Date to a string with date and time in English locale.
     * <p>
     * Uses the pattern "dd MMM yyyy HH:mm" (e.g., "15 Jan 2024 14:30").
     * </p>
     *
     * @param date the date to format, must not be null
     * @return formatted string with date and time in English locale
     */
    public static String formatDateTimeLocaleEN(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ENGLISH);
        return dateFormat.format(date);
    }

    /**
     * Formats a java.util.Date to a string with date only (no time) in English locale.
     * <p>
     * Uses the pattern "dd MMM yyyy" (e.g., "15 Jan 2024").
     * </p>
     *
     * @param date the date to format, must not be null
     * @return formatted string with date only in English locale
     */
    public static String formatDateLocaleEN(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        return dateFormat.format(date);
    }

    /**
     * Formats a LocalDateTime to a string with date and time in English locale.
     * <p>
     * Uses the pattern "dd MMM yyyy HH:mm" (e.g., "15 Jan 2024 14:30").
     * </p>
     *
     * @param date the LocalDateTime to format, must not be null
     * @return formatted string with date and time in English locale
     */
    public static String formatDateTimeLocaleEN(LocalDateTime date) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);
        return date.format(dateFormat);
    }

    /**
     * Formats a LocalDateTime to a string with date and time including seconds in English locale.
     * <p>
     * Uses the pattern "dd-MM-yyyy HH:mm:ss" (e.g., "15-01-2024 14:30:45").
     * </p>
     *
     * @param date the LocalDateTime to format, must not be null
     * @return formatted string with date and time including seconds in English locale
     */
    public static String formatDateTimeEN(LocalDateTime date) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH);
        return date.format(dateFormat);
    }

    /**
     * Formats a LocalDateTime to a string with date using full month name in English locale.
     * <p>
     * Uses the pattern "dd MMMM yyyy" (e.g., "15 January 2024").
     * </p>
     *
     * @param date the LocalDateTime to format, must not be null
     * @return formatted string with date using full month name in English locale
     */
    public static String formatDateWithFullMonthNameLocaleEN(LocalDateTime date) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
        return date.format(dateFormat);
    }

    /**
     * Formats a LocalDateTime to a string with date and time using localized full month name.
     * <p>
     * Uses the pattern "dd MMMM yyyy HH:mm" with the specified locale.
     * Month name is localized according to the provided language tag.
     * </p>
     *
     * @param date the LocalDateTime to format, must not be null
     * @param languageTag the language tag for locale (e.g., "en" for English, "pl" for Polish)
     * @return formatted string with date and time using localized full month name
     */
    public static String formatDateTimeWithFullMonthName(LocalDateTime date, String languageTag) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm", Locale.forLanguageTag(languageTag));
        return date.format(dateFormat);
    }

    /**
     * Formats a LocalDateTime to a string with date using localized full month name.
     * <p>
     * Uses the pattern "dd MMMM yyyy" with the specified locale.
     * Month name is localized according to the provided language tag.
     * </p>
     *
     * @param date the LocalDateTime to format, must not be null
     * @param languageTag the language tag for locale (e.g., "en" for English, "pl" for Polish)
     * @return formatted string with date using localized full month name
     */
    public static String formatDateWithFullMonthName(LocalDateTime date, String languageTag) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.forLanguageTag(languageTag));
        return date.format(dateFormat);
    }

    /**
     * Formats a LocalDateTime to a string with date only (no time) in English locale.
     * <p>
     * Uses the pattern "dd MMM yyyy" (e.g., "15 Jan 2024").
     * </p>
     *
     * @param date the LocalDateTime to format, must not be null
     * @return formatted string with date only in English locale
     */
    public static String formatDateLocaleEN(LocalDateTime date) {
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
        return date.format(dateFormat);
    }

    /**
     * Calculates a new LocalDateTime by adding or subtracting months from the current date and time.
     * <p>
     * Uses LocalDateTime.now() to get the current system date and time, then adds the specified
     * number of months. Negative values subtract months.
     * </p>
     *
     * @param months the number of months to add (can be negative for subtraction)
     * @return a new LocalDateTime representing the current time plus the specified months
     */
    public static LocalDateTime getDatePlusMonthsFromCurrent(long months) {
        return LocalDateTime.now().plusMonths(months);
    }

    /**
     * Calculates a new LocalDateTime by adding or subtracting days from the current date and time.
     * <p>
     * Uses LocalDateTime.now() to get the current system date and time, then adds the specified
     * number of days. Negative values subtract days.
     * </p>
     *
     * @param days the number of days to add (can be negative for subtraction)
     * @return a new LocalDateTime representing the current time plus the specified days
     */
    public static LocalDateTime getDatePlusDaysFromCurrent(long days) {
        return LocalDateTime.now().plusDays(days);
    }

    /**
     * Converts seconds since Unix epoch to a LocalDateTime in the system default timezone.
     * <p>
     * Creates an Instant from the epoch seconds, converts it to the system default timezone,
     * and returns a LocalDateTime. The timezone used depends on the system configuration
     * (ZoneId.systemDefault()).
     * </p>
     *
     * @param seconds the number of seconds since Unix epoch (January 1, 1970 00:00:00 UTC)
     * @return a LocalDateTime representing the given epoch seconds in system default timezone
     */
    public static LocalDateTime secondsToLocalDateTime(long seconds) {
        return Instant.ofEpochSecond(seconds).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
