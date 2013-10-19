package com.zhan_dui.evermemo;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
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
import android.os.Message;
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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.huewu.pla.lib.MultiColumnListView;
import com.umeng.analytics.MobclickAgent;
import com.zhan_dui.adapters.MemosAdapter;
import com.zhan_dui.adapters.MemosAdapter.ItemLongPressedLisener;
import com.zhan_dui.adapters.MemosAdapter.onItemSelectLisener;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.sync.Evernote;
import com.zhan_dui.utils.Logger;
import com.zhan_dui.utils.MarginAnimation;

public class StartActivity extends ActionBarActivity implements
		LoaderCallbacks<Cursor>, OnClickListener, ItemLongPressedLisener,
		onItemSelectLisener {

	private MultiColumnListView mMemosGrid;
	private Context mContext;
	private MemosAdapter mMemosAdapter;
	private LinearLayout mBindEvernotePanel;
	private SharedPreferences mSharedPreferences;
	private Button mBindEvernote;
	private int mBindEvernotePandelHeight;
	public static Evernote mEvernote;
	public static String sShownRate = "ShownRate";
	public static String sStartCount = "StartCount";
	private Menu mMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setLogo(R.drawable.ab_logo);
		mContext = this;
		mEvernote = new Evernote(mContext);
		MobclickAgent.onError(this);
		mMemosGrid = (MultiColumnListView) findViewById(R.id.memos);
		mBindEvernotePanel = (LinearLayout) findViewById(R.id.evernote_panel);
		mBindEvernote = (Button) findViewById(R.id.bind_evernote);
		mBindEvernotePandelHeight = mBindEvernotePanel.getLayoutParams().height;

		LoaderManager manager = getSupportLoaderManager();
		mMemosAdapter = new MemosAdapter(mContext, null,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER, this, this);
		mMemosGrid.setAdapter(mMemosAdapter);

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
				SettingActivity.OPEN_MEMO_WHEN_START_UP, true)) {
			startActivity(new Intent(this, MemoActivity.class));
		}

		mEvernote.sync(true, true, null);
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

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.bind_evernote) {
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

	private Timer mSyncTimer;

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);

		if (mMenu != null) {
			MenuItem syncItem = mMenu.findItem(R.id.sync);
			if (!mEvernote.isLogin()) {
				syncItem.setTitle(R.string.menu_bind);
			} else {
				syncItem.setTitle(R.string.menu_sync);
			}
		}

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
		mSyncTimer = new Timer();
		Logger.e("启动自动更新任务");
		mSyncTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				mEvernote.sync(true, true, null);
			}
		}, 30000, 50000);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mSyncTimer != null) {
			Logger.e("结束定时同步任务");
			mSyncTimer.cancel();
		}
		MobclickAgent.onPause(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start, menu);
		mMenu = menu;
		MenuItem syncItem = menu.findItem(R.id.sync);
		if (!mEvernote.isLogin()) {
			syncItem.setTitle(R.string.menu_bind);
		} else {
			syncItem.setTitle(R.string.menu_sync);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settiing:
			Intent intent = new Intent(mContext, SettingActivity.class);
			startActivity(intent);
			break;
		case R.id.sync:
			if (mEvernote.isLogin() == false) {
				mEvernote.auth();
			} else {
				mEvernote.sync(true, true, new SyncHandler());
			}
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

	@SuppressLint("HandlerLeak")
	class SyncHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case Evernote.SYNC_START:
				findViewById(R.id.sync_progress).setVisibility(View.VISIBLE);
				break;
			case Evernote.SYNC_END:
				findViewById(R.id.sync_progress).setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			findViewById(R.id.more).performClick();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private Menu mContextMenu;
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onActionItemClicked(ActionMode arg0, MenuItem menuItem) {
			switch (menuItem.getItemId()) {
			case R.id.delete:
				if (mMemosAdapter.getSelectedCount() == 0) {
					Toast.makeText(mContext, R.string.delete_select_nothing,
							Toast.LENGTH_SHORT).show();
				} else {
					Builder builder = new Builder(mContext);
					builder.setMessage(R.string.delete_all_confirm)
							.setTitle(R.string.delete_title)
							.setPositiveButton(R.string.delete_sure,
									new DialogInterface.OnClickListener() {

										@Override
										public void onClick(
												DialogInterface dialog,
												int which) {
											mMemosAdapter.deleteSelectedMemos();
										}
									})
							.setNegativeButton(R.string.delete_cancel, null)
							.create().show();
				}
				break;
			default:
				break;
			}
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
			mContextMenu = null;
			mMemosAdapter.setCheckMode(false);
		}

		@Override
		public boolean onPrepareActionMode(ActionMode arg0, Menu menu) {
			mContextMenu = menu;
			menu.findItem(R.id.selected_counts).setTitle(
					mContext.getString(R.string.selected_count,
							mMemosAdapter.getSelectedCount()));
			return false;
		}

	};

	private ActionMode mActionMode;

	@Override
	public void startActionMode() {
		if (mActionMode != null) {
			return;
		}
		mActionMode = startSupportActionMode(mActionModeCallback);
	}

	@Override
	public void onSelect() {
		mContextMenu.findItem(R.id.selected_counts).setTitle(
				mContext.getString(R.string.selected_count,
						mMemosAdapter.getSelectedCount()));
	}

	@Override
	public void onCancelSelect() {
		mContextMenu.findItem(R.id.selected_counts).setTitle(
				mContext.getString(R.string.selected_count,
						mMemosAdapter.getSelectedCount()));
	}

}
