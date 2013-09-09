package com.zhan_dui.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MemoDB extends SQLiteOpenHelper {

	public final static String ID = "_id";
	public final static String STATUS = "status";

	private final String CREATE_MEMO_TABLE = "create table Memo(`_id` integer primary key autoincrement,"
			+ "`content` text,"
			+ "`createdtime` text,"
			+ "`updatedtime` text,"
			+ "`hash` text,"
			+ "`orderid` int,"
			+ "`lastsynctime` text,"
			+ "`status` text,"
			+ "`guid` text,"
			+ "`enid` text,"
			+ "`wallid` text,"
			+ "`attributes` text,"
			+ "`cursorposition` int"
			+ ");";

	public static final String Name = "EverMemo";
	public static final int VERSION = 1;

	public final static String MEMO_TABLE_NAME = "Memo";

	public MemoDB(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

	public MemoDB(Context context) {
		this(context, Name, null, VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_MEMO_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

}
