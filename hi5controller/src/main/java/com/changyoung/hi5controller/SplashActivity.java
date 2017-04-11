package com.changyoung.hi5controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SplashActivity extends Activity {
	private final static String TAG = "HI5:SplashActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			Thread.sleep(300);
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
		}
	}
}
