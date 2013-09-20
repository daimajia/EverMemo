package com.zhan_dui.evermemo;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.edam.type.Note;
import com.huewu.pla.lib.MultiColumnListView;
import com.zhan_dui.adapters.MemosAdapter;
import com.zhan_dui.adapters.MemosAdapter.DeleteRecoverPanelLisener;
import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.sync.Evernote;
import com.zhan_dui.sync.Evernote.EvernoteSyncCallback;
import com.zhan_dui.sync.SyncTask;
import com.zhan_dui.utils.Logger;
import com.zhan_dui.utils.MarginAnimation;

public class StartActivity extends FragmentActivity implements
		LoaderCallbacks<Cursor>, DeleteRecoverPanelLisener, OnClickListener,
		EvernoteSyncCallback {

	private TextView mEverTextView;
	private TextView mMemoTextView;
	private MultiColumnListView mMemosGrid;
	private Context mContext;
	private MemosAdapter mMemosAdapter;
	private LinearLayout mUndoPanel;
	private Button mUndo, mSetting;
	private int mUndoPanelHeight;
	public static Evernote mEvernote;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		mContext = this;
		mEvernote = new Evernote(mContext, this);

		mEverTextView = (TextView) findViewById(R.id.ever);
		mMemoTextView = (TextView) findViewById(R.id.memo);
		mMemosGrid = (MultiColumnListView) findViewById(R.id.memos);
		mUndoPanel = (LinearLayout) findViewById(R.id.undo_panel);
		mUndo = (Button) findViewById(R.id.undo_btn);
		mSetting = (Button) findViewById(R.id.setting_btn);
		mUndoPanelHeight = mUndoPanel.getLayoutParams().height;
		Typeface roboto_bold = Typeface.createFromAsset(getAssets(),
				"fonts/Roboto-Bold.ttf");
		Typeface roboto_thin = Typeface.createFromAsset(getAssets(),
				"fonts/Roboto-Thin.ttf");

		mEverTextView.setTypeface(roboto_bold);
		mMemoTextView.setTypeface(roboto_thin);
		LoaderManager manager = getSupportLoaderManager();
		mMemosAdapter = new MemosAdapter(mContext, null,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER, this);
		mMemosGrid.setAdapter(mMemosAdapter);
		mUndo.setOnClickListener(this);
		mSetting.setOnClickListener(this);
		manager.initLoader(1, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cursorLoader = new CursorLoader(mContext,
				MemoProvider.MEMO_URI, null, null, null, null);
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
		} else if (v.getId() == R.id.setting_btn) {
			startActivity(new Intent(mContext, SettingActivity.class));
		}
	}

	private void deleteMemo(Memo memo) {
		mEvernote.deleteMemo(memo, true);
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
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {

		}

	}

	private Timer mSyncTimer;

	@Override
	protected void onPause() {
		super.onPause();
		// mSyncTimer.cancel();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// mSyncTimer = new Timer();
		// mSyncTimer.schedule(new SyncTask(mContext), 5000, 400000l);
	}

	@Override
	public void CreateCallback(boolean result, Memo memo, Note data) {
		// useless
	}

	@Override
	public void UpdateCallback(boolean result, Memo memo, Note data) {
		// useless
	}

	@Override
	public void DeleteCallback(boolean result, Memo memo) {
		if (result) {
			Toast.makeText(mContext, "删除成功", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mContext, "删除失败", Toast.LENGTH_SHORT).show();
		}
	}

}
