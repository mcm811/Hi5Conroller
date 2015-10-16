package com.changyoung.hi5controller;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by chang on 2015-10-13.
 */
public class Util {
	public static class TimeUtil {
		public static String getLasModified(File file) {
			return getTimeString("yyyy-MM-dd a hh-mm-ss", file.lastModified());
		}

		public static String getLasModified(String format, File file) {
			return getTimeString(format, file.lastModified());
		}

		public static String getTimeString(String format, long time) {
			return new SimpleDateFormat(format, Locale.KOREA).format(new Date(time));
		}
	}

	public static class FileUtil {
		public static String readFileString(String path) {
			StringBuilder sb = new StringBuilder();
			try {
				FileInputStream fileInputStream = new FileInputStream(path);
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String read;
				while ((read = bufferedReader.readLine()) != null) {
					sb.append(read);
					sb.append("\n");
				}

				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();
			} catch (FileNotFoundException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
			return sb.toString();
		}

		public static void delete(String path, boolean recursive) {
			delete(new java.io.File(path), recursive);
		}

		public static void delete(java.io.File dir, boolean recursive) {
			for (java.io.File file : dir.listFiles()) {
				if (file.isDirectory() && recursive) {
					delete(file, recursive);
				} else {
					file.delete();
				}
			}
			dir.delete();
		}

		public static void copy(String sourcePath, String destPath, boolean recursive) throws IOException {
			copy(new java.io.File(sourcePath), new java.io.File(destPath), recursive);
		}

		public static void copy(java.io.File source, java.io.File dest, boolean recursive) throws IOException {
			if (source.isFile()) {
				if (!dest.exists())
					dest.mkdirs();
				copy(source, new java.io.File(dest.getPath() + "/" + source.getName()));
			} else if (source.isDirectory()) {
				for (java.io.File file : source.listFiles()) {
					if (file.isDirectory() && recursive) {
						copy(file, new java.io.File(dest.getPath() + "/" + file.getName()), recursive);
					} else if (file.isFile()) {
						copy(file, new java.io.File(dest.getPath() + "/" + file.getName()));
					}
				}
			}
		}

		private static void copy(File source, File dest) throws IOException {
			try {
				FileChannel inputChannel = new FileInputStream(source).getChannel();
				FileChannel outputChannel = new FileOutputStream(dest).getChannel();
				outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
				inputChannel.close();
				outputChannel.close();
			} finally {
				// 삼성커널에서 setLastModified 미지원 인듯
				if (dest.setLastModified(source.lastModified())) {
					Log.d("Last Time - Source", TimeUtil.getLasModified(source));
					Log.d("Last Time - Dest", TimeUtil.getLasModified(dest));
				}
			}
		}
	}

	public static class UiUtil {
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
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setView(scrollView);

			builder.setPositiveButton("닫기", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			builder.show();
		}

		public static void adMobExitDialog(final Activity context) {
			AdView adView = new AdView(context);
			adView.setAdSize(AdSize.MEDIUM_RECTANGLE);
			if (BuildConfig.DEBUG)
				adView.setAdUnitId(context.getString(R.string.banner_ad_unit_id_debug));
			else
				adView.setAdUnitId(context.getString(R.string.banner_ad_unit_id_release));
			AdRequest adRequest = new AdRequest.Builder()
					.setRequestAgent("android_studio:ad_template").build();
			adView.loadAd(adRequest);

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("HI5 용접 관리");
			builder.setMessage("앱을 종료 하시겠습니까?");
			builder.setView(adView);
			builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					context.finish();
				}
			});
			builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			builder.show();
		}

		public static void hideSoftKeyboard(Activity activity, View view, KeyEvent event) {
			if (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
				InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (view == null)
					view = activity.getCurrentFocus();
				try {
					imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
					view.clearFocus();
				} catch (NullPointerException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
