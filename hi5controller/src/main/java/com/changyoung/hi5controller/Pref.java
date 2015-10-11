package com.changyoung.hi5controller;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Created by chang on 2015-10-11.
 */
public class Pref {
	public final static String PACKAGE_NAME = "Com.Changyoung.HI5Conroller.App";
	public final static String WORK_PATH_KEY = "work_path";
	public final static String BACKUP_PATH_KEY = "backup_path";
	private final static String STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();

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
			return prefs.getString(key, STORAGE_PATH);
		} catch (Exception e) {
			return STORAGE_PATH;
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

	public static String readFileString(String path) {
		String ret = "";
		try {
			FileInputStream fileInputStream = new FileInputStream(path);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String read;
			StringBuilder sb = new StringBuilder();
			while ((read = bufferedReader.readLine()) != null) {
				sb.append(read + "\n");
			}
			ret = sb.toString();

			bufferedReader.close();
			inputStreamReader.close();
			fileInputStream.close();
		} catch (Exception e) {
		}
		return ret;
	}

	public static void textViewActivity(Context context, String title, String text) {
		Intent intent = new Intent(context, TextScrollingActivity.class);
		intent.putExtra("title", title);
		intent.putExtra("text", text);
		context.startActivity(intent);
	}

	public static void textViewDialog(Context context, String text) {
		if (text != null)
			text = "";
		TextView textView = new TextView(context);
		textView.setPadding(10, 10, 10, 10);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
		textView.setText(text);

		ScrollView scrollView = new ScrollView(context);
		scrollView.addView(textView);
		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setView(scrollView);

		dialog.setPositiveButton("닫기", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});

		dialog.show();
	}
}
