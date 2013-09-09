package com.zhan_dui.animation;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class LeftRightMarginAnimation extends Animation {
	private LinearLayout mTarget;
	private final int mOriginMarginLeft;
	private final int mFinalMarginLeft;
	private int mSpan;
	private LayoutParams mOriginLayoutParams;

	public LeftRightMarginAnimation(LinearLayout target, int finalLeft) {
		mTarget = target;
		mFinalMarginLeft = finalLeft;
		mOriginMarginLeft = ((LinearLayout.LayoutParams) target
				.getLayoutParams()).leftMargin;
		mOriginLayoutParams = new LayoutParams(target.getLayoutParams());
		mSpan = Math.abs(mFinalMarginLeft - mOriginMarginLeft);
		setDuration(250);
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		float moffset = interpolatedTime * mSpan;
		int targetLeftMargin = 0;
		if (mOriginMarginLeft < mFinalMarginLeft)
			targetLeftMargin = mOriginMarginLeft + (int) moffset;
		else {
			targetLeftMargin = mOriginMarginLeft - (int) moffset;
		}
		mOriginLayoutParams.setMargins(targetLeftMargin, 0, 0, 0);
		mTarget.setLayoutParams(mOriginLayoutParams);
	}
}
