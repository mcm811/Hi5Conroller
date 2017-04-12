package com.changyoung.hi5controller;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.widget.TextView;

public class TextViewerctivity extends AppCompatActivity {
/*
	private ScaleGestureDetector mScaleDetector;
	private float mScaleFactor = 1.f;
*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.text_viewer_activity);
		Toolbar toolbar = (Toolbar) findViewById(R.id.text_scroll_toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_work_path_main);
		if (fab != null) {
			fab.setOnClickListener(view -> finish());
		}

		//noinspection ConstantConditions
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Intent intent = getIntent();
		getSupportActionBar().setTitle(intent.getExtras().getString("title", "텍스트 뷰어"));

		TextView textView = (TextView) findViewById(R.id.text_scrolling_textView);
		if (textView != null) {
			textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
			textView.setText(intent.getExtras().getString("text", ""));
/*
			mScaleDetector = new ScaleGestureDetector(this, new ScaleListener());
			textView.setOnTouchListener((View v, MotionEvent event) -> {
				event.setLocation(event.getRawX(), event.getRawY());
				mScaleDetector.onTouchEvent(event);
				return true;
			});
*/
		}
	}

/*
	private class ScaleListener
			extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			mScaleFactor *= detector.getScaleFactor();

			// Don't let the object get too small or too large.
			mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

			if (textView != null) {
				textView.setScaleX(mScaleFactor);
				textView.setScaleY(mScaleFactor);
			}

			return true;
		}
	}
*/

}