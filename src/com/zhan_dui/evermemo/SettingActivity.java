package com.zhan_dui.evermemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

import com.evernote.client.android.EvernoteSession;
import com.zhan_dui.sync.Evernote;

public class SettingActivity extends Activity implements OnClickListener {
	private ViewGroup mBindEvernote;

	private Evernote mEvernote;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		overridePendingTransition(R.anim.in_push_right_to_left,
				R.anim.in_stable);
		mContext = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activtiy_setting);
		mBindEvernote = (ViewGroup) findViewById(R.id.bind_evernote);
		mBindEvernote.setOnClickListener(this);
		mEvernote = new Evernote(mContext);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			finish();
			overridePendingTransition(R.anim.in_stable,
					R.anim.out_push_left_to_right);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bind_evernote:
			mEvernote.auth();
			break;

		default:
			break;
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

	public interface LoginCallback {
		public void onAuthFinish(int resultCode);
	}
}
