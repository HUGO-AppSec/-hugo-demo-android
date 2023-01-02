/*
 * Copyright (c) 2015 Jonas Kalderstam.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nononsenseapps.helpers;

import android.content.Context;

import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * A class that helps with displaying locale and preference specific dates
 */
public final class TimeFormatter {

	public static final String WEEKDAY_SHORTEST_FORMAT = "E";

	public static Locale getLocale(final String lang) {
		final Locale locale;
		if (lang == null || lang.isEmpty()) {
			locale = Locale.getDefault();
		} else if (lang.length() == 5) {
			locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
		} else {
			locale = new Locale(lang.substring(0, 2));
		}
		return locale;
	}

	/**
	 * Formats date according to the designated locale
	 */
	public static String getLocalDateString(final Context context, final String lang,
											final String format, final long timeInMillis) {
		return getLocalFormatter(context, lang, format).format(
				new Date(timeInMillis));
	}

	/**
	 * Formats the date according to the locale the user has defined in settings
	 */
	public static String getLocalDateString(final Context context,
											final String format, final long timeInMillis) {
		return getLocalDateString(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				format, timeInMillis);
	}

	/**
	 * Dont use for performance critical settings
	 */
	public static String getLocalDateStringLong(final Context context,
												final long time) {
		return getLocalFormatterLong(context).format(new Date(time));
	}

	public static String getLocalDateOnlyStringLong(final Context context, final long time) {
		return getLocalFormatterLongDateOnly(context).format(new Date(time));
	}

	public static String getLocalTimeOnlyString(final Context context, final long time) {
		final String format;
		if (android.text.format.DateFormat.is24HourFormat(context)) {
			// 00:59
			format = "HH:mm";
		} else {
			// 12:59 am
			format = "h:mm a";
		}
		// like "it_IT"
		String localePrefVal = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(context.getString(R.string.pref_locale), "");
		return new SimpleDateFormat(format, getLocale(localePrefVal)).format(new Date(time));
	}

	/**
	 * Dont use for performance critical settings
	 */
	public static String getLocalDateStringShort(final Context context,
												 final long time) {
		return getLocalFormatterShort(context).format(new Date(time));
	}

	/**
	 * Replaces first "localtime" in format string to a time which respects
	 * global 24h setting.
	 */
	private static String withSuitableTime(final Context context, final String formatString) {
		if (android.text.format.DateFormat.is24HourFormat(context)) {
			// 00:59
			return formatString.replaceFirst("localtime", "HH:mm");
		} else {
			// 12:59 am
			return formatString.replaceFirst("localtime", "h:mm a");
		}
	}

	/**
	 * Removes "localtime" in format string
	 */
	private static String withSuitableDateOnly(final Context context,
											   final String formatString) {
		return formatString.replaceAll("\\s*localtime\\s*", " ");
	}

	/**
	 * @param lang if app is in japanese, it's "ja" and uses values-ja/strings.xml
	 */
	private static SimpleDateFormat getLocalFormatter(final Context context,
													  final String lang, final String format) {
		final Locale locale = getLocale(lang);
		SimpleDateFormat sdf;
		try {
			//noinspection UnusedAssignment
			sdf = new SimpleDateFormat(withSuitableTime(context, format), locale);
		} catch (IllegalArgumentException iae) {
			NnnLogger.error(TimeFormatter.class,
					"Error in translated date format strings. In: values-" + lang
							+ ", value: " + format);
			NnnLogger.exception(iae);
		} finally {
			// just log the error, but crash anyway
			sdf = new SimpleDateFormat(withSuitableTime(context, format), locale);
		}
		return sdf;
	}

	public static GregorianCalendar getLocalCalendar(final Context context) {
		var prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final Locale locale = getLocale(prefs
				.getString(context.getString(R.string.pref_locale), ""));
		return new GregorianCalendar(locale);
	}

	/**
	 * Good for performance critical situations, like lists
	 */
	public static SimpleDateFormat getLocalFormatterLong(final Context context) {
		return getLocalFormatter(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				withSuitableTime(
						context,
						PreferenceManager
								.getDefaultSharedPreferences(context)
								.getString(
										context.getString(R.string.key_pref_dateformat_long),
										context.getString(R.string.dateformat_long_1))));
	}

	public static SimpleDateFormat getDateFormatter(final Context context) {
		return getLocalFormatter(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				context.getString(R.string.dateformat_just_date));
	}

	public static SimpleDateFormat getLocalFormatterLongDateOnly(
			final Context context) {
		return getLocalFormatter(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				withSuitableDateOnly(
						context,
						PreferenceManager
								.getDefaultSharedPreferences(context)
								.getString(
										context.getString(R.string.key_pref_dateformat_long),
										context.getString(R.string.dateformat_long_1))));
	}

	/**
	 * Good for performance critical situations, like lists
	 */
	public static SimpleDateFormat getLocalFormatterShort(final Context context) {
		String userDateFormat = PreferenceManager
				.getDefaultSharedPreferences(context)
				.getString(context.getString(R.string.key_pref_dateformat_short),
						context.getString(R.string.dateformat_short_1));
		return getLocalFormatter(context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				withSuitableTime(context, userDateFormat)); // <-- notice this
	}

	public static SimpleDateFormat getLocalFormatterShortDateOnly(final Context context) {
		return getLocalFormatter(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				withSuitableDateOnly( // <-- notice this!
						context,
						PreferenceManager
								.getDefaultSharedPreferences(context)
								.getString(
										context.getString(R.string.key_pref_dateformat_short),
										context.getString(R.string.dateformat_short_1))));
	}

	public static SimpleDateFormat getLocalFormatterMicro(final Context context) {
		return getLocalFormatter(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				withSuitableTime(context,
						context.getString(R.string.dateformat_micro)));
	}

	/**
	 * Good for performance critical situations, like lists
	 */
	public static SimpleDateFormat getLocalFormatterWeekday(final Context context) {
		return getLocalFormatter(context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				context.getString(R.string.dateformat_weekday));
	}

	/**
	 * Good for performance critical situations, like lists
	 */
	public static SimpleDateFormat getLocalFormatterWeekdayShort(
			final Context context) {
		return getLocalFormatter(
				context,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(context.getString(R.string.pref_locale), ""),
				WEEKDAY_SHORTEST_FORMAT);
	}
}
