package com.zhan_dui.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.ocpsoft.prettytime.PrettyTime;

import android.annotation.SuppressLint;
import android.content.Context;

@SuppressLint("SimpleDateFormat")
public class DateHelper {

	private static final SimpleDateFormat sMemoShowDateFormat = new SimpleDateFormat(
			"M.d a h:m");

	private static final PrettyTime PRETTY_TIME = new PrettyTime();

	public static String getGridDate(Context context, long time) {
		Date date = new Date(time);
		return PRETTY_TIME.format(date);
	}

	public static String getMemoDate(Context context, long time) {
		return sMemoShowDateFormat.format(new Date(time));
	}
}
