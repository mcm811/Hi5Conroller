package com.changyoung.hi5controller;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;

/**
 * Created by chang on 2015-10-12.
 */
public class WeldCountAdapter<T> extends ArrayAdapter<T> {
	private Activity mContext;

	public WeldCountAdapter(Activity context) {
		super(context, R.layout.list_item_weld_count);
		mContext = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
		View row = convertView;

		if (row == null) {
			row = mContext.getLayoutInflater().inflate(R.layout.list_item_weld_count, parent, false);
			viewHolder = new ViewHolder(row);
			row.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) row.getTag();
		}

		viewHolder.update((WeldCountFile) getItem(position));

		return row;
	}

	public void refresh(String path) {
		try {
			clear();
			File dir = new File(path);
			for (File file : dir.listFiles()) {
				if (file.getName().toUpperCase().endsWith(".JOB") || file.getName().toUpperCase().startsWith("HX"))
					add((T) new WeldCountFile(file.getPath()));
			}
			notifyDataSetChanged();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class ViewHolder extends java.lang.Object {
		private TextView tvFileName;
		private TextView tvTime;
		private TextView tvSize;
		private TextView tvCount;
		private TextView tvPreview;
		private TextView tvCN;

		public ViewHolder(View row) {
			tvFileName = (TextView) row.findViewById(R.id.tvFileName);
			tvTime = (TextView) row.findViewById(R.id.tvTime);
			tvSize = (TextView) row.findViewById(R.id.tvSize);
			tvCount = (TextView) row.findViewById(R.id.tvCount);
			tvPreview = (TextView) row.findViewById(R.id.tvPreview);
			tvCN = (TextView) row.findViewById(R.id.tvCN);
		}

		public void update(WeldCountFile jobFile) {
			tvFileName.setText(jobFile.getName());
			tvTime.setText(Util.TimeUtil.getLasModified(jobFile));
			tvSize.setText(((Long) jobFile.length()).toString() + "B");
			tvCount.setText(jobFile.getJobInfo().getString());
			tvPreview.setText(jobFile.getJobInfo().getPreview());
			tvCN.setText(jobFile.getCNList());
		}
	}
}
