package com.zhan_dui.evermemo;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.huewu.pla.lib.MultiColumnListView;
import com.umeng.analytics.MobclickAgent;
import com.zhan_dui.adapters.MemosAdapter;
import com.zhan_dui.adapters.MemosAdapter.DeleteRecoverPanelLisener;
import com.zhan_dui.adapters.MemosAdapter.ItemLongPressedLisener;
import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.sync.Evernote;
import com.zhan_dui.utils.Logger;
import com.zhan_dui.utils.MarginAnimation;

public class StartActivity extends ActionBarActivity implements
		LoaderCallbacks<Cursor>, DeleteRecoverPanelLisener, OnClickListener,
		ItemLongPressedLisener {

	private MultiColumnListView mMemosGrid;
	private Context mContext;
	private MemosAdapter mMemosAdapter;
	private LinearLayout mUndoPanel, mBindEvernotePanel;
	private SharedPreferences mSharedPreferences;
	private Button mUndo, mBindEvernote;
	private int mUndoPanelHeight, mBindEvernotePandelHeight;
	public static Evernote mEvernote;
	public static String sShownRate = "ShownRate";
	public static String sStartCount = "StartCount";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		mContext = this;
		mEvernote = new Evernote(mContext);
		MobclickAgent.onError(this);
		mMemosGrid = (MultiColumnListView) findViewById(R.id.memos);
		mUndoPanel = (LinearLayout) findViewById(R.id.undo_panel);
		mBindEvernotePanel = (LinearLayout) findViewById(R.id.evernote_panel);
		mUndo = (Button) findViewById(R.id.undo_btn);
		mBindEvernote = (Button) findViewById(R.id.bind_evernote);
		mUndoPanelHeight = mUndoPanel.getLayoutParams().height;
		mBindEvernotePandelHeight = mBindEvernotePanel.getLayoutParams().height;

		LoaderManager manager = getSupportLoaderManager();
		mMemosAdapter = new MemosAdapter(mContext, null,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER, this, this);
		mMemosGrid.setAdapter(mMemosAdapter);

		mUndo.setOnClickListener(this);
		manager.initLoader(1, null, this);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		if (mSharedPreferences.getInt(sStartCount, 1) == 1) {
			mBindEvernotePanel.startAnimation(new MarginAnimation(
					mBindEvernotePanel, 0, 0, 0, 0, 600));
			new Timer().schedule(new TimerTask() {

				@Override
				public void run() {

					StartActivity.this.runOnUiThread(new Runnable() {

						@Override
						public void run() {
							mBindEvernotePanel
									.startAnimation(new MarginAnimation(
											mBindEvernotePanel, 0, 0, 0,
											-mBindEvernotePandelHeight));
						}
					});
				}
			}, 5000);
			mSharedPreferences
					.edit()
					.putInt(sStartCount,
							mSharedPreferences.getInt(sStartCount, 1) + 1)
					.commit();
			mBindEvernote.setOnClickListener(this);
		}

		if (mSharedPreferences.getBoolean(
				SettingActivity.OPEN_MEMO_WHEN_START_UP, false)) {
			startActivity(new Intent(this, MemoActivity.class));
		}
		mEvernote.sync();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cursorLoader = new CursorLoader(mContext,
				MemoProvider.MEMO_URI, null, null, null, MemoDB.CREATEDTIME
						+ " desc");
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id" });
		matrixCursor.addRow(new String[] { "0" });
		Cursor c = new MergeCursor(new Cursor[] { matrixCursor, cursor });
		mMemosAdapter.swapCursor(c);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mMemosAdapter.swapCursor(null);
	}

	private DeleteAnimation m2DeleteAnimation = new DeleteAnimation(null);
	private Memo mToDeleteMemo;
	@SuppressLint("HandlerLeak")
	private Handler mHidePanelHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == TO_DELETE) {
				m2DeleteAnimation.setDeleteMemo(mToDeleteMemo);
			} else if (msg.what == NOT_TO_DELETE) {
				m2DeleteAnimation.setDeleteMemo(null);
			}
			mUndoPanel.startAnimation(new MarginAnimation(mUndoPanel, 0, 0, 0,
					-mUndoPanelHeight, m2DeleteAnimation));

		};
	};

	private boolean isDisplay = false;;
	private MarginAnimation m2ShowAnimation;
	private Timer mAnimationTimer;

	private final int TO_DELETE = 0;
	private final int NOT_TO_DELETE = 1;

	@Override
	public void wakeRecoveryPanel(Memo memo) {

		if (isDisplay) {
			mAnimationTimer.cancel();
			m2ShowAnimation.cancel();
			RelativeLayout.LayoutParams mLayoutParams = (android.widget.RelativeLayout.LayoutParams) mUndoPanel
					.getLayoutParams();
			mLayoutParams.setMargins(0, 0, 0, -mUndoPanelHeight);
			mUndoPanel.setLayoutParams(mLayoutParams);
			deleteMemo(mToDeleteMemo);
			MobclickAgent.onEvent(mContext, "delete_memo_from_swipe");
		}
		mToDeleteMemo = memo;
		m2ShowAnimation = new MarginAnimation(mUndoPanel, 0, 0, 0, 0);
		isDisplay = true;
		mUndoPanel.startAnimation(m2ShowAnimation);
		mAnimationTimer = new Timer();
		mAnimationTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				Looper.prepare();
				mHidePanelHandler.sendEmptyMessage(TO_DELETE);
				Looper.loop();
			}
		}, 7000);

	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.undo_btn) {
			mAnimationTimer.cancel();
			ContentValues values = mToDeleteMemo.toContentValues();
			values.put(MemoDB.STATUS, Memo.STATUS_COMMON);
			mHidePanelHandler.sendEmptyMessage(NOT_TO_DELETE);
			getContentResolver().update(
					ContentUris.withAppendedId(MemoProvider.MEMO_URI,
							mToDeleteMemo.getId()), values, null, null);
		} else if (v.getId() == R.id.bind_evernote) {
			mEvernote.auth();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case EvernoteSession.REQUEST_CODE_OAUTH:
			mEvernote.onAuthFinish(resultCode);
			break;
		}
	}

	private void deleteMemo(Memo memo) {
		mEvernote.sync();
		MobclickAgent.onEvent(mContext, "delete_memo");
		Logger.e("开始同步删除memo");
	}

	private class DeleteAnimation implements AnimationListener {

		private Memo memo;

		public DeleteAnimation setDeleteMemo(Memo memo) {
			this.memo = memo;
			return this;
		}

		public DeleteAnimation(Memo memo) {
			setDeleteMemo(memo);
		}

		@Override
		public void onAnimationStart(Animation animation) {

		}

		@Override
		public void onAnimationEnd(Animation animation) {
			isDisplay = false;
			if (memo != null) {
				deleteMemo(memo);
				MobclickAgent.onEvent(mContext, "delete_memo_from_swipe");
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {

		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		if (mSharedPreferences.getInt(MemoActivity.sEditCount, 0) == 5
				&& mSharedPreferences.getBoolean(sShownRate, false) == false) {

			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.rate_for_evernote)
					.setPositiveButton(R.string.rate_rate,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Uri uri = Uri.parse("market://details?id="
											+ mContext.getPackageName());
									Intent goToMarket = new Intent(
											Intent.ACTION_VIEW, uri);
									try {
										startActivity(goToMarket);
									} catch (ActivityNotFoundException e) {
										Toast.makeText(mContext,
												R.string.can_not_open_market,
												Toast.LENGTH_SHORT).show();
									}
								}
							})
					.setNegativeButton(R.string.rate_feedback,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									Intent Email = new Intent(
											Intent.ACTION_SEND);
									Email.setType("text/email");
									Email.putExtra(
											Intent.EXTRA_EMAIL,
											new String[] { getString(R.string.team_email) });
									Email.putExtra(Intent.EXTRA_SUBJECT,
											getString(R.string.feedback));
									Email.putExtra(Intent.EXTRA_TEXT,
											getString(R.string.email_title));
									startActivity(Intent.createChooser(Email,
											getString(R.string.email_chooser)));
								}
							}).create().show();
			mSharedPreferences.edit().putBoolean(sShownRate, true).commit();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settiing:
			Intent intent = new Intent(mContext, SettingActivity.class);
			startActivity(intent);
			break;
		case R.id.sync:
			mEvernote.sync();
			break;
		case R.id.feedback:
			Intent Email = new Intent(Intent.ACTION_SEND);
			Email.setType("text/email");
			Email.putExtra(Intent.EXTRA_EMAIL,
					new String[] { getString(R.string.team_email) });
			Email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback));
			Email.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_title));
			startActivity(Intent.createChooser(Email,
					getString(R.string.email_chooser)));
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			findViewById(R.id.more).performClick();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onActionItemClicked(ActionMode arg0, MenuItem arg1) {
			return false;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.context_menu, menu);
			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode arg0) {
			mActionMode = null;
			mMemosAdapter.setCheckMode(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) {
			return false;
		}

	};

	private ActionMode mActionMode;

	@Override
	public void onMemoItemLongClick(final View view, final int position,
			final Memo memo) {
		if (mActionMode != null) {
			return;
		}
		mActionMode = startSupportActionMode(mActionModeCallback);
		mMemosAdapter.setCheckMode(true);
		mMemosAdapter.toggleCheckedId(memo.getId(), memo);
	}
}
