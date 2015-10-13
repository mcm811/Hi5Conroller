package com.changyoung.hi5controller;

/**
 * Created by chang on 2015-10-11.
 */

public interface Refresh {
	void refresh(boolean forced);

	boolean refresh(String path);

	String refresh(int menuId);

	String onBackPressedFragment();

	void show(String msg);
}