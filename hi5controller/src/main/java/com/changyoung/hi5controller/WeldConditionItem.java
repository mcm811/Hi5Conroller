package com.changyoung.hi5controller;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by chang on 2015-10-14.
 */
public class WeldConditionItem extends java.lang.Object {
	public static final int OUTPUT_DATA = 0;            // 출력 데이터
	public static final int OUTPUT_TYPE = 1;            // 출력 타입
	public static final int SQUEEZE_FORCE = 2;          // 가압력
	public static final int MOVE_TIP_CLEARANCE = 3;     // 이동극 제거율
	public static final int FIXED_TIP_CLEARANCE = 4;    // 고정극 제거율
	public static final int PANNEL_THICKNESS = 5;       // 패널 두께
	public static final int COMMAND_OFFSET = 6;         // 명령 옵셋
	private static final String TAG = "WeldConditionItem";
	private ArrayList<String> rowList;
	private String rowString;

	private boolean itemChecked;

	public WeldConditionItem(String value) {
		rowList = new ArrayList<>();
		setRowString(value);
		setString(value);
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
		String[] header = value.trim().split("=");

		if (header.length != 2)
			return;

		String[] data = header[1].trim().split(",");
		for (String item : data) {
			rowList.add(item.trim());
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
