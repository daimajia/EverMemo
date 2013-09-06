package com.zhan_dui.evermemo;

import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoDB;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.EditText;

public class MemoActivity extends Activity {

	private EditText mContentEditText;
	private MemoDB mMemoDB;
	private Memo memo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
			if (memo.getContent() != "Test")
				mMemoDB.insertMemo(memo);
		}
		return super.onKeyDown(keyCode, event);
	}
}
