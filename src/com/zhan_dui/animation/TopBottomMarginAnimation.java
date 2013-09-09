package com.zhan_dui.animation;

import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class TopBottomMarginAnimation extends Animation {
	private final LinearLayout mTarget;
	private final int mFinalTop;
	private RelativeLayout.LayoutParams mOriginLayoutParams;
	private final int mOriginTop;
	private final int mSpan;

	public TopBottomMarginAnimation(LinearLayout target, int finalTop) {
		mTarget = target;
		mFinalTop = finalTop;
		mOriginLayoutParams = (RelativeLayout.LayoutParams) target
				.getLayoutParams();
		mOriginTop = mOriginLayoutParams.topMargin;
		mSpan = Math.abs(mOriginTop - mFinalTop);
		setDuration(200);
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		float moffset = interpolatedTime * mSpan;
		int targetTop = 0;
		if (mOriginTop - mFinalTop < 0) {
			targetTop = (int) (mOriginTop + moffset);
		} else {
			targetTop = (int) (mOriginTop - moffset);
		}
		mOriginLayoutParams.setMargins(0, targetTop, 0, 0);
		mTarget.setLayoutParams(mOriginLayoutParams);
	}
}
