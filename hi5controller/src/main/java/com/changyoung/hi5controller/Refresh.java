package com.changyoung.hi5controller;

import android.view.View;

/**
 * Created by chang on 2015-10-11.
 * changmin811@gmail.com
 */

@SuppressWarnings("unused")
interface Refresh {
	void refresh(boolean forced);

	boolean refresh(String path);

	String refresh(int menuId);

	String onBackPressedFragment();

	View getFab();

	void show(String msg);
}