package com.changyoung.hi5controller.common;

/**
 * Created by chang on 2015-10-11.
 * changmin811@gmail.com
 */

public interface RefreshHandler {
	void onRefresh(boolean forced);

	String onRefresh(int menuId);

	String onBackPressedFragment();

	void show(String msg);
}