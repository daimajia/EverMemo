package com.zhan_dui.data;

import java.io.Console;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class MemoContentProvider extends ContentProvider {

	public static final int MEMOS = 1;
	public static final int MEMO_ID = 2;

	private MemoDB memoDB;
	private static final String AUTHORITY = "com.zhan_dui.data.MemoContentProvider";
	private static final String MEMO_BASE_PATH = MemoDB.MEMO_TABLE_NAME;
	public static final Uri MEMO_URI = Uri.parse("content://" + AUTHORITY + "/"
			+ MEMO_BASE_PATH);
	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);

	static {
		sURIMatcher.addURI(AUTHORITY, MEMO_BASE_PATH, MEMOS);
		sURIMatcher.addURI(AUTHORITY, MEMO_BASE_PATH + "/#", MEMO_ID);
	}

	@Override
	public boolean onCreate() {
		memoDB = new MemoDB(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(MemoDB.MEMO_TABLE_NAME);
		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case MEMOS:
			Log.e("type", uriType+"");
			break;
		case MEMO_ID:
			queryBuilder
					.appendWhere(MemoDB.ID + "=" + uri.getLastPathSegment());
			break;

		default:
			throw new IllegalArgumentException("Unknown URI");
		}
		Cursor cursor = queryBuilder.query(memoDB.getReadableDatabase(),
				projection, selection, selectionArgs, null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase database = memoDB.getWritableDatabase();
		int rowAffected = 0;
		switch (uriType) {
		case MEMOS:
			rowAffected = database.delete(MemoDB.MEMO_TABLE_NAME, selection,
					selectionArgs);
			break;
		case MEMO_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowAffected = database.delete(MemoDB.MEMO_TABLE_NAME, MemoDB.ID
						+ "=" + id, null);
			} else {
				rowAffected = database.delete(MemoDB.MEMO_TABLE_NAME, selection
						+ " and " + MemoDB.ID + "=" + id, null);
			}
		default:
			break;
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowAffected;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

}
