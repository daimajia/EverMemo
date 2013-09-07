package com.zhan_dui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.zhan_dui.data.Memo;
import com.zhan_dui.evermemo.R;

public class MemosAdapter extends CursorAdapter implements OnClickListener {

	private LayoutInflater mLayoutInflater;
	private int mOutItemId;
	private int mBottomMargin;

	public void setOutItem(int id) {
		if (id < 0 && id > getCount()) {
			mOutItemId = 0;
		}
		mOutItemId = id;
	}

	public MemosAdapter(Context context, Cursor c, boolean autoRequery) {
		super(context, c, autoRequery);
		mBottomMargin = -mContext.getResources().getDimensionPixelSize(
				R.dimen.bottom_margin_left);
		mLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id" });
		matrixCursor.addRow(new String[] { "0" });
		mCursor = new MergeCursor(new Cursor[] { matrixCursor, c });
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
			if (v.getTag() != null && v.getTag() == "1" && position != 0) {
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
			content.setText(memo.getContent());
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		if (cursor.getInt(cursor.getColumnIndex("_id")) == 0) {
			View FirstView = mLayoutInflater.inflate(R.layout.memo_add, parent,
					false);
			FirstView.setTag("1");
			return FirstView;
		} else {
			return mLayoutInflater.inflate(R.layout.memo_item, parent, false);
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}

}
