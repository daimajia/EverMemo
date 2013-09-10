package com.zhan_dui.sync;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.InvalidAuthenticationException;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.User;
import com.evernote.thrift.transport.TTransportException;
import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.evermemo.SettingActivity.LoginCallback;
import com.zhan_dui.utils.MD5;

public class Evernote implements LoginCallback {

	public Context mContext;
	private static final String CONSUMER_KEY = "daimajia";
	private static final String CONSUMER_SECRET = "091e99d78640a556";
	private static final String NOTEBOOK_NAME = "EverMemo";
	public static final String EVERNOTE_TOKEN = "Evernote_Token";
	public static final String EVERNOTE_TOKEN_TIME = "Evernote_Token_Time";
	public static final String EVERNOTE_USER_NAME = "Evernote_User_Name";
	public static final String EVERNOTE_USER_EMAIL = "Evernote_User_Email";
	public static final String EVERNOTE_NOTEBOOK_GUID = "Evenote_Note_Guid";
	private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.SANDBOX;
	private EvernoteSession mEvernoteSession;
	private SharedPreferences mSharedPreferences;

	private EvernoteLoginCallback mEvernoteLoginCallback;
	private EvernoteSyncCallback mEvernoteSyncCallback;
	private ContentResolver mContentResolver;

	public Evernote(Context context) {
		mContext = context;
		mEvernoteSession = EvernoteSession.getInstance(mContext, CONSUMER_KEY,
				CONSUMER_SECRET, EVERNOTE_SERVICE);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);
		mContentResolver = mContext.getContentResolver();

	}

	public Evernote(Context context, EvernoteLoginCallback loginCallback) {
		this(context);
		mEvernoteLoginCallback = loginCallback;
	}

	public Evernote(Context context, EvernoteSyncCallback evernoteSyncCallback) {
		this(context);
		mEvernoteSyncCallback = evernoteSyncCallback;
	}

	public Evernote(Context context,
			EvernoteLoginCallback evernoteLoginCallback,
			EvernoteSyncCallback evernoteSyncCallback) {
		this(context);
		mEvernoteLoginCallback = evernoteLoginCallback;
		mEvernoteSyncCallback = evernoteSyncCallback;
	}

	public void auth() {
		mEvernoteSession.authenticate(mContext);
	}

	public void logout() {
		try {
			mEvernoteSession.logOut(mContext);
		} catch (InvalidAuthenticationException e) {
			Log.e("LogoutErr", e.getStackTrace().toString());
		}
	}

	private void createNotebook(String notebookName, final Memo task,
			final PostMan.NotebookInterface callback) {
		if (mEvernoteSession.isLoggedIn()) {
			Notebook notebook = new Notebook();
			notebook.setName(notebookName);
			notebook.setDefaultNotebook(false);

			try {
				mEvernoteSession
						.getClientFactory()
						.createNoteStoreClient()
						.createNotebook(notebook,
								new OnClientCallback<Notebook>() {

									@Override
									public void onSuccess(Notebook data) {
										mSharedPreferences
												.edit()
												.putString(
														EVERNOTE_NOTEBOOK_GUID,
														data.getGuid())
												.commit();
										callback.onFinshed(true, task);
									}

									@Override
									public void onException(Exception exception) {
										callback.onFinshed(false, task);
									}

								});
			} catch (TTransportException e) {
				e.printStackTrace();
				callback.onFinshed(false, task);
			}
		}
	}

	private void getUserInfo() {
		if (mEvernoteSession.isLoggedIn()) {
			try {
				mEvernoteSession.getClientFactory().createUserStoreClient()
						.getUser(new OnClientCallback<User>() {

							@Override
							public void onSuccess(User user) {
								mSharedPreferences
										.edit()
										.putString(EVERNOTE_USER_NAME,
												user.getName())
										.putString(EVERNOTE_USER_EMAIL,
												user.getEmail()).commit();
								Log.e("My", "获取用户信息成功");
							}

							@Override
							public void onException(Exception exception) {
								Log.e("My", "获取用户信息失败");
							}
						});
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (TTransportException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onAuthFinish(int resultCode) {
		if (resultCode == Activity.RESULT_OK) {
			mSharedPreferences.edit()
					.putString(EVERNOTE_TOKEN, mEvernoteSession.getAuthToken())
					.putLong(EVERNOTE_TOKEN_TIME, System.currentTimeMillis())
					.commit();
			createNotebook(NOTEBOOK_NAME, null, mNotebookCreateCallback);
			getUserInfo();
		}
	}

	public static final String MSG_UNKNOWN_ERROR = "-1";
	public static final String MSG_SUCCESS = "0";

	public interface EvernoteLoginCallback {
		public void onLoginError();

		public void onUserinfo(User user);
	}

	public interface EvernoteSyncCallback {

		public void CreateCallback(boolean result, Memo memo, Note data);

		public void UpdateCallback(boolean result, Memo memo, Note data);

	}

	private void checkAndInsert(String notebookGuid, final Memo memo) {
		if (mEvernoteSession.isLoggedIn()) {
			try {
				mEvernoteSession
						.getClientFactory()
						.createNoteStoreClient()
						.getNotebook(notebookGuid,
								new OnClientCallback<Notebook>() {

									@Override
									public void onSuccess(Notebook data) {
										handleMemo(memo);
									}

									@Override
									public void onException(Exception exception) {
										exception.printStackTrace();
										createNotebook(NOTEBOOK_NAME, memo,
												mNotebookCreateCallback);
									}
								});
			} catch (TTransportException e) {
				e.printStackTrace();
				if (mEvernoteSyncCallback != null) {
					mEvernoteSyncCallback.CreateCallback(false, memo, null);
				}
			}
		}
	}

	private void handleMemo(Memo memo) {
		if (memo.getEnid() != null && memo.getEnid().length() != 0) {
			updateNote(memo);
		} else {
			createNote(memo);
		}
	}

	private interface PostMan {
		public interface NotebookInterface {
			public void onFinshed(boolean result, Memo task);
		}

	}

	private void createNote(final Memo memo) {
		if (mEvernoteSession.isLoggedIn()) {
			try {
				final Note note = memo.toNote(mSharedPreferences.getString(
						EVERNOTE_NOTEBOOK_GUID, null));
				mEvernoteSession.getClientFactory().createNoteStoreClient()
						.createNote(note, new OnClientCallback<Note>() {

							@Override
							public void onSuccess(Note data) {
								Toast.makeText(mContext,
										note.getTitle() + "添加成功",
										Toast.LENGTH_SHORT).show();
								ContentValues values = new ContentValues();
								String hash = MD5.getHex(data.getContentHash());
								values.put(MemoDB.EUID, data.getGuid());
								values.put(MemoDB.HASH, hash);
								memo.setHash(hash);
								memo.setEnid(data.getGuid());

								mContentResolver.update(ContentUris
										.withAppendedId(MemoProvider.MEMO_URI,
												memo.getId()), values, null,
										null);

								if (mEvernoteSyncCallback != null) {
									mEvernoteSyncCallback.CreateCallback(true,
											memo, data);
								}
							}

							@Override
							public void onException(Exception exception) {
								exception.printStackTrace();
								if (mEvernoteLoginCallback != null) {
									mEvernoteSyncCallback.CreateCallback(false,
											memo, null);
								}
							}
						});
			} catch (TTransportException e) {
				e.printStackTrace();
				Toast.makeText(mContext, "添加失败", Toast.LENGTH_SHORT).show();
				if (mEvernoteLoginCallback != null) {
					mEvernoteSyncCallback.CreateCallback(false, memo, null);
				}
			}
		}
	}

	private void updateNote(final Memo memo) {
		if (mEvernoteSession.isLoggedIn()) {
			final Note note = memo.toNote(mSharedPreferences.getString(
					EVERNOTE_NOTEBOOK_GUID, null));
			note.setGuid(memo.getEnid());
			Log.e("EUID", memo.getEnid());
			try {
				mEvernoteSession.getClientFactory().createNoteStoreClient()
						.updateNote(note, new OnClientCallback<Note>() {

							@Override
							public void onSuccess(Note data) {
								ContentValues values = new ContentValues();
								values.put(MemoDB.HASH, data.getContentHash());
								mContentResolver.update(ContentUris
										.withAppendedId(MemoProvider.MEMO_URI,
												memo.getId()), values, null,
										null);
								Toast.makeText(mContext, "更新完成",
										Toast.LENGTH_SHORT).show();
								String hash = MD5.getHex(data.getContentHash());
								memo.setHash(hash);
								if (mEvernoteSyncCallback != null) {
									mEvernoteSyncCallback.UpdateCallback(true,
											memo, data);
								}
							}

							@Override
							public void onException(Exception exception) {
								exception.printStackTrace();
								if (mEvernoteSyncCallback != null) {
									mEvernoteSyncCallback.UpdateCallback(false,
											memo, null);
								}
							}
						});
			} catch (TTransportException e) {
				e.printStackTrace();
				if (mEvernoteSyncCallback != null) {
					mEvernoteSyncCallback.UpdateCallback(false, memo, null);
				}
			}
		}
	}

	private PostMan.NotebookInterface mNotebookCreateCallback = new PostMan.NotebookInterface() {

		@Override
		public void onFinshed(boolean result, Memo task) {
			if (result) {
				if (task != null)
					handleMemo(task);
			} else {
				if (mEvernoteSyncCallback != null) {
					mEvernoteSyncCallback.CreateCallback(false, task, null);
				}
			}
		}
	};

	private void createNote(Memo memo, String notebookGuid) {
		if (mEvernoteSession.isLoggedIn()) {
			if (notebookGuid == null) {
				createNotebook(NOTEBOOK_NAME, memo, mNotebookCreateCallback);
			} else {
				checkAndInsert(notebookGuid, memo);
			}
		}
	}

	public void syncMemo(Memo memo) {
		if (memo == null) {
			Log.e("error ", "memo is null");
		}
		createNote(memo,
				mSharedPreferences.getString(EVERNOTE_NOTEBOOK_GUID, null));
	}

}
