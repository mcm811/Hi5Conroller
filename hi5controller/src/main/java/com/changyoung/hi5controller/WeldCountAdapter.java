package com.changyoung.hi5controller;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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

		viewHolder.update(getItem(position));

		return row;
	}

	public class ViewHolder extends java.lang.Object {
		public TextView tvFileName;
		public TextView tvTime;
		public TextView tvSize;
		public TextView tvCount;
		public TextView tvPreview;
		public TextView tvCN;

		public ViewHolder(View row) {
			tvFileName = (TextView) row.findViewById(R.id.tvFileName);
			tvTime = (TextView) row.findViewById(R.id.tvTime);
			tvSize = (TextView) row.findViewById(R.id.tvSize);
			tvCount = (TextView) row.findViewById(R.id.tvCount);
			tvPreview = (TextView) row.findViewById(R.id.tvPreview);
			tvCN = (TextView) row.findViewById(R.id.tvCN);
		}

		public void update(T jobFile) {
			JobFile jf = (JobFile) jobFile;
			tvFileName.setText(jf.getJobCount().fi.getName());
			tvTime.setText(Util.TimeUtil.getLasModified(jf.getJobCount().fi));
			tvSize.setText(((Long) jf.getJobCount().fi.length()).toString() + "B");
			tvCount.setText(jf.getJobCount().getString());
			tvPreview.setText(jf.getJobCount().getPreview());
			tvCN.setText(jf.getCNList());
		}
	}
}
