package com.zhan_dui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

import com.zhan_dui.evermemo.R;

public class MinMaxRelativeLayout extends RelativeLayout {
	private int minHeight;
	private int maxHeight;

	public MinMaxRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		minHeight = context.getResources().getDimensionPixelSize(
				R.dimen.content_min_height);
		maxHeight = context.getResources().getDimensionPixelSize(
				R.dimen.content_max_height);
	}

//	@Override
//	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		int widthSize = getMeasuredWidth();
//		int heightSize = getMeasuredHeight();
//		Log.e("width", widthSize+"");
//		Log.e("height", heightSize+"");
//		int targetHeight = heightSize;
////		if (heightSize > minHeight && heightSize < maxHeight) {
////			targetHeight = maxHeight;
////		}
////		
////		setMeasuredDimension(widthSize, targetHeight);
//	}
}
