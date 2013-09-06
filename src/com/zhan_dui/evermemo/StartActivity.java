package com.zhan_dui.evermemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.huewu.pla.lib.MultiColumnListView;
import com.huewu.pla.lib.internal.PLA_AdapterView;
import com.huewu.pla.lib.internal.PLA_AdapterView.OnItemClickListener;
import com.zhan_dui.adapters.MemosAdapter;
import com.zhan_dui.data.MemoDB;

public class StartActivity extends Activity {

	private TextView mEverTextView;
	private TextView mMemoTextView;
	private MultiColumnListView mMemosGrid;
	private MemoDB mMemoDB;
	private Context mContext;
	private MemosAdapter mMemosAdapter;
	private LayoutInflater mLayoutInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		mContext = this;
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mEverTextView = (TextView) findViewById(R.id.ever);
		mMemoTextView = (TextView) findViewById(R.id.memo);
		mMemosGrid = (MultiColumnListView) findViewById(R.id.memos);

		Typeface roboto_bold = Typeface.createFromAsset(getAssets(),
				"fonts/Roboto-Bold.ttf");
		Typeface roboto_thin = Typeface.createFromAsset(getAssets(),
				"fonts/Roboto-Thin.ttf");

		mEverTextView.setTypeface(roboto_bold);
		mMemoTextView.setTypeface(roboto_thin);

		mMemoDB = new MemoDB(mContext, MemoDB.Name, null, MemoDB.VERSION);
		mMemosAdapter = new MemosAdapter(mContext, mMemoDB.getAllMemos(), false);
		mMemosGrid.setAdapter(mMemosAdapter);
		mMemosGrid.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(PLA_AdapterView<?> parent, View view,
					int position, long id) {
				Toast.makeText(mContext, position + " ", Toast.LENGTH_SHORT)
						.show();
			}

		});
		mMemosAdapter.notifyDataSetChanged();
//		startActivity(new Intent(mContext, MemoActivity.class));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

}
