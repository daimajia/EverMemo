package com.zhan_dui.sync;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.InvalidAuthenticationException;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteCollectionCounts;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.User;
import com.evernote.thrift.TException;
import com.evernote.thrift.transport.TTransportException;
import com.umeng.analytics.MobclickAgent;
import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.utils.Logger;

public class Evernote {

	public String LogTag = "EverNote";
	public Context mContext;
	private static final String CONSUMER_KEY = "milkliker";
	private static final String CONSUMER_SECRET = "f479109c186d284b";
	private static final String NOTEBOOK_NAME = "EverMemo";
	public static final String EVERNOTE_TOKEN = "Evernote_Token";
	public static final String EVERNOTE_TOKEN_TIME = "Evernote_Token_Time";
	public static final String EVERNOTE_USER_NAME = "Evernote_User_Name";
	public static final String EVERNOTE_USER_EMAIL = "Evernote_User_Email";
	public static final String EVERNOTE_NOTEBOOK_GUID = "Evenote_Note_Guid";
	public static final String LAST_SYNC_DOWN = "LAST_SYNC_DOWN";
	public static boolean Syncing = false;

	private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.PRODUCTION;
	private EvernoteSession mEvernoteSession;
	private SharedPreferences mSharedPreferences;
	private ContentResolver mContentResolver;
	private EvernoteLoginCallback mEvernoteLoginCallback;

	public Evernote(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		mEvernoteSession = EvernoteSession.getInstance(mContext, CONSUMER_KEY,
				CONSUMER_SECRET, EVERNOTE_SERVICE);
	}

	public Evernote(Context context, EvernoteLoginCallback l) {
		this(context);
		mEvernoteLoginCallback = l;
	}

	public interface EvernoteLoginCallback {
		public void onLoginResult(Boolean result);

		public void onUserinfo(Boolean result, User user);

		public void onLogout(Boolean reuslt);
	}

	public boolean isLogin() {
		return mEvernoteSession.isLoggedIn();
	}

	public void auth() {
		mEvernoteSession.authenticate(mContext);
		MobclickAgent.onEvent(mContext, "Bind_EverNote");
	}

	public void onAuthFinish(int resultCode) {
		if (resultCode == Activity.RESULT_OK) {
			mSharedPreferences.edit()
					.putString(EVERNOTE_TOKEN, mEvernoteSession.getAuthToken())
					.putLong(EVERNOTE_TOKEN_TIME, System.currentTimeMillis())
					.commit();
			getUserInfo();
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLoginResult(true);
			}
			sync();
		} else {
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLoginResult(false);
			}
		}
	}

	public String getUsername() {
		return mSharedPreferences.getString(EVERNOTE_USER_NAME, null);
	}

	public void getUserInfo() {
		if (mEvernoteSession.isLoggedIn()) {
			try {
				mEvernoteSession.getClientFactory().createUserStoreClient()
						.getUser(new OnClientCallback<User>() {

							@Override
							public void onSuccess(User user) {
								mSharedPreferences
										.edit()
										.putString(EVERNOTE_USER_NAME,
												user.getUsername())
										.putString(EVERNOTE_USER_EMAIL,
												user.getEmail()).commit();
								if (mEvernoteLoginCallback != null) {
									mEvernoteLoginCallback.onUserinfo(true,
											user);
								}
							}

							@Override
							public void onException(Exception exception) {
								if (mEvernoteLoginCallback != null) {
									mEvernoteLoginCallback.onUserinfo(false,
											null);
								}
							}
						});
			} catch (IllegalStateException e) {
				e.printStackTrace();
				if (mEvernoteLoginCallback != null) {
					mEvernoteLoginCallback.onUserinfo(false, null);
				}
			} catch (TTransportException e) {
				e.printStackTrace();
				if (mEvernoteLoginCallback != null) {
					mEvernoteLoginCallback.onUserinfo(false, null);
				}
			}
		}
	}

	public void Logout() {
		try {
			mEvernoteSession.logOut(mContext);
			mSharedPreferences.edit().remove(EVERNOTE_USER_EMAIL);
			mSharedPreferences.edit().remove(EVERNOTE_USER_NAME);
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLogout(true);
			}
			MobclickAgent.onEvent(mContext, "UnBind_EverNote");
		} catch (InvalidAuthenticationException e) {
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLogout(false);
			}
		}
	}

	public boolean isNotebookExsist(String guid, String name) throws Exception {
		boolean result = false;
		try {
			Notebook notebook = mEvernoteSession.getClientFactory()
					.createNoteStore()
					.getNotebook(mEvernoteSession.getAuthToken(), guid);
			if (notebook.getName().equals(name)) {
				result = true;
				Logger.e(LogTag, guid + "笔记本存在");
				mSharedPreferences.edit()
						.putString(EVERNOTE_NOTEBOOK_GUID, notebook.getGuid())
						.commit();
			}
		} catch (EDAMNotFoundException e) {
			e.printStackTrace();
			if (e.getIdentifier().equals("Notebook.guid")) {
				result = false;
				Logger.e(LogTag, guid + "笔记本不存在");
			}
		}
		return result;
	}

	/**
	 * create a notebook by bookname
	 * 
	 * @param bookname
	 * @return
	 * @throws Exception
	 */
	public boolean createNotebook(String bookname) throws Exception {
		Notebook notebook = new Notebook();
		notebook.setDefaultNotebook(false);
		notebook.setName(bookname);
		boolean result = false;
		try {
			Notebook resultNotebook = mEvernoteSession.getClientFactory()
					.createNoteStore()
					.createNotebook(mEvernoteSession.getAuthToken(), notebook);
			result = true;
			Logger.e(LogTag, "Notebook" + bookname + "不存在，创建成功");
			mSharedPreferences
					.edit()
					.putString(EVERNOTE_NOTEBOOK_GUID, resultNotebook.getGuid())
					.commit();
		} catch (EDAMUserException e) {
			if (e.getErrorCode() == EDAMErrorCode.DATA_CONFLICT) {
				result = true;
				Logger.e(LogTag, "已经存在，无需创建");
			}
		} catch (Exception e) {
			Logger.e(LogTag, "传输出现错误");
			throw e;
		}
		return result;
	}

	public Note createNote(Memo memo) throws Exception {
		try {
			Note note = memo.toNote();
			Logger.e(LogTag, mSharedPreferences.getString(
					EVERNOTE_NOTEBOOK_GUID, "aaaaaaaa"));
			note.setNotebookGuid(mSharedPreferences.getString(
					EVERNOTE_NOTEBOOK_GUID, null));
			Note responseNote = mEvernoteSession.getClientFactory()
					.createNoteStore()
					.createNote(mEvernoteSession.getAuthToken(), note);
			Logger.e(LogTag, "Note创建成功");
			ContentValues values = new ContentValues();
			values.put(MemoDB.ENID, responseNote.getGuid());
			values.put(MemoDB.SYNCSTATUS, Memo.NEED_NOTHING);
			values.put(MemoDB.UPDATEDTIME, responseNote.getUpdated());
			values.put(MemoDB.HASH, responseNote.getContentHash());
			mContentResolver.update(
					ContentUris.withAppendedId(MemoProvider.MEMO_URI,
							memo.getId()), values, null, null);
			return responseNote;
		} catch (EDAMUserException e) {
			throw new Exception("Note格式不合理");
		} catch (EDAMNotFoundException e) {
			throw new Exception("笔记本不存在");
		} catch (Exception e) {
			throw e;
		}
	}

	public boolean deleteNote(Note note) {
		if (note.getGuid() == null) {
			Logger.e(LogTag, "GUID是空，无需删除");
			return true;
		} else {
			try {
				mEvernoteSession
						.getClientFactory()
						.createNoteStore()
						.deleteNote(mEvernoteSession.getAuthToken(),
								note.getGuid());
				Logger.e(LogTag, "Note删除成功");
				return true;
			} catch (EDAMUserException e) {
				Logger.e(LogTag, "Note早已被删除，说明删除成功");
				return true;
			} catch (EDAMNotFoundException e) {
				Logger.e(LogTag, "Note未找到，说明无需删除");
				return true;
			} catch (Exception e) {
				Logger.e(LogTag, "传输失败，说明删除失败");
				return false;
			}
		}
	}

	public Note updateNote(Memo memo) throws Exception {
		try {
			Note responseNote = mEvernoteSession
					.getClientFactory()
					.createNoteStore()
					.updateNote(mEvernoteSession.getAuthToken(),
							memo.toUpdateNote());
			ContentValues values = new ContentValues();
			values.put(MemoDB.SYNCSTATUS, Memo.NEED_NOTHING);
			values.put(MemoDB.UPDATEDTIME, responseNote.getUpdated());
			values.put(MemoDB.HASH, responseNote.getContentHash());
			mContentResolver.update(
					ContentUris.withAppendedId(MemoProvider.MEMO_URI,
							memo.getId()), values, null, null);
			Logger.e(LogTag, "Note更新成功");
			return responseNote;
		} catch (EDAMUserException e) {
			Logger.e(LogTag, "数据格式有误");
			throw new Exception(e.getCause());
		} catch (EDAMNotFoundException e) {
			Logger.e(LogTag, "Note根据GUID没有找到:" + e.getCause());
			throw new Exception("Note未找到");
		} catch (Exception e) {
			Logger.e(LogTag, "传输出现错误:" + e.getCause());
			throw new Exception("传输出现错误:" + e.getCause());
		}
	}

	public void makeSureNotebookExsits(String NotebookName) {
		try {
			if (mSharedPreferences.contains(EVERNOTE_NOTEBOOK_GUID)) {
				if (!isNotebookExsist(mSharedPreferences.getString(
						EVERNOTE_NOTEBOOK_GUID, ""), NOTEBOOK_NAME)) {
					createNotebook(NOTEBOOK_NAME);
				}
			} else {
				List<Notebook> books = mEvernoteSession.getClientFactory()
						.createNoteStore()
						.listNotebooks(mEvernoteSession.getAuthToken());
				int count = books.size();
				for (int i = 0; i < count; i++) {
					Notebook book = books.get(i);
					if (book.getName().equals(NotebookName)) {
						mSharedPreferences
								.edit()
								.putString(EVERNOTE_NOTEBOOK_GUID,
										book.getGuid()).commit();
						return;
					}
				}
				createNotebook(NOTEBOOK_NAME);
			}

		} catch (Exception e) {
			Logger.e(LogTag, "检查笔记本是否存和创建笔记本的时候出现异常");
			Syncing = false;
			return;
		}
	}

	public void downloadNote(String guid) {
		Logger.e(LogTag, "准备添加:" + guid);
		try {
			Note note = mEvernoteSession
					.getClientFactory()
					.createNoteStore()
					.getNote(mEvernoteSession.getAuthToken(), guid, true,
							false, false, false);
			ContentValues values = Memo.buildInsertMemoFromNote(note)
					.toInsertContentValues();
			mContentResolver.insert(MemoProvider.MEMO_URI, values);
		} catch (TTransportException e) {
		} catch (EDAMUserException e) {
		} catch (EDAMSystemException e) {
		} catch (EDAMNotFoundException e) {
		} catch (TException e) {
		}
	}

	public void updateLocalNote(String guid, int _id) {
		Logger.e(LogTag, "准备更新:" + guid);
		try {
			Note note = mEvernoteSession
					.getClientFactory()
					.createNoteStore()
					.getNote(mEvernoteSession.getAuthToken(), guid, true,
							false, false, false);
			Memo memo = Memo.buildInsertMemoFromNote(note);
			ContentValues contentValues = memo.toUpdateContentValues();
			mContentResolver.update(
					ContentUris.withAppendedId(MemoProvider.MEMO_URI, _id),
					contentValues, null, null);
		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (EDAMUserException e) {
			e.printStackTrace();
		} catch (EDAMSystemException e) {
			e.printStackTrace();
		} catch (EDAMNotFoundException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}

	}

	public void download() {
		NoteFilter noteFilter = new NoteFilter();
		String guid = mSharedPreferences.getString(EVERNOTE_NOTEBOOK_GUID, "");
		noteFilter.setNotebookGuid(guid);
		NotesMetadataResultSpec notesMetadataResultSpec = new NotesMetadataResultSpec();
		notesMetadataResultSpec.setIncludeUpdated(true);
		try {
			NoteCollectionCounts noteCollectionCounts = mEvernoteSession
					.getClientFactory()
					.createNoteStore()
					.findNoteCounts(mEvernoteSession.getAuthToken(),
							noteFilter, false);
			Map<String, Integer> maps = noteCollectionCounts
					.getNotebookCounts();
			int maxcount = maps.get(guid);
			NotesMetadataList list = mEvernoteSession
					.getClientFactory()
					.createNoteStore()
					.findNotesMetadata(mEvernoteSession.getAuthToken(),
							noteFilter, 0, maxcount, notesMetadataResultSpec);

			for (int i = 0; i < list.getNotes().size(); i++) {
				NoteMetadata note = list.getNotes().get(i);
				Cursor cursor = mContentResolver.query(MemoProvider.MEMO_URI,
						new String[] { MemoDB.UPDATEDTIME, MemoDB.ID },
						MemoDB.ENID + "=?", new String[] { note.getGuid() },
						null);
				if (cursor.getCount() != 0) {
					cursor.moveToNext();
					if (cursor.getLong(cursor
							.getColumnIndex(MemoDB.UPDATEDTIME)) != note
							.getUpdated()) {
						// 更新数据
						updateLocalNote(note.getGuid(),
								cursor.getInt(cursor.getColumnIndex(MemoDB.ID)));
					}
				} else {
					// 添加数据
					downloadNote(note.getGuid());
				}
				cursor.close();
			}

		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (EDAMUserException e) {
			e.printStackTrace();
		} catch (EDAMSystemException e) {
			e.printStackTrace();
		} catch (EDAMNotFoundException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}
	}

	public void sync() {
		new Thread() {
			@Override
			public void run() {
				if (Syncing) {
					Logger.e(LogTag, "正在同步");
					return;
				}
				Logger.e(LogTag, "开始同步");
				Syncing = true;
				if (mEvernoteSession.isLoggedIn() == false) {
					Logger.e(LogTag, "未登录");
					Syncing = false;
					return;
				}
				makeSureNotebookExsits(NOTEBOOK_NAME);
				Cursor cursor = mContentResolver.query(
						MemoProvider.ALL_MEMO_URI, null, null, null, null);
				while (cursor.moveToNext()) {
					Memo memo = new Memo(cursor);
					if (memo.isNeedSyncDelete()) {
						if (deleteNote(memo.toDeleteNote())) {
							ContentValues values = new ContentValues();
							values.put(MemoDB.SYNCSTATUS, Memo.NEED_NOTHING);
							mContentResolver.update(ContentUris.withAppendedId(
									MemoProvider.MEMO_URI, memo.getId()),
									values, null, null);
						}
					} else {
						if (memo.isNeedSyncUp()) {
							if (memo.getEnid() != null
									&& memo.getEnid().length() != 0) {
								try {
									updateNote(memo);
								} catch (Exception e) {
									Logger.e(LogTag,
											"尝试更新的时候出现错误:" + e.getCause());
									continue;
								}
							} else {
								try {
									createNote(memo);
								} catch (Exception e) {
									Logger.e(LogTag,
											"尝试创建新的Note的时候出现错误:" + e.getCause());
									continue;
								}
							}
						}
					}
				}
				download();
				Syncing = false;
				cursor.close();
			}
		}.start();

	}
}
