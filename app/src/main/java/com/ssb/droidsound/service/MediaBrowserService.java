package com.ssb.droidsound.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.media.MediaBrowserServiceCompat;
import androidx.preference.PreferenceManager;

import com.ssb.droidsound.PlayerActivity;
import com.ssb.droidsound.PlayerServiceConnection;
import com.ssb.droidsound.database.SongDatabase;
import com.ssb.droidsound.database.SongDatabaseProvider;
import com.ssb.droidsound.utils.Log;
import com.ssb.droidsound.utils.Storage;

/**
 * Exposes the app's media collection to Android Auto through the standard
 * {@code MediaBrowserService} contract, and drives playback through the
 * existing {@link PlayerService} (via {@link PlayerServiceConnection}).
 *
 * <p>The browsable tree mirrors the phone browser: the root is the
 * configured MODS directory (see {@code PlayerActivity.setupModsDir()}), whose
 * children are the same directories, archives, playlists, "Filesystem"/"CSDb"/
 * "Local Mediastore" data sources and song files the on-device list shows. This
 * works because the browsing code path is the very same
 * {@link SongDatabase#getFilesInPath} call the UI uses.
 *
 * <p>The one deliberate difference is the top-level order, which the car shows
 * Favorites-first and otherwise reversed (see {@link #reorderRoot}). Deeper
 * levels and the phone UI keep the database order.
 *
 * <p>Media IDs are the raw entry paths prefixed with {@link #BROWSE_PREFIX} or
 * {@link #PLAY_PREFIX} so that {@link #onLoadChildren} can tell browsable
 * containers from playable leaves without hitting the database again.
 */
public class MediaBrowserService extends MediaBrowserServiceCompat {

	private static final String TAG = MediaBrowserService.class.getSimpleName();

	/** Root of the browsable tree (the MODS directory). */
	private static final String ROOT_ID = "__root__";
	/** Prefix of a browsable container (directory, archive or playlist). */
	private static final String BROWSE_PREFIX = "b:";
	/** Prefix of a playable song. */
	private static final String PLAY_PREFIX = "p:";
	/** Lower-case suffix identifying the pinned favorites playlist. */
	private static final String FAVORITES_SUFFIX = "/favorites.plist";

	private static final long PLAYBACK_ACTIONS =
			PlaybackStateCompat.ACTION_PLAY
			| PlaybackStateCompat.ACTION_PAUSE
			| PlaybackStateCompat.ACTION_PLAY_PAUSE
			| PlaybackStateCompat.ACTION_STOP
			| PlaybackStateCompat.ACTION_SKIP_TO_NEXT
			| PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
			| PlaybackStateCompat.ACTION_SEEK_TO
			| PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;

	private Handler mainHandler;
	private Handler playerHandler;
	private MediaSessionCompat session;
	private PlayerServiceConnection player;

	private MediaMetadataCompat.Builder metadataBuilder;
	private int songState = 0;
	private long positionMs = 0;
	private long durationMs = 0;

	/**
	 * Maps a playable child media id to the browsable media id it was listed
	 * under, so {@link #onPlayFromMediaId} can rebuild the sibling play queue
	 * the same way tapping a file does on the phone. Bounded URL-style LRU.
	 */
	private final LinkedHashMap<String, String> itemParents =
			new LinkedHashMap<String, String>(64, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
					return size() > 4096;
				}
			};

	private final Handler.Callback playerCallback = new Handler.Callback() {
		@Override
		@SuppressWarnings("unchecked")
		public boolean handleMessage(Message msg) {
			if (msg.what == 999) {
				Log.d(TAG, "Player service disconnected");
				return true;
			}
			Object obj = msg.obj;
			if (obj instanceof Map) {
				updateSession((Map<String, Object>) obj, msg.what == 0);
				return true;
			}
			return false;
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();

		mainHandler = new Handler(Looper.getMainLooper());
		playerHandler = new Handler(Looper.getMainLooper(), playerCallback);

		session = new MediaSessionCompat(this, "DroidSound");
		// Explicit handler: transport callbacks must arrive on the main thread,
		// where the browser cache and the session are touched.
		session.setCallback(new SessionCallback(), mainHandler);
		// Tapping the "open app" affordance in the car opens the phone UI.
		Intent launch = new Intent(this, PlayerActivity.class);
		launch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		session.setSessionActivity(PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE));
		setSessionToken(session.getSessionToken());
		session.setActive(true);

		metadataBuilder = new MediaMetadataCompat.Builder();
		// Some hosts refuse to render a now-playing screen until a state exists.
		session.setPlaybackState(new PlaybackStateCompat.Builder()
				.setActions(PLAYBACK_ACTIONS)
				.setState(PlaybackStateCompat.STATE_NONE, 0, 0)
				.build());

		player = new PlayerServiceConnection();
		// Bind only: this may run in the background when Android Auto connects,
		// where startService() is restricted. BIND_AUTO_CREATE still starts the
		// player, which goes foreground on its own once playback begins.
		player.bindService(this, playerHandler, false);
	}

	@Override
	public void onDestroy() {
		if (player != null) {
			player.unbindService(this);
			player = null;
		}
		if (session != null) {
			session.setActive(false);
			session.release();
			session = null;
		}
		super.onDestroy();
	}

	@Override
	public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
		return new BrowserRoot(ROOT_ID, null);
	}

	@Override
	public void onLoadChildren(final String parentId, final Result<List<MediaBrowserCompat.MediaItem>> result) {
		// Run the (synchronous, shared) database access on the main thread, the
		// only thread that ever touches SongDatabase besides its own scanner.
		// This keeps the browse code path identical to the UI's.
		result.detach();
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				List<MediaBrowserCompat.MediaItem> items;
				try {
					items = loadChildren(parentId);
				} catch (RuntimeException e) {
					// A browse failure must not take the host down with us.
					Log.w(TAG, "loadChildren failed for " + parentId);
					items = Collections.emptyList();
				}
				try {
					result.sendResult(items);
				} catch (IllegalStateException e) {
					Log.d(TAG, "Result already discarded (host gone?)");
				}
			}
		});
	}

	private List<MediaBrowserCompat.MediaItem> loadChildren(String parentId) {
		String dirPath = pathForBrowseId(parentId);
		if (dirPath == null) {
			return Collections.emptyList();
		}

		SongDatabase db = SongDatabaseProvider.get(this);
		if (db == null) {
			return Collections.emptyList();
		}

		Cursor cursor = db.getFilesInPath(dirPath, SongDatabase.SORT_TITLE);
		if (cursor == null) {
			return Collections.emptyList();
		}

		List<MediaBrowserCompat.MediaItem> items = new ArrayList<MediaBrowserCompat.MediaItem>();
		try {
			int titleIdx = cursor.getColumnIndex("TITLE");
			int typeIdx = cursor.getColumnIndex("TYPE");
			int fileIdx = cursor.getColumnIndex("FILENAME");
			if (fileIdx < 0) {
				fileIdx = cursor.getColumnIndex("NAME");
			}
			int pathIdx = cursor.getColumnIndex("PATH");
			int composerIdx = cursor.getColumnIndex("COMPOSER");
			int subIdx = cursor.getColumnIndex("SUBTITLE");

			if (fileIdx < 0) {
				return items;
			}

			while (cursor.moveToNext()) {
				String fileName = cursor.getString(fileIdx);
				if (fileName == null) {
					continue;
				}

				int type = SongDatabase.TYPE_FILE;
				if (typeIdx >= 0) {
					type = cursor.getInt(typeIdx);
				}

				String rowPath = dirPath;
				if (pathIdx >= 0 && !cursor.isNull(pathIdx)) {
					String p = cursor.getString(pathIdx);
					if (p != null) {
						rowPath = p;
					}
				}
				if (rowPath.endsWith("/")) {
					rowPath = rowPath.substring(0, rowPath.length() - 1);
				}
				String fullPath = rowPath + "/" + fileName;

				String title = null;
				if (titleIdx >= 0) {
					title = cursor.getString(titleIdx);
				}
				if (title == null || title.length() == 0) {
					title = fileName;
				}

				String subtitle = null;
				if (subIdx >= 0) {
					subtitle = cursor.getString(subIdx);
				} else if (composerIdx >= 0) {
					subtitle = cursor.getString(composerIdx);
				}

				boolean browsable = type == SongDatabase.TYPE_DIR
						|| type == SongDatabase.TYPE_ARCHIVE
						|| type == SongDatabase.TYPE_PLIST;

				String mediaId = (browsable ? BROWSE_PREFIX : PLAY_PREFIX) + fullPath;

				MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
						.setMediaId(mediaId)
						.setTitle(title)
						.setSubtitle(subtitle)
						.build();

				int flags = browsable
						? MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
						: MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
				items.add(new MediaBrowserCompat.MediaItem(desc, flags));

				if (!browsable) {
					itemParents.put(mediaId, parentId);
				}
			}
		} finally {
			cursor.close();
		}

		// Android Auto shows the top level in its own order; the phone list is
		// untouched.
		return ROOT_ID.equals(parentId) ? reorderRoot(items) : items;
	}

	/**
	 * Top-level ordering for Android Auto: Favorites first, then every other
	 * entry in reverse of the order the phone displays it in (so loose files,
	 * then folders, then archives).
	 *
	 * <p>Used by {@link #loadChildren} itself rather than only for display, so
	 * the play queue built by {@link #playFromMediaId} matches what the car
	 * shows.
	 */
	private static List<MediaBrowserCompat.MediaItem> reorderRoot(
			List<MediaBrowserCompat.MediaItem> items) {
		List<MediaBrowserCompat.MediaItem> reversed = new ArrayList<MediaBrowserCompat.MediaItem>(items);
		Collections.reverse(reversed);

		List<MediaBrowserCompat.MediaItem> ordered = new ArrayList<MediaBrowserCompat.MediaItem>(reversed.size());
		for (MediaBrowserCompat.MediaItem item : reversed) {
			if (isFavorites(item)) {
				ordered.add(item);
			}
		}
		for (MediaBrowserCompat.MediaItem item : reversed) {
			if (!isFavorites(item)) {
				ordered.add(item);
			}
		}
		return ordered;
	}

	private static boolean isFavorites(MediaBrowserCompat.MediaItem item) {
		String mediaId = item.getMediaId();
		return mediaId != null && mediaId.toLowerCase(Locale.ENGLISH).endsWith(FAVORITES_SUFFIX);
	}

	/**
	 * Resolves a single media id, used by hosts that reload the now-playing
	 * item (for example when the car reconnects). Implemented instead of left
	 * to the base implementation, which reports "not found" for every id.
	 */
	@Override
	public void onLoadItem(String itemId, Result<MediaBrowserCompat.MediaItem> result) {
		result.sendResult(itemForId(itemId));
	}

	/** Media id + file name description for an id this service issued. */
	private MediaBrowserCompat.MediaItem itemForId(String mediaId) {
		boolean browsable = mediaId != null && mediaId.startsWith(BROWSE_PREFIX);
		boolean playable = mediaId != null && mediaId.startsWith(PLAY_PREFIX);
		if (!browsable && !playable) {
			return null;
		}
		String path = mediaId.substring(browsable ? BROWSE_PREFIX.length() : PLAY_PREFIX.length());
		if (path.length() == 0) {
			return null;
		}
		String name = new File(path).getName();
		if (name.length() == 0) {
			name = path;
		}
		MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
				.setMediaId(mediaId)
				.setTitle(name)
				.build();
		int flags = browsable
				? MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
				: MediaBrowserCompat.MediaItem.FLAG_PLAYABLE;
		return new MediaBrowserCompat.MediaItem(desc, flags);
	}

	/** Directory path a browsable media id refers to, or {@code null}. */
	private String pathForBrowseId(String browseId) {
		if (ROOT_ID.equals(browseId)) {
			return getModsDir().getPath();
		}
		if (browseId != null && browseId.startsWith(BROWSE_PREFIX)) {
			return browseId.substring(BROWSE_PREFIX.length());
		}
		return null;
	}

	private File getModsDir() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String md = prefs.getString("modsDir", null);
		if (md != null) {
			return new File(md);
		}
		File def = Storage.getDefaultModsDir();
		return def != null ? def : new File("/MODS");
	}

	private final class SessionCallback extends MediaSessionCompat.Callback {

		@Override
		public void onPlay() {
			if (player == null) {
				return;
			}
			ensurePlayerStarted();
			player.playPause(true);
		}

		@Override
		public void onPause() {
			if (player != null) {
				player.playPause(false);
			}
		}

		@Override
		public void onStop() {
			if (player != null) {
				player.stop();
			}
		}

		@Override
		public void onSkipToNext() {
			if (player != null) {
				player.playNext();
			}
		}

		@Override
		public void onSkipToPrevious() {
			if (player != null) {
				player.playPrev();
			}
		}

		@Override
		public void onSeekTo(long pos) {
			if (player != null) {
				player.seekTo((int) pos);
			}
		}

		@Override
		public void onSetRepeatMode(int repeatMode) {
			if (player == null) {
				return;
			}
			boolean repeat = repeatMode != PlaybackStateCompat.REPEAT_MODE_NONE;
			player.setOption(PlayerService.OPTION_REPEATSONG, Boolean.toString(repeat));
		}

		@Override
		public void onSetShuffleMode(int shuffleMode) {
			if (player == null) {
				return;
			}
			boolean shuffle = shuffleMode != PlaybackStateCompat.SHUFFLE_MODE_NONE;
			player.setOption(PlayerService.OPTION_PLAYBACK_ORDER, shuffle ? "R" : "S");
		}

		@Override
		public void onPlayFromMediaId(String mediaId, Bundle extras) {
			playFromMediaId(mediaId);
		}
	}

	/**
	 * Mirrors {@code PlayerServiceConnection}'s {@code startService()} on the
	 * phone side. Playback usually starts while Android Auto is in the
	 * foreground, but on API 26+ a started service keeps the notification and
	 * the FGS type the player posts in {@code beforePlay()} working even if the
	 * phone screen is off. A background-start refusal is not fatal: the service
	 * is already bound.
	 */
	private void ensurePlayerStarted() {
		try {
			startService(new Intent(this, PlayerService.class));
		} catch (RuntimeException e) {
			Log.d(TAG, "startService refused: %s", e.toString());
		}
	}

	private void playFromMediaId(String mediaId) {
		if (player == null || mediaId == null || !mediaId.startsWith(PLAY_PREFIX)) {
			return;
		}
		String path = mediaId.substring(PLAY_PREFIX.length());

		ensurePlayerStarted();

		String parentId = itemParents.get(mediaId);
		if (parentId != null) {
			String dirPath = pathForBrowseId(parentId);
			if (dirPath != null) {
				if (dirPath.toUpperCase(Locale.ENGLISH).endsWith(".PLIST")) {
					List<MediaBrowserCompat.MediaItem> items = loadChildren(parentId);
					int index = indexOfId(items, mediaId);
					if (player.isConnected()) {
						player.playPlaylist(dirPath, index < 0 ? 0 : index);
					} else {
						player.playMod(path);
					}
					return;
				}

				List<String> names = new ArrayList<String>();
				for (MediaBrowserCompat.MediaItem item : loadChildren(parentId)) {
					if (item.isPlayable()) {
						names.add(item.getMediaId().substring(PLAY_PREFIX.length()));
					}
				}
				int index = names.indexOf(path);
				if (index >= 0 && player.isConnected()) {
					player.playList(names.toArray(new String[names.size()]), index);
					return;
				}
			}
		}

		// No queue context (or not connected yet): play the single song. The
		// connection queues it until the player service is ready.
		player.playMod(path);
	}

	private static int indexOfId(List<MediaBrowserCompat.MediaItem> items, String mediaId) {
		for (int i = 0; i < items.size(); i++) {
			if (mediaId.equals(items.get(i).getMediaId())) {
				return i;
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private void updateSession(Map<String, Object> data, boolean newsong) {
		if (session == null || metadataBuilder == null) {
			return;
		}

		if (newsong) {
			metadataBuilder = new MediaMetadataCompat.Builder();
			positionMs = 0;
			durationMs = 0;
		}

		boolean metadataChanged = newsong;

		String title = stringOf(data, SongMeta.TITLE);
		String subTitle = stringOf(data, SongMeta.SUBTUNE_TITLE);
		if (subTitle != null && subTitle.length() > 0) {
			title = (title == null || title.length() == 0) ? subTitle : title + " - " + subTitle;
		}
		if (title != null && title.length() > 0) {
			metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, title);
			metadataChanged = true;
		} else if (data.containsKey(SongMeta.FILENAME)) {
			String fileName = stringOf(data, SongMeta.FILENAME);
			if (fileName != null) {
				metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE,
						new File(fileName).getName());
				metadataChanged = true;
			}
		}

		String composer = stringOf(data, SongMeta.COMPOSER);
		if (composer != null) {
			metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, composer);
			metadataChanged = true;
		}

		String fileName = stringOf(data, SongMeta.FILENAME);
		if (fileName != null) {
			metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, fileName);
			metadataChanged = true;
		}

		Integer length = intOf(data, SongMeta.LENGTH);
		if (length != null) {
			durationMs = length;
			if (durationMs > 0) {
				metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs);
			}
			metadataChanged = true;
		}

		if (metadataChanged) {
			session.setMetadata(metadataBuilder.build());
		}

		Integer position = intOf(data, SongMeta.POSITION);
		if (position != null) {
			positionMs = position;
		}

		Integer state = intOf(data, SongMeta.STATE);
		if (state != null) {
			songState = state;
		}

		session.setPlaybackState(buildPlaybackState());
	}

	private PlaybackStateCompat buildPlaybackState() {
		int state;
		float speed;
		switch (songState) {
		case 1:
			state = PlaybackStateCompat.STATE_PLAYING;
			speed = 1.0f;
			break;
		case 2:
			state = PlaybackStateCompat.STATE_PAUSED;
			speed = 0.0f;
			break;
		case 0:
			state = PlaybackStateCompat.STATE_STOPPED;
			speed = 0.0f;
			break;
		default:
			state = PlaybackStateCompat.STATE_NONE;
			speed = 0.0f;
			break;
		}
		return new PlaybackStateCompat.Builder()
				.setActions(PLAYBACK_ACTIONS)
				.setState(state, positionMs, speed)
				.build();
	}

	private static String stringOf(Map<String, Object> data, String key) {
		Object value = data.get(key);
		return value instanceof String ? (String) value : null;
	}

	private static Integer intOf(Map<String, Object> data, String key) {
		Object value = data.get(key);
		return value instanceof Integer ? (Integer) value : null;
	}
}
