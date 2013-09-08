package com.zhan_dui.evermemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.zhan_dui.data.Memo;
import com.zhan_dui.utils.DateHelper;

public class MemoActivity extends Activity implements OnClickListener,
		OnKeyListener {

	private EditText mContentEditText;
	private TextView mDateText;
	private Memo memo;
	private Boolean mCreateNew;
	private Context mContext;
	private Button mList;
	private Button mShare;
	private View mBottomBar;

	private final String mBullet = " • ";
	private final String mNewLine = "\n";

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
		mBottomBar.setOnClickListener(this);
		mList.setOnClickListener(this);
		mShare.setOnClickListener(this);
		mContentEditText.setText(memo.getContent());
		if (mCreateNew) {
			mDateText.setText(R.string.new_memo);
		} else {
			mDateText.setText(DateHelper.getReadableDate(mContext,
					mContext.getString(R.string.date_format),
					memo.getCreatedTime()));
		}
		mContentEditText.setOnKeyListener(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			finish();
			overridePendingTransition(R.anim.out_push_up, R.anim.out_push_down);
			break;
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
}
