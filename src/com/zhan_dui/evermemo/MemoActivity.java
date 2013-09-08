package com.zhan_dui.evermemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.EditText;

import com.zhan_dui.data.Memo;

public class MemoActivity extends Activity {

	private EditText mContentEditText;
	private Memo memo;
	private Boolean mCreateNew;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
		mContentEditText = (EditText) findViewById(R.id.content);
		mContentEditText.setText(memo.getContent());
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			overridePendingTransition(R.anim.out_push_up, R.anim.out_push_down);
		}
		return super.onKeyDown(keyCode, event);
	}

}
