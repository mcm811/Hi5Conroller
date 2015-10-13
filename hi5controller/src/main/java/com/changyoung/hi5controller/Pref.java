package com.changyoung.hi5controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

/**
 * Created by chang on 2015-10-11.
 */
public class Pref {
	public final static String EXTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	public final static String STORAGE_PATH = "/storage";
	public final static String PACKAGE_NAME = "com.changyoung.hi5conroller";
	public final static String TAG_NAME = "hi5conroller";
	public final static String WORK_PATH_KEY = "work_path";
	public final static String BACKUP_PATH_KEY = "backup_path";

	public static String getWorkPath(Context context) {
		return getPath(context, WORK_PATH_KEY);
	}

	public static void setWorkPath(Context context, String value) {
		setPath(context, WORK_PATH_KEY, value);
	}

	public static String getBackupPath(Context context) {
		return getWorkPath(context) + "/Backup";
	}

	public static void setBackupPath(Context context, String value) {
		setPath(context, BACKUP_PATH_KEY, value);
	}

	public static String getPath(Context context, String key) {
		try {
			SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
			return prefs.getString(key, EXTERNAL_STORAGE_PATH);
		} catch (Exception e) {
			return EXTERNAL_STORAGE_PATH;
		}
	}

	public static void setPath(Context context, String key, String value) {
		try {
			if (key != null && value != null) {
				SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
				SharedPreferences.Editor prefEditor = prefs.edit();
				prefEditor.putString(key, value);
				prefEditor.commit();
			}
		} catch (Exception e) {
		}
	}
}
