package com.changyoung.hi5controller;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SplashActivity extends Activity {
	private final static String TAG = "SplashActivity";

	private final int MY_PERMISSION_REQUEST_STORAGE = 100;

	private boolean checkPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Log.i(TAG, "CheckPermission : " + checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE));
			if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED
					|| checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
					!= PackageManager.PERMISSION_GRANTED) {
				if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					Toast.makeText(this, "Read/Write external storage", Toast.LENGTH_SHORT).show();
				}
				requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE,
								Manifest.permission.WRITE_EXTERNAL_STORAGE },
						MY_PERMISSION_REQUEST_STORAGE);
				return false;
			} else {
				Log.e(TAG, "permission has been granted");
				return true;
			}
		} else {
			return true;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSION_REQUEST_STORAGE:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED
						&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					Log.d(TAG, "Permission granted");
				} else {
					Log.d(TAG, "Permission always deny");
					startActivity(new Intent(this, MainActivity.class));
				}
				break;
		}
		finish();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			if (checkPermission()) {
				Thread.sleep(1600);
				startActivity(new Intent(this, MainActivity.class));
				finish();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.d("Splash", "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
		}
	}
}
