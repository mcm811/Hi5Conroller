package com.changyoung.hi5controller.common;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by changmin on 2017. 4. 25
 */
public class TimeStringHelper {
	public static String getLasModified(File file) {
		return getTimeString("yyyy-MM-dd a hh-mm-ss", file.lastModified());
	}

	static String getTimeString(String format, long time) {
		return new SimpleDateFormat(format, Locale.KOREA).format(new Date(time));
	}
}
