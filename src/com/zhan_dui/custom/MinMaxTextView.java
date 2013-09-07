package com.zhan_dui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.zhan_dui.evermemo.R;

public class MinMaxTextView extends TextView {

	private int minHeight;
	private int maxHeight;

	public MinMaxTextView(Context context) {
		super(context);
	}

	public MinMaxTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		minHeight = context.getResources().getDimensionPixelSize(
				R.dimen.content_min_height);
		maxHeight = context.getResources().getDimensionPixelSize(
				R.dimen.content_max_height);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthSize = getMeasuredWidth();
		int heightSize = getMeasuredHeight();
		int targetHeight = heightSize;

		if (heightSize > minHeight && heightSize < maxHeight) {
			targetHeight = maxHeight;
		}
		setMeasuredDimension(widthSize, targetHeight);
		requestLayout();
	}
}
