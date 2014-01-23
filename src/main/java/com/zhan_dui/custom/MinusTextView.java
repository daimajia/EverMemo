package com.zhan_dui.custom;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class MinusTextView extends TextView {

	private static int width;

	{
		WindowManager wm = (WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		width = display.getWidth();
	}

	public MinusTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		int widthSize = getMeasuredWidth();
		int heightSize = getMeasuredHeight();

		if (heightSize > width * 0.8) {
			heightSize = (int) (width * 0.8);
		}
		
		heightSize -= 70;

		setMeasuredDimension(widthSize, heightSize);
		requestLayout();
	}

	public int dpToPx(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}

}
