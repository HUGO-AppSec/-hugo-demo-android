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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.nononsenseapps.notepad.R;
import com.nononsenseapps.notepad.prefs.AppearancePrefs;

import java.util.Locale;

/**
 * Contains helper methods for activities
 */
public final class ActivityHelper {

	// TODO everything in this "helpers" namespace could be moved to its own
	//  gradle module. This would speed up builds, but maybe it's harder to manage?

	// forbid instances: it's a static class
	private ActivityHelper() {}

	public static void readAndSetSettings(Activity activity) {
		// Read settings and set
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);

		final String theme = prefs.getString(AppearancePrefs.KEY_THEME,
				activity.getString(R.string.const_theme_light_ab));
		if (activity.getString(R.string.const_theme_light_ab).equals(theme)) {
			activity.setTheme(R.style.ThemeNnnLight);
		} else if (activity.getString(R.string.const_theme_black).equals(theme)) {
			activity.setTheme(R.style.ThemeNnnPitchBlack);
		} else if (activity.getString(R.string.const_theme_classic).equals(theme)) {
			activity.setTheme(R.style.ThemeNnnClassicLight);
		} else // if (theme.equals(getResources().getString(R.string.const_theme_googlenow_dark)))
		{
			activity.setTheme(R.style.ThemeNnnDark);
		}
// TODO move the part above to a ThemeHelper function

		// Set language
		Configuration config = activity.getResources().getConfiguration();

		String lang = prefs.getString(activity.getString(R.string.pref_locale), "");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if (lang == null || lang.isEmpty())
				locale = Locale.getDefault();
			else if (lang.length() == 5) {
				locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			} else {
				locale = new Locale(lang.substring(0, 2));
			}
			// Locale.setDefault(locale);
			config.locale = locale;
			activity.getResources()
					.updateConfiguration(config, activity.getResources().getDisplayMetrics());
		}

		if (activity instanceof OnSharedPreferenceChangeListener) {
			prefs.registerOnSharedPreferenceChangeListener(
					(OnSharedPreferenceChangeListener) activity);
		}
	}

	/**
	 * @return the users's default or selected locale
	 */
	public static Locale getUserLocale(Context context) {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String lang = prefs.getString(context.getString(R.string.pref_locale), null);
		final Locale locale;
		if (lang == null || lang.isEmpty())
			locale = Locale.getDefault();
		else if (lang.length() == 5) {
			locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
		} else {
			locale = new Locale(lang.substring(0, 2));
		}

		return locale;
	}

	/**
	 * Set configured locale on current context
	 */
	public static void setSelectedLanguage(@NonNull Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		Configuration config = context.getResources().getConfiguration();

		String lang = prefs.getString(context.getString(R.string.pref_locale), "");
		if (!config.locale.toString().equals(lang)) {
			Locale locale;
			if ("".equals(lang))
				locale = Locale.getDefault();
			else if (lang.length() == 5) {
				locale = new Locale(lang.substring(0, 2), lang.substring(3, 5));
			} else {
				locale = new Locale(lang.substring(0, 2));
			}
			// Locale.setDefault(locale);
			config.locale = locale;
			context.getResources().updateConfiguration(config,
					context.getResources().getDisplayMetrics());
		}
	}
}
