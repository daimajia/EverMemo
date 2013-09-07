package com.zhan_dui.evermemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huewu.pla.lib.MultiColumnListView;
import com.huewu.pla.lib.internal.PLA_AdapterView;
import com.huewu.pla.lib.internal.PLA_AdapterView.OnItemClickListener;
import com.zhan_dui.adapters.MemosAdapter;
import com.zhan_dui.data.MemoDB;

public class StartActivity extends Activity implements OnTouchListener {

	private TextView mEverTextView;
	private TextView mMemoTextView;
	private MultiColumnListView mMemosGrid;
	private MemoDB mMemoDB;
	private Context mContext;
	private MemosAdapter mMemosAdapter;
	private LayoutInflater mLayoutInflater;
	private int mBottomMarginLeft;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		mContext = this;
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mBottomMarginLeft = -mContext.getResources().getDimensionPixelOffset(
				R.dimen.bottom_margin_left);
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
		mMemosGrid.setOnTouchListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	private float downX, downY, upX, upY;
	private float MIN_DISTANCE = 100;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: {
			downX = event.getX();
			downY = event.getY();
			return false;
		}
		case MotionEvent.ACTION_UP: {
			upX = event.getX();
			upY = event.getY();
			float deltaX, deltaY;
			deltaX = downX - upX;
			deltaY = downY - upY;
			if (Math.abs(deltaX) > MIN_DISTANCE && Math.abs(deltaY) < 80) {
				float currentXPosition = (upX + downX) / 2;
				float currentYPosition = (upY + downY) / 2;
				int position = mMemosGrid.pointToPosition(
						(int) currentXPosition, (int) currentYPosition);

				if (position == 0) {
					return false;
				} else {
					View view = null;
					int start = mMemosGrid.getFirstVisiblePosition();
					int offset = position - start;
					if (offset >= mMemosGrid.getChildCount() || offset < 0) {
						return false;
					} else {
						view = mMemosGrid.getChildAt(offset);
					}
					if (view == null) {
						return false;
					}
					LinearLayout target = (LinearLayout) (view
							.findViewById(R.id.bottom));
					LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) target
							.getLayoutParams();
					if (deltaX > 0) {
						// 右向左滑
						layoutParams.setMargins(-mBottomMarginLeft, 0, 0, 0);
					} else {
						// 左向右滑
						layoutParams.setMargins(0, 0, 0, 0);
						mMemosAdapter.setOutItem(position);
						mMemosAdapter.notifyDataSetChanged();
					}
					target.setLayoutParams(layoutParams);
					return true;
				}
			}
		}
		}

		return false;
	}
}
