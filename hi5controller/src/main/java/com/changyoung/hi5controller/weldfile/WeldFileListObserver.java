package com.changyoung.hi5controller.weldfile;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;

import java.io.File;

/**
 * Created by changmin on 2017. 4. 25
 */
public class WeldFileListObserver extends FileObserver {
	@SuppressWarnings("unused")
	static final String TAG = "HI5:WeldFileListObserver";
	private static final int mask = CREATE | DELETE | DELETE_SELF |
			MOVED_FROM | MOVED_TO | MOVE_SELF | CLOSE_WRITE;
	private final File file;
	private final Handler handler;

	WeldFileListObserver(File file, Handler handler) {
		super(file.getPath(), mask);
		this.file = file;
		this.handler = handler;
		WeldFileListFragment.logD("FILE_OBSERVER: " + file.getPath());
	}

	public void onEvent(int event, String path) {
		if ((event & CREATE) == CREATE)
			WeldFileListFragment.logD(String.format("CREATE: %s/%s", file.getPath(), path));
		else if ((event & DELETE) == DELETE)
			WeldFileListFragment.logD(String.format("DELETE: %s/%s", file.getPath(), path));
		else if ((event & DELETE_SELF) == DELETE_SELF)
			WeldFileListFragment.logD(String.format("DELETE_SELF: %s/%s", file.getPath(), path));
		else if ((event & MOVED_FROM) == MOVED_FROM)
			WeldFileListFragment.logD(String.format("MOVED_FROM: %s/%s", file.getPath(), path));
		else if ((event & MOVED_TO) == MOVED_TO)
			WeldFileListFragment.logD(String.format("MOVED_TO: %s", path == null ? file.getPath() : path));
		else if ((event & MOVE_SELF) == MOVE_SELF)
			WeldFileListFragment.logD(String.format("MOVE_SELF: %s", path == null ? file.getPath() : path));
		else if ((event & CLOSE_WRITE) == CLOSE_WRITE)
			WeldFileListFragment.logD(String.format("CLOSE_WRITE: %s", path == null ? file.getPath() : path));
		else
			return;

		stopWatching();
		Message msg = handler.obtainMessage();
		msg.what = WeldFileListFragment.MSG_REFRESH_DIR;
		msg.obj = file;
		handler.sendMessage(msg);
	}
}
