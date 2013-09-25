package com.zhan_dui.custom;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.TextView;

public class MinusTextView extends TextView {

	public MinusTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthSize = getMeasuredWidth();
		int heightSize = getMeasuredHeight();
		int targetHeight = heightSize - dpToPx(30);
		setMeasuredDimension(widthSize, targetHeight);
		requestLayout();
	}

	public int dpToPx(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}

}
