package com.changyoung.hi5controller.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by changmin on 2017. 4. 25
 */
public class PrefHelper {
	public final static String EXTERNAL_STORAGE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
	public final static String STORAGE_PATH = "/storage";
	public final static String ORDER_TYPE_KEY = "order_type";
	public final static String LAYOUT_TYPE_KEY = "layout_type";
	private final static String WORK_URI_KEY = "work_uri";
	private final static String PACKAGE_NAME = "com.changyoung.hi5controller";
	private final static String WORK_PATH_KEY = "work_path";
	private final static String BACKUP_PATH_KEY = "backup_path";

	static DocumentFile getDocumentFile(Context context, String fileName) throws NullPointerException {
		return getWorkDocumentFile(context).findFile(fileName);
	}

	public static DocumentFile getWorkDocumentFile(Context context) throws NullPointerException {
		DocumentFile documentFile = DocumentFile.fromTreeUri(context, PrefHelper.getWorkPathUri(context));
		if (documentFile == null)
			throw new NullPointerException();
		return documentFile;
	}

	private static Uri getWorkPathUri(Context context) {
		return Uri.parse(getWorkPathUriString(context));
	}

	public static String getWorkPathUriString(Context context) {
		return getPath(context, WORK_URI_KEY);
	}

	private static DocumentFile getWorkPathDocumentFile(Context context, String path) {
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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

	private static String getPath(Context context, @SuppressWarnings("SameParameterValue") String key) {
		return getString(context, key, EXTERNAL_STORAGE_PATH);
	}

	private static void setPath(Context context, String key, String value) {
		putString(context, key, value);
	}

	private static String getString(Context context, String key, @SuppressWarnings("SameParameterValue") String defValue) {
		try {
			SharedPreferences prefs = context.getSharedPreferences(PACKAGE_NAME, Context.MODE_PRIVATE);
			return prefs.getString(key, defValue);
		} catch (Exception e) {
			e.printStackTrace();
			return defValue;
		}
	}

	private static void putString(Context context, String key, String value) {
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
