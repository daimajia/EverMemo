package com.zhan_dui.utils;

import android.util.Log;

public class Logger {

	public static Boolean DEBUG = true;

	public static void i(String tag, String msg) {
		if (DEBUG)
			Log.i(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (DEBUG)
			Log.e(tag, msg);
	}

}
