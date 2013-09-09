package com.zhan_dui.data;

import java.io.Serializable;

import android.content.ContentValues;
import android.database.Cursor;

import com.zhan_dui.utils.MD5;

public class Memo implements Serializable {

	private static final long serialVersionUID = -1123549346312970903L;

	public static final String STATUS_DELETE = "delete";
	public static final String STATUS_COMMON = "common";

	private long mCreatedTime;
	private long mUpdatedTime;
	private long mLastSyncTime;

	private int _id;
	private int mGuid;
	private int mEnid;
	private int mWallId;
	private int mOrder;

	private String mHash;
	private String mContent;
	private String mAttributes;
	private String mStatus;

	public Memo() {
		mCreatedTime = System.currentTimeMillis();
		mUpdatedTime = System.currentTimeMillis();
	}

	public Memo(Cursor cursor) {
		mHash = cursor.getString(cursor.getColumnIndex("hash"));
		mContent = cursor.getString(cursor.getColumnIndex("content"));
		mAttributes = cursor.getString(cursor.getColumnIndex("attributes"));
		mStatus = cursor.getString(cursor.getColumnIndex("status"));

		mLastSyncTime = cursor.getLong(cursor.getColumnIndex("lastsynctime"));
		mCreatedTime = cursor.getLong(cursor.getColumnIndex("createdtime"));
		mUpdatedTime = cursor.getLong(cursor.getColumnIndex("updatedtime"));

		_id = cursor.getInt(cursor.getColumnIndex("_id"));
		mGuid = cursor.getInt(cursor.getColumnIndex("guid"));
		mEnid = cursor.getInt(cursor.getColumnIndex("enid"));
		mWallId = cursor.getInt(cursor.getColumnIndex("wallid"));
		mOrder = cursor.getInt(cursor.getColumnIndex("orderid"));
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
		return values;
	}

	public static Memo build(ContentValues values) {
		Memo memo = new Memo();
		if (values.containsKey("_id")) {
			memo._id = values.getAsInteger("_id");
		}
		memo.mGuid = values.getAsInteger("guid");
		memo.mEnid = values.getAsInteger("enid");
		memo.mWallId = values.getAsInteger("wallid");
		memo.mOrder = values.getAsInteger("orderid");
		memo.setContent(values.getAsString("content"));
		memo.mAttributes = values.getAsString("attributes");
		memo.mStatus = values.getAsString("status");
		memo.mCreatedTime = values.getAsLong("createdtime");
		memo.mUpdatedTime = values.getAsLong("updatedtime");
		memo.mLastSyncTime = values.getAsLong("lastsynctime");
		return memo;
	}

	public void setId(int _id) {
		this._id = _id;
	}

	public void setContent(String content) {
		mContent = content;
		mUpdatedTime = System.currentTimeMillis();
		mStatus = STATUS_COMMON;
		mOrder = 0;
		mAttributes = "";
		mHash = MD5.digest(mContent);
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

	public int getEnid() {
		return mEnid;
	}

	public int getGuid() {
		return mGuid;
	}

	public int getOrder() {
		return mOrder;
	}

	public int getWallId() {
		return mWallId;
	}

	public String getHash() {
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
