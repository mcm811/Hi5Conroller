package com.changyoung.hi5controller.weldcondition;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by changmin811@gmail.com on 2017. 4. 25
 */
class WeldConditionItem {
	/*
			public static final int OUTPUT_TYPE = 1;            // 출력 타입
			public static final int MOVE_TIP_CLEARANCE = 3;     // 이동극 제거율
			public static final int FIXED_TIP_CLEARANCE = 4;    // 고정극 제거율
			public static final int PANEL_THICKNESS = 5;        // 패널 두께
			public static final int COMMAND_OFFSET = 6;         // 명령 옵셋
	*/
	static final int OUTPUT_DATA = 0;            // 출력 데이터
	static final int SQUEEZE_FORCE = 2;          // 가압력
	private final List<String> rowList;
	private String rowString;

//		private boolean itemChecked;

	WeldConditionItem(String value) {
		rowList = new ArrayList<>();
		if (value != null) {
			setRowString(value);
			setString(value);
		}
//			itemChecked = false;
	}

	String get(int index) {
		String ret = null;
		try {
			ret = rowList.get(index);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	void set(int index, String object) {
		rowList.set(index, object);
	}

/*
	public Integer size() {
		return rowList.size();
	}
*/

	public String getString() {
		if (rowList == null)
			return rowString;

		StringBuilder sb = new StringBuilder();
		try {
			Iterator iter = rowList.iterator();
			String outputData = (String) iter.next();
			if (Integer.parseInt(outputData) < 10)
				sb.append("\t- ").append(outputData).append("=").append(outputData);
			else
				sb.append("\t-").append(outputData).append("=").append(outputData);
			while (iter.hasNext()) {
				sb.append(",").append(iter.next());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return rowString;
		}

		return sb.toString();
	}

	private void setString(String value) {
		try {
			String[] header = value.trim().split("=");

			if (header.length != 2)
				return;

			String[] data = header[1].trim().split(",");
			for (String item : data) {
				rowList.add(item.trim());
			}
		} catch (NullPointerException e) {
			WeldConditionFragment.logD(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setRowString(String rowString) {
		this.rowString = rowString;
	}

/*
	public String getRowString() {
		return rowString;
	}

	public boolean isItemChecked() {
		return itemChecked;
	}

	public void setItemChecked(boolean itemChecked) {
		this.itemChecked = itemChecked;
	}
*/
}
