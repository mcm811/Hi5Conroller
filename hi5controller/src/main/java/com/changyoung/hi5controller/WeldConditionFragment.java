package com.changyoung.hi5controller;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnWorkPathListener} interface
 * to handle interaction events.
 * Use the {@link WeldConditionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WeldConditionFragment extends Fragment
		implements Refresh {
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_WORK_PATH = "workPath";
	private static final String TAG = "WeldConditionFragment";

	private View mView;
	private ListView mListView;
	private WeldCondition.ConditionAdapter adapter;
	private Snackbar snackbar;

	private String mWorkPath;
	private int lastPosition = 0;

	private OnWorkPathListener mListener;

	public WeldConditionFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param workPath Parameter 1.
	 * @return A new instance of fragment WeldConditionFragment.
	 */
	public static WeldConditionFragment newInstance(String workPath) {
		WeldConditionFragment fragment = new WeldConditionFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mWorkPath = getArguments().getString(ARG_WORK_PATH) + "/ROBOT.SWD";
		} else {
			mWorkPath = onGetWorkPath();
		}
		refresh(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.fragment_weld_condition, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) mView.findViewById(R.id.srl);
		refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refresh(true);
				refresher.setRefreshing(false);
			}
		});

		FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.weld_condition_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				fab_click(0);
			}
		});
		fab.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Util.UiUtil.textViewActivity(getContext(), "ROBOT.SWD", Util.FileUtil.readFileString(onGetWorkPath()));
				return true;
			}
		});

		mListView = (ListView) mView.findViewById(R.id.weld_condition_list_view);
		mListView.setAdapter(adapter);
		mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				adapter.getItem(position).setItemChecked(mListView.isItemChecked(position));
				if (mListView.isItemChecked(position)) {
					lastPosition = position;
					view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.tab3_textview_background));
				} else {
					view.setBackgroundColor(Color.TRANSPARENT);
				}
				fab_setImage();
				snackbar_setCheckedItem();
			}
		});
		mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				lastPosition = position;
				fab_click(position);
				return true;
			}
		});

		fab_setImage();

		return mView;
	}

	@Override
	public void onResume() {
		super.onResume();
		refresh(true);
	}

	private void snackbar_setCheckedItem() {
		if (mView == null)
			return;
		try {
			if (snackbar == null || !snackbar.isShown()) {
				snackbar = Snackbar
						.make(mView.findViewById(R.id.coordinator_layout),
								String.valueOf(mListView.getCheckedItemCount()) + "개 항목 선택됨",
								Snackbar.LENGTH_INDEFINITE)
						.setAction("선택 취소", new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								setCheckedItem(false);
								snackbar = null;
							}
						});
			}
			if (mListView.getCheckedItemCount() > 0 && adapter.getCount() > 0) {
				if (snackbar.isShown())
					snackbar.setText(String.valueOf(mListView.getCheckedItemCount()) + "개 항목 선택됨");
				else
					snackbar.show();
			} else {
				snackbar.dismiss();
				snackbar = null;
			}
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setCheckedItem(boolean value) {
		try {
			if (adapter.getCount() == 0)
				mListView.clearChoices();
			SparseBooleanArray checkedArray = mListView.getCheckedItemPositions();
			for (int i = 0; i < checkedArray.size(); i++) {
				if (checkedArray.valueAt(i)) {
					int position = checkedArray.keyAt(i);
					mListView.setItemChecked(position, value);
					adapter.getItem(position).setItemChecked(value);
				}
			}
			adapter.notifyDataSetChanged();
			mListView.refreshDrawableState();
			fab_setImage();
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		snackbar_setCheckedItem();
	}

	private void logDebug(String msg) {
		try {
			Log.d(getActivity().getPackageName(), TAG + ": " + msg);
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String onGetWorkPath() {
		if (mListener != null) {
			return mListener.onGetWorkPath() + "/ROBOT.SWD";
		}
		return null;
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		try {
			mListener = (OnWorkPathListener) activity;
		} catch (NullPointerException e) {
		} catch (ClassCastException e) {
			e.printStackTrace();
//			throw new ClassCastException(activity.toString()
//					+ " must implement OnPathChangedListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void refresh(boolean forced) {
		try {
			mWorkPath = onGetWorkPath();
			if (adapter == null)
				adapter = new WeldCondition.ConditionAdapter(getActivity());
			if (forced || adapter.getCount() == 0) {
				adapter.refresh(mWorkPath);
				if (mListView != null)
					mListView.refreshDrawableState();
			}
			setCheckedItem(true);
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mView != null) {
			if (adapter.getCount() > 0) {
				mView.findViewById(R.id.list_title).setVisibility(View.VISIBLE);
				mView.findViewById(R.id.imageView).setVisibility(View.GONE);
				mView.findViewById(R.id.textView).setVisibility(View.GONE);
			} else {
				mView.findViewById(R.id.list_title).setVisibility(View.GONE);
				mView.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
				mView.findViewById(R.id.textView).setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public boolean refresh(String path) {
		return false;
	}

	@Override
	public String refresh(int menuId) {
		return null;
	}

	@Override
	public String onBackPressedFragment() {
		if (mListView != null) {
			if (mListView.getCheckedItemCount() > 0) {
				setCheckedItem(false);
				return "unslect";
			}
		}
		return null;
	}

	@Override
	public void show(String msg) {
		try {
			if (msg == null)
				return;
			Snackbar.make(mView.findViewById(R.id.coordinator_layout), msg, Snackbar.LENGTH_SHORT)
					.setAction("Action", null).show();
			logDebug(msg);
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fab_setImage() {
		try {
			FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.weld_condition_fab);
			if (adapter.getCount() == 0)
				fab.setImageResource(R.drawable.ic_refresh_white);
//			else if (mListView.getCheckedItemCount() == 0)
//				fab.setImageResource(R.drawable.ic_subject_white);
			else
				fab.setImageResource(R.drawable.ic_edit_white);
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fab_click(int position) {
		final String dialog_title1 = getContext().getString(R.string.weld_condition_dialog_title1) + " ";
		final String dialog_title2 = getContext().getString(R.string.weld_condition_dialog_title2) + " ";

		if (snackbar != null) {
			snackbar.dismiss();
			snackbar = null;
		}

		if (adapter.getCount() == 0) {
			refresh(false);
			if (adapter.getCount() == 0)
				show("항목이 없습니다");
			return;
		}

		final ArrayList<Integer> checkedPositions = new ArrayList<>();
		try {
			SparseBooleanArray checkedList = mListView.getCheckedItemPositions();
			for (int i = 0; i < checkedList.size(); i++) {
				if (checkedList.valueAt(i)) {
					checkedPositions.add(checkedList.keyAt(i));
				}
			}
			if (checkedPositions.size() == 0)
				lastPosition = position;
		} catch (NullPointerException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		final View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_weld_condition, null);
		final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
		dialogBuilder.setView(dialogView);

		// Custom Title
		TextView textViewTitle = new TextView(getContext());
		textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		textViewTitle.setTypeface(Typeface.DEFAULT_BOLD);
		textViewTitle.setPadding(20, 10, 20, 10);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
			textViewTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
		textViewTitle.setText("용접 조건 수정");
		dialogBuilder.setCustomTitle(textViewTitle);

		AdView adView = new AdView(getContext());
		adView.setAdSize(AdSize.BANNER);
		adView.setScaleX(0.95f);
		adView.setScaleY(0.95f);
		if (BuildConfig.DEBUG)
			adView.setAdUnitId(getActivity().getString(R.string.banner_ad_unit_id_debug));
		else
			adView.setAdUnitId(getActivity().getString(R.string.banner_ad_unit_id_release));
		AdRequest adRequest = new AdRequest.Builder()
				.setRequestAgent("android_studio:ad_template").build();
		adView.loadAd(adRequest);
		LinearLayout linearLayout = (LinearLayout) dialogView.findViewById(R.id.linearLayout1);
		linearLayout.addView(adView, linearLayout.getChildCount());

		final ArrayList<TextInputLayout> tilList = new ArrayList<>();
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout1));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout2));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout3));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout4));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout5));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout6));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout7));

		// 임계치
		final int[] valueMax = {2000, 100, 350, 500, 500, 500, 500, 1000, 1000};
		for (int index = 0; index < tilList.size(); index++) {
			final TextInputLayout til = tilList.get(index);
			final EditText et = til.getEditText();
			if (index == 0) {                                           // outputData
				et.setText(adapter.getItem(lastPosition).get(index));   // 기본선택된 자료값 가져오기
			} else {
				et.setGravity(Gravity.CENTER);
				et.setSelectAllOnFocus(true);
				et.setSingleLine();
				try {
					til.setTag(til.getHint());
					til.setHint(til.getTag() + "(" + adapter.getItem(lastPosition).get(index) + ")");
				} catch (NullPointerException e) {
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			final int finalIndex = index;
			et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					if (hasFocus) {
						final SeekBar sampleSeekBar = (SeekBar) dialogView.findViewById(R.id.sampleSeekBar);
						if (et.getText().length() == 0) {
							et.setText(adapter.getItem(sampleSeekBar.getProgress()).get(finalIndex));
							et.selectAll();
						}
					} else {
						try {
							// 임계치 처리 (1, 2번 정수, 3번부터 부동소수)
							if (finalIndex < 3) {
								Integer etNumber = Integer.parseInt(et.getText().toString());
								if (etNumber > valueMax[finalIndex])
									etNumber = valueMax[finalIndex];
								et.setText(String.valueOf(etNumber));
							} else {
								Float etNumber = Float.parseFloat(et.getText().toString());
								if (etNumber > (float) valueMax[finalIndex])
									etNumber = (float) valueMax[finalIndex];
								et.setText(String.format("%.1f", etNumber));
							}
						} catch (NumberFormatException e) {
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
			et.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Util.UiUtil.hideSoftKeyboard(getActivity(), v, event);
					Log.d(TAG, "KeyCode: " + String.valueOf(keyCode));
					return false;
				}
			});
		}

		final TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
		statusText.setText(adapter.getItem(lastPosition).get(0));
		if (checkedPositions.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (Integer pos : checkedPositions) {
				sb.append(String.valueOf(pos + 1));
				sb.append(" ");
			}
			sb.insert(0, dialog_title1);
			statusText.setText(sb.toString().trim());
		} else {
			String buf = dialog_title1 + String.valueOf(lastPosition + 1);
			statusText.setText(buf);
		}

		final SeekBar sampleSeekBar = (SeekBar) dialogView.findViewById(R.id.sampleSeekBar);
		sampleSeekBar.setMax(adapter.getCount() - 1);
		sampleSeekBar.setProgress(Integer.parseInt(adapter.getItem(lastPosition).get(0)) - 1);
		sampleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					try {
						tilList.get(0).getEditText().setText(adapter.getItem(progress).get(0));
					} catch (NullPointerException e) {
					} catch (Exception e) {
						e.printStackTrace();
					}
					for (int index = 1; index < tilList.size(); index++) {
						try {
							// 샘플바를 움직이면 힌트에 기존 값을 보여주도록 세팅한다
							tilList.get(index).setHint(tilList.get(index).getTag() + "(" + adapter.getItem(progress).get(index) + ")");
						} catch (NullPointerException e) {
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (checkedPositions.size() == 0) {
						lastPosition = progress;
						statusText.setText(String.format("%s %d", dialog_title1, lastPosition + 1));
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		// 선택 시작
		final SeekBar beginSeekBar = (SeekBar) dialogView.findViewById(R.id.sbBegin);
		beginSeekBar.setMax(adapter.getCount() - 1);
		beginSeekBar.setProgress(0);

		// 선택 끝
		final SeekBar endSeekBar = (SeekBar) dialogView.findViewById(R.id.sbEnd);
		endSeekBar.setMax(adapter.getCount() - 1);
		endSeekBar.setProgress(endSeekBar.getMax());

		beginSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					int sb1Progress = beginSeekBar.getProgress();
					int sb2Progress = endSeekBar.getMax() - endSeekBar.getProgress();
					if (sb1Progress > sb2Progress) {
						sb2Progress = sb1Progress;
						endSeekBar.setProgress(endSeekBar.getMax() - sb1Progress);
					}
					if (sb1Progress == 0 && sb2Progress == 0 || sb1Progress == beginSeekBar.getMax() && sb2Progress == endSeekBar.getMax()) {
						if (checkedPositions.size() > 0) {
							StringBuilder sb = new StringBuilder();
							for (int pos : checkedPositions) {
								sb.append(String.valueOf(pos + 1));
								sb.append(" ");
							}
							sb.insert(0, dialog_title1);
							statusText.setText(sb.toString().trim());
						} else {
							String buf = dialog_title1 + String.valueOf(lastPosition + 1);
							statusText.setText(buf);
						}
					} else {
						String buf = dialog_title2 + String.valueOf(sb1Progress + 1) + " ~ " + String.valueOf(sb2Progress + 1);
						statusText.setText(buf);
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		endSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					int sb1Progress = beginSeekBar.getProgress();
					int sb2Progress = endSeekBar.getMax() - endSeekBar.getProgress();
					if (sb2Progress < sb1Progress) {
						sb1Progress = sb2Progress;
						beginSeekBar.setProgress(sb2Progress);
					}
					if (sb1Progress == 0 && sb2Progress == 0 || sb1Progress == beginSeekBar.getMax() && sb2Progress == endSeekBar.getMax()) {
						if (checkedPositions.size() > 0) {
							StringBuilder sb = new StringBuilder();
							for (int pos : checkedPositions) {
								sb.append(String.valueOf(pos + 1));
								sb.append(" ");
							}
							sb.insert(0, dialog_title1);
							statusText.setText(sb.toString().trim());
						} else {
							String buf = dialog_title1 + String.valueOf(lastPosition + 1);
							statusText.setText(buf);
						}
					} else {
						String buf = dialog_title2 + String.valueOf(sb1Progress + 1) + " ~ " + String.valueOf(sb2Progress + 1);
						statusText.setText(buf);
					}
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		dialogBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				setCheckedItem(true);
			}
		});

		dialogBuilder.setPositiveButton("저장", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int seekBegin = beginSeekBar.getProgress() + 1;
				int seekEnd = endSeekBar.getMax() - endSeekBar.getProgress() + 1;
				boolean isSeek = !((seekBegin == 1 && seekEnd == 1) || (seekBegin == beginSeekBar.getMax() + 1 && seekEnd == endSeekBar.getMax() + 1));
				boolean isUpdate = false;

				if (checkedPositions.size() == 0)
					checkedPositions.add(lastPosition);
				if (isSeek) {
					checkedPositions.clear();
					for (int rowNum = seekBegin - 1; rowNum < seekEnd; rowNum++) {
						checkedPositions.add(rowNum);
					}
				}
				for (int rowNum : checkedPositions) {
					for (int colNum = 1; colNum < tilList.size(); colNum++) {
						try {
							if (tilList.get(colNum).getEditText().getText().toString().length() > 0) {
								adapter.getItem(rowNum).set(colNum, tilList.get(colNum).getEditText().getText().toString());
								isUpdate = true;
							}
						} catch (NullPointerException e) {
							e.printStackTrace();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
//					mListView.setItemChecked(rowNum, false);
//					adapter.getItem(rowNum).setItemChecked(false);
				}
				if (isUpdate) {
					adapter.update(onGetWorkPath());
					adapter.notifyDataSetChanged();
					fab_setImage();
				}
				setCheckedItem(true);
			}
		});

		dialogBuilder.show();
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction : this fragment to be communicated
	 * to the activity and potentially other fragments contained : that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnWorkPathListener {
		String onGetWorkPath();
	}
}
