package com.changyoung.hi5controller.common;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;

/**
 * Created by changmin on 2017. 4. 25
 */
public class FileHelper {
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

/*
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

	private static void delete(File dir, boolean recursive) throws IOException {
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

	private static void copy(File source, File dest, boolean recursive) throws IOException {
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
*/

	static void copy(File source, File dest) throws IOException {
		try {
			FileChannel inputChannel = new FileInputStream(source).getChannel();
			FileChannel outputChannel = new FileOutputStream(dest).getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
			inputChannel.close();
			outputChannel.close();
		} finally {
			// 삼성커널에서 setLastModified 미지원 인듯
			if (dest.setLastModified(source.lastModified())) {
				Log.i("Last Time - Source", TimeStringHelper.getLasModified(source));
				Log.i("Last Time - Dest", TimeStringHelper.getLasModified(dest));
			}
		}
	}

	@Nullable
	public static String backupDocumentFile(Context context, View view) {
		String ret = null;
		boolean sourceChecked = false;
		try {
			DocumentFile source = PrefHelper.getWorkDocumentFile(context);
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

				String destString = source.getName() + TimeStringHelper.getTimeString("_yyyyMMdd_HHmmss", System.currentTimeMillis());
				DocumentFile backupDest = backup.findFile(destString);
				if (backupDest == null)
					backupDest = backup.createDirectory(destString);
				new AsyncTaskProgressDialogDocumentFile(context, view, "백업", null).execute(source, backupDest);
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
//				File source = new File(PrefHelper.getWorkPath(context));
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
//						+ TimeStringHelper.getTimeString("_yyyyMMdd_HHmmss",
//						System.currentTimeMillis()));
//				if (source.exists() && source.isDirectory()) {
//					if (dest.exists())
//						FileHelper.delete(dest, false);
//					if (dest.mkdirs())
//						Log.i("backup", "dest.mkdirs");
//					new AsyncTaskProgressDialogFile(context, view, "백업").execute(source, dest);
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
