package com.changyoung.hi5controller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by chang on 2015-10-14.
 * changmin811@gmail.com
 */
public class WeldCondition {
	public static class ConditionItem {
		public static final int OUTPUT_DATA = 0;            // 출력 데이터
		public static final int OUTPUT_TYPE = 1;            // 출력 타입
		public static final int SQUEEZE_FORCE = 2;          // 가압력
		public static final int MOVE_TIP_CLEARANCE = 3;     // 이동극 제거율
		public static final int FIXED_TIP_CLEARANCE = 4;    // 고정극 제거율
		public static final int PANNEL_THICKNESS = 5;       // 패널 두께
		public static final int COMMAND_OFFSET = 6;         // 명령 옵셋
		private static final String TAG = "ConditionItem";
		private ArrayList<String> rowList;
		private String rowString;

		private boolean itemChecked;

		public ConditionItem(String value) {
			rowList = new ArrayList<>();
			if (value != null) {
				setRowString(value);
				setString(value);
			}
			itemChecked = false;
		}

		public String get(int index) {
			String ret = null;
			try {
				ret = rowList.get(index);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ret;
		}

		public String set(int index, String object) {
			return rowList.set(index, object);
		}

		public Integer size() {
			return rowList.size();
		}

		public String getString() {
			if (rowList == null)
				return rowString;

			StringBuilder sb = new StringBuilder();
			try {
				Iterator iter = rowList.iterator();
				String outputData = (String) iter.next();
				if (Integer.parseInt(outputData) < 10)
					sb.append("\t- " + outputData + "=" + outputData);
				else
					sb.append("\t-" + outputData + "=" + outputData);
				while (iter.hasNext()) {
					sb.append("," + iter.next());
				}
			} catch (Exception e) {
				e.printStackTrace();
				return rowString;
			}

			return sb.toString();
		}

		public void setString(String value) {
			try {
				String[] header = value.trim().split("=");

				if (header.length != 2)
					return;

				String[] data = header[1].trim().split(",");
				for (String item : data) {
					rowList.add(item.trim());
				}
			} catch (NullPointerException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public String getRowString() {
			return rowString;
		}

		public void setRowString(String rowString) {
			this.rowString = rowString;
		}

		public boolean isItemChecked() {
			return itemChecked;
		}

		public void setItemChecked(boolean itemChecked) {
			this.itemChecked = itemChecked;
		}
	}

	public static class ConditionLoader extends AsyncTaskLoader<List<ConditionItem>> {

		LoaderCallBack mCallBack;
		ConditionAdapter mAdapter;
		List<ConditionItem> mList;
		IntentReceiver mReceiver;

		public ConditionLoader(Context context,
		                       LoaderCallBack callBack,
		                       ConditionAdapter adapter) {
			super(context);
			mCallBack = callBack;
			mAdapter = adapter;
		}

		@NonNull
		@Override
		public List<ConditionItem> loadInBackground() {
			String path = mCallBack.getWeldConditionWorkPath() + "/ROBOT.SWD";
			return mAdapter.getWciList(path);
		}

		@Override
		public void deliverResult(List<ConditionItem> data) {
			if (isReset()) {
				if (data != null)
					onReleaseResources(data);
			}

			List<ConditionItem> oldList = mList;
			mList = data;

			if (isStarted()) {
				super.deliverResult(data);
			}

			if (oldList != null)
				onReleaseResources(oldList);
		}

		protected void onReleaseResources(List<ConditionItem> data) {
			// For a simple List<> there is nothing to do.  For something
			// like a Cursor, we would close it here.
		}

		@Override
		protected void onStartLoading() {
			if (mList != null) {
				deliverResult(mList);
			}

			if (mReceiver == null) {
				mReceiver = new IntentReceiver(this);
			}

			if (takeContentChanged() || mList == null) {
				forceLoad();
			}
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		public void onCanceled(List<ConditionItem> data) {
			super.onCanceled(data);
			onReleaseResources(data);
		}

		@Override
		protected void onReset() {
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

		interface LoaderCallBack {
			String getWeldConditionWorkPath();
		}
	}

	public static class IntentReceiver extends BroadcastReceiver {
		public static final String WELD_CONDITION_LOAD = "com.changyoung.hi5controller.weld_condition_load";
		public static final String WELD_CONDITION_UPDATE = "com.changyoung.hi5controller.weld_condition_update";

		final ConditionLoader mConditionLoader;

		public IntentReceiver(ConditionLoader conditionLoader) {
			this.mConditionLoader = conditionLoader;
			IntentFilter filter = new IntentFilter(WELD_CONDITION_LOAD);
			filter.addAction(WELD_CONDITION_UPDATE);
			conditionLoader.getContext().registerReceiver(this, filter);
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			mConditionLoader.onContentChanged();
		}
	}

	public static class ConditionAdapter extends ArrayAdapter<ConditionItem> {
		private static final String TAG = "ConditionAdapter";

		private Activity mContext;

		public ConditionAdapter(Activity context) {
			super(context, R.layout.list_item_weld_condition);
			mContext = context;
		}

		public void setData(List<ConditionItem> data) {
			clear();
			if (data != null)
				addAll(data);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			View row = convertView;

			if (row == null) {
				row = mContext.getLayoutInflater().inflate(R.layout.list_item_weld_condition, parent, false);
				viewHolder = new ViewHolder(row);
				row.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) row.getTag();
			}

			ConditionItem item = getItem(position);
			row.setBackgroundColor(item.isItemChecked() ? ContextCompat.getColor(mContext, R.color.tab3_textview_background) : Color.TRANSPARENT);
			viewHolder.update(item);

			return row;
		}

		@NonNull
		public List<ConditionItem> getWciList(String path) {
			List<ConditionItem> list = new ArrayList<>();
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
						list.add(new ConditionItem(rowString));
					if (rowString.startsWith("#005"))
						addText = true;
				}
				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();
				notifyDataSetChanged();
			} catch (FileNotFoundException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
			return list;
		}

		public void refresh(String path) {
			clear();
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
						add(new ConditionItem(rowString));
					if (rowString.startsWith("#005"))
						addText = true;
				}
				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();
				notifyDataSetChanged();
			} catch (FileNotFoundException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public String update(String path) {
			String ret;
			StringBuilder sb = new StringBuilder();
			File file = new File(path);

			try {
				FileInputStream fileInputStream = new FileInputStream(path);
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String rowString;
				boolean addText = true;
				boolean wciText = true;
				while ((rowString = bufferedReader.readLine()) != null) {
					if (!addText && wciText) {
						for (int i = 0; i < getCount(); i++) {
							sb.append(getItem(i).getString());
							sb.append("\n");
						}
						sb.append("\n");
						wciText = false;
					}
					if (rowString.startsWith("#006"))
						addText = true;
					if (addText) {
						sb.append(rowString);
						sb.append("\n");
					}
					if (rowString.startsWith("#005"))
						addText = false;
				}
				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();

				FileOutputStream fileOutputStream = new FileOutputStream(path);
				OutputStreamWriter outputStreamReader = new OutputStreamWriter(fileOutputStream, "EUC-KR");
				BufferedWriter bufferedWriter = new BufferedWriter(outputStreamReader);

				bufferedWriter.write(sb.toString());

				bufferedWriter.close();
				outputStreamReader.close();
				fileOutputStream.close();

				ret = "저장 완료: " + file.getName();
			} catch (FileNotFoundException e) {
				ret = "저장 실패" + file.getName();
			} catch (Exception e) {
				e.printStackTrace();
				ret = "저장 실패" + file.getName();
			}

			return ret;
		}

		public class ViewHolder {
			private ArrayList<TextView> tvList;

			public ViewHolder(View row) {
				tvList = new ArrayList<>();
				tvList.add((TextView) row.findViewById(R.id.tvOutputData));
				tvList.add((TextView) row.findViewById(R.id.tvOutputType));
				tvList.add((TextView) row.findViewById(R.id.tvSqueezeForce));
				tvList.add((TextView) row.findViewById(R.id.tvMoveTipClearance));
				tvList.add((TextView) row.findViewById(R.id.tvFixedTipClearance));
				tvList.add((TextView) row.findViewById(R.id.tvPannelThickness));
				tvList.add((TextView) row.findViewById(R.id.tvCommandOffset));
			}

			public void update(ConditionItem conditionItem) {
				for (int i = 0; i < tvList.size(); i++) {
					tvList.get(i).setText(conditionItem.get(i));
				}
			}
		}
	}
}