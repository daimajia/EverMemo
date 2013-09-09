package com.zhan_dui.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

public class MemoProvider extends ContentProvider {

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
			queryBuilder.appendWhere(MemoDB.STATUS + "!='" + Memo.STATUS_DELETE
					+ "'");
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
		ContentValues values = new ContentValues();
		values.put(MemoDB.STATUS, Memo.STATUS_DELETE);
		switch (uriType) {
		case MEMOS:
			rowAffected = database.delete(MemoDB.MEMO_TABLE_NAME, selection,
					selectionArgs);
			break;
		case MEMO_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowAffected = database.update(MemoDB.MEMO_TABLE_NAME, values,
						MemoDB.ID + "=" + id, null);
			} else {
				rowAffected = database.update(MemoDB.MEMO_TABLE_NAME, values,
						selection + " and " + MemoDB.ID + "=" + id,
						selectionArgs);
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
		if (values.containsKey(MemoDB.ID)) {
			values.remove(MemoDB.ID);
		}
		Memo memo = Memo.build(values);
		if (memo.getContent().trim().length() == 0) {
			return null;
		}
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase database = memoDB.getWritableDatabase();
		Uri itemUri = null;
		switch (uriType) {
		case MEMOS:
			long newID = database.insert(MemoDB.MEMO_TABLE_NAME, null, values);
			if (newID > 0) {
				itemUri = ContentUris.withAppendedId(uri, newID);
				getContext().getContentResolver().notifyChange(itemUri, null);
			}
		default:
			break;
		}
		return itemUri;

	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int updateCount = 0;
		switch (sURIMatcher.match(uri)) {
		case MEMOS:
			updateCount = memoDB.getWritableDatabase().update(
					MemoDB.MEMO_TABLE_NAME, values, selection, selectionArgs);
			break;
		case MEMO_ID:
			String where = "";
			if (!TextUtils.isEmpty(selection)) {
				where += " and " + selection;
			}
			updateCount = memoDB.getWritableDatabase().update(
					MemoDB.MEMO_TABLE_NAME, values,
					MemoDB.ID + "=" + uri.getLastPathSegment() + where,
					selectionArgs);
			break;

		default:
			break;
		}
		if (updateCount > 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}
		return updateCount;
	}
}
