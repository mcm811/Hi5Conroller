package com.changyoung.hi5controller;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnWorkPathListener} interface
 * to handle interaction events.
 * Use the {@link WeldConditionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WeldConditionFragment extends android.support.v4.app.Fragment
		implements Refresh {
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_WORK_PATH = "workPath";
	private static final String TAG = "WeldConditionFragment";

	private View mView;
	private ListView mListView;
	private WeldConditionAdapter<WeldConditionItem> adapter;
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
				fab_click();
			}
		});
		fab.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Util.UiUtil.textViewActivity(getContext(), "ROBOT.SWD", Util.FileUtil.readFileString(onGetWorkPath()));
				return true;
			}
		});

		final int selectedBackGroundColor = ContextCompat.getColor(getContext(), R.color.tab3_textview_background);
		mListView = (ListView) mView.findViewById(R.id.weld_condition_list_view);
		mListView.setAdapter(adapter);
		mListView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				adapter.getItem(position).setItemChecked(mListView.isItemChecked(position));
				if (mListView.isItemChecked(position)) {
					lastPosition = position;
					view.setBackgroundColor(selectedBackGroundColor);
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
				fab_click();
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
			if (forced || adapter.getCount() == 0) {
				mWorkPath = onGetWorkPath();
				if (adapter == null)
					adapter = new WeldConditionAdapter<>(getActivity());
				adapter.refresh(mWorkPath);
				if (mListView != null)
					mListView.refreshDrawableState();
			}
			setCheckedItem(true);
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
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
			else if (mListView.getCheckedItemCount() == 0)
				fab.setImageResource(R.drawable.ic_subject_white);
			else
				fab.setImageResource(R.drawable.ic_edit_white);
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fab_click() {
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

		final ArrayList<Integer> positions = new ArrayList<>();
		try {
			SparseBooleanArray checkedList = mListView.getCheckedItemPositions();
			for (int i = 0; i < checkedList.size(); i++) {
				if (checkedList.valueAt(i)) {
					positions.add(checkedList.keyAt(i));
				}
			}
			if (positions.size() == 0)
				lastPosition = 0;
		} catch (NullPointerException e) {
			return;
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_weld_condition, null);
		AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
		dialog.setView(dialogView);

		final ArrayList<EditText> etList = new ArrayList<>();
		etList.add((EditText) dialogView.findViewById(R.id.etOutputData));
		etList.add((EditText) dialogView.findViewById(R.id.etOutputType));
		etList.add((EditText) dialogView.findViewById(R.id.etSqueezeForce));
		etList.add((EditText) dialogView.findViewById(R.id.etMoveTipClearance));
		etList.add((EditText) dialogView.findViewById(R.id.etFixedTipClearance));
		etList.add((EditText) dialogView.findViewById(R.id.etPannelThickness));
		etList.add((EditText) dialogView.findViewById(R.id.etCommandOffset));

		// 임계치
		int[] etMax = {2000, 100, 350, 500, 500, 500, 500, 1000, 1000};
		for (int i = 0; i < etList.size(); i++) {
			final EditText et = etList.get(i);
			if (i == 0) {                                           // outputData
				et.setText(adapter.getItem(lastPosition).get(i));   // 기본선택된 자료값 가져오기
			} else {
				et.setSingleLine();
				et.setSelectAllOnFocus(true);
			}
			// 임계치 설정
			if (i < 2) {
				final int maxValue = etMax[i];
				et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						try {
							Integer etNumber = Integer.parseInt(et.getText().toString());
							if (etNumber > maxValue)
								etNumber = maxValue;
							et.setText(String.valueOf(etNumber));
						} catch (NumberFormatException e) {
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			} else {
				final Float maxValue = (float) etMax[i];
				et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						try {
							Float etNumber = Float.parseFloat(et.getText().toString());
							if (etNumber > maxValue)
								etNumber = maxValue;
							et.setText(String.format("%.1f", etNumber));
						} catch (NumberFormatException e) {
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
			et.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Util.UiUtil.hideSoftKeyboard(getActivity(), v, event);
					Log.d(TAG, "KeyCode: " + String.valueOf(keyCode));
					return false;
				}
			});

			final TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
			statusText.setText(adapter.getItem(lastPosition).get(0));
			if (positions.size() > 0) {
				StringBuilder sb = new StringBuilder();
				for (Integer pos : positions) {
					sb.append(String.valueOf(pos + 1));
					sb.append(" ");
				}
				sb.insert(0, "수정 항목: ");
				statusText.setText(sb.toString().trim());
			} else {
				String buf = "수정 항목: " + String.valueOf(lastPosition + 1);
				statusText.setText(buf);
			}

			final SeekBar sampleSeekBar = (SeekBar) dialogView.findViewById(R.id.sampleSeekBar);
			sampleSeekBar.setMax(adapter.getCount() - 1);
			sampleSeekBar.setProgress(Integer.parseInt(adapter.getItem(lastPosition).get(0)) - 1);
			sampleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						for (int i = 0; i < etList.size(); i++) {
							if (!etList.get(i).getText().toString().equals(""))
								etList.get(i).setText(adapter.getItem(sampleSeekBar.getProgress()).get(i));
						}
						if (positions.size() == 0) {
							lastPosition = sampleSeekBar.getProgress();
							String buf = "수정 항목: " + String.valueOf(lastPosition + 1);
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
							if (positions.size() > 0) {
								StringBuilder sb = new StringBuilder();
								for (int pos : positions) {
									sb.append(String.valueOf(pos + 1));
									sb.append(" ");
								}
								sb.insert(0, "수정 항목: ");
								statusText.setText(sb.toString().trim());
							} else {
								String buf = "수정 항목: " + String.valueOf(lastPosition + 1);
								statusText.setText(buf);
							}
						} else {
							String buf = "수정 범위: " + String.valueOf(sb1Progress + 1) + " ~ " + String.valueOf(sb2Progress + 1);
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
							if (positions.size() > 0) {
								StringBuilder sb = new StringBuilder();
								for (int pos : positions) {
									sb.append(String.valueOf(pos + 1));
									sb.append(" ");
								}
								sb.insert(0, "수정 항목: ");
								statusText.setText(sb.toString().trim());
							} else {
								String buf = "수정 항목: " + String.valueOf(lastPosition + 1);
								statusText.setText(buf);
							}
						} else {
							String buf = "수정 범위: " + String.valueOf(sb1Progress + 1) + " ~ " + String.valueOf(sb2Progress + 1);
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

			dialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					setCheckedItem(false);
				}
			});

			dialog.setPositiveButton("저장", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int seekBegin = beginSeekBar.getProgress() + 1;
					int seekEnd = endSeekBar.getMax() - endSeekBar.getProgress() + 1;
					boolean isSeek = !((seekBegin == 1 && seekEnd == 1) || (seekBegin == beginSeekBar.getMax() + 1 && seekEnd == endSeekBar.getMax() + 1));
					boolean isUpdate = false;

					if (positions.size() == 0)
						positions.add(lastPosition);
					if (isSeek) {
						positions.clear();
						for (int rowNum = seekBegin - 1; rowNum < seekEnd; rowNum++) {
							positions.add(rowNum);
						}
					}
					for (int rowNum : positions) {
						for (int colNum = 1; colNum < etList.size(); colNum++) {
							if (!etList.get(colNum).getText().toString().equals("")) {
								adapter.getItem(rowNum).set(colNum, etList.get(colNum).getText().toString());
								isUpdate = true;
							}
						}
						mListView.setItemChecked(rowNum, false);
						adapter.getItem(rowNum).setItemChecked(false);
					}
					if (isUpdate) {
						adapter.update(onGetWorkPath());
						adapter.notifyDataSetChanged();
					} else {
						setCheckedItem(false);
					}
					fab_setImage();
				}
			});
		}

		dialog.show();
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
