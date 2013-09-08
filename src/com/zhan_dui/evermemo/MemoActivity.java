package com.zhan_dui.evermemo;

import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.EditText;

public class MemoActivity extends Activity {

	private EditText mContentEditText;
	private MemoDB mMemoDB;
	private Memo memo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		overridePendingTransition(R.anim.push_up, R.anim.push_down);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_memo);
		mContentEditText = (EditText) findViewById(R.id.content);
		mMemoDB = new MemoDB(this, MemoDB.Name, null, MemoDB.VERSION);
		memo = new Memo();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			memo.setContent(mContentEditText.getEditableText().toString());
			mMemoDB.insertMemo(memo);
			finish();
			overridePendingTransition(R.anim.out_push_up, R.anim.out_push_down);
		}
		return super.onKeyDown(keyCode, event);
	}

}
