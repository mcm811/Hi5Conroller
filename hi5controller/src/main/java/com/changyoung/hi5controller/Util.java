package com.changyoung.hi5controller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by chang on 2015-10-13.
 * changmin811@gmail.com
 */
public class Util {
	public static class Pref {
		public final static String EXTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
		public final static String STORAGE_PATH = "/storage";
		public final static String PACKAGE_NAME = "com.changyoung.hi5conroller";
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
				e.printStackTrace();
				return EXTERNAL_STORAGE_PATH;
			}
		}

		public static void setPath(Context context, String key, String value) {
			try {
				if (key != null && value != null) {
					SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
					SharedPreferences.Editor prefEditor = prefs.edit();
					prefEditor.putString(key, value);
					prefEditor.apply();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class TimeUtil {
		public static String getLasModified(File file) {
			return getTimeString("yyyy-MM-dd a hh-mm-ss", file.lastModified());
		}

		@SuppressWarnings("unused")
		public static String getLasModified(String format, File file) {
			return getTimeString(format, file.lastModified());
		}

		public static String getTimeString(String format, long time) {
			return new SimpleDateFormat(format, Locale.KOREA).format(new Date(time));
		}
	}

	public static class FileUtil {
		private final static String TAG = "FileUtil";

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
				Log.d(TAG, e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return sb.toString();
		}

		@SuppressWarnings("unused")
		public static void delete(String path, boolean recursive) {
			delete(new File(path), recursive);
		}

		public static void delete(File dir, boolean recursive) {
			if (dir.isDirectory() && recursive) {
				for (File file : dir.listFiles()) {
					delete(file, true);
				}
			}
			if (dir.delete())
				Log.d(TAG, String.format("succeed dir.delete: %s", dir.getPath()));
			else
				Log.d(TAG, String.format("failed dir.delete: %s", dir.getPath()));
		}

		public static void copy(String sourcePath, String destPath, boolean recursive) throws IOException {
			copy(new File(sourcePath), new File(destPath), recursive);
		}

		public static void copy(File source, File dest, boolean recursive) throws IOException {
			if (source.isFile()) {
				if (!dest.exists() && !dest.mkdirs())
					return;
				copy(source, new File(dest.getPath() + "/" + source.getName()));
			} else if (source.isDirectory()) {
				for (File file : source.listFiles()) {
					if (file.isDirectory() && recursive) {
						copy(file, new File(dest.getPath() + "/" + file.getName()), true);
					} else if (file.isFile()) {
						copy(file, new File(dest.getPath() + "/" + file.getName()));
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

		public static String backup(Context context, View view) {
			String ret = null;
			boolean sourceChecked = false;
			try {
				File source = new File(Util.Pref.getWorkPath(context));

				if (source.exists()) {
					for (File file : source.listFiles()) {
						String fileName = file.getName().toUpperCase();
						if (fileName.startsWith("HX") || fileName.endsWith("JOB") || fileName.startsWith("ROBOT")) {
							sourceChecked = true;
							break;
						}
					}
				}
				if (!sourceChecked) {
					ret = "백업 실패: 작업 경로에 job, robot, hx 파일이 없습니다";
					throw new Exception();
				}

				@SuppressWarnings("SpellCheckingInspection")
				File dest = new File(source.getPath() + "/Backup/" + source.getName()
						+ Util.TimeUtil.getTimeString("_yyyyMMdd_HHmmss",
						System.currentTimeMillis()));
				if (source.exists() && source.isDirectory()) {
					if (dest.exists())
						Util.FileUtil.delete(dest, false);
					if (dest.mkdirs())
						Log.d("backup", "dest.mkdirs");
					new AsyncTaskFileDialog(context, view, "백업").execute(source, dest);
					return null;
				}
				throw new Exception();
			} catch (IOException e) {
				e.printStackTrace();
				ret = "백업 실패: 파일 복사 오류";
			} catch (Exception e) {
				e.printStackTrace();
				if (ret == null)
					ret = "백업 실패";
			}
			return ret;
		}
	}

	public static class UiUtil {
		public static void textViewActivity(Context context, String title, String text) {
			Intent intent = new Intent(context, TextScrollingActivity.class);
			intent.putExtra("title", title);
			intent.putExtra("text", text);
			context.startActivity(intent);
		}

		@SuppressWarnings("unused")
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
			adView.setScaleX(0.95f);
			adView.setScaleY(0.95f);
			if (BuildConfig.DEBUG)
				adView.setAdUnitId(context.getString(R.string.banner_ad_unit_id_debug));
			else
				adView.setAdUnitId(context.getString(R.string.banner_ad_unit_id_release));
			AdRequest adRequest = new AdRequest.Builder()
					.setRequestAgent("android_studio:ad_template").build();
			adView.loadAd(adRequest);

			TextView tvMessage = new TextView(context);
			tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
			tvMessage.setTypeface(Typeface.DEFAULT_BOLD);
			tvMessage.setPadding(40, 10, 40, 0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
				tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
			tvMessage.setText("종료 하시겠습니까?");

			LinearLayout linearLayout = new LinearLayout(context);
			linearLayout.setOrientation(LinearLayout.VERTICAL);
			linearLayout.addView(adView);
			linearLayout.addView(tvMessage);

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("HI5 용접 관리");
			builder.setView(linearLayout);
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
					if (view != null) {
						imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
						view.clearFocus();
					}
				} catch (NullPointerException e) {
					Log.d("hideSoftKeyboard", e.getLocalizedMessage());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class AsyncTaskFileDialog extends AsyncTask<File, String, String> {
		private static final int MSG_REFRESH_DIR = 0;
		private static final int MSG_REFRESH_PARENT_DIR = 1;

		private final static long BASE_SLEEP_TIME = 250;
		private ProgressDialog progressDialog;
		private Context mContext;
		private View view;
		private String msg;
		private Handler handler;

		public AsyncTaskFileDialog(Context context, View view, String msg) {
			mContext = context;
			this.view = view;
			this.msg = msg;
		}

		public AsyncTaskFileDialog(Context context, View view, String msg, Handler handler) {
			mContext = context;
			this.view = view;
			this.msg = msg;
			this.handler = handler;
		}

		@Override
		protected void onPreExecute() {
			progressDialog = new ProgressDialog(mContext);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setTitle(msg);
			progressDialog.setMessage(msg);
			progressDialog.show();
			super.onPreExecute();
		}

		private String asyncTaskCopy(File source, File dest) {
			if (source.isFile()) {
				if (!dest.exists() && !dest.mkdirs())
					return "대상 폴더 생성 실패";
				try {
					publishProgress("max", "1");
					publishProgress("progress", "1",
							String.format("%s %s 중", source.getName(), msg));
					FileUtil.copy(source, new File(dest.getPath() + "/" + source.getName()));
				} catch (IOException e) {
					return e.getLocalizedMessage();
				}
			} else if (source.isDirectory()) {
				try {
					File[] files = source.listFiles();
					publishProgress("max", String.valueOf(files.length));
					for (int i = 0, filesLength = files.length; i < filesLength; i++) {
						final long sleepTime = BASE_SLEEP_TIME / filesLength;
						File file = files[i];
						if (file.isFile()) {
							publishProgress("progress", String.valueOf(i),
									String.format("%s %s 중", file.getName(), msg));
							FileUtil.copy(file, new File(dest.getPath() + "/" + file.getName()));
							if (filesLength < 20) {
								try {
									Thread.sleep(sleepTime);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
					}
				} catch (IOException e) {
					return e.getLocalizedMessage();
				}
			}
			return String.format("%s 완료: %s", msg, dest.getName());
		}

		private void searchFile(List<File> deleteList, File dir) {
			if (dir.isDirectory()) {
				for (File file : dir.listFiles()) {
					searchFile(deleteList, file);
				}
			}
			deleteList.add(dir);
		}

		private String asyncTaskDelete(File dir) {
			List<File> deleteList = new ArrayList<>();
			searchFile(deleteList, dir);
			publishProgress("max", String.valueOf(deleteList.size()));
			long sleepTime = BASE_SLEEP_TIME / deleteList.size();

			for (int i = 0, deleteListSize = deleteList.size(); i < deleteListSize; i++) {
				File file = deleteList.get(i);
				publishProgress("progress", String.valueOf(i),
						String.format("%s %s 중", file.getName(), msg));
				//noinspection ResultOfMethodCallIgnored
				file.delete();
				if (deleteListSize < 20) {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			return "삭제 완료: " + dir.getName();
		}

		@Override
		protected String doInBackground(File... params) {
			if (msg.equals("백업") || msg.equals("복원")) {
				if (params.length != 2) return "인수 오류";
				return asyncTaskCopy(params[0], params[1]);
			} else if (msg.equals("삭제")) {
				if (params.length != 1) return "인수 오류";
				return asyncTaskDelete(params[0]);
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(String... progress) {
			if (progress[0].equals("progress")) {
				progressDialog.setProgress(Integer.parseInt(progress[1]));
				progressDialog.setMessage(progress[2]);
			} else if (progress[0].equals("max")) {
				progressDialog.setMax(Integer.parseInt(progress[1]));
			}
		}

		@Override
		protected void onPostExecute(String result) {
			progressDialog.dismiss();
			if (result != null && view != null) {
				Snackbar.make(view, result, Snackbar.LENGTH_SHORT)
						.setAction("Action", null).show();
				Log.d("AsyncTask", "onPostExecute: " + result);
			}
//			if (handler != null) {
//				Message msg = handler.obtainMessage();
//				msg.what = MSG_REFRESH_DIR;
//				msg.obj = null;
//				handler.sendMessage(msg);
//			}
		}
	}
}
