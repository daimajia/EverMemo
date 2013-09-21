package com.zhan_dui.evermemo;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseObject;

public class FeedbackActivity extends Activity implements OnClickListener {
	private EditText mFeedback;
	private EditText mConnection;
	private Context mContext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feedback);
		mContext = this;
		mFeedback = (EditText) findViewById(R.id.suggestion);

		Parse.initialize(this, "sHXLkJsCOdAsGw5LB1c9AS0hLvaPjea04lxuLIon",
				"SflhwLgAMHtcJmSnzYIxAbv46892fyLq0CviKEfz");
		findViewById(R.id.send).setOnClickListener(this);
		findViewById(R.id.back).setOnClickListener(this);
		mFeedback = (EditText) findViewById(R.id.suggestion);
		mConnection = (EditText) findViewById(R.id.connection);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.send:
			String feedback = mFeedback.getText().toString();
			String connection = mConnection.getText().toString();
			if (feedback.length() == 0 || connection.length() == 0) {
				Toast.makeText(mContext, R.string.empty, Toast.LENGTH_SHORT)
						.show();
			} else {
				ParseObject feed = new ParseObject("Feedback");
				feed.put("content", feedback);
				feed.put("phone", android.os.Build.MODEL);
				feed.put("os", android.os.Build.VERSION.SDK_INT);
				feed.put("connection", connection);
				feed.saveInBackground();
				Toast.makeText(mContext, R.string.thanks, Toast.LENGTH_SHORT)
						.show();
				finish();
			}
			break;
		case R.id.back:
			finish();
			break;
		default:
			break;
		}
	}
}
