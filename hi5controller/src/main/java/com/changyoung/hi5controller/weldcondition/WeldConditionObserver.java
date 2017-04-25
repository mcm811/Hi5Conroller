package com.changyoung.hi5controller.weldcondition;

import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.util.Locale;

/**
 * Created by changmin811@gmail.com on 2017. 4. 25
 */
class WeldConditionObserver extends FileObserver {
	//		static final String TAG = "HI5:WeldConditionObserver";
	private static final int mask = CREATE | DELETE | DELETE_SELF | MOVED_FROM | MOVED_TO | MOVE_SELF | CLOSE_WRITE;
	private final File file;
	private final Handler handler;

/*
	public WeldConditionObserver(File file, Handler handler) {
		super(file.getPath(), mask);
		this.file = file;
		this.handler = handler;
		logD("FILE_OBSERVER: " + file.getPath());
	}
*/

	WeldConditionObserver(String path, Handler handler) {
		super(path, mask);
		this.file = new File(path);
		this.handler = handler;
		WeldConditionFragment.logD("FILE_OBSERVER: " + path);
	}

	public void onEvent(int event, String path) {
		if ((event & CREATE) == CREATE)
			WeldConditionFragment.logD(String.format(Locale.KOREA, "CREATE: %s/%s", file.getPath(), path));
		else if ((event & DELETE) == DELETE)
			WeldConditionFragment.logD(String.format(Locale.KOREA, "DELETE: %s/%s", file.getPath(), path));
		else if ((event & DELETE_SELF) == DELETE_SELF)
			WeldConditionFragment.logD(String.format(Locale.KOREA, "DELETE_SELF: %s/%s", file.getPath(), path));
		else if ((event & MOVED_FROM) == MOVED_FROM)
			WeldConditionFragment.logD(String.format(Locale.KOREA, "MOVED_FROM: %s/%s", file.getPath(), path));
		else if ((event & MOVED_TO) == MOVED_TO)
			WeldConditionFragment.logD(String.format(Locale.KOREA, "MOVED_TO: %s", path == null ? file.getPath() : path));
		else if ((event & MOVE_SELF) == MOVE_SELF)
			WeldConditionFragment.logD(String.format(Locale.KOREA, "MOVE_SELF: %s", path == null ? file.getPath() : path));
		else if ((event & CLOSE_WRITE) == CLOSE_WRITE)
			WeldConditionFragment.logD(String.format(Locale.KOREA, "CLOSE_WRITE: %s", path == null ? file.getPath() : path));
		else
			return;

		stopWatching();
		Message msg = handler.obtainMessage();
		msg.what = WeldConditionFragment.MSG_REFRESH;
		msg.obj = file;
		handler.sendMessage(msg);
	}
}
