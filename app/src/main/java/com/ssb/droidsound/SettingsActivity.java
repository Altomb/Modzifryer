package com.ssb.droidsound;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.ssb.droidsound.database.SongDatabase;
import com.ssb.droidsound.plugins.DroidSoundPlugin;
import com.ssb.droidsound.plugins.SidplayPlugin;
import com.ssb.droidsound.plugins.VICEPlugin;
import com.ssb.droidsound.utils.Log;
import com.ssb.droidsound.utils.SystemBars;

/**
 * Host for the settings UI.
 *
 * <p>The platform's {@code android.preference} framework was removed from the SDK, so the
 * preference tree now lives in a {@link PreferenceFragmentCompat} instead of being inflated
 * directly by a {@code PreferenceActivity}.
 */
public class SettingsActivity extends FragmentActivity {

	protected static final String TAG = SettingsActivity.class.getSimpleName();
	private SongDatabase songDatabase;
	private boolean doFullScan;
	private SharedPreferences prefs;
	private String modsDir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		DroidSoundPlugin.setContext(getApplicationContext());

		songDatabase = PlayerActivity.songDatabase;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		modsDir = prefs.getString("modsDir", null);

		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
.replace(android.R.id.content, new SettingsFragment())
				.commit();
		}

		// Edge-to-edge is enforced at this targetSdk, so the preference list needs
		// padding to keep the first and last rows clear of the system bars.
		SystemBars.padForSystemBars(findViewById(android.R.id.content));
	}

	/**
	 * Inflates the preference tree and wires the plugin option listeners.
	 */
	public static class SettingsFragment extends PreferenceFragmentCompat {

		private SongDatabase songDatabase;
		private boolean doFullScan;
		private SharedPreferences prefs;
		private String modsDir;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			// The host activity owns the dialog state; keep a reference for the
			// scan/rescan confirmations below.
			SettingsActivity host = (SettingsActivity) requireActivity();
			songDatabase = host.songDatabase;
			doFullScan = host.doFullScan;
			modsDir = host.modsDir;
			prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
		}

		class AudiopPrefsListener implements Preference.OnPreferenceChangeListener {

			private DroidSoundPlugin plugin;

			AudiopPrefsListener(DroidSoundPlugin pi) {
				plugin = pi;
			}

			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				String k = preference.getKey();
				String k2 = k.substring(k.indexOf('.') + 1);

				Log.d(TAG, "CHANGED " + k);

				if (k.equals("SidPlugin.sidengine")) {
					boolean isVice = ((String) newValue).startsWith("VICE");
					/* FIXME: Both sid model and resampling actually could be done
					 * also in sidplayplugin, but it's not currently supported. */
					Preference pp = findPreference("SidPlugin.filter_bias");
					if (pp != null) pp.setEnabled(isVice);
					pp = findPreference("SidPlugin.sid_model");
					if (pp != null) pp.setEnabled(isVice);
					pp = findPreference("SidPlugin.resampling");
					if (pp != null) pp.setEnabled(isVice);
				}

				if (newValue instanceof String) {
					try {
						int i = Integer.parseInt((String) newValue);
						newValue = Integer.valueOf(i);
					} catch (NumberFormatException e) {
					}
				}

				plugin.setOption(k2, newValue);
				return true;
			}
		}

		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			setPreferencesFromResource(R.xml.preferences, rootKey);

			prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());

			String s = prefs.getString("SidPlugin.sidengine", null);
			Preference p = findPreference("SidPlugin.resampling");
			if (s != null && p != null) {
				p.setEnabled(!s.startsWith("Sidplay"));
			}

			Preference pref = findPreference("rescan_pref");
			pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Log.d(TAG, "Rescan database");
					getActivity().showDialog(R.string.scan_db);
					return true;
				}
			});

			pref = findPreference("help_pref");
			pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					startActivity(new Intent(getActivity(), HelpActivity.class));
					return true;
				}
			});

			pref = findPreference("download_link");
			pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(Intent.ACTION_VIEW,
							Uri.parse("http://swimsuitboys.com/droidsound/dl/"));
					startActivity(intent);
					return true;
				}
			});

			PackageInfo pinfo = null;
			try {
				pinfo = getActivity().getPackageManager()
						.getPackageInfo(getActivity().getPackageName(), 0);
			} catch (NameNotFoundException e) {
			}

			List<DroidSoundPlugin> list = DroidSoundPlugin.createPluginList();
			String appName = getString(pinfo.applicationInfo.labelRes);

			PreferenceScreen aScreen = (PreferenceScreen) findPreference("audio_prefs");

			for (int i = 0; i < aScreen.getPreferenceCount(); i++) {
				p = aScreen.getPreference(i);
				Log.d(TAG, "Pref '%s'", p.getKey());
				if (p instanceof PreferenceGroup) {
					PreferenceGroup pg = (PreferenceGroup) p;

					DroidSoundPlugin plugin = null;
					for (DroidSoundPlugin pl : list) {
						if (pl.getClass().getSimpleName().equals(pg.getKey())) {
							plugin = pl;
							break;
						}
					}
					if (plugin != null) {
						for (int j = 0; j < pg.getPreferenceCount(); j++) {
							p = pg.getPreference(j);

							Log.d(TAG, "Pref %s for %s", p.getKey(), plugin.getClass().getName());

							p.setOnPreferenceChangeListener(new AudiopPrefsListener(plugin));
						}
					}
				}
			}

			list.add(new SidplayPlugin());
			list.add(new VICEPlugin());

			PreferenceScreen abScreen = (PreferenceScreen) findPreference("about_prefs");

			if (abScreen != null) {
				PreferenceCategory pc = new PreferenceCategory(requireContext());
				pc.setTitle("Droidsound");
				abScreen.addPreference(pc);

				p = new Preference(requireContext());
				p.setTitle("Application");
				p.setSummary(String.format("%s v%s\n(C) 2010-2012 by Jonas Minnberg (Sasq)",
						appName, pinfo.versionName));
				abScreen.addPreference(p);

				pc = new PreferenceCategory(requireContext());
				pc.setTitle("Plugins");
				abScreen.addPreference(pc);

				for (DroidSoundPlugin pl : list) {
					String info = pl.getVersion();
					if (info != null) {
						p = new Preference(requireContext());
						p.setTitle(pl.getClass().getSimpleName());
						p.setSummary(info);
						abScreen.addPreference(p);
					}
				}

				pc = new PreferenceCategory(requireContext());
				pc.setTitle("Other");
				abScreen.addPreference(pc);

				p = new Preference(requireContext());
				p.setTitle("Icons");
				p.setSummary("G-Flat SVG by poptones");
				abScreen.addPreference(p);

				p = new Preference(requireContext());
				p.setTitle("ViewPageIndicator");
				p.setSummary("by Jake Wharton");
				abScreen.addPreference(p);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		// Resource ids are not compile-time constants under AGP 8+, so these
		// dispatches use if/else rather than `case R.string.x:`.
		if (id == R.string.recreate_confirm) {
			builder.setMessage(id);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					songDatabase.rescan(modsDir);
					finish();
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
		} else if (id == R.string.scan_db) {
			doFullScan = false;
			builder.setTitle(id);
			builder.setMultiChoiceItems(R.array.scan_opts, null, new OnMultiChoiceClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which, boolean isChecked) {
					Log.d(TAG, "%d %s", which, String.valueOf(isChecked));
					doFullScan = isChecked;
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
					if (doFullScan) {
						showDialog(R.string.recreate_confirm);
					} else {
						songDatabase.scan(true, modsDir);
						finish();
					}
				}
			});
		} else {
			builder.setMessage(id);
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
		}

		AlertDialog alert = builder.create();
		return alert;
	}

}
