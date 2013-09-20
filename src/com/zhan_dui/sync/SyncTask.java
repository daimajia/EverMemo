package com.zhan_dui.sync;

import java.util.TimerTask;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.zhan_dui.utils.Logger;

public class SyncTask extends TimerTask {

	private final String LogTag = "SyncTask";
	private Context mContext;
	private SharedPreferences mSharedPreferences;
	private static final String LAST_SYNC_TIME = "sync_task_last_sync_time";
	private static final long MIN_DURATION = 300000l;// 五分钟

	public SyncTask(Context context) {
		mContext = context;
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
	}

	@Override
	public void run() {
		long last = mSharedPreferences.getLong(LAST_SYNC_TIME, 0l);
		long now = System.currentTimeMillis();
		ConnectivityManager manager = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		if (now - last > MIN_DURATION && info != null && info.isAvailable()
				&& info.getType() == ConnectivityManager.TYPE_WIFI) {
			Logger.e(LogTag, "开始同步");
			mSharedPreferences.edit().putLong(LAST_SYNC_TIME, now);
			Evernote mEvernote = new Evernote(mContext);
			mEvernote.sync(true);
		}
	}
}
