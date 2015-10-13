package com.changyoung.hi5controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SplashActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			Thread.sleep(800);
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} catch (Exception e) {
			Log.d("Splash", "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
		}
	}
}
