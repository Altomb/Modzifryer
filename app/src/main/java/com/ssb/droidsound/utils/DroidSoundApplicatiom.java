package com.ssb.droidsound;

import android.app.Application;

import com.ssb.droidsound.utils.Storage;

public class DroidSoundApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		// Must happen before any plugin or SongDatabase resolves a working
		// directory, both of which run on their own threads.
		Storage.init(this);
	}
}
