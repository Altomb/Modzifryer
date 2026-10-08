package com.ssb.droidsound.utils;

import java.io.File;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

/**
 * Central placement of all storage locations. Needs a Context; call
 * {@link #init(Context)} once from DroidSoundApplication.onCreate(),
 * everything else derives paths from it.
 */
public class Storage {

	private static Context appContext;

	private Storage() {
	}

	/** Called from {@code DroidSoundApplication.onCreate()}. */
	public static void init(Context context) {
		appContext = context.getApplicationContext();
	}

	private static Context requireContext() {
		if (appContext == null) {
			throw new IllegalStateException("Storage.init() was not called; DroidSoundApplication must run first");
		}
		return appContext;
	}

	/**
	 * True when the app may write to the shared root of external storage. Only
	 * the case before scoped storage, i.e. API 28 and below.
	 */
	public static boolean hasSharedStorageAccess() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
	}

	/**
	 * Root the app may write to: shared external storage when allowed,
	 * app-specific external storage otherwise.
	 */
	public static File getWritableRoot() {
		if (hasSharedStorageAccess()) {
			return Environment.getExternalStorageDirectory();
		}
		File dir = requireContext().getExternalFilesDir(null);
		if (dir == null) {
			// No external volume mounted. Internal storage always works.
			return requireContext().getFilesDir();
		}
		return dir;
	}

	/**
	 * The {@code droidsound} working directory: song database, playlists,
	 * themes, plugin data, caches.
	 */
	public static File getDroidsoundDir() {
		return ensure(new File(getWritableRoot(), "droidsound"));
	}

	/**
	 * Default location searched for chip music, {@code MODS} under the
	 * writable root. PlayerActivity lets the user override this via prefs.
	 */
	public static File getDefaultModsDir() {
		return ensure(new File(getWritableRoot(), "MODS"));
	}

	/** A named sub-directory of the droidsound working directory. */
	public static File getDroidsoundSubDir(String name) {
		return ensure(new File(getDroidsoundDir(), name));
	}

	/** Creates the directory if needed and returns it. */
	public static File ensure(File dir) {
		if (!dir.exists())
			dir.mkdirs();
		return dir;
	}

}
