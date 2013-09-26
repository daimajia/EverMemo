package com.zhan_dui.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.InvalidAuthenticationException;
import com.evernote.client.android.OnClientCallback;
import com.evernote.edam.notestore.NoteCollectionCounts;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.User;
import com.evernote.thrift.transport.TTransportException;
import com.umeng.analytics.MobclickAgent;
import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.evermemo.SettingActivity.LoginCallback;
import com.zhan_dui.utils.Logger;
import com.zhan_dui.utils.Network;

public class Evernote implements LoginCallback {

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
	public static final long SYNC_DOWN_SPAN = 300l;
	private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE = EvernoteSession.EvernoteService.PRODUCTION;
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

	public boolean isLogin() {
		return mEvernoteSession.isLoggedIn();
	}

	public String getUsername() {
		return mSharedPreferences.getString(EVERNOTE_USER_NAME, null);
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
		MobclickAgent.onEvent(mContext, "Bind_EverNote");
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
										if (callback != null && task != null)
											callback.onFinshed(true, task);
									}

									@Override
									public void onException(Exception exception) {
										Logger.e(LogTag, "NoteBook创建失败");
										if (callback != null && task != null)
											callback.onFinshed(false, task);
									}

								});
			} catch (TTransportException e) {
				e.printStackTrace();
				Logger.e(LogTag, "NoteBook创建失败");
				if (callback != null && task != null)
					callback.onFinshed(false, task);
			}
		}
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

	@Override
	public void onAuthFinish(int resultCode) {
		if (resultCode == Activity.RESULT_OK) {
			mSharedPreferences.edit()
					.putString(EVERNOTE_TOKEN, mEvernoteSession.getAuthToken())
					.putLong(EVERNOTE_TOKEN_TIME, System.currentTimeMillis())
					.commit();
			makeSureNotebookExist(NOTEBOOK_NAME, true);
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

	private void makeSureNotebookExist(final String notebookname,
			final boolean syncdown) {
		if (mEvernoteSession.isLoggedIn()) {
			try {
				mEvernoteSession.getClientFactory().createNoteStoreClient()
						.listNotebooks(new OnClientCallback<List<Notebook>>() {

							@Override
							public void onSuccess(List<Notebook> data) {
								boolean find = false;
								for (Notebook notebook : data) {
									if (notebook.getName().equals(notebookname)) {
										Logger.e(LogTag, "确认名为" + notebookname
												+ "存在，重新设置Preference属性");
										mSharedPreferences
												.edit()
												.putString(
														EVERNOTE_NOTEBOOK_GUID,
														notebook.getGuid())
												.commit();
										find = true;
										if (syncdown) {
											syncDown();
										}
										break;
									}
								}
								if (!find) {
									Logger.e(LogTag, "未发现名为" + notebookname
											+ "的NoteBook，开始创建");
									createNotebook(notebookname, null, null);
								}
							}

							@Override
							public void onException(Exception exception) {
								Logger.e(
										LogTag,
										"列出所有Notebooks失败:"
												+ exception.getCause());
							}
						});
			} catch (TTransportException e) {
				Logger.e(LogTag, "列出所有Notebooks失败:" + e.getCause());
				e.printStackTrace();
			}
		}
	}

	private void checkAndInsert(String notebookGuid, final Memo memo) {
		if (mEvernoteSession.isLoggedIn()) {

			if (notebookGuid == null) {
				createNotebook(NOTEBOOK_NAME, memo, mNotebookCreateCallback);
				return;
			}

			try {
				mEvernoteSession
						.getClientFactory()
						.createNoteStoreClient()
						.getNotebook(notebookGuid,
								new OnClientCallback<Notebook>() {

									@Override
									public void onSuccess(Notebook data) {
										Logger.e(LogTag, NOTEBOOK_NAME
												+ "Notebook存在");
										handleMemo(memo);
									}

									@Override
									public void onException(Exception exception) {
										Logger.e(LogTag,
												NOTEBOOK_NAME + "Notebook不存在:"
														+ exception.getCause());
										createNotebook(NOTEBOOK_NAME, memo,
												mNotebookCreateCallback);
									}

								});

			} catch (TTransportException e) {
				Logger.e(LogTag, NOTEBOOK_NAME + "存在性检查错误:" + e.getCause());
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
								values.put(MemoDB.ENID, data.getGuid());
								values.put(MemoDB.HASH, data.getContentHash());
								values.put(MemoDB.SYNCSTATUS, Memo.NEED_NOTHING);
								values.put(MemoDB.UPDATEDTIME,
										data.getUpdated());
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
								Logger.e(LogTag,
										"Memo Note创建失败:" + exception.getCause());
								if (mEvernoteLoginCallback != null) {
									mEvernoteSyncCallback.CreateCallback(false,
											memo, null);
								}
							}
						});
			} catch (TTransportException e) {
				Logger.e(LogTag, "Memo Note创建失败:" + e.getCause());
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
								values.put(MemoDB.SYNCSTATUS, Memo.NEED_NOTHING);
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
								Logger.e(LogTag,
										"Memo Note更新失败:" + exception.getCause());
								if (mEvernoteSyncCallback != null) {
									mEvernoteSyncCallback.UpdateCallback(false,
											memo, null);
								}
							}
						});
			} catch (TTransportException e) {
				Logger.e(LogTag, "Memo Note更新失败:" + e.getCause());
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
		if (memo != null && mEvernoteSession.isLoggedIn()) {
			ContentValues values = new ContentValues();
			values.put(MemoDB.SYNCSTATUS, Memo.NEED_SYNC_UP);
			int ret = mContentResolver.update(
					ContentUris.withAppendedId(MemoProvider.MEMO_URI,
							memo.getId()), values, null, null);
			Logger.e(LogTag, "将" + ret + "个Memo设置为正在更新");
			createNote(memo,
					mSharedPreferences.getString(EVERNOTE_NOTEBOOK_GUID, null));
		} else {
			Logger.e(LogTag, "授权不可用或者Memo是Null");
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
										ContentValues values = new ContentValues();
										values.put(MemoDB.SYNCSTATUS,
												Memo.NEED_NOTHING);
										mContentResolver.update(ContentUris
												.withAppendedId(
														MemoProvider.MEMO_URI,
														memo.getId()), values,
												null, null);
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

	private void deleteMemo(Memo memo) {
		Logger.e(LogTag, "准备删除Memo");
		ContentValues values = new ContentValues();
		values.put(MemoDB.SYNCSTATUS, Memo.NEED_SYNC_DELETE);
		int ret = mContentResolver
				.update(ContentUris.withAppendedId(MemoProvider.MEMO_URI,
						memo.getId()), values, null, null);
		Logger.e(LogTag, "将" + ret + "个Memo设置为需要同步删除");
		if (memo != null && TextUtils.isEmpty(memo.getEnid()) == false) {
			deleteNote(memo);
		} else {
			Logger.e(LogTag,
					"打算删除的Memo缺少Enid信息，放弃，同时更新SyncStatus为NEED_NOTHING,返回删除成功标志");
			values.put(MemoDB.SYNCSTATUS, Memo.NEED_NOTHING);
			mContentResolver.update(
					ContentUris.withAppendedId(MemoProvider.MEMO_URI,
							memo.getId()), values, null, null);
			if (mEvernoteSyncCallback != null) {
				mEvernoteSyncCallback.DeleteCallback(true, memo);
			}
		}
	}

	public void deleteMemo(Memo memo, boolean onlywifi) {
		if (onlywifi) {
			if (Network.isWifi(mContext)) {
				deleteMemo(memo);
			}
		} else {
			deleteMemo(memo);
		}
	}

	private void syncUp() {
		if (mEvernoteSession.isLoggedIn())
			new SyncUpThread().start();
	}

	private void syncDown() {
		Logger.e(LogTag, "开始SyncDown");
		if (mEvernoteSession.isLoggedIn())
			new SyncDownThread().start();
	}

	public void syncUp(boolean onlywifi) {
		if (onlywifi) {
			if (Network.isWifi(mContext))
				syncUp();
		} else {
			syncUp();
		}
	}

	public void sync(boolean onlywifi) {
		long currentSeconds = TimeUnit.MILLISECONDS.toSeconds(System
				.currentTimeMillis());
		long previous = TimeUnit.MILLISECONDS.toSeconds(mSharedPreferences
				.getLong(LAST_SYNC_DOWN, 0));
		Logger.e(LogTag, "当前时间戳:" + currentSeconds);
		Logger.e(LogTag, "上一次时间戳：" + previous);
		if (onlywifi) {
			if (Network.isWifi(mContext)) {
				syncUp();
				if (currentSeconds - previous > SYNC_DOWN_SPAN) {
					new Thread() {
						public void run() {
							try {
								Thread.sleep(15000);
								syncDown();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						};
					}.start();
					mSharedPreferences
							.edit()
							.putLong(LAST_SYNC_DOWN, System.currentTimeMillis())
							.commit();
				}
			}
		} else {
			syncUp();
			if (currentSeconds - previous > SYNC_DOWN_SPAN) {
				new Thread() {
					public void run() {
						try {
							Thread.sleep(15000);
							syncDown();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}

					};
				}.start();
				mSharedPreferences.edit()
						.putLong(LAST_SYNC_DOWN, System.currentTimeMillis())
						.commit();
			}
		}
	}

	public void syncDown(boolean onlywifi) {
		if (onlywifi) {
			if (Network.isWifi(mContext))
				syncDown();
			else {
				return;
			}
		} else {
			syncDown();
		}
	}

	class SyncUpThread extends Thread {
		@Override
		public void run() {
			super.run();
			String selection = MemoDB.STATUS + "!='" + Memo.STATUS_DELETE
					+ "' and " + MemoDB.SYNCSTATUS + " != '"
					+ Memo.NEED_NOTHING + "'";
			Cursor cursor = mContentResolver.query(MemoProvider.MEMO_URI, null,
					selection, null, null);
			Logger.e(LogTag, "发现需要同步的数量:" + cursor.getCount());
			while (cursor.moveToNext()) {
				Memo memo = new Memo(cursor);

				if (memo.isNeedSyncDelete()) {
					Logger.e(LogTag, memo.getId() + "需要向上删除");
					deleteMemo(memo);
				} else if (memo.isNeedUpload()) {
					Logger.e(LogTag, memo.getId() + "需要向上添加");
					syncMemo(memo);
				}
			}
			cursor.close();
		}
	}

	class SyncDownThread extends Thread {
		@Override
		public void run() {
			super.run();
			startSyncDown(mSharedPreferences.getString(EVERNOTE_NOTEBOOK_GUID,
					null));
		}
	}

	private void startSyncDown(final String notebookguid) {
		NoteFilter filter = new NoteFilter();
		filter.setNotebookGuid(notebookguid);
		try {
			mEvernoteSession
					.getClientFactory()
					.createNoteStoreClient()
					.findNoteCounts(filter, false,
							new OnClientCallback<NoteCollectionCounts>() {

								@Override
								public void onSuccess(
										final NoteCollectionCounts data) {

									new Thread() {
										@Override
										public void run() {
											super.run();
											if (data.getNotebookCounts() != null
													&& data.getNotebookCounts()
															.get(notebookguid) != null) {
												int maxcount = data
														.getNotebookCounts()
														.get(notebookguid);
												getAllNotesInDirectory(
														notebookguid, maxcount);
											}
										}
									}.start();

								}

								@Override
								public void onException(Exception exception) {

								}
							});
		} catch (TTransportException e) {
			e.printStackTrace();
		}
	}

	@SuppressLint("UseSparseArrays")
	private void getAllNotesInDirectory(final String notebookguid,
			final int maxcount) {
		final ArrayList<String> prepareToDownload = new ArrayList<String>();
		final HashMap<Integer, String> prepareToUpdate = new HashMap<Integer, String>();
		if (notebookguid == null) {
			Logger.e(LogTag, "SyncDown获取数据出错1");
			return;
		}
		NoteFilter noteFilter = new NoteFilter();
		noteFilter.setNotebookGuid(notebookguid);
		noteFilter.setAscending(false);
		NotesMetadataResultSpec notesMetadataResultSpec = new NotesMetadataResultSpec();
		notesMetadataResultSpec.setIncludeUpdated(true);
		try {

			mEvernoteSession
					.getClientFactory()
					.createNoteStoreClient()
					.findNotesMetadata(noteFilter, 0, maxcount,
							notesMetadataResultSpec,
							new OnClientCallback<NotesMetadataList>() {

								@Override
								public void onSuccess(
										final NotesMetadataList data) {
									new Thread() {
										@Override
										public void run() {
											super.run();
											Logger.e(LogTag, "获取数据成功,大小是："
													+ data.getNotesSize());
											for (int i = 0; i < data.getNotes()
													.size(); i++) {
												NoteMetadata currentNoteMetadata = data
														.getNotes().get(i);
												Logger.e(
														LogTag,
														"获取到的数据:"
																+ currentNoteMetadata
																		.getUpdated()
																+ ""
																+ currentNoteMetadata
																		.getGuid());
												String selection = MemoDB.ENID
														+ "=?";
												Cursor cursor = mContentResolver
														.query(MemoProvider.MEMO_URI,
																new String[] {
																		MemoDB.UPDATEDTIME,
																		MemoDB.ID },
																selection,
																new String[] { currentNoteMetadata
																		.getGuid() },
																null);
												if (cursor.moveToNext()) {
													Logger.e(
															LogTag,
															"数据库存储时间:"
																	+ cursor.getLong(cursor
																			.getColumnIndex(MemoDB.UPDATEDTIME))
																	+ "");
													Logger.e(
															LogTag,
															"获取的存储时间:"
																	+ currentNoteMetadata
																			.getUpdated()
																	+ "");
													if (cursor.getLong(cursor
															.getColumnIndex(MemoDB.UPDATEDTIME)) != currentNoteMetadata
															.getUpdated()) {
														prepareToUpdate.put(
																cursor.getInt(cursor
																		.getColumnIndex(MemoDB.ID)),
																currentNoteMetadata
																		.getGuid());
													}
												} else {
													// 需要添加guid代表的信息
													prepareToDownload
															.add(currentNoteMetadata
																	.getGuid());
												}
												cursor.close();
											}
											startDownloadNeedDownload(prepareToDownload);
											startUpdateNeedUpdate(prepareToUpdate);
										}
									}.start();

								}

								@Override
								public void onException(Exception exception) {
									Logger.e(LogTag, "SyncDown获取数据出错2");
								}

							});
		} catch (TTransportException e) {
			Logger.e(LogTag, "SyncDown获取数据出错");
			e.printStackTrace();
		}
	}

	private void startDownloadNeedDownload(ArrayList<String> needToDownload) {
		for (final String guid : needToDownload) {
			try {
				mEvernoteSession
						.getClientFactory()
						.createNoteStoreClient()
						.getNote(guid, true, true, true, true,
								new OnClientCallback<Note>() {

									@Override
									public void onSuccess(final Note data) {

										new Thread() {
											@Override
											public void run() {
												super.run();

												Cursor cursor = mContentResolver
														.query(MemoProvider.MEMO_URI,
																new String[] { MemoDB.ID },
																MemoDB.ENID
																		+ "=?",
																new String[] { guid },
																null);

												if (cursor.getCount() == 0) {
													Memo memo = Memo
															.buildInsertMemoFromNote(data);
													ContentValues values = memo
															.toContentValues();
													Uri ret = mContentResolver
															.insert(MemoProvider.MEMO_URI,
																	values);
													Logger.e(
															LogTag,
															"添加了_id为:"
																	+ ret.getLastPathSegment());
												}
												cursor.close();
											}
										}.start();

									}

									@Override
									public void onException(Exception exception) {
										Logger.e(LogTag, "添加失败了");
									}
								});
			} catch (TTransportException e) {
				e.printStackTrace();
				Logger.e(LogTag, "添加失败了");
			}
		}
	}

	private void startUpdateNeedUpdate(HashMap<Integer, String> needToUpdate) {

		Iterator<Integer> iterator = needToUpdate.keySet().iterator();
		while (iterator.hasNext()) {
			final int _id = iterator.next();
			String guid = needToUpdate.get(_id);
			try {
				mEvernoteSession
						.getClientFactory()
						.createNoteStoreClient()
						.getNote(guid, true, true, true, true,
								new OnClientCallback<Note>() {

									@Override
									public void onSuccess(final Note data) {
										new Thread() {
											@Override
											public void run() {
												super.run();
												ContentValues values = Memo
														.buildUpdateMemoFromNote(
																data, _id)
														.toContentValues();
												mContentResolver.update(
														ContentUris
																.withAppendedId(
																		MemoProvider.MEMO_URI,
																		_id),
														values, null, null);
												Logger.e("更新了数据");
											}
										}.start();
									}

									@Override
									public void onException(Exception exception) {
										Logger.e(LogTag, "添加失败了");
									}
								});
			} catch (TTransportException e) {
				e.printStackTrace();
				Logger.e(LogTag, "添加失败了");
			}
		}
	}
}
