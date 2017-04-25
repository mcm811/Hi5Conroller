package com.changyoung.hi5controller.weldcount;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.Helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class WeldCountFileEditorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private final WeldCountFile mFile;
	private final List<WeldCountFile.Job> mDataset;
	private final Activity mActivity;
	private final WeldCountFragment weldCountFragment;

	WeldCountFileEditorAdapter(WeldCountFragment weldCountFragment,
	                           Activity activity,
	                           WeldCountFile weldCountFile) {
		this.weldCountFragment = weldCountFragment;
		mFile = weldCountFile;
		mDataset = new ArrayList<>();
		for (int i = 0; i < mFile.size(); i++) {
			final WeldCountFile.Job job = mFile.get(i);
			if (job.getRowType() == WeldCountFile.Job.JOB_SPOT)
				mDataset.add(job);
		}
		mActivity = activity;
	}

	void reloadFile() {
		mFile.readFile();
	}

	public String getName() {
		return mFile.getName();
	}

	boolean checkA() {
		return !mFile.getMoveList().isEmpty();
	}

	int updateZeroA() {
		return mFile.updateZeroA();
	}

	void saveFile() {
		mFile.saveFile(mActivity);
	}

	void setBeginNumber(int beginNumber) {
		for (WeldCountFile.Job job : mDataset) {
			job.setCN(String.valueOf(beginNumber++));
			if (beginNumber > WeldCountFile.VALUE_MAX)
				beginNumber = WeldCountFile.VALUE_MAX;
		}
		notifyDataSetChanged();
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		Context context = parent.getContext();
		final View v = LayoutInflater.from(context)
				.inflate(R.layout.weldcount_file_editor_view_holder_item, parent, false);
		final ViewHolder holder = new ViewHolder(v);
		holder.editText.setOnFocusChangeListener((v1, hasFocus) -> {
			try {
				if (!hasFocus) {
					final TextInputEditText editText = (TextInputEditText) v1;
					final String editTextString = editText.getText().toString();
					final WeldCountFile.Job item = (WeldCountFile.Job) v1.getTag(R.string.tag_item);
					if (editTextString.equals("")) {
						editText.setText(item.getCN());
						return;
					}
					Integer valueInteger = Integer.parseInt(editTextString);
					if (valueInteger > WeldCountFile.VALUE_MAX)
						valueInteger = WeldCountFile.VALUE_MAX;
					final String valueString = String.format(Locale.KOREA, "%d", valueInteger);
					if (!valueString.equals(editText.getText().toString()))
						editText.setText(valueString);
					item.setCN(valueString);
					if (!item.getCN().equals(valueString))
						weldCountFragment.mWeldCountAdapter.notifyItemChanged((int) v1.getTag(R.string.tag_position));
				}
			} catch (NumberFormatException e) {
				WeldCountFragment.logD(e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		holder.editText.setOnEditorActionListener((v12, actionId, event) -> {
			if (actionId == 6) {
				Helper.UiHelper.hideSoftKeyboard(mActivity, v12, event);
				return true;
			}
			return false;
		});
		return holder;
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder rh, final int position) {
		final ViewHolder holder = (ViewHolder) rh;
		final WeldCountFile.Job item = mDataset.get(position);
		holder.textInputLayout.setHint(String.format(Locale.KOREA, "%03d", item.getRowNumber()));
		holder.editText.setText(item.getCN());
		holder.editText.setTag(R.string.tag_position, position);
		holder.editText.setTag(R.string.tag_item, item);
		if (position < mDataset.size() - 1) {
			holder.editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		} else {
			holder.editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
		}
	}

	@Override
	public int getItemCount() {
		return mDataset.size();
	}

	private class ViewHolder extends RecyclerView.ViewHolder {
		final View mItemView;
		final TextInputLayout textInputLayout;
		final TextInputEditText editText;

		ViewHolder(View itemView) {
			super(itemView);
			mItemView = itemView;
			textInputLayout = (TextInputLayout) mItemView.findViewById(R.id.textInputLayout);
			editText = (TextInputEditText) mItemView.findViewById(R.id.editText);
		}
	}
}
