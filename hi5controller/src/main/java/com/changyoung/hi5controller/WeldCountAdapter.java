package com.changyoung.hi5controller;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by chang on 2015-10-12.
 */
public class WeldCountAdapter<T> extends ArrayAdapter<T> {
	private Activity mContext;

	public WeldCountAdapter(Activity context, List<T> objects) {
		super(context, R.layout.list_item_weld_count, objects);
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
			// TODO: JobFile 클래스를 구현 했을때 주석 해제 할것
//			tvFileName.setText(jobFile.JobCount.fi.Name);
//			tvTime.setText(jobFile.JobCount.fi.LastWriteTime.ToString());
//			tvSize.setText(jobFile.JobCount.fi.Length.ToString() + "B");
//			tvCount.setText(jobFile.JobCount.GetString());
//			tvPreview.setText(jobFile.JobCount.Preview);
//			tvCN.setText(jobFile.GetCNList());
		}
	}
}
