package com.zhan_dui.adapters;

import java.text.SimpleDateFormat;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.zhan_dui.data.Memo;
import com.zhan_dui.evermemo.MemoActivity;
import com.zhan_dui.evermemo.R;
import com.zhan_dui.utils.DateHelper;

public class MemosAdapter extends CursorAdapter implements OnClickListener,
		OnTouchListener {

	private LayoutInflater mLayoutInflater;
	private int mOutItemId;
	private int mBottomMargin;
	private SimpleDateFormat mSimpleDateFormat;
	private GestureDetectorCompat mGestureDetectorCompat;
	private View mCurrentTouchHover;
	private View mCurrentTouchBottom;

	public void setOutItem(int id) {
		if (id < 0 && id > getCount()) {
			mOutItemId = 0;
		}
		mOutItemId = id;
	}

	public MemosAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		mBottomMargin = -mContext.getResources().getDimensionPixelSize(
				R.dimen.bottom_margin_left);
		mLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mSimpleDateFormat = new SimpleDateFormat(
				context.getString(R.string.date_format));
		mGestureDetectorCompat = new GestureDetectorCompat(mContext,
				new ItemGuestureDetector());
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		if (!mDataValid) {
			throw new IllegalStateException(
					"this should only be called when the cursor is valid");
		}
		if (!mCursor.moveToPosition(position)) {
			throw new IllegalStateException("couldn't move cursor to position "
					+ position);
		}
		View v;
		if (convertView == null) {
			v = newView(mContext, mCursor, parent);
		} else {
			v = convertView;
			Log.e("position", position + "");
			if (v.getTag() != null && position != 0) {
				v = newView(mContext, mCursor, parent);
			}
			if (v.getTag() == null && position == 0) {
				v = newView(mContext, mCursor, parent);
			}

			if (position != 0) {
				LinearLayout.LayoutParams layoutParams = (LayoutParams) v
						.findViewById(R.id.bottom).getLayoutParams();
				if (position == mOutItemId) {
					layoutParams.setMargins(0, 0, 0, 0);
				} else {
					layoutParams.setMargins(-mBottomMargin, 0, 0, 0);
				}
				v.findViewById(R.id.bottom).setLayoutParams(layoutParams);
			}
		}
		bindView(v, mContext, mCursor);

		return v;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		int _id = cursor.getInt(cursor.getColumnIndex("_id"));
		if (cursor != null && view != null && _id != 0) {
			Memo memo = new Memo(cursor);
			TextView content = (TextView) view.findViewById(R.id.content);
			TextView date = (TextView) view.findViewById(R.id.date);
			content.setText(memo.getContent());
			date.setText(DateHelper.getReadableDate(mContext,
					mSimpleDateFormat, memo.getCreatedTime()));
			View bottomView = view.findViewById(R.id.bottom);
			View hoverView = view.findViewById(R.id.hover);
			bottomView.setTag(R.string.memo_data, memo);
			hoverView.setTag(R.string.memo_data, memo);
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		if (cursor.getInt(cursor.getColumnIndex("_id")) == 0) {
			View FirstView = mLayoutInflater.inflate(R.layout.memo_add, parent,
					false);
			FirstView.setTag("1");
			FirstView.setOnClickListener(this);
			return FirstView;
		} else {
			View commonView = mLayoutInflater.inflate(R.layout.memo_item,
					parent, false);
			View bottom = commonView.findViewById(R.id.bottom);
			View hover = commonView.findViewById(R.id.hover);
			hover.setTag(bottom);
			bottom.setOnClickListener(this);
			hover.setOnClickListener(this);
			hover.setOnTouchListener(this);
			return commonView;
		}
	}

	@Override
	public void onClick(View v) {
		long span = System.currentTimeMillis() - mLastChangeStatus;
		if (span < 500) {
			return;
		}

		if (v.getTag() == "1") {
			mContext.startActivity(new Intent(mContext, MemoActivity.class));
		} else {
			switch (v.getId()) {
			case R.id.bottom:

				break;
			default:
				break;
			}
		}

	}

	private long mLastChangeStatus = System.currentTimeMillis();

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		mCurrentTouchHover = v;
		mCurrentTouchBottom = (View) v.getTag();
		return mGestureDetectorCompat.onTouchEvent(event);
	}

	class ItemGuestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			float moveDistance = e1.getX() - e2.getX();

			if (moveDistance > 0 && moveDistance < 20) {
				return true;
			}

			if (moveDistance > -25 && moveDistance < 0) {
				return true;
			}
			LinearLayout.LayoutParams layoutParams = (LayoutParams) mCurrentTouchBottom
					.getLayoutParams();
			if (moveDistance > 0) {
				// 右向左滑
				layoutParams.setMargins(-mBottomMargin, 0, 0, 0);
			} else if (moveDistance < -0) {
				// 左向右滑
				layoutParams.setMargins(0, 0, 0, 0);
			}
			mLastChangeStatus = System.currentTimeMillis();
			mCurrentTouchBottom.setLayoutParams(layoutParams);
			return true;
		}

	}

}
