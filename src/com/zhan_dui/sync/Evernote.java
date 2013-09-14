package com.zhan_dui.sync;

import java.util.List;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

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
import com.zhan_dui.utils.Logger;
import com.zhan_dui.utils.MD5;

public class Evernote implements LoginCallback {

	public String LogTag = "EverNote";
	public Context mContext;
	private static final String CONSUMER_KEY = "daimajia-4925";
	private static final String CONSUMER_SECRET = "28efc69651a408e9";
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

	public void Loggerout() {
		try {
			mEvernoteSession.logOut(mContext);
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLogout(true);
			}
		} catch (InvalidAuthenticationException e) {
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLogout(false);
			}
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
										Logger.e(LogTag, "NoteBook创建成功");
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
										Logger.e(LogTag, "NoteBook创建失败");
										callback.onFinshed(false, task);
									}

								});
			} catch (TTransportException e) {
				e.printStackTrace();
				Logger.e(LogTag, "NoteBook创建失败");
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

	@Override
	public void onAuthFinish(int resultCode) {
		if (resultCode == Activity.RESULT_OK) {
			mSharedPreferences.edit()
					.putString(EVERNOTE_TOKEN, mEvernoteSession.getAuthToken())
					.putLong(EVERNOTE_TOKEN_TIME, System.currentTimeMillis())
					.commit();
			createNotebook(NOTEBOOK_NAME, null, mNotebookCreateCallback);
			getUserInfo();
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLoginResult(true);
			}
		} else {
			if (mEvernoteLoginCallback != null) {
				mEvernoteLoginCallback.onLoginResult(false);
			}
		}
	}

	public interface EvernoteLoginCallback {
		public void onLoginResult(Boolean result);

		public void onUserinfo(Boolean result, User user);

		public void onLogout(Boolean reuslt);
	}

	public interface EvernoteSyncCallback {

		public void CreateCallback(boolean result, Memo memo, Note data);

		public void UpdateCallback(boolean result, Memo memo, Note data);

		public void DeleteCallback(boolean result, Memo memo);

	}

	private void checkAndInsert(String notebookGuid, final Memo memo) {
		if (mEvernoteSession.isLoggedIn()) {
			try {
				mEvernoteSession.getClientFactory().createNoteStoreClient()
						.listNotebooks(new OnClientCallback<List<Notebook>>() {

							@Override
							public void onSuccess(List<Notebook> data) {
								for (Notebook notebook : data) {
									if (notebook.getName()
											.equals(NOTEBOOK_NAME)) {
										Logger.e(LogTag, "发现名为" + NOTEBOOK_NAME
												+ "的Notebook");
										mSharedPreferences
												.edit()
												.putString(
														EVERNOTE_NOTEBOOK_GUID,
														notebook.getGuid())
												.commit();
										handleMemo(memo);
										return;
									}
								}
								Logger.e(LogTag, "未发现名为" + NOTEBOOK_NAME
										+ "的Notebook");
								createNotebook(NOTEBOOK_NAME, memo,
										mNotebookCreateCallback);
							}

							@Override
							public void onException(Exception exception) {
								Logger.e(LogTag, "获取Notebook列表出错");
							}
						});

			} catch (TTransportException e) {
				Logger.e(LogTag, "获取列表出错了");
				e.printStackTrace();
				if (mEvernoteSyncCallback != null) {
					mEvernoteSyncCallback.CreateCallback(false, memo, null);
				}
			}
		}
	}

	private void handleMemo(Memo memo) {
		Logger.e(LogTag, "开始处理Memo");
		if (memo.getEnid() != null && memo.getEnid().length() != 0) {
			Logger.e(LogTag, "决定更新Memo");
			updateNote(memo);
		} else {
			Logger.e(LogTag, "决定创建Memo");
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
								Logger.e(LogTag, "Memo Note创建成功");
								ContentValues values = new ContentValues();
								String hash = MD5.getHex(data.getContentHash());
								values.put(MemoDB.EUID, data.getGuid());
								values.put(MemoDB.HASH, hash);
								Logger.e(LogTag, "Memo Note创建成功-更新数据库");
								mContentResolver.update(ContentUris
										.withAppendedId(MemoProvider.MEMO_URI,
												memo.getId()), values, null,
										null);
								Logger.e(LogTag, "Memo Note创建成功-准备回调");
								if (mEvernoteSyncCallback != null) {
									Logger.e(LogTag, "Memo Note创建成功-回调");
									mEvernoteSyncCallback.CreateCallback(true,
											memo, data);
								}
							}

							@Override
							public void onException(Exception exception) {
								Logger.e(LogTag, "Memo Note创建失败");
								exception.printStackTrace();
								if (mEvernoteLoginCallback != null) {
									mEvernoteSyncCallback.CreateCallback(false,
											memo, null);
								}
							}
						});
			} catch (TTransportException e) {
				e.printStackTrace();
				Logger.e(LogTag, "Memo Note创建失败");
				if (mEvernoteLoginCallback != null) {
					mEvernoteSyncCallback.CreateCallback(false, memo, null);
				}
			}
		}
	}

	private void updateNote(final Memo memo) {
		if (mEvernoteSession.isLoggedIn()) {
			final Note note = memo.toNote();

			note.setGuid(memo.getEnid());
			note.setTitle(memo.getTitle());

			try {
				mEvernoteSession.getClientFactory().createNoteStoreClient()
						.updateNote(note, new OnClientCallback<Note>() {

							@Override
							public void onSuccess(Note data) {
								Logger.e(LogTag, "Memo Note更新成功");
								ContentValues values = new ContentValues();
								values.put(MemoDB.HASH, data.getContentHash());
								mContentResolver.update(ContentUris
										.withAppendedId(MemoProvider.MEMO_URI,
												memo.getId()), values, null,
										null);
								if (mEvernoteSyncCallback != null) {
									mEvernoteSyncCallback.UpdateCallback(true,
											memo, data);
								}
							}

							@Override
							public void onException(Exception exception) {
								Logger.e(LogTag, "Memo Note更新失败");
								if (mEvernoteSyncCallback != null) {
									mEvernoteSyncCallback.UpdateCallback(false,
											memo, null);
								}
							}
						});
			} catch (TTransportException e) {
				Logger.e(LogTag, "Memo Note更新失败");
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
				if (task != null) {
					Logger.e(LogTag, "Memo不为空，开始处理");
					handleMemo(task);
				} else {
					Logger.e(LogTag, "Memo为空，放弃");
				}
			} else {
				Logger.e(LogTag, "创建Notebook失败，放弃");
				if (mEvernoteSyncCallback != null) {
					mEvernoteSyncCallback.CreateCallback(false, task, null);
				}
			}
		}
	};

	private void createNote(Memo memo, String notebookGuid) {
		if (mEvernoteSession.isLoggedIn()) {
			Logger.e(LogTag, "授权可用");
			checkAndInsert(notebookGuid, memo);

		} else {
			Logger.e(LogTag, "授权不可用");
		}
	}

	public void syncMemo(Memo memo) {
		if (memo != null) {
			createNote(memo,
					mSharedPreferences.getString(EVERNOTE_NOTEBOOK_GUID, null));
		}
	}

	private void deleteNote(final Memo memo) {
		if (mEvernoteSession.isLoggedIn()) {
			try {
				Note note = memo.toDeleteNote();
				mEvernoteSession
						.getClientFactory()
						.createNoteStoreClient()
						.deleteNote(note.getGuid(),
								new OnClientCallback<Integer>() {

									@Override
									public void onSuccess(Integer data) {
										Logger.e(LogTag, "删除Memo成功");
										if (mEvernoteSyncCallback != null) {
											mEvernoteSyncCallback
													.DeleteCallback(true, memo);
										}
									}

									@Override
									public void onException(Exception exception) {
										Logger.e(LogTag, "删除Memo失败");
										if (mEvernoteSyncCallback != null) {
											mEvernoteSyncCallback
													.DeleteCallback(false, memo);
										}
									}
								});
			} catch (TTransportException e) {
				Logger.e(LogTag, "删除Memo失败");
				if (mEvernoteSyncCallback != null) {
					mEvernoteSyncCallback.DeleteCallback(false, memo);
				}
				e.printStackTrace();
			}
		}
	}

	public void deleteMemo(Memo memo) {
		Logger.e(LogTag, "准备删除Memo");
		if (memo != null && TextUtils.isEmpty(memo.getEnid()) == false) {
			deleteNote(memo);
		} else {
			Logger.e(LogTag, "打算删除的Memo缺少Enid信息，放弃");
			if (mEvernoteSyncCallback != null) {
				mEvernoteSyncCallback.DeleteCallback(false, memo);
			}
		}
	}
}
