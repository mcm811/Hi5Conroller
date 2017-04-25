package com.changyoung.hi5controller.common;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.changyoung.hi5controller.weldutil.WeldTextViewerActivity;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.changyoung.hi5controller.weldfile.WeldFileListFragment.MSG_REFRESH_DIR;

/**
 * Created by chang on 2015-10-13.
 * changmin811@gmail.com
 */
public class Helper {
	public static class Pref {
		public final static String EXTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
		public final static String STORAGE_PATH = "/storage";
		public final static String ORDER_TYPE_KEY = "order_type";
		public final static String LAYOUT_TYPE_KEY = "layout_type";
		final static String PACKAGE_NAME = "com.changyoung.hi5controller";
		final static String WORK_PATH_KEY = "work_path";
		final static String WORK_URI_KEY = "work_uri";
		final static String BACKUP_PATH_KEY = "backup_path";

		static DocumentFile getDocumentFile(Context context, String fileName) throws NullPointerException {
			return getWorkDocumentFile(context).findFile(fileName);
		}

		public static DocumentFile getWorkDocumentFile(Context context) throws NullPointerException {
			DocumentFile documentFile = DocumentFile.fromTreeUri(context, Pref.getWorkPathUri(context));
			if (documentFile == null)
				throw new NullPointerException();
			return documentFile;
		}

		static Uri getWorkPathUri(Context context) {
			return Uri.parse(getWorkPathUriString(context));
		}

		public static String getWorkPathUriString(Context context) {
			return getPath(context, WORK_URI_KEY);
		}

		static DocumentFile getWorkPathDocumentFile(Context context, String path) {
			File file = new File(path);
			if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				try {
					Uri uri = getWorkPathUri(context);
					DocumentFile documentFile = DocumentFile.fromTreeUri(context, uri);
					return documentFile.findFile(file.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				DocumentFile documentFile = DocumentFile.fromFile(file);
				return documentFile.findFile(file.getName());
			}
			return null;
		}

		public static InputStream getWorkPathInputStream(Context context, String path) {
			InputStream inputStream = null;
			try {
				if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					try {
						DocumentFile documentFile = getWorkPathDocumentFile(context, path);
						if (documentFile != null)
							inputStream = context.getContentResolver().openInputStream(documentFile.getUri());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					inputStream = new FileInputStream(path);
					Log.e("HI5", "FileInputStream:" + path);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return inputStream;
		}

		public static OutputStream getWorkPathOutputStream(Context context, String path) {
			OutputStream outputStream = null;
			try {
				if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					try {
						DocumentFile documentFile = getWorkPathDocumentFile(context, path);
						if (documentFile != null)
							outputStream = context.getContentResolver().openOutputStream(documentFile.getUri());
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					outputStream = new FileOutputStream(path, false);
					Log.e("HI5", "FileOutputStream:" + path);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return outputStream;
		}

		public static void setWorkPathUri(Context context, String value) {
			setPath(context, WORK_URI_KEY, value);
		}

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

		static String getPath(Context context, @SuppressWarnings("SameParameterValue") String key) {
			return getString(context, key, EXTERNAL_STORAGE_PATH);
		}

		static void setPath(Context context, String key, String value) {
			putString(context, key, value);
		}

		static String getString(Context context, String key, @SuppressWarnings("SameParameterValue") String defValue) {
			try {
				SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
				return prefs.getString(key, defValue);
			} catch (Exception e) {
				e.printStackTrace();
				return defValue;
			}
		}

		static void putString(Context context, String key, String value) {
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

		public static int getInt(Context context, String key, int defValue) {
			try {
				SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
				return prefs.getInt(key, defValue);
			} catch (Exception e) {
				e.printStackTrace();
				return defValue;
			}
		}

		public static void putInt(Context context, String key, int value) {
			try {
				if (key != null) {
					SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
					SharedPreferences.Editor prefEditor = prefs.edit();
					prefEditor.putInt(key, value);
					prefEditor.apply();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class TimeHelper {
		public static String getLasModified(File file) {
			return getTimeString("yyyy-MM-dd a hh-mm-ss", file.lastModified());
		}

		@SuppressWarnings("unused")
		public static String getLasModified(String format, File file) {
			return getTimeString(format, file.lastModified());
		}

		static String getTimeString(String format, long time) {
			return new SimpleDateFormat(format, Locale.KOREA).format(new Date(time));
		}
	}

	@SuppressWarnings("unused")
	public static class FileHelper {
		private final static String TAG = "FileHelper";

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
				Log.i(TAG, e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return sb.toString();
		}

		@SuppressWarnings("unused")
		public static void delete(Activity activity, String path, boolean recursive) throws IOException {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				delete(new File(path), recursive);
			} else {
				Uri uri = UriHelper.getWorkPathFileUri(activity, path);
				if (uri != null) {
					if (recursive) {
						UriHelper.deleteTreeUri(activity, uri);
					} else {
						UriHelper.deleteUri(activity, uri);
					}
				}
			}
		}

		static void delete(File dir, boolean recursive) throws IOException {
			if (dir.isDirectory() && recursive) {
				for (File file : dir.listFiles()) {
					delete(file, true);
				}
			}
			if (dir.delete())
				Log.i(TAG, String.format("succeed dir.delete: %s", dir.getPath()));
			else
				Log.i(TAG, String.format("failed dir.delete: %s", dir.getPath()));
		}

		public static void copy(String sourcePath, String destPath, boolean recursive) throws IOException {
			copy(new File(sourcePath), new File(destPath), recursive);
		}

		static void copy(File source, File dest, boolean recursive) throws IOException {
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
					Log.i("Last Time - Source", TimeHelper.getLasModified(source));
					Log.i("Last Time - Dest", TimeHelper.getLasModified(dest));
				}
			}
		}

		@Nullable
		public static String backupDocumentFile(Context context, View view) {
			String ret = null;
			boolean sourceChecked = false;
			try {
				DocumentFile source = Pref.getWorkDocumentFile(context);
				if (source.exists()) {
					for (DocumentFile file : source.listFiles()) {
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

				if (source.exists() && source.isDirectory()) {
					DocumentFile backup = source.findFile("Backup");
					if (backup == null)
						backup = source.createDirectory("Backup");

					String destString = source.getName() + TimeHelper.getTimeString("_yyyyMMdd_HHmmss", System.currentTimeMillis());
					DocumentFile backupDest = backup.findFile(destString);
					if (backupDest == null)
						backupDest = backup.createDirectory(destString);
					new AsyncTaskDocumentFileDialog(context, view, "백업", null).execute(source, backupDest);
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

//		static String backup(Context context, View view) {
//			String ret = null;
//			boolean sourceChecked = false;
//			try {
//				File source = new File(Helper.Pref.getWorkPath(context));
//
//				if (source.exists()) {
//					for (File file : source.listFiles()) {
//						String fileName = file.getName().toUpperCase();
//						if (fileName.startsWith("HX") || fileName.endsWith("JOB") || fileName.startsWith("ROBOT")) {
//							sourceChecked = true;
//							break;
//						}
//					}
//				}
//				if (!sourceChecked) {
//					ret = "백업 실패: 작업 경로에 job, robot, hx 파일이 없습니다";
//					throw new Exception();
//				}
//
//				File dest = new File(source.getPath() + "/Backup/" + source.getName()
//						+ TimeHelper.getTimeString("_yyyyMMdd_HHmmss",
//						System.currentTimeMillis()));
//				if (source.exists() && source.isDirectory()) {
//					if (dest.exists())
//						FileHelper.delete(dest, false);
//					if (dest.mkdirs())
//						Log.i("backup", "dest.mkdirs");
//					new AsyncTaskFileDialog(context, view, "백업").execute(source, dest);
//					return null;
//				}
//				throw new Exception();
//			} catch (IOException e) {
//				e.printStackTrace();
//				ret = "백업 실패: 파일 복사 오류";
//			} catch (Exception e) {
//				e.printStackTrace();
//				if (ret == null)
//					ret = "백업 실패";
//			}
//			return ret;
//		}
	}

	public static class UiHelper {
		public static void textViewActivity(Activity context, String title, String text) {
			Intent intent = new Intent(context, WeldTextViewerActivity.class);
			intent.putExtra("title", title);
			intent.putExtra("text", text);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				//noinspection unchecked
				context.startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(context).toBundle());
			} else {
				context.startActivity(intent);
			}
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

			builder.setPositiveButton("닫기", (dialog, which) -> {
			});

			builder.show();
		}

		public static void adMobExitDialog(final Activity context, AdView adView) {
			TextView tvMessage = new TextView(context);
			tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
			tvMessage.setTypeface(Typeface.DEFAULT_BOLD);
			tvMessage.setPadding(40, 10, 40, 0);
			tvMessage.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
			tvMessage.setText("종료 하시겠습니까?");

			LinearLayout linearLayout = new LinearLayout(context);
			linearLayout.setOrientation(LinearLayout.VERTICAL);
			linearLayout.addView(adView);
			linearLayout.addView(tvMessage);

			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setTitle("HI5 용접 관리");
			builder.setView(linearLayout);
			builder.setPositiveButton("확인", (dialog, which) -> context.finish());
			builder.setNegativeButton("취소", (dialog, which) -> {
			});
			builder.setOnCancelListener(dialog -> linearLayout.removeView(adView));
			builder.setOnDismissListener(dialog -> linearLayout.removeView(adView));
			builder.show();
		}

		@SuppressWarnings("unused")
		public static TranslateAnimation getCenterTranslateAnimation(View parentView, View view) {
			return getCenterTranslateAnimation(parentView, view, 1.0f);
		}

		public static TranslateAnimation getCenterTranslateAnimation(View parentView, View view, float scale) {
			float fromX = view.getX();
			float toX = (parentView.getWidth() - view.getWidth()) / 2f - fromX;
			float fromY = view.getY();
			float toY = (parentView.getHeight() - view.getHeight()) / 2f - fromY;
			return new TranslateAnimation(
					TranslateAnimation.RELATIVE_TO_SELF, 0f,
					TranslateAnimation.ABSOLUTE, toX / scale,
					TranslateAnimation.RELATIVE_TO_SELF, 0f,
					TranslateAnimation.ABSOLUTE, toY / scale
			);
		}

		@SuppressWarnings("unused")
		public static void clearFocus(Activity activity) {
			View view = activity.getCurrentFocus();
			if (view != null)
				view.clearFocus();
		}

		public static void hideSoftKeyboard(Activity activity, View view, KeyEvent event) {
			if (event == null || event.getKeyCode() == KeyEvent.KEYCODE_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_NUMPAD_ENTER || event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
				final InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
				if (view == null)
					view = activity.getCurrentFocus();
				try {
					if (view != null) {
						imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
						if (view.hasFocus())
							view.clearFocus();
					}
				} catch (NullPointerException e) {
					Log.i("hideSoftKeyboard", e.getLocalizedMessage());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@SuppressWarnings("unused")
		public static void showSoftKeyboard(Activity activity, View view) {
			InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
			if (view == null)
				view = activity.getCurrentFocus();
			try {
				if (view != null) {
					Log.i("showSoftKeyboard", "imm.showSoftInput");
					imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
				}
			} catch (NullPointerException e) {
				Log.i("showSoftKeyboard", e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class AsyncTaskDocumentFileDialog extends AsyncTask<DocumentFile, String, String> {
//		private static final int MSG_REFRESH_DIR = 0;
//		private static final int MSG_REFRESH_PARENT_DIR = 1;

		//		private final static long BASE_SLEEP_TIME = 250;
		private final Context mContext;
		private final View view;
		private final String msg;
		private final Handler handler;
		private ProgressDialog progressDialog;

		/**
		 * @noinspection SameParameterValue, SameParameterValue
		 */
		public AsyncTaskDocumentFileDialog(Context context, View view, String msg, Handler handler) {
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

		private void copy(DocumentFile source, DocumentFile dest) throws IOException {
			ContentResolver contentResolver = mContext.getContentResolver();
			InputStream inputStream = contentResolver.openInputStream(source.getUri());
			OutputStream outputStream = contentResolver.openOutputStream(dest.getUri());
			if (inputStream != null && outputStream != null) {
				final byte[] buffer = new byte[1024 * 16];
				for (int r; (r = inputStream.read(buffer)) != -1; ) {
					outputStream.write(buffer, 0, r);
				}
				inputStream.close();
				outputStream.close();
			}
		}

		private String asyncTaskCopy(DocumentFile source, DocumentFile dest) {
			if (source.isFile()) {
				try {
					publishProgress("max", "1");
					publishProgress("progress", "1",
							String.format("%s %s 중", source.getName(), msg));
					DocumentFile destFile = dest.findFile(source.getName());
					if (destFile == null)
						destFile = dest.createFile("application/octet-stream", source.getName());
					copy(source, destFile);
				} catch (IOException e) {
					return e.getLocalizedMessage();
				}
			} else if (source.isDirectory()) {
				try {
					DocumentFile[] files = source.listFiles();
					publishProgress("max", String.valueOf(files.length));
					for (int i = 0, filesLength = files.length; i < filesLength; i++) {
						DocumentFile file = files[i];
						if (file.isFile()) {
							publishProgress("progress", String.valueOf(i),
									String.format("%s %s 중", file.getName(), msg));
							DocumentFile destFile = dest.findFile(file.getName());
							if (destFile == null)
								destFile = dest.createFile("application/octet-stream", file.getName());
							copy(file, destFile);
						}
					}
				} catch (Exception e) {
					return e.getLocalizedMessage();
				}
			}
			return String.format("%s 완료: %s", msg, dest.getName());
		}

		private String asyncTaskDelete(DocumentFile dir) {
			try {
				DocumentFile docFile = Pref.getDocumentFile(mContext, dir.getName());
				if (docFile != null) {
					if (docFile.delete()) {
						Log.e("HI5", "Delete success");
						return "삭제: " + dir.getName();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return "삭제 실패: " + dir.getName();
		}

		@Override
		protected String doInBackground(DocumentFile... params) {
			try {
				if (msg.equals("백업") || msg.equals("복원")) {
					if (params.length != 2) return "인수 오류";
					return asyncTaskCopy(params[0], params[1]);
				} else if (msg.equals("삭제")) {
					if (params.length != 1) return "인수 오류";
					return asyncTaskDelete(params[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
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
				Log.i("AsyncTask", "onPostExecute: " + result);
			}
			if (handler != null) {
				Message msg = handler.obtainMessage();
				msg.what = MSG_REFRESH_DIR;
				msg.obj = null;
				handler.sendMessage(msg);
			}
		}
	}

	public static class AsyncTaskFileDialog extends AsyncTask<File, String, String> {
//		private static final int MSG_REFRESH_DIR = 0;
//		private static final int MSG_REFRESH_PARENT_DIR = 1;

		private final static long BASE_SLEEP_TIME = 250;
		private final Context mContext;
		private final View view;
		private final String msg;
		private final Handler handler;
		private ProgressDialog progressDialog;

		/**
		 * @noinspection SameParameterValue
		 */
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
					FileHelper.copy(source, new File(dest.getPath() + "/" + source.getName()));
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
							FileHelper.copy(file, new File(dest.getPath() + "/" + file.getName()));
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
				File[] dirs = dir.listFiles();
				if (dirs != null) {
					for (File file : dirs) {
						searchFile(deleteList, file);
					}
				}
			}
			deleteList.add(dir);
		}

		private String asyncTaskDelete(File dir) {
			try {
				DocumentFile docFile = Pref.getDocumentFile(mContext, dir.getName());
				if (docFile != null) {
					if (docFile.delete()) {
						Log.e("HI5", "Delete success");
						return "삭제: " + dir.getName();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

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
			try {
				if (msg.equals("백업") || msg.equals("복원")) {
					if (params.length != 2) return "인수 오류";
					return asyncTaskCopy(params[0], params[1]);
				} else if (msg.equals("삭제")) {
					if (params.length != 1) return "인수 오류";
					return asyncTaskDelete(params[0]);
				}
			} catch (Exception e) {
				e.printStackTrace();
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
				Log.i("AsyncTask", "onPostExecute: " + result);
			}
			if (handler != null) {
				Message msg = handler.obtainMessage();
				msg.what = MSG_REFRESH_DIR;
				msg.obj = null;
				handler.sendMessage(msg);
			}
		}
	}

	@SuppressLint("NewApi")
	public static final class UriHelper {

		static final String TAG = "TAG";
		private static final String PRIMARY_VOLUME_NAME = "primary";

		/** @noinspection unused*/
		@NonNull
		public static String getSdCardPath() {
			String sdCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

			try {
				sdCardDirectory = new File(sdCardDirectory).getCanonicalPath();
			} catch (IOException ioe) {
				Log.e(TAG, "Could not get SD directory", ioe);
			}
			return sdCardDirectory;
		}

		/** @noinspection unused*/
		public static ArrayList<String> getExtSdCardPaths(Context con) {
			ArrayList<String> paths = new ArrayList<>();
			File[] files = ContextCompat.getExternalFilesDirs(con, "external");
			File firstFile = files[0];
			for (File file : files) {
				if (file != null && !file.equals(firstFile)) {
					int index = file.getAbsolutePath().lastIndexOf("/Android/data");
					if (index < 0) {
						Log.w("", "Unexpected external file dir: " + file.getAbsolutePath());
					} else {
						String path = file.getAbsolutePath().substring(0, index);
						try {
							path = new File(path).getCanonicalPath();
						} catch (IOException e) {
							// Keep non-canonical path.
						}
						paths.add(path);
					}
				}
			}
			return paths;
		}

		public static String getFullPathFromTreeUri(final Uri treeUri, Context con) {
			if (treeUri == null) {
				return null;
			}

			try {
				String volumePath = UriHelper.getVolumePath(UriHelper.getVolumeIdFromTreeUri(treeUri), con);
				if (volumePath == null) {
					return File.separator;
				}
				if (volumePath.endsWith(File.separator)) {
					volumePath = volumePath.substring(0, volumePath.length() - 1);
				}

				String documentPath = UriHelper.getDocumentPathFromTreeUri(treeUri);
				if (documentPath.endsWith(File.separator)) {
					documentPath = documentPath.substring(0, documentPath.length() - 1);
				}

				if (documentPath.length() > 0) {
					if (documentPath.startsWith(File.separator)) {
						return volumePath + documentPath;
					} else {
						return volumePath + File.separator + documentPath;
					}
				} else {
					return volumePath;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		private static String getVolumePath(final String volumeId, Context con) {
			try {
				StorageManager mStorageManager =
						(StorageManager) con.getSystemService(Context.STORAGE_SERVICE);
				Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");

				Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
				Method getUuid = storageVolumeClazz.getMethod("getUuid");
				Method getPath = storageVolumeClazz.getMethod("getPath");
				Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
				Object result = getVolumeList.invoke(mStorageManager);

				final int length = Array.getLength(result);
				for (int i = 0; i < length; i++) {
					Object storageVolumeElement = Array.get(result, i);
					String uuid = (String) getUuid.invoke(storageVolumeElement);
					Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

					// primary volume?
					if (primary && PRIMARY_VOLUME_NAME.equals(volumeId)) {
						return (String) getPath.invoke(storageVolumeElement);
					}

					// other volumes?
					if (uuid != null) {
						if (uuid.equals(volumeId)) {
							return (String) getPath.invoke(storageVolumeElement);
						}
					}
				}

				// not found.
				return null;
			} catch (Exception ex) {
				return null;
			}
		}

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		private static String getVolumeIdFromTreeUri(final Uri treeUri) {
			try {
				final String docId = DocumentsContract.getTreeDocumentId(treeUri);
				final String[] split = docId.split(":");

				if (split.length > 0) {
					return split[0];
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		private static String getDocumentPathFromTreeUri(final Uri treeUri) {
			try {
				final String docId = DocumentsContract.getTreeDocumentId(treeUri);
				final String[] split = docId.split(":");
				if ((split.length >= 2) && (split[1] != null)) {
					return split[1];
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return File.separator;
		}

		static void deleteUri(Activity activity, Uri uri) {
			DocumentsContract.deleteDocument(activity.getContentResolver(), uri);
		}

		static void deleteTreeUri(Activity activity, Uri uri) {
			try {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
					Uri childDocUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
					DocumentFile pickedTree = DocumentFile.fromTreeUri(activity, childDocUri);
					for (DocumentFile docFile : pickedTree.listFiles()) {
						try {
							if (docFile.isDirectory()) {
								deleteTreeUri(activity, docFile.getUri());
							} else {
								deleteUri(activity, docFile.getUri());
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		static Uri getWorkPathFileUri(Activity activity, String path) {
			Uri uri = null;
			try {
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
					File file = new File(path);
					Log.e("HI5", "File.getName:" + file.getName());

					Uri wu = Uri.parse(Pref.getPath(activity, Pref.WORK_URI_KEY));
					Uri childDocUri = DocumentsContract.buildChildDocumentsUriUsingTree(wu, DocumentsContract.getTreeDocumentId(wu));

					DocumentFile pickedTree = DocumentFile.fromTreeUri(activity, childDocUri);
					for (DocumentFile docFile : pickedTree.listFiles()) {
						try {
							if (docFile.getName().equalsIgnoreCase(file.getName())) {
								uri = docFile.getUri();
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return uri;
		}
	}
}