package com.changyoung.hi5controller.weldcount;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WeldCountAsyncTaskLoader extends AsyncTaskLoader<List<WeldCountFile>> {
	private final WeldCountFragment.OnWorkPathListener mCallBack;
	private List<WeldCountFile> mList;
	private WeldCountBroadcastReceiver mReceiver;

	WeldCountAsyncTaskLoader(Context context,
	                         WeldCountFragment.OnWorkPathListener callBack) {
		super(context);
		mCallBack = callBack;
	}

	@Override
	public List<WeldCountFile> loadInBackground() {
		WeldCountFragment.logD("loadInBackground");
		if (mCallBack == null)
			return null;
		String path = mCallBack.onGetWorkPath();
		List<WeldCountFile> list = new ArrayList<>();
		try {
			File dir = new File(path);
			File[] dirs = dir.listFiles();
			if (dirs != null) {
				for (File file : dirs) {
					if (file.getName().toUpperCase().endsWith(".JOB")
							|| file.getName().toUpperCase().startsWith("HX"))
						list.add(new WeldCountFile(file.getPath()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		WeldCountFragment.logD("loadInBackground() list.size: " + list.size());
		return list;
	}

	@Override
	public void deliverResult(List<WeldCountFile> data) {
		WeldCountFragment.logD("deliverResult");

		if (isReset()) {
			if (data != null)
				onReleaseResources(data);
		}

		List<WeldCountFile> oldList = mList;
		mList = data;

		if (isStarted()) {
			super.deliverResult(data);
		}

		if (oldList != null)
			onReleaseResources(oldList);
		if (mList != null)
			WeldCountFragment.logD("deliverResult() mList.size: " + mList.size());
	}

	private void onReleaseResources(List<WeldCountFile> data) {
		// For a simple List<> there is nothing to do.  For something
		// like a Cursor, we would close it here.
		if (data != null)
			WeldCountFragment.logD("dataSize: " + data.size());
	}

	@Override
	protected void onStartLoading() {
		WeldCountFragment.logD("onStartLoading");

		if (mList != null) {
			deliverResult(mList);
		}

		if (mReceiver == null) {
			mReceiver = new WeldCountBroadcastReceiver(this);
		}

		if (takeContentChanged() || mList == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		WeldCountFragment.logD("onStopLoading");
		cancelLoad();
	}

	@Override
	public void onCanceled(List<WeldCountFile> data) {
		WeldCountFragment.logD("onCanceled");
		super.onCanceled(data);
		onReleaseResources(data);
	}

	@Override
	protected void onReset() {
		WeldCountFragment.logD("onReset");

		super.onReset();

		onStopLoading();

		if (mList != null) {
			onReleaseResources(mList);
			mList = null;
		}

		if (mReceiver != null) {
			getContext().unregisterReceiver(mReceiver);
			mReceiver = null;
		}
	}

	public static class WeldCountBroadcastReceiver extends BroadcastReceiver {
		public static final String WELD_COUNT_LOAD = "com.changyoung.hi5controller.weld_count_load";
		public static final String WELD_COUNT_UPDATE = "com.changyoung.hi5controller.weld_count_update";

		final WeldCountAsyncTaskLoader mLoader;

		public WeldCountBroadcastReceiver(WeldCountAsyncTaskLoader loader) {
			this.mLoader = loader;
			IntentFilter filter = new IntentFilter(WELD_COUNT_LOAD);
			filter.addAction(WELD_COUNT_UPDATE);
			mLoader.getContext().registerReceiver(this, filter);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			mLoader.onContentChanged();
		}
	}
}
