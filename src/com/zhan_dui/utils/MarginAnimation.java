package com.zhan_dui.utils;

import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class MarginAnimation extends Animation {

	private final int finalLeft, finalRight, finalTop, finalBottom;
	private final int originLeft, originRight, originTop, originBottom;
	private final int spanLeft, spanRight, spanTop, spanBottom;
	private MarginLayoutParams originLayoutParams;
	private ViewGroup mTarget;

	public MarginAnimation(ViewGroup target, int finalLeft, int finalTop,
			int finalRight, int finalBottom) {
		this.finalBottom = finalBottom;
		this.finalTop = finalTop;
		this.finalRight = finalRight;
		this.finalLeft = finalLeft;
		originLayoutParams = (MarginLayoutParams) target.getLayoutParams();
		originBottom = originLayoutParams.bottomMargin;
		originLeft = originLayoutParams.leftMargin;
		originRight = originLayoutParams.rightMargin;
		originTop = originLayoutParams.topMargin;
		spanLeft = originLeft - this.finalLeft;
		spanRight = originRight - this.finalRight;
		spanBottom = originBottom - this.finalBottom;
		spanTop = originTop - this.finalTop;
		mTarget = target;
		setDuration(250);
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		super.applyTransformation(interpolatedTime, t);
		int newLeft, newRight, newTop, newBottom;
		newLeft = (int) (originLeft - (spanLeft * interpolatedTime));
		newRight = (int) (originRight - (spanRight * interpolatedTime));
		newTop = (int) (originTop - (spanTop * interpolatedTime));
		newBottom = (int) (originBottom - (spanBottom * interpolatedTime));
		originLayoutParams.setMargins(newLeft, newTop, newRight, newBottom);
		mTarget.setLayoutParams(originLayoutParams);
	}
}
