package com.zhan_dui.evermemo;

import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.umeng.analytics.MobclickAgent;
import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.sync.Evernote;

public class MemoActivity extends ActionBarActivity implements OnClickListener,
		OnKeyListener {

	private EditText mContentEditText;
	private Memo memo;
	private boolean mCreateNew;
	private Context mContext;

	private String mLastSaveContent;

	private Timer mTimer;
	private Evernote mEvernote;

	private boolean mTextChanged = false;

	private final String mBullet = " • ";
	private final String mNewLine = "\n";
	public static final String LogTag = "MemoActivity Log";
	public static String sEditCount = "EditCount";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mContext = this;
		overridePendingTransition(R.anim.push_up, R.anim.push_down);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayShowTitleEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_memo);
		mContentEditText = (EditText) findViewById(R.id.content);
		Bundle bundle = getIntent().getExtras();
		if (bundle != null && bundle.getSerializable("memo") != null) {
			memo = (Memo) bundle.getSerializable("memo");
			mCreateNew = false;
			mLastSaveContent = memo.getContent();

		} else {
			memo = new Memo();
			mCreateNew = true;
		}

		mContentEditText.setText(Html.fromHtml(memo.getContent()));
		if (mCreateNew) {
			// mDateText.setText(R.string.new_memo);
			mContentEditText.requestFocus();
			getWindow().setSoftInputMode(
					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
			MobclickAgent.onEvent(mContext, "new_memo");
		} else {
			// mDateText.setText(DateHelper.getMemoDate(mContext,
			// memo.getCreatedTime()));
			MobclickAgent.onEvent(mContext, "edit_memo");
		}

		mContentEditText.setOnKeyListener(this);
		mEvernote = new Evernote(mContext);
		findViewById(R.id.edit_container).setOnClickListener(this);
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
		case R.id.edit_container:
			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.toggleSoftInputFromWindow(
					findViewById(R.id.edit_container)
							.getApplicationWindowToken(),
					InputMethodManager.SHOW_FORCED, 0);
			break;
		case R.id.list:
			clickList();
			break;
		case R.id.delete:
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setMessage(R.string.give_up_edit)
					.setTitle(R.string.give_up_title)
					.setPositiveButton(R.string.give_up_sure,
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									deleteAndLeave();
									MobclickAgent.onEvent(mContext,
											"delete_memo");
								}
							}).setNegativeButton(R.string.give_up_cancel, null)
					.create().show();
			break;
		default:
			break;
		}
	}

	private void share() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(android.content.Intent.EXTRA_TITLE,
				getText(R.string.share_title));
		shareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
				mContentEditText.getText());

		startActivity(Intent.createChooser(shareIntent,
				getText(R.string.share_via)));
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

		if (event.getAction() != KeyEvent.KEYCODE_BACK) {
			mTextChanged = true;
		}

		if (event.getAction() == KeyEvent.ACTION_DOWN
				&& keyCode == KeyEvent.KEYCODE_ENTER) {
			return clickEnter();
		}
		return false;
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
		if (toLeave && mTextChanged) {
			mEvernote.sync(true, false, null);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mTimer.cancel();
		MobclickAgent.onPause(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		mTimer = new Timer();
		mTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				saveMemo(false);
			}
		}, 5000, 10000);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.memo, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			saveMemoAndLeave();
			break;
		case R.id.li:
			clickList();
			break;
		case R.id.delete:
			deleteAndLeave();
			break;
		case R.id.share_to:
			share();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void saveMemoAndLeave() {
		saveMemo(true);
		int count = PreferenceManager.getDefaultSharedPreferences(mContext)
				.getInt(sEditCount, 0);
		if (count < 5) {
			count++;
			PreferenceManager.getDefaultSharedPreferences(mContext).edit()
					.putInt(sEditCount, count).commit();
		}
		mEvernote.sync(true, false, null);
		finish();
		overridePendingTransition(R.anim.out_push_up, R.anim.out_push_down);
	}

	private void deleteAndLeave() {
		if (memo.getId() != 0) {
			getContentResolver().delete(
					ContentUris.withAppendedId(MemoProvider.MEMO_URI,
							memo.getId()), null, null);
			mEvernote.sync(true, false, null);
		}
		finish();
		overridePendingTransition(R.anim.out_push_up, R.anim.out_push_down);
	}
}
