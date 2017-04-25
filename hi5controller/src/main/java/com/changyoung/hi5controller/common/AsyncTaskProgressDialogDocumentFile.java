package com.changyoung.hi5controller.common;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.changyoung.hi5controller.weldfile.WeldFileListFragment.MSG_REFRESH_DIR;

/**
 * Created by changmin on 2017. 4. 25
 */
public class AsyncTaskProgressDialogDocumentFile extends AsyncTask<DocumentFile, String, String> {
	private final Context mContext;
	private final View view;
	private final String msg;
	private final Handler handler;
	private ProgressDialog progressDialog;

	/**
	 * @noinspection SameParameterValue, SameParameterValue
	 */
	public AsyncTaskProgressDialogDocumentFile(Context context, View view, String msg, Handler handler) {
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
