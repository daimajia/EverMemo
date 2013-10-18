package com.zhan_dui.adapters;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zhan_dui.data.Memo;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.evermemo.MemoActivity;
import com.zhan_dui.evermemo.R;
import com.zhan_dui.utils.DateHelper;

public class MemosAdapter extends CursorAdapter implements OnClickListener,
		OnLongClickListener {

	private LayoutInflater mLayoutInflater;

	private boolean mCheckMode;
	private HashMap<Integer, Memo> mCheckedItems;
	private Typeface mRobotoThin;
	private ItemLongPressedLisener mItemLongPressedLisener;
	private onItemSelectLisener mOnItemSelectLisener;

	public MemosAdapter(Context context, Cursor c, int flags,
			ItemLongPressedLisener itemLongPressedLisener,
			onItemSelectLisener selectLisener) {
		super(context, c, flags);
		mLayoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mRobotoThin = Typeface.createFromAsset(context.getAssets(),
				"fonts/Roboto-Thin.ttf");
		mContext.getContentResolver().registerContentObserver(
				MemoProvider.MEMO_URI, false,
				new UpdateObserver(mUpdateHandler));
		mItemLongPressedLisener = itemLongPressedLisener;
		mOnItemSelectLisener = selectLisener;
	}

	@SuppressLint("HandlerLeak")
	private Handler mUpdateHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			notifyDataSetChanged();
		};
	};

	class UpdateObserver extends ContentObserver {
		private Handler mHandler;

		public UpdateObserver(Handler handler) {
			super(handler);
			mHandler = handler;
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.sendEmptyMessage(0);
		}

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
			boolean isFirst = (Boolean) v.getTag(R.string.memo_first);
			if (isFirst && position != 0) {
				v = newView(mContext, mCursor, parent);
			}
			if (!isFirst && position == 0) {
				v = newView(mContext, mCursor, parent);
			}
		}

		if (position != 0) {
			v.findViewById(R.id.hover).setTag(R.string.memo_position, position);
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
			content.setText(Html.fromHtml(memo.getContent()));
			date.setText(DateHelper.getGridDate(mContext, memo.getCreatedTime()));
			View hoverView = view.findViewById(R.id.hover);
			View uploadView = view.findViewById(R.id.uploading);
			hoverView.setTag(R.string.memo_data, memo);
			hoverView.setTag(R.string.memo_id, _id);
			if (memo.isSyncingUp()) {
				uploadView.setVisibility(View.VISIBLE);
			} else {
				uploadView.setVisibility(View.INVISIBLE);
			}
			if (mCheckMode) {
				if (isChecked(memo.getId())) {
					hoverView
							.setBackgroundResource(R.drawable.hover_multi_background_normal);
				} else {
					hoverView
							.setBackgroundResource(R.drawable.hover_border_normal);
				}
			} else {
				hoverView.setBackgroundResource(R.drawable.hover_background);
			}
		}
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		if (cursor.getInt(cursor.getColumnIndex("_id")) == 0) {
			View firstView = mLayoutInflater.inflate(R.layout.memo_add, parent,
					false);
			firstView.setTag(R.string.memo_first, true);
			TextView textView = (TextView) firstView.findViewById(R.id.plus);
			textView.setTypeface(mRobotoThin);
			firstView.setOnClickListener(this);
			return firstView;
		} else {
			View commonView = mLayoutInflater.inflate(R.layout.memo_item,
					parent, false);
			commonView.setTag(R.string.memo_first, false);
			final View hover = commonView.findViewById(R.id.hover);
			hover.setOnClickListener(this);
			hover.setOnLongClickListener(this);
			return commonView;
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getTag(R.string.memo_first) != null
				&& (Boolean) v.getTag(R.string.memo_first) == true) {
			mContext.startActivity(new Intent(mContext, MemoActivity.class));
		} else {
			switch (v.getId()) {
			case R.id.hover:
				Memo memo = (Memo) v.getTag(R.string.memo_data);
				if (mCheckMode) {
					toggleCheckedId(memo.getId(), memo, v);
				} else {
					Intent intent = new Intent(mContext, MemoActivity.class);
					intent.putExtra("memo", memo);
					mContext.startActivity(intent);
				}
				break;
			default:
				break;
			}
		}

	}

	public interface ItemLongPressedLisener {
		public void startActionMode();
	}

	public interface onItemSelectLisener {
		public void onSelect();

		public void onCancelSelect();
	}

	public void setCheckMode(boolean check) {
		mCheckMode = check;
		if (mCheckMode == false) {
			mCheckedItems = null;
		}
		notifyDataSetChanged();
	}

	@SuppressLint("UseSparseArrays")
	public void toggleCheckedId(int _id, Memo memo, View v) {
		if (mCheckedItems == null) {
			mCheckedItems = new HashMap<Integer, Memo>();
		}
		if (mCheckedItems.containsKey(_id) == false) {
			mCheckedItems.put(_id, memo);
			mOnItemSelectLisener.onSelect();
		} else {
			mCheckedItems.remove(_id);
			mOnItemSelectLisener.onCancelSelect();
		}
		notifyDataSetChanged();
	}

	public boolean isChecked(int _id) {
		if (mCheckedItems == null || mCheckedItems.containsKey(_id) == false) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean onLongClick(View v) {
		if (mCheckMode == false) {
			mItemLongPressedLisener.startActionMode();
			setCheckMode(true);
		}
		Memo memo = (Memo) v.getTag(R.string.memo_data);
		toggleCheckedId(memo.getId(), memo, v);
		return true;
	}

	public int getSelectedCount() {
		if (mCheckedItems == null) {
			return 0;
		} else {
			return mCheckedItems.size();
		}
	}

}
