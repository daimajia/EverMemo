package com.zhan_dui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public class MinMaxViewGroup extends ViewGroup {
	public MinMaxViewGroup(Context context) {
		super(context);
	}

	public MinMaxViewGroup(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {

	}

}
