package com.changyoung.hi5controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by chang on 2015-10-14.
 * changmin811@gmail.com
 */
public class WeldConditionFragment extends Fragment
		implements Refresh, LoaderManager.LoaderCallbacks<List<WeldConditionFragment.WeldConditionItem>> {

	private static final int MSG_REFRESH = 0;

	private static final String ARG_WORK_PATH = "workPath";
	private static final String TAG = "WeldConditionFragment";

	private View mView;
	private ListView mListView;
	private WeldConditionAdapter adapter;
	private Snackbar snackbar;

	private LooperHandler looperHandler;
	private WeldConditionObserver observer;

	//	private String mWorkPath;
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
	@SuppressWarnings("unused")
	public static WeldConditionFragment newInstance(String workPath) {
		WeldConditionFragment fragment = new WeldConditionFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		fragment.setArguments(args);
		return fragment;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "onAttach");
		super.onAttach(activity);
		try {
			mListener = (OnWorkPathListener) activity;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ClassCastException(activity.toString()
					+ " must implement OnPathChangedListener");
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// onCreated -> onCreatedView -> onActivityCreated
		logD("onCreated");
		super.onCreate(savedInstanceState);
//		if (getArguments() != null) {
//			mWorkPath = getArguments().getString(ARG_WORK_PATH) + "/ROBOT.SWD";
//		} else {
//			mWorkPath = onGetWorkPath();
//		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		logD("onCreatedView");
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
				dialog_show(0);
			}
		});
		fab.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Helper.UiHelper.textViewActivity(getContext(), "ROBOT.SWD", Helper.FileHelper.readFileString(onGetWorkPath()));
				return true;
			}
		});

		adapter = new WeldConditionAdapter(getActivity());
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
				dialog_show(position);
				return true;
			}
		});

		looperHandler = new LooperHandler(Looper.getMainLooper());
		observer = new WeldConditionObserver(onGetWorkPath(), looperHandler);
		observer.startWatching();

		return mView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		logD("onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		try {
			getLoaderManager().initLoader(0, null, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}

	@Override
	public void onDetach() {
		Log.d(TAG, "onDetach");
		super.onDetach();
		mListener = null;

		adapter.clear();
		adapter = null;
		mView = null;
		mListView = null;
		snackbar = null;
		getLoaderManager().destroyLoader(0);
		looperHandler = null;
	}

	@Override
	public void refresh(boolean forced) {
		try {
			if (isAdded()) {
				Log.d(TAG, "refresh: restartLoader");
				getLoaderManager().restartLoader(0, null, this);
				observer = new WeldConditionObserver(onGetWorkPath(), looperHandler);
				observer.startWatching();
			}
		} catch (IllegalStateException e) {
			Log.d(TAG, e.getLocalizedMessage());
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
				return "cancel";
			}
		}
		return null;
	}

	@Override
	public void show(String msg) {
		try {
			if (msg != null && isAdded()) {
				Snackbar.make(mView.findViewById(R.id.coordinator_layout), msg, Snackbar.LENGTH_SHORT)
						.setAction("Action", null).show();
				logD(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void dialog_show(int position) {
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

		final List<Integer> checkedPositions = new ArrayList<>();
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

		@SuppressLint("InflateParams") final View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_weld_condition, null);
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

		final List<TextInputLayout> tilList = new ArrayList<>();
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
			if (et != null) {
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
						logD(e.getLocalizedMessage());
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
								logD(e.getLocalizedMessage());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
				et.setOnKeyListener(new View.OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						Helper.UiHelper.hideSoftKeyboard(getActivity(), v, event);
						//Log.d(TAG, "KeyCode: " + String.valueOf(keyCode));
						return false;
					}
				});
			}
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
						EditText editText0 = tilList.get(0).getEditText();
						if (editText0 != null)
							editText0.setText(adapter.getItem(progress).get(0));
					} catch (NullPointerException e) {
						logD(e.getLocalizedMessage());
					} catch (Exception e) {
						e.printStackTrace();
					}
					for (int index = 1; index < tilList.size(); index++) {
						try {
							// 샘플바를 움직이면 힌트에 기존 값을 보여주도록 세팅한다
							tilList.get(index).setHint(tilList.get(index).getTag() + "(" + adapter.getItem(progress).get(index) + ")");
						} catch (NullPointerException e) {
							logD(e.getLocalizedMessage());
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
							EditText editText = tilList.get(colNum).getEditText();
							if (editText != null && editText.getText().toString().length() > 0) {
								adapter.getItem(rowNum).set(colNum, editText.getText().toString());
								isUpdate = true;
							}
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

	private void snackbar_setCheckedItem() {
		try {
			if (mView != null && isAdded()) {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setCheckedItem(boolean value) {
		try {
			if (isAdded()) {
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		snackbar_setCheckedItem();
	}

	private void fab_setImage() {
		try {
			if (isAdded()) {
				FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.weld_condition_fab);
				if (adapter.getCount() == 0)
					fab.setImageResource(R.drawable.ic_refresh_white);
				else
					fab.setImageResource(R.drawable.ic_edit_white);
			}
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

	private void logD(String msg) {
		try {
			Log.d(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Loader<List<WeldConditionItem>> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, String.format("ID_%d onCreateLoader()", id));
		return new WeldConditionLoader(getActivity(), mListener);
	}

	@Override
	public void onLoadFinished(Loader<List<WeldConditionItem>> loader, List<WeldConditionItem> data) {
		Log.d(TAG, String.format("id:%d, onLoadFinished() size:%d", loader.getId(), data.size()));
		adapter.setData(data);
		if (mListView != null)
			mListView.refreshDrawableState();
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
		setCheckedItem(true);
	}

	@Override
	public void onLoaderReset(Loader<List<WeldConditionItem>> loader) {
		Log.d(TAG, String.format("ID_%d onLoaderReset()", loader.getId()));
		adapter.setData(null);
		adapter.notifyDataSetInvalidated();
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

	public static class WeldConditionObserver extends FileObserver {
		static final String TAG = "WeldConditionObserver";
		static final int mask = CREATE | DELETE | DELETE_SELF |
				MOVED_FROM | MOVED_TO | MOVE_SELF | CLOSE_WRITE;
		File file;
		private Handler handler;

		@SuppressWarnings("unused")
		public WeldConditionObserver(File file, Handler handler) {
			super(file.getPath(), mask);
			this.file = file;
			this.handler = handler;
			Log.d(TAG, "FILE_OBSERVER: " + file.getPath());
		}

		public WeldConditionObserver(String path, Handler handler) {
			super(path, mask);
			this.file = new File(path);
			this.handler = handler;
			Log.d(TAG, "FILE_OBSERVER: " + path);
		}

		public void onEvent(int event, String path) {
			if ((event & CREATE) == CREATE)
				Log.d(TAG, String.format("CREATE: %s/%s", file.getPath(), path));
			else if ((event & DELETE) == DELETE)
				Log.d(TAG, String.format("DELETE: %s/%s", file.getPath(), path));
			else if ((event & DELETE_SELF) == DELETE_SELF)
				Log.d(TAG, String.format("DELETE_SELF: %s/%s", file.getPath(), path));
			else if ((event & MOVED_FROM) == MOVED_FROM)
				Log.d(TAG, String.format("MOVED_FROM: %s/%s", file.getPath(), path));
			else if ((event & MOVED_TO) == MOVED_TO)
				Log.d(TAG, String.format("MOVED_TO: %s", path == null ? file.getPath() : path));
			else if ((event & MOVE_SELF) == MOVE_SELF)
				Log.d(TAG, String.format("MOVE_SELF: %s", path == null ? file.getPath() : path));
			else if ((event & CLOSE_WRITE) == CLOSE_WRITE)
				Log.d(TAG, String.format("CLOSE_WRITE: %s", path == null ? file.getPath() : path));
			else
				return;

			stopWatching();
			Message msg = handler.obtainMessage();
			msg.what = MSG_REFRESH;
			msg.obj = file;
			handler.sendMessage(msg);
		}
	}

	public static class WeldConditionItem {
		//		public static final int OUTPUT_DATA = 0;            // 출력 데이터
//		public static final int OUTPUT_TYPE = 1;            // 출력 타입
//		public static final int SQUEEZE_FORCE = 2;          // 가압력
//		public static final int MOVE_TIP_CLEARANCE = 3;     // 이동극 제거율
//		public static final int FIXED_TIP_CLEARANCE = 4;    // 고정극 제거율
//		public static final int PANEL_THICKNESS = 5;       // 패널 두께
//		public static final int COMMAND_OFFSET = 6;         // 명령 옵셋
		private static final String TAG = "WeldConditionItem";
		private List<String> rowList;
		private String rowString;

		private boolean itemChecked;

		public WeldConditionItem(String value) {
			rowList = new ArrayList<>();
			if (value != null) {
				setRowString(value);
				setString(value);
			}
			itemChecked = false;
		}

		public String get(int index) {
			String ret = null;
			try {
				ret = rowList.get(index);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return ret;
		}

		public String set(int index, String object) {
			return rowList.set(index, object);
		}

//		public Integer size() {
//			return rowList.size();
//		}

		public String getString() {
			if (rowList == null)
				return rowString;

			StringBuilder sb = new StringBuilder();
			try {
				Iterator iter = rowList.iterator();
				String outputData = (String) iter.next();
				if (Integer.parseInt(outputData) < 10)
					sb.append("\t- ").append(outputData).append("=").append(outputData);
				else
					sb.append("\t-").append(outputData).append("=").append(outputData);
				while (iter.hasNext()) {
					sb.append(",").append(iter.next());
				}
			} catch (Exception e) {
				e.printStackTrace();
				return rowString;
			}

			return sb.toString();
		}

		public void setString(String value) {
			try {
				String[] header = value.trim().split("=");

				if (header.length != 2)
					return;

				String[] data = header[1].trim().split(",");
				for (String item : data) {
					rowList.add(item.trim());
				}
			} catch (NullPointerException e) {
				Log.d(TAG, e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

//		public String getRowString() {
//			return rowString;
//		}

		public void setRowString(String rowString) {
			this.rowString = rowString;
		}

		public boolean isItemChecked() {
			return itemChecked;
		}

		public void setItemChecked(boolean itemChecked) {
			this.itemChecked = itemChecked;
		}
	}

	public static class WeldConditionLoader extends AsyncTaskLoader<List<WeldConditionItem>> {
		private static final String TAG = "WeldConditionLoader";

		WeldConditionFragment.OnWorkPathListener mCallBack;
		List<WeldConditionItem> mList;
		WeldConditionReceiver mReceiver;

		public WeldConditionLoader(Context context,
		                           WeldConditionFragment.OnWorkPathListener callBack) {
			super(context);
			mCallBack = callBack;
		}

		@Override
		public List<WeldConditionItem> loadInBackground() {
			Log.d(TAG, "loadInBackground");
			if (mCallBack == null)
				return null;
			String path = mCallBack.onGetWorkPath() + "/ROBOT.SWD";
			List<WeldConditionItem> list = new ArrayList<>();
			try {
				FileInputStream fileInputStream = new FileInputStream(path);
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String rowString;
				boolean addText = false;
				while ((rowString = bufferedReader.readLine()) != null) {
					if (rowString.startsWith("#006"))
						break;
					if (addText && rowString.trim().length() > 0)
						list.add(new WeldConditionItem(rowString));
					if (rowString.startsWith("#005"))
						addText = true;
				}
				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
			Log.d(TAG, "loadInBackground() list.size: " + list.size());
			return list;
		}

		@Override
		public void deliverResult(List<WeldConditionItem> data) {
			Log.d(TAG, "deliverResult");

			if (isReset()) {
				if (data != null)
					onReleaseResources(data);
			}

			List<WeldConditionItem> oldList = mList;
			mList = data;

			if (isStarted()) {
				super.deliverResult(data);
			}

			if (oldList != null)
				onReleaseResources(oldList);

			if (mList != null)
				Log.d(TAG, "deliverResult() mList.size: " + mList.size());
		}

		protected void onReleaseResources(List<WeldConditionItem> data) {
			// For a simple List<> there is nothing to do.  For something
			// like a Cursor, we would close it here.
			if (data != null) {
				Log.d(TAG, "dataSize: " + data.size());
			}
		}

		@Override
		protected void onStartLoading() {
			Log.d(TAG, "onStartLoading");

			if (mList != null) {
				deliverResult(mList);
			}

			if (mReceiver == null) {
				mReceiver = new WeldConditionReceiver(this);
			}

			if (takeContentChanged() || mList == null) {
				forceLoad();
			}
		}

		@Override
		protected void onStopLoading() {
			Log.d(TAG, "onStopLoading");
			cancelLoad();
		}

		@Override
		public void onCanceled(List<WeldConditionItem> data) {
			Log.d(TAG, "onCanceled");
			super.onCanceled(data);
			onReleaseResources(data);
		}

		@Override
		protected void onReset() {
			Log.d(TAG, "onReset");

			super.onReset();

			onStopLoading();

			if (mList != null) {
				onReleaseResources(mList);
				mList = null;
			}

			if (mReceiver != null) {
				getContext().unregisterReceiver(mReceiver);
				mReceiver = null;
			}
		}

		public static class WeldConditionReceiver extends BroadcastReceiver {
			public static final String WELD_CONDITION_LOAD = "com.changyoung.hi5controller.weld_condition_load";
			public static final String WELD_CONDITION_UPDATE = "com.changyoung.hi5controller.weld_condition_update";

			final WeldConditionLoader mLoader;

			public WeldConditionReceiver(WeldConditionLoader loader) {
				mLoader = loader;
				IntentFilter filter = new IntentFilter(WELD_CONDITION_LOAD);
				filter.addAction(WELD_CONDITION_UPDATE);
				mLoader.getContext().registerReceiver(this, filter);
			}

			@Override
			public void onReceive(Context context, Intent intent) {
				mLoader.onContentChanged();
			}
		}
	}

	public static class WeldConditionAdapter extends ArrayAdapter<WeldConditionItem> {
//		private static final String TAG = "WeldConditionAdapter";

		private Activity mContext;

		public WeldConditionAdapter(Activity context) {
			super(context, R.layout.list_item_weld_condition);
			mContext = context;
		}

		public void setData(List<WeldConditionItem> data) {
			clear();
			if (data != null) {
				addAll(data);
				notifyDataSetChanged();
			} else {
				notifyDataSetInvalidated();
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			View row = convertView;

			if (row == null) {
				row = mContext.getLayoutInflater().inflate(R.layout.list_item_weld_condition, parent, false);
				viewHolder = new ViewHolder(row);
				row.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) row.getTag();
			}

			WeldConditionItem item = getItem(position);
			row.setBackgroundColor(item.isItemChecked() ? ContextCompat.getColor(mContext, R.color.tab3_textview_background) : Color.TRANSPARENT);
			viewHolder.update(item);

			return row;
		}

		public String update(String path) {
			String ret;
			StringBuilder sb = new StringBuilder();
			File file = new File(path);

			try {
				FileInputStream fileInputStream = new FileInputStream(path);
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String rowString;
				boolean addText = true;
				boolean wciText = true;
				while ((rowString = bufferedReader.readLine()) != null) {
					if (!addText && wciText) {
						for (int i = 0; i < getCount(); i++) {
							sb.append(getItem(i).getString());
							sb.append("\n");
						}
						sb.append("\n");
						wciText = false;
					}
					if (rowString.startsWith("#006"))
						addText = true;
					if (addText) {
						sb.append(rowString);
						sb.append("\n");
					}
					if (rowString.startsWith("#005"))
						addText = false;
				}
				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();

				FileOutputStream fileOutputStream = new FileOutputStream(path);
				OutputStreamWriter outputStreamReader = new OutputStreamWriter(fileOutputStream, "EUC-KR");
				BufferedWriter bufferedWriter = new BufferedWriter(outputStreamReader);

				bufferedWriter.write(sb.toString());

				bufferedWriter.close();
				outputStreamReader.close();
				fileOutputStream.close();

				ret = "저장 완료: " + file.getName();
			} catch (FileNotFoundException e) {
				ret = "저장 실패" + file.getName();
			} catch (Exception e) {
				e.printStackTrace();
				ret = "저장 실패" + file.getName();
			}

			return ret;
		}

		public class ViewHolder {
			private List<TextView> tvList;

			public ViewHolder(View row) {
				tvList = new ArrayList<>();
				tvList.add((TextView) row.findViewById(R.id.tvOutputData));
				tvList.add((TextView) row.findViewById(R.id.tvOutputType));
				tvList.add((TextView) row.findViewById(R.id.tvSqueezeForce));
				tvList.add((TextView) row.findViewById(R.id.tvMoveTipClearance));
				tvList.add((TextView) row.findViewById(R.id.tvFixedTipClearance));
				tvList.add((TextView) row.findViewById(R.id.tvPanelThickness));
				tvList.add((TextView) row.findViewById(R.id.tvCommandOffset));
			}

			public void update(WeldConditionItem weldConditionItem) {
				for (int i = 0; i < tvList.size(); i++) {
					tvList.get(i).setText(weldConditionItem.get(i));
				}
			}
		}
	}

	private class LooperHandler extends Handler {
		public LooperHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MSG_REFRESH:
					Log.d(TAG, "MSG_REFRESH");
					refresh(true);
					break;
				default:
					break;
			}
		}
	}
}