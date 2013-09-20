package com.zhan_dui.evermemo;

import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.edam.type.Note;
import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.sync.Evernote;
import com.zhan_dui.sync.Evernote.EvernoteSyncCallback;
import com.zhan_dui.utils.DateHelper;
import com.zhan_dui.utils.Logger;
import com.zhan_dui.utils.MarginAnimation;

@SuppressLint("NewApi")
public class MemoActivity extends FragmentActivity implements OnClickListener,
		OnKeyListener, OnTouchListener, EvernoteSyncCallback {

	private EditText mContentEditText;
	private TextView mDateText;
	private Memo memo;
	private boolean mCreateNew;
	private Context mContext;
	private Button mList;
	private Button mShare;
	private View mBottomBar;
	private ViewGroup mPullSaveLayout;
	private TextView mPullSaveTextView;
	private int mPullMarginTop;

	private String mLastSaveContent;

	private Timer mTimer;
	private Evernote mEvernote;

	private final String mBullet = " • ";
	private final String mNewLine = "\n";
	public static final String LogTag = "MemoActivity Log";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		overridePendingTransition(R.anim.push_up, R.anim.push_down);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.getSerializable("memo") != null) {
			memo = (Memo) bundle.getSerializable("memo");
			mCreateNew = false;
			mLastSaveContent = memo.getContent();
		} else {
			memo = new Memo();
			mCreateNew = true;
		}
		setContentView(R.layout.activity_memo);
		mDateText = (TextView) findViewById(R.id.time);
		mContentEditText = (EditText) findViewById(R.id.content);
		mList = (Button) findViewById(R.id.list);
		mShare = (Button) findViewById(R.id.share);
		mBottomBar = findViewById(R.id.bottom_bar);
		mPullSaveLayout = (ViewGroup) findViewById(R.id.pull_save);
		mPullSaveTextView = (TextView) mPullSaveLayout
				.findViewById(R.id.pull_save_text);
		mPullMarginTop = ((ViewGroup.MarginLayoutParams) mPullSaveLayout
				.getLayoutParams()).topMargin;
		mBottomBar.setOnClickListener(this);
		mList.setOnClickListener(this);
		mShare.setOnClickListener(this);
		mContentEditText.setText(Html.fromHtml(memo.getContent()));
		if (mCreateNew) {
			mDateText.setText(R.string.new_memo);
		} else {
			mDateText.setText(DateHelper.getReadableDate(mContext,
					mContext.getString(R.string.date_format),
					memo.getCreatedTime()));
		}
		mContentEditText.setOnKeyListener(this);
		mPullLayoutParams = (ViewGroup.MarginLayoutParams) mPullSaveLayout
				.getLayoutParams();
		mEvernote = new Evernote(mContext, this);
		// mContentEditText.setOnTouchListener(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			saveMemoAndLeave();
			return true;
		default:
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.list:
			clickList();
			break;

		default:
			break;
		}
	}

	private boolean clickEnter() {
		int currentPosition = mContentEditText.getSelectionStart();
		int newPosition = currentPosition;
		String currentText = mContentEditText.getText().toString();
		StringBuffer contentBuffer = new StringBuffer(currentText);
		int maxEnd = contentBuffer.length();
		int before3 = ((currentPosition - mBullet.length()) < 0) ? 0
				: (currentPosition - mBullet.length());
		int start = currentText.lastIndexOf(mNewLine, currentPosition - 1) + 1;
		start = (start == -1) ? 0 : start;
		int end = ((start + mBullet.length()) > maxEnd) ? maxEnd
				: (start + mBullet.length());

		if (contentBuffer.substring(start, end).equals(mBullet)) {
			if (maxEnd == end) {
				contentBuffer.replace(start, end, "\n");
				mContentEditText.setText(contentBuffer);
				mContentEditText.setSelection(contentBuffer.length());
				return true;
			} else if (contentBuffer.substring(before3, currentPosition)
					.equals(mBullet)) {
				contentBuffer.replace(start, end, "");
				newPosition = currentPosition - (end - start) + 1;
				mContentEditText.setText(contentBuffer);
				mContentEditText.setSelection(newPosition);
			} else {
				contentBuffer.insert(currentPosition, mNewLine + mBullet);
				mContentEditText.setText(contentBuffer);
				newPosition = ((currentPosition + mBullet.length() + mNewLine
						.length()) > contentBuffer.length()) ? contentBuffer
						.length()
						: (currentPosition + mBullet.length() + mNewLine
								.length());
				mContentEditText.setSelection(newPosition);
			}
			return true;
		}
		return false;

	}

	private void clickList() {
		int currentPosition = mContentEditText.getSelectionStart();
		int newPosition = currentPosition;
		String currentText = mContentEditText.getText().toString();
		StringBuffer contentBuffer = new StringBuffer(currentText);
		int maxEnd = contentBuffer.length();
		int start = currentText.lastIndexOf(mNewLine, currentPosition - 1) + 1;
		int end = ((start + mBullet.length()) > maxEnd) ? maxEnd
				: (start + mBullet.length());
		if (contentBuffer.substring(start, end).equals(mBullet)) {
			contentBuffer.replace(start, start + mBullet.length(), "");
			newPosition -= mBullet.length();
			newPosition = (newPosition < start) ? start : newPosition;
			newPosition = newPosition < 0 ? 0 : newPosition;
		} else {
			contentBuffer.insert(start, mBullet);
			if (currentPosition < currentPosition + mBullet.length()) {
				newPosition = currentPosition + mBullet.length();
			} else {
				newPosition += mBullet.length();
			}
		}
		mContentEditText.setText(contentBuffer);
		mContentEditText.setSelection(newPosition);
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_ENTER) {
			return clickEnter();
		}
		return false;
	}

	private ViewGroup.MarginLayoutParams mPullLayoutParams;

	private int dy;
	private int maxMarginTop = 60;
	private final float DRAG_RATIO = 0.1f;
	private boolean remeber = false;
	private boolean mstarttomove = false;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (mContentEditText.getScrollY() != 0)
			return false;

		if (mstarttomove) {
			mContentEditText.setSelection(0);
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Logger.e(LogTag, "Action Down");
			dy = (int) event.getY();
			return false;
		case MotionEvent.ACTION_MOVE:
			if (mContentEditText.getScrollY() != 0) {
				return false;
			} else if (!remeber) {
				dy = (int) event.getY();
				remeber = true;
			}
			int y = (int) event.getY();
			Logger.e(LogTag, "Action Move" + (y - dy) + "  " + (y - dy)
					* DRAG_RATIO);
			int newTop = (int) (mPullLayoutParams.topMargin + (y - dy)
					* DRAG_RATIO);
			newTop = (newTop > maxMarginTop) ? maxMarginTop : newTop;
			if (newTop > 0) {
				mPullSaveTextView.setText(R.string.relaxe_save_leave);
			} else {
				mPullSaveTextView.setText(R.string.pull_save_leave);
			}
			if (newTop > mPullMarginTop) {
				mPullLayoutParams.topMargin = newTop;
				mPullSaveLayout.setLayoutParams(mPullLayoutParams);
			}
			if (y - dy != 0)
				mstarttomove = true;
			break;
		case MotionEvent.ACTION_UP:
			Logger.e(LogTag, "Action Up");
			if (mPullLayoutParams.topMargin < 0) {
				mPullSaveLayout.startAnimation(new MarginAnimation(
						mPullSaveLayout, 0, mPullMarginTop, 0, 0));
			}
			if (mPullLayoutParams.topMargin > 0) {
				mPullSaveLayout.startAnimation(new MarginAnimation(
						mPullSaveLayout, 0, 0, 0, 0));
				saveMemoAndLeave();
			}
			mstarttomove = false;
			remeber = false;
			break;
		}
		return true;
	}

	private void saveMemo(Boolean toLeave) {
		if (mCreateNew
				&& mContentEditText.getText().toString().trim().length() == 0) {
			return;
		}

		if (mLastSaveContent == null) {
			mLastSaveContent = new String(mContentEditText.getText().toString());
		} else {
			if (mLastSaveContent.equals(mContentEditText.getText().toString())) {
				return;
			}
		}
		memo.setContent(Html.toHtml(mContentEditText.getText()));
		memo.setCursorPosition(mContentEditText.getSelectionStart());
		ContentValues values = memo.toContentValues();
		values.put(MemoDB.SYNCSTATUS, Memo.NEED_SYNC_UP);
		if (mCreateNew) {
			mCreateNew = false;
			Uri retUri = getContentResolver().insert(MemoProvider.MEMO_URI,
					values);
			memo.setId(Integer.valueOf(retUri.getLastPathSegment()));
		} else {
			if (mContentEditText.getText().toString().trim().length() == 0) {
				getContentResolver().delete(
						ContentUris.withAppendedId(MemoProvider.MEMO_URI,
								memo.getId()), null, null);
				mCreateNew = true;
			} else {
				getContentResolver().update(
						ContentUris.withAppendedId(MemoProvider.MEMO_URI,
								memo.getId()), values, null, null);
			}
		}
		if (toLeave) {
			mEvernote.sync(true);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mTimer.cancel();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				saveMemo(false);
			}
		}, 5000, 10000);
	}

	private void saveMemoAndLeave() {
		saveMemo(true);
		finish();
		overridePendingTransition(R.anim.out_push_up, R.anim.out_push_down);
	}

	@Override
	public void CreateCallback(boolean result, Memo memo, Note data) {
		if (result == true) {
			this.memo.setHash(data.getContentHash());
			this.memo.setEnid(data.getGuid());
			Toast.makeText(mContext, "添加成功", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mContext, "添加失败", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void UpdateCallback(boolean result, Memo memo, Note data) {
		if (result) {
			Toast.makeText(mContext, "修改成功", Toast.LENGTH_SHORT).show();
		} else {
			Toast.makeText(mContext, "修改失败", Toast.LENGTH_SHORT).show();
		}
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
