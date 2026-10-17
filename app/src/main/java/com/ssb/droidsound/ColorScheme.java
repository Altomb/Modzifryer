package com.ssb.droidsound;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.preference.PreferenceManager;

/**
 * Reads the user's interface color scheme preference and helps apply it.
 *
 * <p>The player is themed at runtime from a fake-CSS asset (see
 * {@link ThemeManager}), so the scheme mainly selects which CSS asset to load.
 * The night mode of the resource configuration is forced as well, so the
 * platform widgets and the settings screen follow the same choice.
 *
 * <p>The preference has three values:
 * <ul>
 *   <li>{@link #SYSTEM} (default) - follow the Android day/night setting,</li>
 *   <li>{@link #DARK} - the original white-on-black scheme,</li>
 *   <li>{@link #LIGHT} - black text on a white background.</li>
 * </ul>
 */
public final class ColorScheme {

	/** Key of the {@code ListPreference} in {@code res/xml/preferences.xml}. */
	public static final String PREF_KEY = "color_scheme";

	public static final String SYSTEM = "system";
	public static final String DARK = "dark";
	public static final String LIGHT = "light";

	private ColorScheme() {
	}

	/** Returns the raw preference value, never {@code null}. */
	public static String get(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String scheme = prefs.getString(PREF_KEY, SYSTEM);
		return scheme != null ? scheme : SYSTEM;
	}

	/** True when the current configuration is in night (dark) mode. */
	public static boolean isNight(Context context) {
		Configuration config = context.getResources().getConfiguration();
		return (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
	}

	/** True when the interface should use the black-on-white colors. */
	public static boolean isLight(Context context) {
		String scheme = get(context);
		if (LIGHT.equals(scheme)) {
			return true;
		}
		if (DARK.equals(scheme)) {
			return false;
		}
		return !isNight(context);
	}

	/**
	 * Wraps a base context so that it resolves resources for the selected
	 * scheme. With the "system" scheme the context is returned untouched, which
	 * keeps the normal day/night resource resolution.
	 */
	public static Context wrap(Context newBase) {
		String scheme = get(newBase);

		int night;
		if (LIGHT.equals(scheme)) {
			night = Configuration.UI_MODE_NIGHT_NO;
		} else if (DARK.equals(scheme)) {
			night = Configuration.UI_MODE_NIGHT_YES;
		} else {
			return newBase;
		}

		Configuration config = new Configuration(newBase.getResources().getConfiguration());
		config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
		return newBase.createConfigurationContext(config);
	}
}
