package com.zhan_dui.evermemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;

public class SettingActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		overridePendingTransition(R.anim.in_push_right_to_left,
				R.anim.in_stable);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activtiy_setting);
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
}
