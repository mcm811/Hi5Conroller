package com.changyoung.hi5controller;

import android.app.Activity;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by chang on 2015-10-10.
 */
public class FileListAdapter<T> extends ArrayAdapter<T> {
	private Activity mContext;

	public FileListAdapter(Activity context, List<T> objects) {
		super(context, R.layout.list_item_file, objects);
		mContext = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		View row = convertView;

		if (row == null) {
			row = mContext.getLayoutInflater().inflate(R.layout.list_item_file, parent, false);
			viewHolder = new ViewHolder((TextView) row.findViewById(R.id.file_picker_time),
					(TextView) row.findViewById(R.id.file_picker_text),
					(ImageView) row.findViewById(R.id.file_picker_image),
					(FloatingActionButton) row.findViewById(R.id.file_picker_fab));
			row.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) row.getTag();
		}

		File file = (File) getItem(position);
		if (position == 0) {
			String p = file.getParent();
			if (p == null) {
				viewHolder.Update(null, ".", R.drawable.ic_android);
			} else {
				viewHolder.Update(null, file.getParentFile().getName() + "/..", R.drawable.ic_file_upload);
			}
		} else {
			viewHolder.Update(new SimpleDateFormat("yyyy-dd-MM a hh:mm:ss").format(new Date(file.lastModified())),
					file.getName(), file.isFile() ? R.drawable.ic_description : R.drawable.ic_folder_open);
		}

		return row;
	}

	public class ViewHolder extends Object {
		private TextView TimeTextView;
		private android.widget.TextView TextView;
		private android.widget.ImageView ImageView;
		private FloatingActionButton Fab;

		public ViewHolder(TextView timeTextView, TextView textView, ImageView imageView, FloatingActionButton fab) {
			TimeTextView = timeTextView;
			TextView = textView;
			ImageView = imageView;
			Fab = fab;
		}

		public void Update(String fileTime, String fileName, int fileImageResourceId) {
			if (fileTime == null) {
				TimeTextView.setText("");
				TimeTextView.setVisibility(View.GONE);
			} else {
				TimeTextView.setText(fileTime);
				TimeTextView.setVisibility(View.VISIBLE);
			}
			TextView.setText(fileName);
			ImageView.setImageResource(fileImageResourceId);
			Fab.setImageResource(fileImageResourceId);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				Fab.setVisibility(View.VISIBLE);
				ImageView.setVisibility(View.GONE);
			} else {
				Fab.setVisibility(View.GONE);
				ImageView.setVisibility(View.VISIBLE);
			}
		}
	}
}