package com.changyoung.hi5controller.weldcondition;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.UiHelper;

import java.util.List;
import java.util.Locale;

/**
 * Created by changmin811@gmail.com on 2017. 4. 25
 */
public class WeldConditionSqueezeForceAdapter extends WeldConditionAdapter {
	private final WeldConditionFragment weldConditionFragment;

	WeldConditionSqueezeForceAdapter(WeldConditionFragment weldConditionFragment,
	                                 Activity activity,
	                                 List<WeldConditionItem> dataSet) {
		super(weldConditionFragment, activity, dataSet);
		this.weldConditionFragment = weldConditionFragment;
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		mContext = parent.getContext();
		final View v = LayoutInflater.from(mContext)
				.inflate(R.layout.weldcondition_squeeze_force_view_holder_item, parent, false);
		final ViewHolder holder = new ViewHolder(v);
		holder.editText.setOnFocusChangeListener((v12, hasFocus) -> {
			try {
				if (!hasFocus) {
					final EditText editText = (EditText) v12;
					final String editTextString = editText.getText().toString();
					final WeldConditionItem item = (WeldConditionItem) v12.getTag(R.string.tag_item);
					if (editTextString.equals("")) {
						editText.setText(item.get(WeldConditionItem.SQUEEZE_FORCE));
						return;
					}
					Integer valueInteger = Integer.parseInt(editTextString);
					if (valueInteger > WeldConditionFragment.valueMax[WeldConditionItem.SQUEEZE_FORCE])
						valueInteger = WeldConditionFragment.valueMax[WeldConditionItem.SQUEEZE_FORCE];
					final String valueString = String.format(Locale.KOREA, "%d", valueInteger);
					if (!valueString.equals(editText.getText().toString()))
						editText.setText(valueString);
					item.set(WeldConditionItem.SQUEEZE_FORCE, valueString);
					if (!item.get(WeldConditionItem.SQUEEZE_FORCE).equals(valueString))
						weldConditionFragment.mWeldConditionAdapter.notifyItemChanged((int) v12.getTag(R.string.tag_position));
				}
			} catch (NumberFormatException e) {
				WeldConditionFragment.logD(e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		holder.editText.setOnEditorActionListener((v1, actionId, event) -> {
//					Log.i("onEditorAction", "actionId: " + String.valueOf(actionId));
			if (actionId == 6) {
				UiHelper.hideSoftKeyboard(mActivity, v1, event);
				return true;
			}
			return false;
		});
		return holder;
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder rh, final int position) {
		final ViewHolder holder = (ViewHolder) rh;
		final WeldConditionItem item = mDataset.get(position);
		holder.textInputLayout.setHint(String.format(Locale.KOREA, "%03d",
				Integer.parseInt(item.get(WeldConditionItem.OUTPUT_DATA))));
		holder.editText.setText(item.get(WeldConditionItem.SQUEEZE_FORCE));
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

	void showSqueezeForceEditorDialog() {
		if (weldConditionFragment.mSnackbar != null) {
			weldConditionFragment.mSnackbar.dismiss();
			weldConditionFragment.mSnackbar = null;
		}

		if (weldConditionFragment.mWeldConditionAdapter.getItemCount() == 0) {
			weldConditionFragment.onRefresh(false);
			if (weldConditionFragment.mWeldConditionAdapter.getItemCount() == 0)
				weldConditionFragment.show("항목이 없습니다");
			return;
		}

		@SuppressLint("InflateParams")
		View dialogView = LayoutInflater.from(weldConditionFragment.getContext())
				.inflate(R.layout.weldcondition_squeeze_force_editor_dialog, null);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(weldConditionFragment.getContext());
		dialogBuilder.setView(dialogView);

		RecyclerView mSqueezeForceRecyclerView = (RecyclerView) dialogView.findViewById(R.id.recycler_view);
		mSqueezeForceRecyclerView.setHasFixedSize(true);
		RecyclerView.LayoutManager layoutManager = new GridLayoutManager(weldConditionFragment.getContext(), 4,
				LinearLayoutManager.VERTICAL, false);
		mSqueezeForceRecyclerView.setLayoutManager(layoutManager);
		mSqueezeForceRecyclerView.setAdapter(weldConditionFragment.mWeldConditionSqueezeForceAdapter);

/*
		AdView adView = new AdView(getContext());
		adView.setAdSize(AdSize.BANNER);
		adView.setScaleX(0.85f);
		adView.setScaleY(0.85f);
		if (BuildConfig.DEBUG)
			adView.setAdUnitId(getContext().getString(R.string.banner_ad_unit_id_debug));
		else
			adView.setAdUnitId(getContext().getString(R.string.banner_ad_unit_id_release));
		AdRequest adRequest = new AdRequest.Builder()
				.setRequestAgent("android_studio:ad_template").build();
		adView.loadAd(adRequest);
		LinearLayout sfLinearLayout = (LinearLayout)
				dialogView.findViewById(R.id.linearLayout_WeldCondition_SqueezeForce);
		sfLinearLayout.addView(adView, sfLinearLayout.getChildCount());
*/

		TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
		statusText.setText(String.format(Locale.KOREA, "가압력 수정 (용접 조건: %d개)", weldConditionFragment.mWeldConditionAdapter.getItemCount()));

		dialogBuilder.setNegativeButton("취소", (dialog, which) -> weldConditionFragment.onRefresh(true));

		dialogBuilder.setPositiveButton("저장", (dialog, which) -> {
			if (weldConditionFragment.mWeldConditionSqueezeForceAdapter.getItemCount() > 0) {
				weldConditionFragment.mWeldConditionSqueezeForceAdapter.update(weldConditionFragment.onGetWorkPath());
				weldConditionFragment.mWeldConditionAdapter.notifyDataSetChanged();
				weldConditionFragment.show("저장 완료: " + weldConditionFragment.onGetWorkPath());
			}
		});
		AlertDialog alertDialog = dialogBuilder.create();
		Window window = alertDialog.getWindow();
		if (window != null) {
			window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
					| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
			window.getAttributes().windowAnimations = R.style.AlertDialogAnimation;
		}
		alertDialog.setCanceledOnTouchOutside(false);
		alertDialog.show();
	}

	private class ViewHolder extends RecyclerView.ViewHolder {
		final View mItemView;
		final TextInputLayout textInputLayout;
		final EditText editText;

		ViewHolder(View itemView) {
			super(itemView);
			mItemView = itemView;
			textInputLayout = (TextInputLayout) mItemView.findViewById(R.id.textInputLayout);
			editText = (EditText) mItemView.findViewById(R.id.editText);
		}
	}
}
