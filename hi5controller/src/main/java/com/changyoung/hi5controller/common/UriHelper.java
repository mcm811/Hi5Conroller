package com.changyoung.hi5controller.common;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by changmin on 2017. 4. 25
 */
@SuppressLint("NewApi")
public final class UriHelper {
	private static final String TAG = "UriHelper";
	private static final String PRIMARY_VOLUME_NAME = "primary";

	/**
	 * @noinspection unused
	 */
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

	/**
	 * @noinspection unused
	 */
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

/*
	private static void deleteUri(Activity activity, Uri uri) {
		DocumentsContract.deleteDocument(activity.getContentResolver(), uri);
	}

	static void deleteTreeUri(Activity activity, Uri uri) {
		try {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File file = new File(path);
				Log.e("HI5", "File.getName:" + file.getName());

				Uri wu = Uri.parse(PrefHelper.getPath(activity, PrefHelper.WORK_URI_KEY));
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
*/
}
