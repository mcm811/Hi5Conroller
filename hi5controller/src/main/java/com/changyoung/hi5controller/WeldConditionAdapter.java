package com.changyoung.hi5controller;

import android.app.Activity;
import android.graphics.Color;
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

/**
 * Created by chang on 2015-10-14.
 */
public class WeldConditionAdapter<T> extends ArrayAdapter<T> {
	private static final String TAG = "WeldConditionAdapter";

	private Activity mContext;

	public WeldConditionAdapter(Activity context) {
		super(context, R.layout.list_item_weld_condition);
		mContext = context;
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

		WeldConditionItem item = (WeldConditionItem) getItem(position);
		row.setBackgroundColor(item.isItemChecked() ? ContextCompat.getColor(mContext, R.color.tab3_textview_background) : Color.TRANSPARENT);
		viewHolder.update(item);

		return row;
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
					add((T) new WeldConditionItem(rowString));
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
		String ret = null;
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
						sb.append(((WeldConditionItem) getItem(i)).getString());
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

	public class ViewHolder extends java.lang.Object {
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

		public void update(WeldConditionItem weldConditionItem) {
			for (int i = 0; i < tvList.size(); i++) {
				tvList.get(i).setText(weldConditionItem.get(i));
			}
		}
	}
}
