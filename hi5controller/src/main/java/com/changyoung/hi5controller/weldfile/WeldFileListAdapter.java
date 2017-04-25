package com.changyoung.hi5controller.weldfile;

import android.content.Context;
import android.os.Build;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.AsyncTaskProgressDialogFile;
import com.changyoung.hi5controller.common.FileHelper;
import com.changyoung.hi5controller.common.TimeStringHelper;
import com.changyoung.hi5controller.common.UiHelper;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by changmin on 2017. 4. 25
 */
class WeldFileListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private final WeldFileListFragment weldFileListFragment;
	private final List<File> mDataset;

	WeldFileListAdapter(WeldFileListFragment weldFileListFragment, List<File> dataset) {
		this.weldFileListFragment = weldFileListFragment;
		mDataset = dataset;
	}

	void clear() {
		mDataset.clear();
	}

	void add(File item) {
		mDataset.add(item);
	}

	void insert(File item, @SuppressWarnings("SameParameterValue") int index) {
		mDataset.add(index, item);
	}

	void sort(Comparator<File> comparator) {
		//noinspection Java8ListSort
		Collections.sort(mDataset, comparator);
	}

/*
	public void setData(List<File> data) {
		mDataset.clear();
		mDataset.addAll(data);
		notifyDataSetChanged();
	}
*/

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		final View v = LayoutInflater.from(context)
				.inflate(R.layout.weldfile_list_view_holder_item, parent, false);
		// set the view's size, margins, paddings and layout parameters
		final ViewHolder holder = new ViewHolder(v);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			holder.mFileFab.setVisibility(View.VISIBLE);
			holder.mFileImageView.setVisibility(View.GONE);
		} else {
			holder.mFileFab.setVisibility(View.GONE);
			holder.mFileImageView.setVisibility(View.VISIBLE);
		}
		holder.mItemView.setOnClickListener(v12 -> {
			final int position = (int) v12.getTag();
			final File file = mDataset.get(position);
			if (position == 0) {
				String p = file.getParent();
				if (p == null)
					p = File.pathSeparator;
				weldFileListFragment.refreshFilesList(p);
			} else if (file.isDirectory()) {
				weldFileListFragment.refreshFilesList(file.getPath());
			} else {
				UiHelper.textViewActivity(weldFileListFragment.getActivity(), file.getName(),
						FileHelper.readFileString(file.getPath()));
			}
		});
		holder.mItemView.setOnLongClickListener(v1 -> {
			final int position = (int) v1.getTag();
			if (position == 0)
				return false;

			final File file = mDataset.get(position);
			String actionName = file.isDirectory() ? "폴더 삭제" : "파일 삭제";
			String fileType = file.isDirectory() ? "이 폴더를" : "이 파일을";
			String msg = String.format("%s 완전히 삭제 하시겠습니까?\n\n%s\n\n수정한 날짜: %s",
					fileType, file.getName(), TimeStringHelper.getLasModified(file));

			AlertDialog.Builder builder = new AlertDialog.Builder(weldFileListFragment.getActivity());
			builder.setTitle(actionName)
					.setMessage(msg)
					.setNegativeButton("취소", (dialog, which) -> weldFileListFragment.show("삭제가 취소 되었습니다"))
					.setPositiveButton("삭제", (dialog, which) -> {
						try {
							new AsyncTaskProgressDialogFile(weldFileListFragment.getContext(),
									weldFileListFragment.snackbarView, "삭제", weldFileListFragment.looperHandler)
									.execute(file);
						} catch (Exception e) {
							e.printStackTrace();
							weldFileListFragment.show("삭제할 수 없습니다");
						}
					});
			builder.create().show();

			return true;
		});
		return holder;
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder rh, final int position) {
		final ViewHolder holder = (ViewHolder) rh;
		String fileTime = null;
		String fileName;
		int fileImageResourceId;
		final File file = mDataset.get(position);
		if (position == 0) {
			String p = file.getParent();
			if (p == null) {
				fileName = ".";
				fileImageResourceId = R.drawable.ic_android;
			} else {
				fileName = file.getParentFile().getName() + "/..";
				fileImageResourceId = R.drawable.ic_arrow_upward;
			}
		} else {
			fileTime = TimeStringHelper.getLasModified(file);
			fileName = file.getName();
			fileImageResourceId = file.isFile() ? R.drawable.ic_description : R.drawable.ic_folder;
		}

		if (fileTime == null) {
			holder.mFileTimeTextView.setText("");
			holder.mFileTimeTextView.setVisibility(View.GONE);
		} else {
			holder.mFileTimeTextView.setText(fileTime);
			holder.mFileTimeTextView.setVisibility(View.VISIBLE);
		}
		holder.mFileNameTextView.setText(fileName);
		holder.mFileImageView.setImageResource(fileImageResourceId);
		holder.mFileFab.setImageResource(fileImageResourceId);
		holder.mItemView.setTag(position);
	}

	@Override
	public int getItemCount() {
		return mDataset.size();
	}

	class ViewHolder extends RecyclerView.ViewHolder {
		final View mItemView;
		final TextView mFileTimeTextView;
		final TextView mFileNameTextView;
		final ImageView mFileImageView;
		final FloatingActionButton mFileFab;

		ViewHolder(View itemView) {
			super(itemView);
			mItemView = itemView;
			mFileTimeTextView = (TextView) itemView.findViewById(R.id.file_time);
			mFileNameTextView = (TextView) itemView.findViewById(R.id.file_text);
			mFileImageView = (ImageView) itemView.findViewById(R.id.file_image);
			mFileFab = (FloatingActionButton) itemView.findViewById(R.id.file_fab);
		}
	}
}
