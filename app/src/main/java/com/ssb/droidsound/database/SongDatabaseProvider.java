package com.ssb.droidsound.database;

import android.content.Context;
import android.os.Environment;

import com.ssb.droidsound.utils.Log;

/**
 * Process-wide, lazily created {@link SongDatabase} shared by the UI
 * ({@code PlayerActivity}) and by the Android Auto {@code MediaBrowserService}.
 *
 * <p>The database runs on its own thread and is expensive to build, so it must
 * only ever be instantiated once per process. Both entry points go through this
 * provider instead of constructing their own copy: two instances would fight
 * over the same SQLite file and scan the collection twice.
 */
public final class SongDatabaseProvider {

	private static final String TAG = SongDatabaseProvider.class.getSimpleName();

	private static SongDatabase instance;

	private SongDatabaseProvider() {
	}

	/** Returns the shared database, creating it on first use. */
	public static synchronized SongDatabase get(Context context) {
		if (instance != null) {
			return instance;
		}

		Log.d(TAG, "Creating SongDatabase");

		Context app = context.getApplicationContext();
		SongDatabase db = new SongDatabase(app);
		// Publish before waiting so a second caller reuses the same instance.
		instance = db;

		CSDBParser csdb = new CSDBParser();
		db.registerDataSource(CSDBParser.DUMP_NAME, csdb);
		db.registerDataSource(CSDBParser.DUMP_NAME + ".ZIP", csdb);

		db.registerDataSource(MediaSource.NAME, new MediaSource(app));

		FileSystemSource.BasePath = Environment.getExternalStorageDirectory().getPath();
		db.registerDataSource(FileSystemSource.NAME, new FileSystemSource());

		Thread thread = new Thread(db, "SongDatabase");
		thread.start();

		// Wait for the thread to come up *and* for the first open() to finish,
		// so a query right after get() cannot race it. Bounded: a broken
		// database must not hang the caller's main thread.
		long deadline = System.currentTimeMillis() + 5000;
		while ((!db.isReady() || !db.isOpen()) && System.currentTimeMillis() < deadline) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		if (!db.isOpen()) {
			Log.w(TAG, "Database still not open after 5s; queries may return empty");
		}

		Log.d(TAG, "Created");
		return instance;
	}

	/** Shuts the shared database down; the next {@link #get} builds a new one. */
	public static synchronized void quit() {
		if (instance != null) {
			instance.quit();
			instance = null;
		}
	}
}
