package com.changyoung.hi5controller.weldcondition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.AsyncTaskLoader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by changmin811@gmail.com on 2017. 4. 25
 */
public class WeldConditionLoader extends AsyncTaskLoader<List<WeldConditionItem>> {
	private final WeldConditionFragment.OnWorkPathListener mCallBack;
	private List<WeldConditionItem> mList;
	private WeldConditionReceiver mReceiver;

	WeldConditionLoader(Context context,
	                    WeldConditionFragment.OnWorkPathListener callBack) {
		super(context);
		mCallBack = callBack;
	}

	@Override
	public List<WeldConditionItem> loadInBackground() {
		WeldConditionFragment.logD("loadInBackground");
		if (mCallBack == null)
			return null;
		String path = mCallBack.onGetWorkPath() + "/ROBOT.SWD";
		List<WeldConditionItem> list = new ArrayList<>();
		try {
			FileInputStream fileInputStream = new FileInputStream(path);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String rowString;
			boolean addText = false;
			while ((rowString = bufferedReader.readLine()) != null) {
				if (rowString.startsWith("#006"))
					break;
				if (addText && rowString.trim().length() > 0)
					list.add(new WeldConditionItem(rowString));
				if (rowString.startsWith("#005"))
					addText = true;
			}
			bufferedReader.close();
			inputStreamReader.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			WeldConditionFragment.logD(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		WeldConditionFragment.logD("loadInBackground() list.size: " + list.size());
		return list;
	}

	@Override
	public void deliverResult(List<WeldConditionItem> data) {
		WeldConditionFragment.logD("deliverResult");

		if (isReset()) {
			if (data != null)
				onReleaseResources(data);
		}

		List<WeldConditionItem> oldList = mList;
		mList = data;

		if (isStarted()) {
			super.deliverResult(data);
		}

		if (oldList != null)
			onReleaseResources(oldList);

		if (mList != null)
			WeldConditionFragment.logD("deliverResult() mList.size: " + mList.size());
	}

	private void onReleaseResources(List<WeldConditionItem> data) {
		// For a simple List<> there is nothing to do.  For something
		// like a Cursor, we would close it here.
		if (data != null) {
			WeldConditionFragment.logD("dataSize: " + data.size());
		}
	}

	@Override
	protected void onStartLoading() {
		WeldConditionFragment.logD("onStartLoading");

		if (mList != null) {
			deliverResult(mList);
		}

		if (mReceiver == null) {
			mReceiver = new WeldConditionReceiver(this);
		}

		if (takeContentChanged() || mList == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		WeldConditionFragment.logD("onStopLoading");
		cancelLoad();
	}

	@Override
	public void onCanceled(List<WeldConditionItem> data) {
		WeldConditionFragment.logD("onCanceled");
		super.onCanceled(data);
		onReleaseResources(data);
	}

	@Override
	protected void onReset() {
		WeldConditionFragment.logD("onReset");

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

	public static class WeldConditionReceiver extends BroadcastReceiver {
		public static final String WELD_CONDITION_LOAD = "com.changyoung.hi5controller.weld_condition_load";
		public static final String WELD_CONDITION_UPDATE = "com.changyoung.hi5controller.weld_condition_update";

		final WeldConditionLoader mLoader;

		public WeldConditionReceiver(WeldConditionLoader loader) {
			mLoader = loader;
			IntentFilter filter = new IntentFilter(WELD_CONDITION_LOAD);
			filter.addAction(WELD_CONDITION_UPDATE);
			mLoader.getContext().registerReceiver(this, filter);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			mLoader.onContentChanged();
		}
	}
}
