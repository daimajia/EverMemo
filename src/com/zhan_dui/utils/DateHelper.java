package com.zhan_dui.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;

import com.zhan_dui.evermemo.R;

@SuppressLint("SimpleDateFormat")
public class DateHelper {

	public static final long ONE_DAY_TIMESTAMP = 86400000;

	public static String getReadableDate(Context context,
			SimpleDateFormat dateFormat, long timemillisecond) {
		long span = System.currentTimeMillis() - timemillisecond;
		long timeSpan = span / ONE_DAY_TIMESTAMP;
		if (timeSpan == 0) {
			return context.getString(R.string.today);
		} else if (timeSpan == 1) {
			return context.getString(R.string.yesterday);
		} else {
			return dateFormat.format(new Date(timemillisecond));
		}
	}

	public static String getReadableDate(Context context, String dateFormat,
			long timemillisecond) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		long span = System.currentTimeMillis() - timemillisecond;
		long timeSpan = span / ONE_DAY_TIMESTAMP;
		if (timeSpan == 0) {
			return context.getString(R.string.today);
		} else if (timeSpan == 1) {
			return context.getString(R.string.yesterday);
		} else {
			return simpleDateFormat.format(new Date(timemillisecond));
		}
	}
}
