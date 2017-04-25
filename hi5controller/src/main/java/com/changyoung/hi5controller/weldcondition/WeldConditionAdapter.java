package com.changyoung.hi5controller.weldcondition;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.PrefHelper;
import com.changyoung.hi5controller.common.UiHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by changmin811@gmail.com on 2017. 4. 25
 */
class WeldConditionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	final List<WeldConditionItem> mDataset;
	final Activity mActivity;
	private final WeldConditionFragment weldConditionFragment;
	private final SparseBooleanArray mSelectedItems;
	private final Handler mHandler;
	Context mContext;

	WeldConditionAdapter(WeldConditionFragment weldConditionFragment,
	                     Activity activity,
	                     List<WeldConditionItem> dataSet) {
		this.weldConditionFragment = weldConditionFragment;
		mDataset = dataSet;
		mActivity = activity;
		mSelectedItems = new SparseBooleanArray();
		mHandler = new Handler();
	}

	private boolean toggleSelection(int position) {
		if (mSelectedItems.get(position, false)) {
			mSelectedItems.delete(position);
			return false;
		}
		mSelectedItems.put(position, true);
		return true;
	}

	void clearSelections() {
		mSelectedItems.clear();
		notifyDataSetChanged();
	}

	int getSelectedItemCount() {
		return mSelectedItems.size();
	}

	private ArrayList<Integer> getSelectedItems() {
		ArrayList<Integer> items = null;
		try {
			items = new ArrayList<>(mSelectedItems.size());
			for (int i = 0; i < mSelectedItems.size(); i++) {
				items.add(mSelectedItems.keyAt(i));
			}
		} catch (NullPointerException e) {
			WeldConditionFragment.logD(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return items;
	}

	void onLoadInstanceState(Bundle savedInstanceState) {
		try {
			if (savedInstanceState != null) {
				ArrayList<Integer> items =
						savedInstanceState.getIntegerArrayList("weld_condition_selected_items");
				if (items != null) {
					for (Integer item : items) {
						mSelectedItems.put(item, true);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void onSaveInstanceState(Bundle outState) {
		outState.putIntegerArrayList("weld_condition_selected_items", getSelectedItems());
	}

	public List<WeldConditionItem> getData() {
		return mDataset;
	}

	public void setData(List<WeldConditionItem> data) {
		mDataset.clear();
		if (data != null) {
			mDataset.addAll(data);
			notifyDataSetChanged();
		} else {
			mSelectedItems.clear();
		}
	}

	private WeldConditionItem getItem(int index) {
		return mDataset.get(index);
	}

	void update(String path) {
		WeldConditionFragment.logD("update:" + path);
		String ret;
		StringBuilder sb = new StringBuilder();
		weldConditionFragment.mWeldConditionObserver.stopWatching();
		try {
			InputStream inputStream = PrefHelper.getWorkPathInputStream(weldConditionFragment.getActivity(), path);
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "EUC-KR");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String rowString;
			boolean addText = true;
			boolean wciText = true;
			while ((rowString = bufferedReader.readLine()) != null) {
				if (!addText && wciText) {
					for (int i = 0; i < getItemCount(); i++) {
						sb.append(mDataset.get(i).getString());
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
			inputStream.close();

			OutputStream outputStream = PrefHelper.getWorkPathOutputStream(weldConditionFragment.getActivity(), path);
			OutputStreamWriter outputStreamReader = new OutputStreamWriter(outputStream, "EUC-KR");
			BufferedWriter bufferedWriter = new BufferedWriter(outputStreamReader);

			bufferedWriter.write(sb.toString());

			bufferedWriter.close();
			outputStreamReader.close();
			outputStream.close();

			ret = "저장 완료: " + path;
		} catch (FileNotFoundException e) {
			ret = "저장 실패" + path;
		} catch (Exception e) {
			e.printStackTrace();
			ret = "저장 실패" + path;
		}
		weldConditionFragment.mWeldConditionObserver.startWatching();
		WeldConditionFragment.logD(ret);
	}

	void showEditorDialog(int position) {
		final String dialog_title1 = weldConditionFragment.getContext()
				.getString(R.string.weldcondition_dialog_title1) + " ";
		final String dialog_title2 = weldConditionFragment.getContext()
				.getString(R.string.weldcondition_dialog_title2) + " ";

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

		final List<Integer> checkedPositions = weldConditionFragment.mWeldConditionAdapter.getSelectedItems();
		if (checkedPositions == null)
			return;
		if (checkedPositions.size() == 0)
			weldConditionFragment.mLastPosition = position;

		@SuppressLint("InflateParams")            final View dialogView = LayoutInflater.from(weldConditionFragment.getContext())
				.inflate(R.layout.weldcondition_editor_dialog, null);
		final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(weldConditionFragment.getContext());
		dialogBuilder.setView(dialogView);

		// Custom Title
		TextView textViewTitle = new TextView(weldConditionFragment.getContext());
		textViewTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
		textViewTitle.setTypeface(Typeface.DEFAULT_BOLD);
		textViewTitle.setPadding(20, 10, 20, 10);
		textViewTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
		textViewTitle.setText("용접 조건 수정");
		dialogBuilder.setCustomTitle(textViewTitle);

/*
		AdView adView = new AdView(getContext());
		adView.setAdSize(AdSize.BANNER);
		adView.setScaleX(0.95f);
		adView.setScaleY(0.95f);
		if (BuildConfig.DEBUG)
			adView.setAdUnitId(getContext().getString(R.string.banner_ad_unit_id_debug));
		else
			adView.setAdUnitId(getContext().getString(R.string.banner_ad_unit_id_release));
		AdRequest adRequest = new AdRequest.Builder()
				.setRequestAgent("android_studio:ad_template").build();
		adView.loadAd(adRequest);
		LinearLayout linearLayout = (LinearLayout) dialogView.findViewById(R.id.linearLayout1);
		linearLayout.addView(adView, linearLayout.getChildCount());
*/

		final List<TextInputLayout> tilList = new ArrayList<>();
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout1));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout2));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout3));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout4));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout5));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout6));
		tilList.add((TextInputLayout) dialogView.findViewById(R.id.textInputLayout7));

		for (int index = 0; index < tilList.size(); index++) {
			final TextInputLayout textInputLayout = tilList.get(index);
			final EditText editText = textInputLayout.getEditText();
			if (editText != null) {
				if (index == 0) {
					editText.setText(weldConditionFragment.mWeldConditionAdapter.getItem(weldConditionFragment.mLastPosition).get(index));
				} else {
					editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
					editText.setGravity(Gravity.CENTER);
					editText.setSelectAllOnFocus(true);
					editText.setSingleLine();
					try {
						textInputLayout.setTag(textInputLayout.getHint());
						textInputLayout.setHint(textInputLayout.getTag()
								+ "(" + weldConditionFragment.mWeldConditionAdapter.getItem(weldConditionFragment.mLastPosition).get(index) + ")");
					} catch (NullPointerException e) {
						WeldConditionFragment.logD(e.getLocalizedMessage());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				final int finalIndex = index;
				editText.setOnFocusChangeListener((v, hasFocus) -> {
					if (hasFocus) {
						final SeekBar sampleSeekBar =
								(SeekBar) dialogView.findViewById(R.id.sampleSeekBar);
						if (editText.getText().length() == 0) {
							editText.setText(weldConditionFragment.mWeldConditionAdapter
									.getItem(sampleSeekBar.getProgress()).get(finalIndex));
							editText.selectAll();
						}
					} else {
						try {
							// 임계치 처리 (1, 2번 정수, 3번부터 부동소수)
							if (finalIndex < 3) {
								Integer etNumber =
										Integer.parseInt(editText.getText().toString());
								if (etNumber > WeldConditionFragment.valueMax[finalIndex])
									etNumber = WeldConditionFragment.valueMax[finalIndex];
								editText.setText(String.format(Locale.KOREA, "%d", etNumber));
							} else {
								Float etNumber =
										Float.parseFloat(editText.getText().toString());
								if (etNumber > (float) WeldConditionFragment.valueMax[finalIndex])
									etNumber = (float) WeldConditionFragment.valueMax[finalIndex];
								editText.setText(String.format(Locale.KOREA, "%.1f", etNumber));
							}
						} catch (NumberFormatException e) {
							WeldConditionFragment.logD(e.getLocalizedMessage());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				editText.setOnEditorActionListener((v, actionId, event) -> {
					if (actionId == 6)
						UiHelper.hideSoftKeyboard(weldConditionFragment.getActivity(), v, event);
					Log.i("onEditorAction", "actionId: " + String.valueOf(actionId));
					return false;
				});
			}
		}

		final TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
		statusText.setText(weldConditionFragment.mWeldConditionAdapter.getItem(weldConditionFragment.mLastPosition).get(0));
		if (checkedPositions.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (Integer pos : checkedPositions) {
				sb.append(String.valueOf(pos + 1));
				sb.append(" ");
			}
			sb.insert(0, dialog_title1);
			statusText.setText(sb.toString().trim());
		} else {
			String buf = dialog_title1 + String.valueOf(weldConditionFragment.mLastPosition + 1);
			statusText.setText(buf);
		}

		final SeekBar sampleSeekBar = (SeekBar) dialogView.findViewById(R.id.sampleSeekBar);
		sampleSeekBar.setMax(weldConditionFragment.mWeldConditionAdapter.getItemCount() - 1);
		sampleSeekBar.setProgress(Integer.parseInt(weldConditionFragment.mWeldConditionAdapter.getItem(weldConditionFragment.mLastPosition).get(0)) - 1);
		sampleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					try {
						EditText editText0 = tilList.get(0).getEditText();
						if (editText0 != null)
							editText0.setText(weldConditionFragment.mWeldConditionAdapter.getItem(progress).get(0));
					} catch (NullPointerException e) {
						WeldConditionFragment.logD(e.getLocalizedMessage());
					} catch (Exception e) {
						e.printStackTrace();
					}
					for (int index = 1; index < tilList.size(); index++) {
						try {
							// 샘플바를 움직이면 힌트에 기존 값을 보여주도록 세팅한다
							tilList.get(index).setHint(tilList.get(index).getTag()
									+ "(" + weldConditionFragment.mWeldConditionAdapter.getItem(progress).get(index) + ")");
						} catch (NullPointerException e) {
							WeldConditionFragment.logD(e.getLocalizedMessage());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if (checkedPositions.size() == 0) {
						weldConditionFragment.mLastPosition = progress;
						statusText.setText(String.format(Locale.KOREA, "%s %d", dialog_title1, weldConditionFragment.mLastPosition + 1));
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
		beginSeekBar.setMax(weldConditionFragment.mWeldConditionAdapter.getItemCount() - 1);
		beginSeekBar.setProgress(0);

		// 선택 끝
		final SeekBar endSeekBar = (SeekBar) dialogView.findViewById(R.id.sbEnd);
		endSeekBar.setMax(weldConditionFragment.mWeldConditionAdapter.getItemCount() - 1);
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
					if (sb1Progress == 0 && sb2Progress == 0
							|| sb1Progress == beginSeekBar.getMax() && sb2Progress == endSeekBar.getMax()) {
						if (checkedPositions.size() > 0) {
							StringBuilder sb = new StringBuilder();
							for (int pos : checkedPositions) {
								sb.append(String.valueOf(pos + 1));
								sb.append(" ");
							}
							sb.insert(0, dialog_title1);
							statusText.setText(sb.toString().trim());
						} else {
							String buf = dialog_title1 + String.valueOf(weldConditionFragment.mLastPosition + 1);
							statusText.setText(buf);
						}
					} else {
						String buf = dialog_title2 + String.valueOf(sb1Progress + 1)
								+ " ~ " + String.valueOf(sb2Progress + 1);
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
					if (sb1Progress == 0 && sb2Progress == 0
							|| sb1Progress == beginSeekBar.getMax() && sb2Progress == endSeekBar.getMax()) {
						if (checkedPositions.size() > 0) {
							StringBuilder sb = new StringBuilder();
							for (int pos : checkedPositions) {
								sb.append(String.valueOf(pos + 1));
								sb.append(" ");
							}
							sb.insert(0, dialog_title1);
							statusText.setText(sb.toString().trim());
						} else {
							String buf = dialog_title1 + String.valueOf(weldConditionFragment.mLastPosition + 1);
							statusText.setText(buf);
						}
					} else {
						String buf = dialog_title2 + String.valueOf(sb1Progress + 1)
								+ " ~ " + String.valueOf(sb2Progress + 1);
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

		dialogBuilder.setNegativeButton("취소", (dialog, which) -> weldConditionFragment.setCheckedItem(true));

		dialogBuilder.setPositiveButton("저장", (dialog, which) -> {
			int seekBegin = beginSeekBar.getProgress() + 1;
			int seekEnd = endSeekBar.getMax() - endSeekBar.getProgress() + 1;
			boolean isSeek = !((seekBegin == 1 && seekEnd == 1)
					|| (seekBegin == beginSeekBar.getMax() + 1 && seekEnd == endSeekBar.getMax() + 1));
			boolean isUpdate = false;

			if (checkedPositions.size() == 0)
				checkedPositions.add(weldConditionFragment.mLastPosition);
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
							weldConditionFragment.mWeldConditionAdapter.getItem(rowNum).set(colNum, editText.getText().toString());
							isUpdate = true;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (isUpdate) {
				weldConditionFragment.mWeldConditionAdapter.update(weldConditionFragment.onGetWorkPath());
				weldConditionFragment.mWeldConditionAdapter.notifyDataSetChanged();
				weldConditionFragment.setImageFab();
			}
			weldConditionFragment.setCheckedItem(true);
		});

		AlertDialog alertDialog = dialogBuilder.create();
		try {
			Window window = alertDialog.getWindow();
			if (window != null) {
				window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
				window.getAttributes().windowAnimations = R.style.AlertDialogAnimation;
			}
			alertDialog.setCanceledOnTouchOutside(false);
			alertDialog.show();
		} catch (Exception e) {
			e.printStackTrace();
		}

		final String ttsMsg = weldConditionFragment.getContext().getString(R.string.tts_squeeze_force_value);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			weldConditionFragment.mTextToSpeech.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null, null);
		} else {
			//noinspection deprecation
			weldConditionFragment.mTextToSpeech.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null);
		}

		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, weldConditionFragment.getActivity().getPackageName());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, weldConditionFragment.getContext().getString(R.string.tts_squeeze_force_value));

		weldConditionFragment.mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(weldConditionFragment.getActivity());
		weldConditionFragment.mSpeechRecognizer.setRecognitionListener(new RecognitionListener() {
			@Override
			public void onReadyForSpeech(Bundle params) {

			}

			@Override
			public void onBeginningOfSpeech() {

			}

			@Override
			public void onRmsChanged(float rmsDB) {

			}

			@Override
			public void onBufferReceived(byte[] buffer) {

			}

			@Override
			public void onEndOfSpeech() {

			}

			@Override
			public void onError(int error) {
//					try {
//						Thread.sleep(5000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					logD("MicResult:Fail: " + error);
//					mSpeechRecognizer.startListening(intent);
			}

			@Override
			public void onResults(Bundle results) {
				String key = SpeechRecognizer.RESULTS_RECOGNITION;
				ArrayList<String> list = results.getStringArrayList(key);
				int num = -1;
				if (list != null) {
					for (String item : list) {
						WeldConditionFragment.logD("MicResult:" + item);
						try {
							num = Integer.parseInt(item);
							break;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				if (num != -1) {
					try {
						EditText editText = tilList.get(2).getEditText();
						if (editText != null)
							editText.setText(String.format(Locale.KOREA, "%d", num));
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					WeldConditionFragment.logD("MicResult:Fail");
					weldConditionFragment.mSpeechRecognizer.startListening(intent);
				}
			}

			@Override
			public void onPartialResults(Bundle partialResults) {

			}

			@Override
			public void onEvent(int eventType, Bundle params) {

			}
		});
		new Handler().postDelayed(() -> weldConditionFragment.mSpeechRecognizer.startListening(intent), 1500);
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
		mContext = parent.getContext();
		final View v = LayoutInflater.from(mContext)
				.inflate(R.layout.weldcondition_view_holder_item, parent, false);
		final ViewHolder holder = new ViewHolder(v);
		holder.mItemView.setOnClickListener(v14 -> {
			UiHelper.hideSoftKeyboard(mActivity, v14, null);
			final int position = (int) v14.getTag();
			if (toggleSelection(position))
				weldConditionFragment.mLastPosition = position;
			//noinspection ResourceAsColor
			holder.mItemView.setBackgroundColor(mSelectedItems.get(position, false)
					? ContextCompat.getColor(mContext, R.color.weldConditionSelectedItem)
					: Color.TRANSPARENT);
			weldConditionFragment.setCheckedItemSnackbar();
			weldConditionFragment.setImageFab();
		});
		holder.mItemView.setOnLongClickListener(v13 -> {
			UiHelper.hideSoftKeyboard(mActivity, v13, null);
			weldConditionFragment.mLastPosition = (int) v13.getTag();
			showEditorDialog(weldConditionFragment.mLastPosition);
			return true;
		});
		final EditText editText = (EditText) holder.tvList.get(WeldConditionItem.SQUEEZE_FORCE);
		editText.setOnFocusChangeListener((v12, hasFocus) -> {
			try {
				if (!hasFocus) {
					final EditText editText1 = (EditText) v12;
					final String editTextString = editText1.getText().toString();
					final WeldConditionItem item = (WeldConditionItem) editText1.getTag();
					if (editTextString.equals("")) {
						editText1.setText(item.get(WeldConditionItem.SQUEEZE_FORCE));
						return;
					}
					Integer squeezeForce = Integer.parseInt(editTextString);
					if (squeezeForce > WeldConditionFragment.valueMax[WeldConditionItem.SQUEEZE_FORCE])
						squeezeForce = WeldConditionFragment.valueMax[WeldConditionItem.SQUEEZE_FORCE];
					final String squeezeForceString = String.format(Locale.KOREA, "%d", squeezeForce);
					if (!squeezeForceString.equals(editText1.getText().toString()))
						editText1.setText(squeezeForceString);
					if (!item.get(WeldConditionItem.SQUEEZE_FORCE).equals(squeezeForceString)) {
						item.set(WeldConditionItem.SQUEEZE_FORCE, squeezeForceString);
						weldConditionFragment.mSaveFlag = true;
						weldConditionFragment.setImageFab();
					}
				} else {
					// 키보드가 나온후에 한줄 스크롤 하기 위해 0.2초의 딜레이 후 스크롤 한다
					mHandler.postDelayed(() -> {
						if (!weldConditionFragment.mLayoutManager.isSmoothScrolling()) {
							final int scrollPosition = holder.getLayoutPosition() + 1;
							if (scrollPosition != 0) {
//											Log.i("onFocusChange", String.format(Locale.KOREA, "scrollTo: %d", scrollPosition));
								weldConditionFragment.mRecyclerView.scrollToPosition(scrollPosition);
							}
						}
					}, 250);
				}
			} catch (NumberFormatException e) {
				Log.i("onFocusChange", e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		editText.setOnEditorActionListener((v1, actionId, event) -> {
//					Log.i("onEditorAction", "actionId: " + String.valueOf(actionId));
			if (actionId == 5) {
				final int scrollPosition = holder.getLayoutPosition() + 2;
//						Log.i("onEditorAction", String.format(Locale.KOREA, "scrollTo: %d", scrollPosition));
				weldConditionFragment.mRecyclerView.scrollToPosition(scrollPosition);
			}
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
		WeldConditionItem item = mDataset.get(position);
		//noinspection ResourceAsColor
		holder.mItemView.setBackgroundColor(mSelectedItems.get(position, false)
				? ContextCompat.getColor(mContext, R.color.weldConditionSelectedItem)
				: Color.TRANSPARENT);
		for (int i = 0; i < holder.tvList.size(); i++) {
			holder.tvList.get(i).setText(item.get(i));
		}
		holder.mItemView.setTag(position);
		holder.tvList.get(WeldConditionItem.SQUEEZE_FORCE).setTag(item);
		if (position < mDataset.size() - 1) {
			holder.tvList.get(WeldConditionItem.SQUEEZE_FORCE)
					.setImeOptions(EditorInfo.IME_ACTION_NEXT);
		} else {
			holder.tvList.get(WeldConditionItem.SQUEEZE_FORCE)
					.setImeOptions(EditorInfo.IME_ACTION_DONE);
		}
	}

	@Override
	public int getItemCount() {
		return mDataset.size();
	}

	private class ViewHolder extends RecyclerView.ViewHolder {
		final View mItemView;
		final List<TextView> tvList;

		ViewHolder(View itemView) {
			super(itemView);
			mItemView = itemView;
			tvList = new ArrayList<>();
			tvList.add((TextView) mItemView.findViewById(R.id.tvOutputData));
			tvList.add((TextView) mItemView.findViewById(R.id.tvOutputType));
			tvList.add((TextView) mItemView.findViewById(R.id.tvSqueezeForce));
			tvList.add((TextView) mItemView.findViewById(R.id.tvMoveTipClearance));
			tvList.add((TextView) mItemView.findViewById(R.id.tvFixedTipClearance));
			tvList.add((TextView) mItemView.findViewById(R.id.tvPanelThickness));
			tvList.add((TextView) mItemView.findViewById(R.id.tvCommandOffset));
		}
	}
}
