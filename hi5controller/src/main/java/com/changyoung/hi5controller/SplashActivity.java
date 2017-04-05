package com.changyoung.hi5controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SplashActivity extends Activity {
	private final static String TAG = "HI5:SplashActivity";

	/*
	private final int PERMISSION_REQUEST_STORAGE = 100;

	private void checkRWPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int writeExtPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			int readExtPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
			Log.e(TAG, "WRITE_EXTERNAL_STORAGE : " + writeExtPerm);
			Log.e(TAG, "READ_EXTERNAL_STORAGE : " + readExtPerm);

			if (writeExtPerm != PackageManager.PERMISSION_GRANTED || readExtPerm != PackageManager.PERMISSION_GRANTED) {
				if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					Toast.makeText(this, "Read/Write external storage", Toast.LENGTH_SHORT).show();
				}
				ActivityCompat.requestPermissions(this,
						new String[] {
								Manifest.permission.READ_EXTERNAL_STORAGE,
								Manifest.permission.WRITE_EXTERNAL_STORAGE
						},
						PERMISSION_REQUEST_STORAGE);
			} else {
				Log.e(TAG, "permission has been granted");
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_STORAGE:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED
						&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					Log.e(TAG, "Permission granted");
				} else {
					Log.e(TAG, "Permission always deny");
				}
				break;
		}
	}
	*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
//			checkRWPermission();
			Thread.sleep(1600);
			startActivity(new Intent(this, MainActivity.class));
			finish();
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("Splash", "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
		}
	}
}
