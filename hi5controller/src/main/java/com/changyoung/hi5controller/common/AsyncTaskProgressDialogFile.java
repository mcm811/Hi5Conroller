package com.changyoung.hi5controller.common;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.changyoung.hi5controller.weldfile.WeldFileListFragment.MSG_REFRESH_DIR;

/**
 * Created by changmin on 2017. 4. 25
 */
public class AsyncTaskProgressDialogFile extends AsyncTask<File, String, String> {
	private final static long BASE_SLEEP_TIME = 250;
	private final Context mContext;
	private final View view;
	private final String msg;
	private final Handler handler;
	private ProgressDialog progressDialog;

	/**
	 * @noinspection SameParameterValue
	 */
	public AsyncTaskProgressDialogFile(Context context, View view, String msg, Handler handler) {
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
			DocumentFile docFile = PrefHelper.getDocumentFile(mContext, dir.getName());
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
