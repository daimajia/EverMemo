package com.zhan_dui.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;

import android.content.ContentValues;
import android.database.Cursor;

import com.evernote.client.android.EvernoteUtil;
import com.evernote.edam.type.Note;

public class Memo implements Serializable {

	private static final long serialVersionUID = -1123549346312970903L;

	public static final String STATUS_DELETE = "delete";
	public static final String STATUS_COMMON = "common";

	private long mCreatedTime;
	private long mUpdatedTime;
	private long mLastSyncTime;

	private int _id;

	private int mWallId;
	private int mOrder;
	private int mCursorPosition;
	private byte[] mHash;

	private String mGuid;
	private String mEnid;
	private String mContent;
	private String mAttributes;
	private String mStatus;

	public Memo() {
		mCreatedTime = System.currentTimeMillis();
		mUpdatedTime = System.currentTimeMillis();
	}

	public Memo(Cursor cursor) {
		mHash = cursor.getBlob(cursor.getColumnIndex("hash"));
		mContent = cursor.getString(cursor.getColumnIndex("content"));
		mAttributes = cursor.getString(cursor.getColumnIndex("attributes"));
		mStatus = cursor.getString(cursor.getColumnIndex("status"));
		mGuid = cursor.getString(cursor.getColumnIndex("guid"));
		mEnid = cursor.getString(cursor.getColumnIndex("enid"));

		mLastSyncTime = cursor.getLong(cursor.getColumnIndex("lastsynctime"));
		mCreatedTime = cursor.getLong(cursor.getColumnIndex("createdtime"));
		mUpdatedTime = cursor.getLong(cursor.getColumnIndex("updatedtime"));

		_id = cursor.getInt(cursor.getColumnIndex("_id"));
		mWallId = cursor.getInt(cursor.getColumnIndex("wallid"));
		mOrder = cursor.getInt(cursor.getColumnIndex("orderid"));
		mCursorPosition = cursor
				.getInt(cursor.getColumnIndex("cursorposition"));
	}

	public ContentValues toContentValues() {
		ContentValues values = new ContentValues();
		values.put("_id", _id);
		values.put("guid", mGuid);
		values.put("enid", mEnid);
		values.put("wallid", mWallId);
		values.put("orderid", mOrder);
		values.put("lastsynctime", mLastSyncTime);
		values.put("createdtime", mCreatedTime);
		values.put("updatedtime", mUpdatedTime);
		values.put("status", mStatus);
		values.put("attributes", mAttributes);
		values.put("content", mContent);
		values.put("hash", mHash);
		values.put("cursorposition", mCursorPosition);
		return values;
	}

	public static Memo build(ContentValues values) {
		Memo memo = new Memo();
		if (values.containsKey("_id")) {
			memo._id = values.getAsInteger("_id");
		}
		memo.mGuid = values.getAsString("guid");
		memo.mEnid = values.getAsString("enid");
		memo.mWallId = values.getAsInteger("wallid");
		memo.mOrder = values.getAsInteger("orderid");
		memo.setContent(values.getAsString("content"));
		memo.mAttributes = values.getAsString("attributes");
		memo.mStatus = values.getAsString("status");
		memo.mCreatedTime = values.getAsLong("createdtime");
		memo.mUpdatedTime = values.getAsLong("updatedtime");
		memo.mLastSyncTime = values.getAsLong("lastsynctime");
		memo.mCursorPosition = values.getAsInteger("cursorposition");
		return memo;
	}

	public void setId(int _id) {
		this._id = _id;
	}

	public String getTitle() {
		BufferedReader reader = new BufferedReader(new StringReader(
				getContent()));
		try {
			return reader.readLine();
		} catch (IOException e) {
			return "No Title";
		}
	}

	public Note toNote(String notebookGuid) {
		Note note = toNote();
		note.setNotebookGuid(notebookGuid);
		return note;
	}

	public Note toNote() {
		Note note = new Note();
		note.setTitle(getTitle());
		note.setContent(convertContentToEvernote());
		return note;
	}

	private String convertContentToEvernote() {
		String EvernoteContent = EvernoteUtil.NOTE_PREFIX
				+ getContent().replace("\n", "<br/>")
				+ EvernoteUtil.NOTE_SUFFIX;
		return EvernoteContent;
	}

	public void setContent(String content) {
		mContent = content;
		mUpdatedTime = System.currentTimeMillis();
		mStatus = STATUS_COMMON;
		mOrder = 0;
		mAttributes = "";
	}

	public void setCursorPosition(int cursorPosition) {
		mCursorPosition = cursorPosition;
	}

	public void setGuid(String guid) {
		mGuid = guid;
	}

	public void setEnid(String enid) {
		mEnid = enid;
	}

	public void setHash(byte[] hash) {
		mHash = hash;
	}

	public long getUpdatedTime() {
		return mUpdatedTime;
	}

	public long getCreatedTime() {
		return mCreatedTime;
	}

	public long getLastSyncTime() {
		return mLastSyncTime;
	}

	public int getId() {
		return _id;
	}

	public int getOrder() {
		return mOrder;
	}

	public int getWallId() {
		return mWallId;
	}

	public int getCursorPosition() {
		return mCursorPosition;
	}

	public String getEnid() {
		return mEnid;
	}

	public String getGuid() {
		return mGuid;
	}

	public byte[] getHash() {
		return mHash;
	}

	public String getAttributes() {
		return mAttributes;
	}

	public String getContent() {
		return mContent;
	}

	public String getStatus() {
		return mStatus;
	}

}
