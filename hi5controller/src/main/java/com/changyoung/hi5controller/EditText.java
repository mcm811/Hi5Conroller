package com.changyoung.hi5controller;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

/**
 * Created by changmin on 2015. 10. 31..
 * changmin811@gmail.com
 */
public class EditText extends android.support.v7.widget.AppCompatEditText {
	public EditText(Context context) {
		super(context);
	}

	public EditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditText(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENTER
				|| keyCode == KeyEvent.KEYCODE_ESCAPE) {
			clearFocus();
		}
		return super.onKeyPreIme(keyCode, event);
	}
}