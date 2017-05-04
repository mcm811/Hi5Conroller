package com.changyoung.hi5controller.weldcount;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.SeekBar;
import android.widget.TextView;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.TimeStringHelper;
import com.changyoung.hi5controller.common.UiHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WeldCountRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
	private final WeldCountFragment weldCountFragment;
	private final List<WeldCountJobFile> mDataset;
	private final Activity mActivity;
	private final View mSnackbarView;
	private Context mContext;

	WeldCountRecyclerViewAdapter(WeldCountFragment weldCountFragment, Activity activity, View snackbarView, List<WeldCountJobFile> dataSet) {
		this.weldCountFragment = weldCountFragment;
		mDataset = dataSet;
		mActivity = activity;
		mSnackbarView = snackbarView;
	}

	private void show(String msg) {
		try {
			if (msg != null) {
				Snackbar.make(mSnackbarView, msg, Snackbar.LENGTH_LONG)
						.setAction("Action", null)
						.show();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	boolean checkA() {
		WeldCountJobFileEditorRecyclerViewAdapter weldCountJobFileEditorRecyclerViewAdapter;
		for (WeldCountJobFile weldCountJobFile : mDataset) {
			if (weldCountJobFile.getTotal() > 0) {
				weldCountJobFileEditorRecyclerViewAdapter = new WeldCountJobFileEditorRecyclerViewAdapter(weldCountFragment, weldCountFragment.getActivity(), weldCountJobFile);
				if (weldCountJobFileEditorRecyclerViewAdapter.checkA()) {
					return true;
				}
			}
		}
		return false;
	}

	void updateZeroA() {
		weldCountFragment.observer.stopWatching();
		WeldCountJobFileEditorRecyclerViewAdapter weldCountJobFileEditorRecyclerViewAdapter;
		int updatedMoveCount = 0;
		int updatedFileCount = 0;
		int checkedFileCount = 0;
		for (WeldCountJobFile weldCountJobFile : mDataset) {
			if (weldCountJobFile.getTotal() > 0) {
				weldCountJobFileEditorRecyclerViewAdapter = new WeldCountJobFileEditorRecyclerViewAdapter(weldCountFragment, weldCountFragment.getActivity(), weldCountJobFile);
				int ret = weldCountJobFileEditorRecyclerViewAdapter.updateZeroA();
				if (ret > 0) {
					weldCountJobFileEditorRecyclerViewAdapter.saveFile();
					updatedMoveCount += ret;
					updatedFileCount++;
				}
				checkedFileCount++;
			}
		}
		if (updatedFileCount > 0) {
			notifyDataSetChanged();
			show("MOVE A=0: " + updatedFileCount + "개 파일, " + updatedMoveCount + "개 MOVE 수정");
		} else {
			show("MOVE A=0: " + checkedFileCount + "개 파일 확인");
		}
		weldCountFragment.observer.startWatching();
	}

	void setData(List<WeldCountJobFile> data, int orderType) {
		mDataset.clear();
		if (data != null) {
			//noinspection Java8ListSort
			Collections.sort(data, (obj1, obj2) -> {
				int ret = 0;
				if (obj1.isDirectory() && obj2.isDirectory())
					ret = obj1.getName().compareToIgnoreCase(obj2.getName());
				else if (obj1.isFile() && obj2.isFile())
					ret = obj1.getName().compareToIgnoreCase(obj2.getName());
				else if (obj1.isDirectory() && obj2.isFile())
					ret = -1;
				else if (obj1.isFile() && obj2.isDirectory()) {
					ret = 1;
				}
				return ret;
			});
			if (orderType == WeldCountFragment.ORDER_TYPE_DESCEND)
				Collections.reverse(data);
			mDataset.addAll(data);
		}
		notifyDataSetChanged();
	}

	void sortName(final int orderType) {
		//noinspection Java8ListSort
		Collections.sort(mDataset, (obj1, obj2) -> {
			int ret = 0;
			if (obj1.isDirectory() && obj2.isDirectory())
				ret = obj1.getName().compareToIgnoreCase(obj2.getName());
			else if (obj1.isFile() && obj2.isFile())
				ret = obj1.getName().compareToIgnoreCase(obj2.getName());
			else if (obj1.isDirectory() && obj2.isFile())
				ret = -1;
			else if (obj1.isFile() && obj2.isDirectory())
				ret = 1;
			return orderType == 0 ? ret : -ret;
		});
		notifyDataSetChanged();
	}

	private void showFileEditorDialog(final int position) {
		final WeldCountJobFile weldCountJobFile = weldCountFragment.mWeldCountRecyclerViewAdapter.getItem(position);
		if (weldCountJobFile.getTotal() == 0) {
			show("CN 항목이 없습니다");
			return;
		}

		@SuppressLint("InflateParams")
		View dialogView = LayoutInflater.from(mContext)
				.inflate(R.layout.weldcount_file_editor_dialog, null);
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
		dialogBuilder.setView(dialogView);

		RecyclerView mFileEditorDialogRecyclerView = (RecyclerView) dialogView.findViewById(R.id.recycler_view);
		mFileEditorDialogRecyclerView.setHasFixedSize(true);

		RecyclerView.LayoutManager layoutManager =
				new GridLayoutManager(weldCountFragment.getContext(), 4, LinearLayoutManager.VERTICAL, false);
		mFileEditorDialogRecyclerView.setLayoutManager(layoutManager);

		weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter = new WeldCountJobFileEditorRecyclerViewAdapter(weldCountFragment, weldCountFragment.getActivity(),
				weldCountJobFile);
		mFileEditorDialogRecyclerView.setAdapter(weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter);
		
		TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
		statusText.setText(String.format(Locale.KOREA, "계열 수정 (CN: %d개)",
				weldCountJobFile.getTotal()));

		final TextInputEditText etBeginNumber = (TextInputEditText) dialogView.findViewById(R.id.etBeginNumber);
		final SeekBar sbBeginNumber = (SeekBar) dialogView.findViewById(R.id.sampleSeekBar);
		etBeginNumber.setOnFocusChangeListener((v, hasFocus) -> {
			try {
				if (!hasFocus) {
					Integer beginNumber = Integer.parseInt(etBeginNumber.getText().toString());
					if (beginNumber > 255)
						beginNumber = 255;
					etBeginNumber.setText(String.valueOf(beginNumber));
					sbBeginNumber.setProgress(beginNumber - 1);
					weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.setBeginNumber(beginNumber);
				}
			} catch (NumberFormatException e) {
				WeldCountFragment.logD(e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		etBeginNumber.setOnKeyListener((v, keyCode, event) -> {
			UiHelper.hideSoftKeyboard(mActivity, v, event);
			return false;
		});

		final int etListSize = weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.getItemCount();
		sbBeginNumber.setMax(254);
		sbBeginNumber.setProgress(0);
		sbBeginNumber.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Integer beginNumber = sbBeginNumber.getProgress() + 1;
				etBeginNumber.setText(String.valueOf(beginNumber));
				if (etListSize < 30)
					weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.setBeginNumber(beginNumber);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				UiHelper.hideSoftKeyboard(mActivity, seekBar, null);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Integer beginNumber = sbBeginNumber.getProgress() + 1;
				etBeginNumber.setText(String.valueOf(beginNumber));
				if (etListSize >= 30)
					weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.setBeginNumber(beginNumber);
			}
		});

		dialogBuilder.setNegativeButton("취소", (dialog, which) -> {
			weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.reloadFile();
			weldCountFragment.mWeldCountRecyclerViewAdapter.notifyDataSetChanged();
		});

		dialogBuilder.setPositiveButton("저장", (dialog, which) -> {
			if (weldCountJobFile.getTotal() > 0) {
				weldCountFragment.observer.stopWatching();
				weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.saveFile();
				weldCountFragment.observer.startWatching();
				weldCountFragment.mWeldCountRecyclerViewAdapter.notifyDataSetChanged();
				show("저장 완료: " + weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.getName());
			}
		});

		dialogBuilder.setNeutralButton("복구(A=0)", (dialog, which) -> {
			if (weldCountJobFile.getTotal() > 0) {
				weldCountFragment.observer.stopWatching();
				weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.updateZeroA();       // A=0
				weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.saveFile();
				weldCountFragment.observer.startWatching();
				weldCountFragment.mWeldCountRecyclerViewAdapter.notifyDataSetChanged();
				show("저장 완료: " + weldCountFragment.mWeldCountJobFileEditorRecyclerViewAdapter.getName());
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

		final String ttsMsg = mContext.getString(R.string.tts_begin_number);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			weldCountFragment.mTts.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null, null);
		} else {
			//noinspection deprecation
			weldCountFragment.mTts.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null);
		}

		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, weldCountFragment.getActivity().getPackageName());
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
		intent.putExtra(RecognizerIntent.EXTRA_PROMPT, mContext.getString(R.string.tts_begin_number));

		weldCountFragment.mRecognizer = SpeechRecognizer.createSpeechRecognizer(weldCountFragment.getActivity());
		weldCountFragment.mRecognizer.setRecognitionListener(new RecognitionListener() {
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
//					mRecognizer.startListening(intent);
			}

			@Override
			public void onResults(Bundle results) {
				String key = SpeechRecognizer.RESULTS_RECOGNITION;
				ArrayList<String> list = results.getStringArrayList(key);
				int num = -1;
				if (list != null) {
					for (String item : list) {
						WeldCountFragment.logD("MicResult:" + item);
						try {
							num = Integer.parseInt(item);
							break;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				if (num != -1) {
					sbBeginNumber.setProgress(num - 1);
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					WeldCountFragment.logD("MicResult:Fail");
					weldCountFragment.mRecognizer.startListening(intent);
				}
			}

			@Override
			public void onPartialResults(Bundle partialResults) {

			}

			@Override
			public void onEvent(int eventType, Bundle params) {

			}
		});
		new Handler().postDelayed(() -> weldCountFragment.mRecognizer.startListening(intent), 1500);
	}

	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		mContext = parent.getContext();
		final View v = LayoutInflater.from(mContext)
				.inflate(R.layout.weldcount_view_holder_item, parent, false);
		final ViewHolder holder = new ViewHolder(v);
		holder.mItemView.setOnClickListener(v1 -> {
			final int position = (int) v1.getTag();
			final WeldCountJobFile weldCountJobFile = mDataset.get(position);
			if (weldCountJobFile.getTotal() == 0) {
				UiHelper.textViewActivity(mActivity,
						weldCountJobFile.getName(),
						weldCountJobFile.getRowText());
			} else {
				final float scale = 1.2f;
				AnimationSet animationSet = new AnimationSet(true);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					animationSet.addAnimation(UiHelper
							.getCenterTranslateAnimation(weldCountFragment.mView, holder.mItemView, scale));
				animationSet.addAnimation(new ScaleAnimation(1f, scale, 1f, scale,
						ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
						ScaleAnimation.RELATIVE_TO_SELF, 0.5f));
				animationSet.setDuration(200);
				animationSet.setInterpolator(new DecelerateInterpolator());
				animationSet.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {

					}

					@Override
					public void onAnimationEnd(Animation animation) {
						showFileEditorDialog(position);
					}

					@Override
					public void onAnimationRepeat(Animation animation) {

					}
				});
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					holder.mItemView.setElevation(holder.mItemView.getElevation() + 1f);
				holder.mItemView.startAnimation(animationSet);
			}
		});
		holder.mItemView.setOnLongClickListener(v12 -> {
//				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//				builder.setItems(R.array.dialog_items, (dialog, which) -> {
//					//String[] items = getResources().getStringArray(R.array.dialog_items);
//					if (which == 0) {
//						showFileEditorDialog((int) v12.getTag());
//					} else if (which == 1) {
//						final WeldCountJobFile weldCountJobFile = mDataset.get((int) v12.getTag());
//						UiHelper.textViewActivity(mActivity, weldCountJobFile.getName(),
//								weldCountJobFile.getRowText());
//					}
//				});
//				builder.create().show();

			final WeldCountJobFile weldCountJobFile = mDataset.get((int) v12.getTag());
			UiHelper.textViewActivity(mActivity, weldCountJobFile.getName(),
					weldCountJobFile.getRowText());

			return true;
		});
		return holder;
	}

	@Override
	public void onBindViewHolder(final RecyclerView.ViewHolder rh, final int position) {
		final ViewHolder holder = (ViewHolder) rh;
		final WeldCountJobFile jobFile = mDataset.get(position);
		holder.tvJobFileName.setText(jobFile.getName());
		holder.tvJobFileTime.setText(TimeStringHelper.getLasModified(jobFile));
		holder.tvJobFileSize.setText(String.format(Locale.KOREA, "%dB", jobFile.length()));

		final String infoString = jobFile.getInfo();
		if (infoString == null || infoString.isEmpty()) {
			holder.tvInfo.setVisibility(View.GONE);
		} else {
			holder.tvInfo.setVisibility(View.VISIBLE);
			holder.tvInfo.setText(infoString);
		}
		final String previewString = jobFile.getPreview();
		if (previewString == null || previewString.isEmpty()) {
			holder.tvPreview.setVisibility(View.GONE);
		} else {
			holder.tvPreview.setVisibility(View.VISIBLE);
			holder.tvPreview.setText(previewString);
		}
		final String cnString = jobFile.getCNList();
		if (cnString == null || cnString.isEmpty()) {
			holder.tvCN.setVisibility(View.GONE);
		} else {
			holder.tvCN.setVisibility(View.VISIBLE);
			holder.tvCN.setText(cnString);
		}
		final String moveListString = jobFile.getMoveList();
		if (moveListString == null || moveListString.isEmpty()) {
			holder.tvMove.setVisibility(View.GONE);
		} else {
			holder.tvMove.setVisibility(View.VISIBLE);
			holder.tvMove.setText(moveListString);
		}
		holder.mItemView.setTag(position);
	}

	@Override
	public int getItemCount() {
		return mDataset.size();
	}

	private WeldCountJobFile getItem(int position) {
		return mDataset.get(position);
	}

	private class ViewHolder extends RecyclerView.ViewHolder {
		final View mItemView;
		final TextView tvJobFileName;
		final TextView tvJobFileTime;
		final TextView tvJobFileSize;
		final TextView tvInfo;
		final TextView tvPreview;
		final TextView tvCN;
		final TextView tvMove;

		ViewHolder(View itemView) {
			super(itemView);
			mItemView = itemView;
			tvJobFileName = (TextView) itemView.findViewById(R.id.tvJobFileName);
			tvJobFileTime = (TextView) itemView.findViewById(R.id.tvJobFileTime);
			tvJobFileSize = (TextView) itemView.findViewById(R.id.tvJobFileSize);
			tvInfo = (TextView) itemView.findViewById(R.id.tvInfo);
			tvPreview = (TextView) itemView.findViewById(R.id.tvPreview);
			tvCN = (TextView) itemView.findViewById(R.id.tvCN);
			tvMove = (TextView) itemView.findViewById(R.id.tvMove);
		}
	}
}
