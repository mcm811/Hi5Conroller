package com.changyoung.hi5controller;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.Animation;
import android.view.animation.Transformation;

/**
 * Created by changmin on 2015. 11. 2..
 * changmin811@gmila.com
 */
public class Rotate3dAnimation extends Animation {
	private final float mFromDegrees;
	private final float mToDegrees;
	private final float mPivotX;
	private final float mPivotY;
	private final float mDepthZ;
	private final boolean mReverse;
	private Camera mCamera;

	public Rotate3dAnimation(float fromDegrees, float toDegrees, float pivotX,
	                         float pivotY, float depthZ, boolean reverse) {
		mFromDegrees = fromDegrees;
		mToDegrees = toDegrees;
		mPivotX = pivotX;
		mPivotY = pivotY;
		mDepthZ = depthZ;
		mReverse = reverse;
	}

	@Override
	public void initialize(int width, int height, int parentWidth,
	                       int parentHeight) {
		super.initialize(width, height, parentWidth, parentHeight);
		mCamera = new Camera();
	}

	@Override
	protected void applyTransformation(float interpolatedTime, Transformation t) {
		final float fromDegrees = mFromDegrees;
		float degrees = fromDegrees
				+ ((mToDegrees - fromDegrees) * interpolatedTime);

		final float pivotX = mPivotX;
		final float pivotY = mPivotY;
		final Camera camera = mCamera;

		final Matrix matrix = t.getMatrix();

		camera.save();
		if (mReverse) {
			camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
		} else {
			camera.translate(0.0f, 0.0f, mDepthZ * (1.0f - interpolatedTime));
		}
		camera.rotateZ(degrees);
		camera.getMatrix(matrix);
		camera.restore();

		matrix.preTranslate(-pivotX, -pivotY);
		matrix.postTranslate(pivotX, pivotY);
	}
}