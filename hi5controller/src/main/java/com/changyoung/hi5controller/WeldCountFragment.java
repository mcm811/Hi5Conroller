package com.changyoung.hi5controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Created by changmin on 2015-10-13.
 * changmin811@gmail.com
 */
public class WeldCountFragment extends Fragment
		implements Refresh, LoaderManager.LoaderCallbacks<List<WeldCountFragment.WeldCountFile>> {

	private static final int MSG_REFRESH = 0;

	//	private static final String ARG_WORK_PATH = "workPath";
	private static final String TAG = "HI5:WeldCountFrag";

	private static final int ORDER_TYPE_ASCEND = 0;
	private static final int ORDER_TYPE_DESCEND = 1;

	private static final int LAYOUT_TYPE_LINEAR = 0;
	private static final int LAYOUT_TYPE_GRID = 1;
//	private static final int LAYOUT_TYPE_STAGGERRED = 2;

	private View mView;
	private RecyclerView mRecyclerView;
	private RecyclerView.LayoutManager mLinearLayoutManager;
	private RecyclerView.LayoutManager mGridLayoutManager;
	private WeldCountAdapter mWeldCountAdapter;

	private WeldCountFileEditorAdapter mWeldCountFileEditorAdapter;
	private FloatingActionButton mFabSort;
	private FloatingActionButton mFabUpdate;
	private FloatingActionButton mFabStorage;

	private LooperHandler looperHandler;
	private WeldCountObserver observer;
	private TextToSpeech mTts;
	private SpeechRecognizer mRecognizer;

	private int mOrderType = ORDER_TYPE_ASCEND;
	private int mLayoutType = LAYOUT_TYPE_LINEAR;

	//	private String mWorkPath;

	private OnWorkPathListener mListener;

	public WeldCountFragment() {
		// Required empty public constructor
	}

	private static void logD(String msg) {
		try {
			Log.i(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*
	public WeldCountFragment newInstance(String workPath) {
		WeldCountFragment fragment = new WeldCountFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		fragment.setArguments(args);
		return fragment;
	}
*/

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent resultDataIntent) {
		final int OPEN_DIRECTORY_REQUEST_CODE = 1000;
		logD("onActivityResult");
		switch (requestCode) {
			case OPEN_DIRECTORY_REQUEST_CODE:
				if (resultCode == Activity.RESULT_OK) {
					if (resultDataIntent != null) {
						Uri uri = resultDataIntent.getData();
						if (uri != null) {
							Activity activity = getActivity();

							final int rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
							activity.grantUriPermission(activity.getPackageName(), uri, rwFlags);

							final int takeFlags = resultDataIntent.getFlags() & rwFlags;
							activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);

							String path = Helper.UriHelper.getFullPathFromTreeUri(uri, activity);
							onSetWorkUri(uri.toString(), path);

							WeldFileListFragment workPathFragment = (WeldFileListFragment) getChildFragmentManager().findFragmentById(R.id.weldfile_fragment);
							if (workPathFragment != null)
								workPathFragment.refreshFilesList(path);
							show("경로 설정 완료: " + path);
						}
					}
				}
				break;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		logD("onCreate");
		super.onCreate(savedInstanceState);

		try {
			mListener = (OnWorkPathListener) getActivity();
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ClassCastException(getActivity().toString() + " must implement OnPathChangedListener");
		}

//		if (getArguments() != null) {
//			mWorkPath = getArguments().getString(ARG_WORK_PATH);
//		} else {
//			mWorkPath = onGetWorkPath();
//		}
		mLayoutType = Helper.Pref.getInt(getContext(), Helper.Pref.LAYOUT_TYPE_KEY, LAYOUT_TYPE_LINEAR);
		mOrderType = Helper.Pref.getInt(getContext(), Helper.Pref.ORDER_TYPE_KEY, ORDER_TYPE_ASCEND);
		logD("OrderType:" + mOrderType);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		switch (newConfig.orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				logD("Set Orientation: PORTRAIT LinearLayout");
				mRecyclerView.setLayoutManager(mLinearLayoutManager);
				mLayoutType = LAYOUT_TYPE_LINEAR;
				Helper.Pref.putInt(getContext(), Helper.Pref.LAYOUT_TYPE_KEY, mLayoutType);
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				logD("Set Orientation: GidLayout");
				mRecyclerView.setLayoutManager(mGridLayoutManager);
				mLayoutType = LAYOUT_TYPE_GRID;
				Helper.Pref.putInt(getContext(), Helper.Pref.LAYOUT_TYPE_KEY, mLayoutType);
				break;
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		logD("onCreateView");
		mView = inflater.inflate(R.layout.weldcount_fragment, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) mView.findViewById(R.id.srl);
		refresher.setOnRefreshListener(() -> {
			refresh(true);
			refresher.setRefreshing(false);
		});

		mFabStorage = (FloatingActionButton) mView.findViewById(R.id.fab_weldcount_storage);
		if (mFabStorage != null) {
			mFabStorage.setOnClickListener(v -> {
				logD("FabStorage");
				int OPEN_DIRECTORY_REQUEST_CODE = 1000;
				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
					int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
					flags |= Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
					flags |= Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
					intent.setFlags(flags);
					startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
					logD("FabStorage:OPEN_DIRECTORY_REQUEST_CODE");
				} else {
					show(refresh(R.id.nav_usbstorage));
				}
			});
		}

		mFabUpdate = (FloatingActionButton) mView.findViewById(R.id.fab_weldcount_update);
		if (mFabUpdate != null) {
			mFabUpdate.setOnClickListener(v -> {
				AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
				builder.setTitle("비정상 MOVE A=0 복구");
				builder.setIcon(R.drawable.ic_error_outline);
				builder.setMessage("문제 있는 모든 JOB 파일을 수정합니다.\n비정상 MOVE(SPOT 이전 라인)를 A=0로 복구 하시겠습니까?");
				builder.setPositiveButton("복구", (dialog, which) -> onUpdateA());
				builder.setNegativeButton("취소", (dialog, which) -> {
				});
				builder.show();
			});
		}

		mFabSort = (FloatingActionButton) mView.findViewById(R.id.fab_weldcount_sort);
		if (mFabSort != null) {
			mFabSort.setOnClickListener(new View.OnClickListener() {
				private void startOnClickAnimationFab() {
					final float fromDegree = (mOrderType == ORDER_TYPE_ASCEND) ? 180f : 0f;
					final float toDegree = (fromDegree + 180f) % 360f;
					final RotateAnimation animation = new RotateAnimation(fromDegree, toDegree,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					animation.setDuration(500);
					animation.setFillAfter(true);
					animation.setInterpolator(new AccelerateDecelerateInterpolator());
					mFabSort.startAnimation(animation);
					logD(String.format(Locale.KOREA, "FabSort from: %.0f, to: %.0f", fromDegree, toDegree));
				}

				private void startOnClickAnimationRecyclerView() {
					mRecyclerView.clearAnimation();
					AlphaAnimation animation = new AlphaAnimation(1.0f, 0.5f);
					animation.setDuration(400);
					animation.setInterpolator(new AccelerateInterpolator());
					animation.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {

						}

						@Override
						public void onAnimationEnd(Animation animation) {
							mOrderType = (mOrderType == ORDER_TYPE_ASCEND) ? ORDER_TYPE_DESCEND : ORDER_TYPE_ASCEND;
							Helper.Pref.putInt(getContext(), Helper.Pref.ORDER_TYPE_KEY, mOrderType);
							mWeldCountAdapter.sortName(mOrderType);

							AlphaAnimation expand = new AlphaAnimation(0.5f, 1.0f);
							expand.setDuration(100);
							expand.setInterpolator(new DecelerateInterpolator());
							mRecyclerView.startAnimation(expand);
							logD("OrderType:" + mOrderType);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {

						}
					});
					mRecyclerView.startAnimation(animation);
				}

				@Override
				public void onClick(View v) {
					startOnClickAnimationFab();
					startOnClickAnimationRecyclerView();
				}
			});
/*
			mFabSort.setOnLongClickListener(new View.OnLongClickListener() {
				private void startOnLongClickAnimationFab() {
					final float fromDegree = (mOrderType == ORDER_TYPE_ASCEND) ? 180f : 0f;
					final float toDegree = ((fromDegree + 180) % (360f * 4f)) * 2;
					final RotateAnimation animation = new RotateAnimation(fromDegree, toDegree,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					animation.setDuration(500);
					animation.setFillAfter(true);
					animation.setInterpolator(new AccelerateDecelerateInterpolator());
					mFabSort.startAnimation(animation);
					logD(String.format(Locale.KOREA, "LongClick_type:%d, from: %.0f, to: %.0f", mOrderType, fromDegree, toDegree));
				}

				private void startOnLongClickAnimationRecyclerView() {
					mRecyclerView.clearAnimation();
					AlphaAnimation animation = new AlphaAnimation(1.0f, 0.5f);
					animation.setDuration(400);
					animation.setInterpolator(new AccelerateInterpolator());
					animation.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {

						}

						@Override
						public void onAnimationEnd(Animation animation) {
							if (mLayoutManager instanceof GridLayoutManager) {
								mLayoutType = LAYOUT_TYPE_STAGGERRED;
								mLayoutManager = new StaggeredGridLayoutManager(2,
										StaggeredGridLayoutManager.VERTICAL);
							} else if (mLayoutManager instanceof StaggeredGridLayoutManager) {
								mLayoutType = LAYOUT_TYPE_LINEAR;
								mLayoutManager = new LinearLayoutManager(getContext());
							} else if (mLayoutManager instanceof LinearLayoutManager) {
								mLayoutType = LAYOUT_TYPE_GRID;
								mLayoutManager = new GridLayoutManager(getContext(), 2);
							}
							mRecyclerView.setLayoutManager(mLayoutManager);
							Helper.Pref.putInt(getContext(), Helper.Pref.LAYOUT_TYPE_KEY, mLayoutType);

							AlphaAnimation expand = new AlphaAnimation(0.5f, 1.0f);
							expand.setDuration(100);
							expand.setInterpolator(new DecelerateInterpolator());
							mRecyclerView.startAnimation(expand);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {

						}
					});
					mRecyclerView.startAnimation(animation);
				}

				@Override
				public boolean onLongClick(View v) {
					startOnLongClickAnimationFab();
					startOnLongClickAnimationRecyclerView();
					return true;
				}
			});
*/
		}

		mRecyclerView = (RecyclerView) mView.findViewById(R.id.recycler_view);
		if (mRecyclerView != null) {
			mRecyclerView.setHasFixedSize(true);
/*
			if (mLayoutType == LAYOUT_TYPE_LINEAR)
				mLayoutManager = new LinearLayoutManager(getContext());
			else if (mLayoutType == LAYOUT_TYPE_GRID)
				mLayoutManager = new GridLayoutManager(getContext(), 2);
			else if (mLayoutType == LAYOUT_TYPE_STAGGERRED)
				mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
*/
			final int orientation = getResources().getConfiguration().orientation;
			mLinearLayoutManager = new LinearLayoutManager(getContext());
			mGridLayoutManager = new GridLayoutManager(getContext(), 2);
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				mRecyclerView.setLayoutManager(mLinearLayoutManager);
			} else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mRecyclerView.setLayoutManager(mGridLayoutManager);
			}
			mWeldCountAdapter = new WeldCountAdapter(getActivity(), mFabSort, new ArrayList<>());
			mRecyclerView.setAdapter(mWeldCountAdapter);
		}

		looperHandler = new LooperHandler(Looper.getMainLooper());
		observer = new WeldCountObserver(onGetWorkPath(), looperHandler);
		observer.startWatching();

		try {
			mTts = new TextToSpeech(getContext(), status -> {
			});
//			mTts.setLanguage(Locale.KOREAN);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mTts.speak("우측하단 버튼을 눌러 작업경로를 설정하세요", TextToSpeech.QUEUE_FLUSH, null, null);
		} else {
			mTts.speak("우측하단 버튼을 눌러 작업경로를 설정하세요", TextToSpeech.QUEUE_FLUSH, null);
		}


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
		logD("onResume");
		super.onResume();
	}

	@Override
	public void onDetach() {
		logD("onDetach");
		super.onDetach();
		mListener = null;
		mWeldCountAdapter = null;
		mView = null;
		mRecyclerView = null;
		getLoaderManager().destroyLoader(0);
		if (mTts != null) {
			mTts.shutdown();
			mTts = null;
		}
		if (mRecognizer != null) {
			mRecognizer.stopListening();
			mRecognizer.destroy();
			mRecognizer = null;
		}
	}

	@Override
	public void refresh(boolean forced) {
		try {
			if (isAdded()) {
				logD("refresh:restartLoader: " + onGetWorkPath());
				getLoaderManager().restartLoader(0, null, this);
				observer = new WeldCountObserver(onGetWorkPath(), looperHandler);
				observer.startWatching();
			}
		} catch (IllegalStateException e) {
			logD(e.getLocalizedMessage());
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
		return null;
	}

	@Override
	public void show(String msg) {
		try {
			if (msg != null && isAdded()) {
				Snackbar.make(mFabSort, msg, Snackbar.LENGTH_SHORT)
						.setAction("Action", null)
						.show();
				logD(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*
	@Override
	public View getFab() {
		return mFabSort;
	}
*/

	private boolean onUpdateA() {
		if (mWeldCountAdapter != null) {
			mWeldCountAdapter.updateA("0");
			toggleFabUpdate();
			return true;
		}
		return false;
	}

	private void toggleFabUpdate() {
		if (mWeldCountAdapter != null) {
			if (mWeldCountAdapter.checkA())
				mFabUpdate.show();
			else
				mFabUpdate.hide();
		}
	}

//	private void onSetWorkPath(String path) {
//		if (mListener != null) {
//			mListener.onSetWorkPath(path);
//		}
//	}

	private String onGetWorkPath() {
		if (mListener != null) {
			return mListener.onGetWorkPath();
		}
		return null;
	}

	private void onSetWorkUri(String uri, String path) {
		if (mListener != null) {
			mListener.onSetWorkUri(uri, path);
		}
	}

//	private String onGetWorkUri() {
//		if (mListener != null) {
//			return mListener.onGetWorkUri();
//		}
//		return null;
//	}

	@Override
	public Loader<List<WeldCountFile>> onCreateLoader(int id, Bundle args) {
		logD(String.format(Locale.KOREA, "id:%d, onCreateLoader()", id));
		return new WeldCountLoader(getActivity(), mListener);
	}

	@Override
	public void onLoadFinished(Loader<List<WeldCountFile>> loader, List<WeldCountFile> data) {
		logD(String.format(Locale.KOREA, "onLoadFinished: id:%d, size:%d", loader.getId(), data.size()));
		mWeldCountAdapter.setData(data, mOrderType);
		if (mFabSort != null) {
			final float fromDegree = (mOrderType == ORDER_TYPE_ASCEND) ? 0f : 180f;
			final float toDegree = ((fromDegree + 180f) % 360f) + 360 * 2;
			final RotateAnimation animation = new RotateAnimation(fromDegree, toDegree,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(500);
			animation.setFillAfter(true);
			animation.setInterpolator(new AccelerateDecelerateInterpolator());
			mFabSort.startAnimation(animation);
			logD("OrderType:" + mOrderType);
			logD(String.format(Locale.KOREA, "LoadFinished: from: %.0f, to: %.0f", fromDegree, toDegree));
		}

		if (mFabStorage != null) {
			final float fromDegree = (mOrderType == ORDER_TYPE_ASCEND) ? 0f : 180f;
			final float toDegree = ((fromDegree + 180f) % 360f) + 360 * 2;
			final RotateAnimation animation = new RotateAnimation(toDegree, fromDegree,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(500);
			animation.setFillAfter(true);
			animation.setInterpolator(new AccelerateDecelerateInterpolator());
			mFabStorage.startAnimation(animation);
		}

		if (mRecyclerView != null)
			mRecyclerView.refreshDrawableState();
		if (mView != null) {
			if (mWeldCountAdapter.getItemCount() > 0) {
				mView.findViewById(R.id.imageView).setVisibility(View.GONE);
				mView.findViewById(R.id.textView).setVisibility(View.GONE);
			} else {
				mView.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
				mView.findViewById(R.id.textView).setVisibility(View.VISIBLE);
			}
		}

		toggleFabUpdate();
	}

	@Override
	public void onLoaderReset(Loader<List<WeldCountFile>> loader) {
		logD(String.format(Locale.KOREA, "id: %d, onLoaderReset()", loader.getId()));
		mWeldCountAdapter.setData(null, ORDER_TYPE_ASCEND);
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnWorkPathListener {
		String onGetWorkPath();

//		void onSetWorkPath(String path);

		String onGetWorkUri();

		void onSetWorkUri(String uri, String path);
	}

	@SuppressWarnings("unused")
	public static class WeldCountObserver extends FileObserver {
		static final String TAG = "HI5:WeldCountObserver";
		static final int mask = CREATE | DELETE | DELETE_SELF |
				MOVED_FROM | MOVED_TO | MOVE_SELF | CLOSE_WRITE;
		final File file;
		private final Handler handler;

		@SuppressWarnings("unused")
		public WeldCountObserver(File file, Handler handler) {
			super(file.getPath(), mask);
			this.file = file;
			this.handler = handler;
			logD("FILE_OBSERVER: " + file.getPath());
		}

		WeldCountObserver(String path, Handler handler) {
			super(path, mask);
			this.file = new File(path);
			this.handler = handler;
			logD("FILE_OBSERVER: " + path);
		}

		public void onEvent(int event, String path) {
			if ((event & CREATE) == CREATE)
				logD(String.format(Locale.KOREA, "CREATE: %s/%s", file.getPath(), path));
			else if ((event & DELETE) == DELETE)
				logD(String.format(Locale.KOREA, "DELETE: %s/%s", file.getPath(), path));
			else if ((event & DELETE_SELF) == DELETE_SELF)
				logD(String.format(Locale.KOREA, "DELETE_SELF: %s/%s", file.getPath(), path));
			else if ((event & MOVED_FROM) == MOVED_FROM)
				logD(String.format(Locale.KOREA, "MOVED_FROM: %s/%s", file.getPath(), path));
			else if ((event & MOVED_TO) == MOVED_TO)
				logD(String.format(Locale.KOREA, "MOVED_TO: %s", path == null ? file.getPath() : path));
			else if ((event & MOVE_SELF) == MOVE_SELF)
				logD(String.format(Locale.KOREA, "MOVE_SELF: %s", path == null ? file.getPath() : path));
			else if ((event & CLOSE_WRITE) == CLOSE_WRITE)
				logD(String.format(Locale.KOREA, "CLOSE_WRITE: %s", path == null ? file.getPath() : path));
			else
				return;

			stopWatching();
			Message msg = handler.obtainMessage();
			msg.what = MSG_REFRESH;
			msg.obj = file;
			handler.sendMessage(msg);
		}
	}

	@SuppressWarnings("unused")
	static class WeldCountFile extends File {
		static final int VALUE_MAX = 255;
		private static final String TAG = "HI5:WeldCountFile";
		private List<Job> jobList;
		private JobInfo jobInfo;

		WeldCountFile(String path) {
			super(path);
			readFile();
		}

		Job get(Integer index) {
			return jobList.get(index);
		}

//		public void set(Integer index, Job value) {
//			jobList.set(index, value);
//		}

		Integer size() {
			return jobList.size();
		}

		JobInfo getJobInfo() {
			return jobInfo;
		}

		void readFile() {
			jobList = readFile(getPath(), new ArrayList<>());
			jobInfo = createJobInfo(jobList, new JobInfo());
		}

		private List<Job> readFile(String fileName, List<Job> items) {
			try {
				FileInputStream fileInputStream = new FileInputStream(fileName);
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String rowString;
				Integer rowNumber = 0;
				while ((rowString = bufferedReader.readLine()) != null) {
					items.add(new Job(rowNumber++, rowString));
				}

				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();
			} catch (FileNotFoundException e) {
				logD(e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return items;
		}

		void saveFile(Activity activity) {
			jobInfo = saveFile(getPath(), jobList, activity);
		}

		private JobInfo
		saveFile(String fileName, List<Job> items, Activity activity) {
			try {
				logD("fileName:" + fileName);
				OutputStream outputStream = Helper.Pref.getWorkPathOutputStream(activity, fileName);

				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "EUC-KR");
				BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
				for (Job item : items) {
					bufferedWriter.write(item.getRowString());
					bufferedWriter.newLine();
				}
				bufferedWriter.close();
				outputStreamWriter.close();
				outputStream.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

			return createJobInfo(jobList, new JobInfo());
		}

		private JobInfo createJobInfo(List<Job> jobList, JobInfo jobInfo) {
			for (Job job : jobList) {
				String cn = job.getCN();
				if (cn != null) {
					jobInfo.setTotal(jobInfo.getTotal() + 1);
				}
				String gn = job.getGN();
				if (gn != null) {
					jobInfo.IncreaseGN(gn);
				}
				String g = job.getG();
				if (g != null) {
					jobInfo.IncreaseG(g);
				}
				if (job.getRowType() == Job.JOB_MOVE) {
					jobInfo.setStep(jobInfo.getStep() + 1);
				}
				if (job.getRowType() == Job.JOB_COMMENT) {
					if (jobInfo.getPreview() == null)
						jobInfo.setPreview(job.getRowString().trim());
				}
			}
			return jobInfo;
		}

		String getCNList() {
			StringBuilder sb = new StringBuilder();
			Integer n = 0;
			for (Job job : jobList) {
				String cn = job.getCN();
				if (cn != null) {
					if (++n > 500) {    // 500개 까지만 보여줌
						sb.append("...");
						break;
					}
					sb.append(cn).append(" ");
				}
			}
			if (sb.length() > 0)
				sb.insert(0, "CN: ");
			return sb.toString();
		}

//		public String getCNTest() {
//			StringBuilder sb = new StringBuilder();
//			for (Job job : jobList) {
//				String cn = job.getCN();
//				if (cn != null) {
//					sb.append(String.format(Locale.KOREA, "%d: CN=%s\n", job.getRowNumber(), cn));
//				}
//			}
//			return sb.toString();
//		}

		String getRowText() {
			StringBuilder sb = new StringBuilder();
			for (Job Job : jobList) {
				sb.append(Job.getRowString());
				sb.append("\n");
			}
			return sb.toString();
		}

		String getMoveList() {
			StringBuilder sb = new StringBuilder();
//			Integer n = 0;
			Job prevJob = null;
			for (Job job : jobList) {
				if (prevJob != null && job.isSpot()) {
					String mv = prevJob.getA();
					if (mv != null) {
//						if (mv.contains("A=0") && ++n < 100) {
//							sb.append(mv).append(" ");
//						} else {
//							sb.insert(0, " ").insert(0, mv);
//						}
						if (!mv.contains("A=0")) {
							sb.append(mv).append(" ");
						}
					}
				}
				prevJob = job;
			}
			if (sb.length() > 0)
				sb.insert(0, "MV: ");
			return sb.toString();
		}

		int updateA(String value) {
			int ret = 0;
			Job prevJob = null;
			for (Job job : jobList) {
				if (prevJob != null && job.isSpot()) {
					String mv = prevJob.getA();
					if (mv != null) {
						if (!mv.contains("A=0")) {
							if (prevJob.setA(value))
								ret++;
						}
					}
				}
				prevJob = job;
			}
			return ret;
		}

//		public void updateCN(Integer start) {
//			for (Job job : jobList) {
//				if (job.getRowType() == Job.JOB_SPOT) {
//					job.setCN((start++).toString());
//				}
//			}
//		}

//		public void updateCN(Integer index, String value) {
//			jobList.get(index).setCN(value);
//		}

		@SuppressWarnings("unused")
		class JobInfo {
			private static final String TAG = "HI5:JobInfo";
			private Integer total;         // SPOT 의 총 카운트
			private Integer step;          // S1 S2 S3 붙은 것들 젤 마지막 S번호 값
			private List<Integer> gnList;  // SPOT 단어 다음에 나오는 첫번째 단어 분석 해서 종류 결정(GN1, GN2, GN3, G1, G2)
			private List<Integer> gList;
			private String preview;

			JobInfo() {
				total = 0;
				step = 0;
				gnList = new ArrayList<>();
				gList = new ArrayList<>();
			}

			void IncreaseGN(String strIndex) {
				Integer index = Integer.parseInt(strIndex);
				if (gnList.size() < index) {
					for (Integer i = gnList.size(); i < index; i++) {
						gnList.add(0);
					}
				}
				gnList.set(index - 1, gnList.get(index - 1) + 1);
			}

			void IncreaseG(String strIndex) {
				Integer index = Integer.parseInt(strIndex);
				if (gList.size() < index) {
					for (Integer i = gList.size(); i < index; i++) {
						gList.add(0);
					}
				}
				gList.set(index - 1, gList.get(index - 1) + 1);
			}

			Integer getTotal() {
				return total;
			}

			void setTotal(Integer value) {
				total = value;
			}

			Integer getStep() {
				return step;
			}

			void setStep(Integer value) {
				step = value;
			}

			String getPreview() {
				return preview;
			}

			void setPreview(String value) {
				preview = value;
			}

			public String getString() {
				StringBuilder sb = new StringBuilder();

				if (total > 0)
					sb.append("Total: ").append(total.toString());

				Integer n = 1;
				for (Integer item : gnList) {
					sb.append(",  GN").append((n++).toString()).append(": ").append(item.toString());
				}

				n = 1;
				for (Integer item : gList) {
					sb.append(",  G").append((n++).toString()).append(": ").append(item.toString());
				}

				if (step > 0) {
					if (total > 0)
						sb.append(",  ");
					sb.append("Step: ").append(step.toString());
				}

				return sb.toString();
			}
		}

		class Job {
			static final int JOB_COMMENT = 1;
			static final int JOB_SPOT = 2;
			static final int JOB_MOVE = 3;
			static final int JOB_WAIT = 4;
			static final int JOB_DO = 5;
			static final int JOB_HEADER = 0;
			static final int JOB_CALL = 6;
			static final int JOB_END = 7;
			static final int JOB_ETC = 8;
			RowJob row;

			Job(Integer rowNumber, String rowString) {
				Integer rowType = getRowType(rowString);

				switch (rowType) {
					case JOB_HEADER:
						row = new HeaderJob(rowNumber, rowString);
						break;
					case JOB_COMMENT:
						row = new CommentJob(rowNumber, rowString);
						break;
					case JOB_SPOT:
						row = new SpotJob(rowNumber, rowString);
						break;
					case JOB_MOVE:
						row = new MoveJob(rowNumber, rowString);
						break;
					case JOB_WAIT:
						row = new WaitJob(rowNumber, rowString);
						break;
					case JOB_DO:
						row = new DoJob(rowNumber, rowString);
						break;
					case JOB_CALL:
						row = new CallJob(rowNumber, rowString);
						break;
					case JOB_END:
						row = new EndJob(rowNumber, rowString);
						break;
					case JOB_ETC:
						row = new EtcJob(rowNumber, rowString);
						break;
				}
			}

			private Integer getRowType(String rowString) {
				Integer rowType = JOB_ETC;
				String[] s = rowString.trim().split(" ");
				if (s.length > 0) {
					if (s[0].equals("Program"))
						rowType = JOB_HEADER;
					else if (s[0].startsWith("'"))
						rowType = JOB_COMMENT;
					else if (s[0].startsWith("SPOT"))
						rowType = JOB_SPOT;
					else if (s[0].startsWith("S"))
						rowType = JOB_MOVE;
					else if (s[0].startsWith("WAIT"))
						rowType = JOB_WAIT;
					else if (s[0].startsWith("DO"))
						rowType = JOB_DO;
					else if (s[0].startsWith("END"))
						rowType = JOB_END;
				}
				return rowType;
			}

			boolean isSpot() {
				return getRowType() == JOB_SPOT;
			}

			String getCN() {
				if (getRowType() == JOB_SPOT)
					return ((SpotJob) row).getCN();
				else
					return null;
			}

			void setCN(String value) {
				if (getRowType() == JOB_SPOT)
					((SpotJob) row).setCN(value);
			}

			String getGN() {
				if (getRowType() == JOB_SPOT)
					return ((SpotJob) row).getGN();
				else
					return null;
			}

			String getG() {
				if (getRowType() == JOB_SPOT)
					return ((SpotJob) row).getG();
				else
					return null;
			}

			public String getA() {
				if (getRowType() == JOB_MOVE)
					return ((MoveJob) row).getA();
				else
					return null;
			}

			public boolean setA(String value) {
				if (getRowType() == JOB_MOVE)
					return ((MoveJob) row).setA(value);
				else
					return false;
			}

			Integer getRowType() {
				return row.getRowType();
			}

			Integer getRowNumber() {
				return row.getRowNumber();
			}

			String getRowString() {
				return row.getRowString();
			}

			@SuppressWarnings("unused")
			public class JobValue {
				private String mType;
				private String mValue;

				JobValue(String str) {
					setUpdate(str);
				}

				String getValue() {
					return mValue;
				}

				void setValue(String value) {
					this.mValue = value;
				}

				public String getType() {
					return mType;
				}

				public void setType(String type) {
					this.mType = type;
				}

				boolean equalType(String s) {
					return !(mType == null || s == null) && mType.equals(s);
				}

				String getUpdate() {
					return mType == null || mType.isEmpty() ? mValue : mType + "=" + mValue;
				}

				void setUpdate(String value) {
					if (value != null) {
						String[] s = value.trim().split("=");
						if (s.length == 2) {
							mType = s[0];
							mValue = s[1];
						} else {
							mType = "";
							mValue = value;
						}
					}
				}
			}

			@SuppressWarnings("unused")
			public class RowJob {
				private Integer mRowType;
				private Integer mRowNumber;
				private String mRowString;

				RowJob(Integer rowType, Integer rowNumber, String rowString) {
					mRowType = rowType;
					mRowNumber = rowNumber;
					mRowString = rowString;
				}

				Integer getRowType() {
					return mRowType;
				}

				public void setRowType(Integer value) {
					mRowType = value;
				}

				Integer getRowNumber() {
					return mRowNumber;
				}

				String getRowString() {
					return mRowString;
				}

				void setRowString(String rowString) {
					this.mRowString = rowString;
				}
			}

			class HeaderJob extends RowJob {
//				String version;
//				String mechType;
//				String totalAxis;
//				String auxAxis;

				HeaderJob(Integer rowNumber, String rowString) {
					super(JOB_HEADER, rowNumber, rowString);
				}
			}

			class CommentJob extends RowJob {
				CommentJob(Integer rowNumber, String rowString) {
					super(JOB_COMMENT, rowNumber, rowString);
				}
			}

			class SpotJob extends RowJob {
				final List<JobValue> mJobValueList;
				StringBuilder mComment;

				SpotJob(Integer rowNumber, String rowString) {
					super(JOB_SPOT, rowNumber, rowString);
					mJobValueList = new ArrayList<>();

					// split commands, comments
					String rs = rowString;
					try {
						String[] cs = rs.trim().split("'");
						if (cs.length == 2) {
							rs = cs[0];
							mComment = new StringBuilder("'");
							mComment.append(cs[1]);
						} else if (cs.length > 2) {
							rs = cs[0];
							mComment = new StringBuilder("'");
							mComment.append(cs[1]);
							for (int i = 2; i < cs.length; i++) {
								if (!cs[1].trim().equals(cs[i].trim())) {
									mComment.append("'");
									mComment.append(cs[i]);
								}
							}
						}
//						if (mComment != null) logD("mComment:" + mComment);
					} catch (Exception e) {
						e.printStackTrace();
					}
//					logD("rs:" + rs);

					String[] s = rs.trim().split(" +");
					if (s.length == 2) {
						String[] f = s[1].split(",");
						for (String aF : f) {
							mJobValueList.add(new JobValue(aF));
						}
					}
				}

				void Update() {
					StringBuilder rs = new StringBuilder("     SPOT ");
					for (JobValue jv : mJobValueList) {
						rs.append(jv.getUpdate());
						rs.append(",");
					}
					int n = rs.lastIndexOf(",");
					if (n != -1)
						rs.deleteCharAt(n);
					if (mComment != null && mComment.length() > 0) {
						rs.append(" ");
						rs.append(mComment);
					}
					setRowString(rs.toString());
					logD("UPDATE:" + rs);
				}

				String getCN() {
					for (JobValue s : mJobValueList) {
						if (s.equalType("CN"))
							return s.getValue();
					}
					return null;
				}

				void setCN(String value) {
					for (JobValue s : mJobValueList) {
						if (s.equalType("CN")) {
							s.setValue(value);
							Update();
						}
					}
				}

				String getGN() {
					for (JobValue s : mJobValueList) {
						if (s.equalType("GN"))
							return s.getValue();
					}
					return null;
				}

				String getG() {
					for (JobValue s : mJobValueList) {
						if (s.equalType("G"))
							return s.getValue();
					}
					return null;
				}
			}

			@SuppressWarnings("unused")
			public class MoveJob extends RowJob {
				final List<JobValue> mJobValueList;
				StringBuilder mComment;
				String mStep;
				String mParam;

				MoveJob(Integer rowNumber, String rowString) {
					super(JOB_MOVE, rowNumber, rowString);
					mJobValueList = new ArrayList<>();

					// split commands, comments
					String rs = rowString;
					try {
						String[] cs = rs.trim().split("'");
						if (cs.length == 2) {
							rs = cs[0];
							mComment = new StringBuilder("'" + cs[1]);
						} else if (cs.length > 2) {
							rs = cs[0];
							mComment = new StringBuilder("'" + cs[1]);
							for (int i = 2; i < cs.length; i++) {
								if (!cs[1].trim().equals(cs[i].trim())) {
									mComment.append("'");
									mComment.append(cs[i]);
								}
							}
						}
//						if (mComment != null) logD("mComment:" + mComment);
					} catch (Exception e) {
						e.printStackTrace();
					}
//					logD("rs:" + rs);

					String[] s = rs.trim().split(" +");
//					logD("s.Length:" + s.length);
//					for (String ds : s) {
//						logD("s[" + ds + "]");
//					}
					if (s.length == 4) {
						mStep = s[0].substring(1);
						//logD("step:" + mStep + ";");
						String[] f = s[2].split(",");
						for (String aF : f) {
							mJobValueList.add(new JobValue(aF));
						}
						mParam = s[3].trim();
					}
				}

				public void Update() {
					StringBuilder rs = new StringBuilder("S" + getStep() + (getStep().length() == 1 ? "   " : "  ") + "MOVE ");
					for (JobValue jv : mJobValueList) {
						rs.append(jv.getUpdate());
						rs.append(",");
					}
					int n = rs.lastIndexOf(",");
					if (n != -1)
						rs.deleteCharAt(n);
					if (mParam != null && !mParam.isEmpty()) {
						rs.append("  ");
						rs.append(mParam);
					}
					if (mComment != null && mComment.length() > 0) {
						rs.append(" ");
						rs.append(mComment);
					}
					setRowString(rs.toString());
					logD("UPDATE:" + rs);
				}

				public String getA() {
					for (JobValue s : mJobValueList) {
						if (s.equalType("A")) {
							//logD("value[" + s.getValue() + "] " + getStep());
							//if (!s.getValue().equals("0"))
							return "S" + getStep() + ":A=" + s.getValue();
						}
					}
					return null;
				}

				public boolean setA(String value) {
					boolean ret = false;
					for (JobValue s : mJobValueList) {
						if (s.equalType("A")) {
							logD("Step:" + getStep() + " value:" + s.getValue());
							s.setValue(value);
							logD("Step:" + getStep() + " value:" + s.getValue());
							Update();
							ret = true;
						}
					}
					return ret;
				}

				String getStep() {
					return mStep;
				}
			}

			class WaitJob extends RowJob {
				WaitJob(Integer rowNumber, String rowString) {
					super(JOB_WAIT, rowNumber, rowString);
				}
			}

			class DoJob extends RowJob {
				DoJob(Integer rowNumber, String rowString) {
					super(JOB_DO, rowNumber, rowString);
				}
			}

			class CallJob extends RowJob {
				CallJob(Integer rowNumber, String rowString) {
					super(JOB_CALL, rowNumber, rowString);
				}
			}

			class EndJob extends RowJob {
				EndJob(Integer rowNumber, String rowString) {
					super(JOB_END, rowNumber, rowString);
				}
			}

			class EtcJob extends RowJob {
				EtcJob(Integer rowNumber, String rowString) {
					super(JOB_ETC, rowNumber, rowString);
				}
			}
		}
	}

	public static class WeldCountLoader extends AsyncTaskLoader<List<WeldCountFile>> {
		private static final String TAG = "HI5:WeldCountLoader";

		final WeldCountFragment.OnWorkPathListener mCallBack;
		List<WeldCountFile> mList;
		WeldCountReceiver mReceiver;

		WeldCountLoader(Context context,
		                WeldCountFragment.OnWorkPathListener callBack) {
			super(context);
			mCallBack = callBack;
		}

		@Override
		public List<WeldCountFile> loadInBackground() {
			logD("loadInBackground");
			if (mCallBack == null)
				return null;
			String path = mCallBack.onGetWorkPath();
			List<WeldCountFile> list = new ArrayList<>();
			try {
				File dir = new File(path);
				File[] dirs = dir.listFiles();
				if (dirs != null) {
					for (File file : dirs) {
						if (file.getName().toUpperCase().endsWith(".JOB")
								|| file.getName().toUpperCase().startsWith("HX"))
							list.add(new WeldCountFile(file.getPath()));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			logD("loadInBackground() list.size: " + list.size());
			return list;
		}

		@Override
		public void deliverResult(List<WeldCountFile> data) {
			logD("deliverResult");

			if (isReset()) {
				if (data != null)
					onReleaseResources(data);
			}

			List<WeldCountFile> oldList = mList;
			mList = data;

			if (isStarted()) {
				super.deliverResult(data);
			}

			if (oldList != null)
				onReleaseResources(oldList);
			if (mList != null)
				logD("deliverResult() mList.size: " + mList.size());
		}

		void onReleaseResources(List<WeldCountFile> data) {
			// For a simple List<> there is nothing to do.  For something
			// like a Cursor, we would close it here.
			if (data != null)
				logD("dataSize: " + data.size());
		}

		@Override
		protected void onStartLoading() {
			logD("onStartLoading");

			if (mList != null) {
				deliverResult(mList);
			}

			if (mReceiver == null) {
				mReceiver = new WeldCountReceiver(this);
			}

			if (takeContentChanged() || mList == null) {
				forceLoad();
			}
		}

		@Override
		protected void onStopLoading() {
			logD("onStopLoading");
			cancelLoad();
		}

		@Override
		public void onCanceled(List<WeldCountFile> data) {
			logD("onCanceled");
			super.onCanceled(data);
			onReleaseResources(data);
		}

		@Override
		protected void onReset() {
			logD("onReset");

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

		public static class WeldCountReceiver extends BroadcastReceiver {
			public static final String WELD_COUNT_LOAD = "com.changyoung.hi5controller.weld_count_load";
			public static final String WELD_COUNT_UPDATE = "com.changyoung.hi5controller.weld_count_update";

			final WeldCountLoader mLoader;

			public WeldCountReceiver(WeldCountLoader loader) {
				this.mLoader = loader;
				IntentFilter filter = new IntentFilter(WELD_COUNT_LOAD);
				filter.addAction(WELD_COUNT_UPDATE);
				mLoader.getContext().registerReceiver(this, filter);
			}

			@Override
			public void onReceive(Context context, Intent intent) {
				mLoader.onContentChanged();
			}
		}
	}

	public class WeldCountAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		final List<WeldCountFile> mDataset;
		final Activity mActivity;
		final View mSnackbarView;
		Context mContext;

		WeldCountAdapter(Activity activity, View snackbarView, List<WeldCountFile> dataSet) {
			mDataset = dataSet;
			mActivity = activity;
			mSnackbarView = snackbarView;
		}

		void show(String msg) {
			try {
				if (msg != null) {
					Snackbar.make(mSnackbarView, msg, Snackbar.LENGTH_SHORT)
							.setAction("Action", null)
							.show();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		boolean checkA() {
			WeldCountFileEditorAdapter weldCountFileEditorAdapter;
			for (WeldCountFile weldCountFile : mDataset) {
				if (weldCountFile.getJobInfo().getTotal() > 0) {
					weldCountFileEditorAdapter = new WeldCountFileEditorAdapter(getActivity(), mSnackbarView, weldCountFile);
					if (weldCountFileEditorAdapter.checkA()) {
						return true;
					}
				}
			}
			return false;
		}

		void updateA(String value) {
			observer.stopWatching();
			WeldCountFileEditorAdapter weldCountFileEditorAdapter;
			int updatedMoveCount = 0;
			int updatedFileCount = 0;
			int checkedFileCount = 0;
			for (WeldCountFile weldCountFile : mDataset) {
				if (weldCountFile.getJobInfo().getTotal() > 0) {
					weldCountFileEditorAdapter = new WeldCountFileEditorAdapter(getActivity(), mSnackbarView, weldCountFile);
					int ret = weldCountFileEditorAdapter.updateA(value);
					if (ret > 0) {
						weldCountFileEditorAdapter.saveFile();
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
			observer.startWatching();
		}

		void setData(List<WeldCountFile> data, int orderType) {
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
				if (orderType == ORDER_TYPE_DESCEND)
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
			final WeldCountFile weldCountFile = mWeldCountAdapter.getItem(position);
			if (weldCountFile.getJobInfo().getTotal() == 0) {
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
					new GridLayoutManager(getContext(), 4, LinearLayoutManager.VERTICAL, false);
			mFileEditorDialogRecyclerView.setLayoutManager(layoutManager);

			mWeldCountFileEditorAdapter = new WeldCountFileEditorAdapter(getActivity(),
					mSnackbarView, weldCountFile);
			mFileEditorDialogRecyclerView.setAdapter(mWeldCountFileEditorAdapter);

/*
			// 배너 광고
			AdView adView = new AdView(mContext);
			adView.setAdSize(AdSize.BANNER);
			adView.setScaleX(0.85f);
			adView.setScaleY(0.85f);
			if (BuildConfig.DEBUG)
				adView.setAdUnitId(mContext.getString(R.string.banner_ad_unit_id_debug));
			else
				adView.setAdUnitId(mContext.getString(R.string.banner_ad_unit_id_release));
			AdRequest adRequest = new AdRequest.Builder()
					.setRequestAgent("android_studio:ad_template").build();
			adView.loadAd(adRequest);
			LinearLayout linearLayoutWeldCount = (LinearLayout)
					dialogView.findViewById(R.id.linearLayout_WeldCount);
			linearLayoutWeldCount.addView(adView);
*/

			TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
			statusText.setText(String.format(Locale.KOREA, "계열 수정 (CN: %d개)",
					weldCountFile.getJobInfo().getTotal()));

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
						mWeldCountFileEditorAdapter.setBeginNumber(beginNumber);
					}
				} catch (NumberFormatException e) {
					logD(e.getLocalizedMessage());
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			etBeginNumber.setOnKeyListener((v, keyCode, event) -> {
				Helper.UiHelper.hideSoftKeyboard(mActivity, v, event);
				return false;
			});

			final int etListSize = mWeldCountFileEditorAdapter.getItemCount();
			sbBeginNumber.setMax(254);
			sbBeginNumber.setProgress(0);
			sbBeginNumber.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					Integer beginNumber = sbBeginNumber.getProgress() + 1;
					etBeginNumber.setText(String.valueOf(beginNumber));
					if (etListSize < 30)
						mWeldCountFileEditorAdapter.setBeginNumber(beginNumber);
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					Helper.UiHelper.hideSoftKeyboard(mActivity, seekBar, null);
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					Integer beginNumber = sbBeginNumber.getProgress() + 1;
					etBeginNumber.setText(String.valueOf(beginNumber));
					if (etListSize >= 30)
						mWeldCountFileEditorAdapter.setBeginNumber(beginNumber);
				}
			});

			dialogBuilder.setNegativeButton("취소", (dialog, which) -> {
				mWeldCountFileEditorAdapter.reloadFile();
				mWeldCountAdapter.notifyDataSetChanged();
			});

			dialogBuilder.setPositiveButton("저장", (dialog, which) -> {
				if (weldCountFile.getJobInfo().getTotal() > 0) {
					observer.stopWatching();
					mWeldCountFileEditorAdapter.saveFile();
					observer.startWatching();
					mWeldCountAdapter.notifyDataSetChanged();
					show("저장 완료: " + mWeldCountFileEditorAdapter.getName());
				}
			});

			dialogBuilder.setNeutralButton("복구(A=0)", (dialog, which) -> {
				if (weldCountFile.getJobInfo().getTotal() > 0) {
					observer.stopWatching();
					mWeldCountFileEditorAdapter.updateA("0");       // A=0
					mWeldCountFileEditorAdapter.saveFile();
					observer.startWatching();
					mWeldCountAdapter.notifyDataSetChanged();
					show("저장 완료: " + mWeldCountFileEditorAdapter.getName());
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
				mTts.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null, null);
			} else {
				//noinspection deprecation
				mTts.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null);
			}

			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getActivity().getPackageName());
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT, mContext.getString(R.string.tts_begin_number));

			mRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
			mRecognizer.setRecognitionListener(new RecognitionListener() {
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
							logD("MicResult:" + item);
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
						logD("MicResult:Fail");
						mRecognizer.startListening(intent);
					}
				}

				@Override
				public void onPartialResults(Bundle partialResults) {

				}

				@Override
				public void onEvent(int eventType, Bundle params) {

				}
			});
			new Handler().postDelayed(() -> mRecognizer.startListening(intent), 1500);
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			mContext = parent.getContext();
			final View v = LayoutInflater.from(mContext)
					.inflate(R.layout.weldcount_view_holder_item, parent, false);
			final ViewHolder holder = new ViewHolder(v);
			holder.mItemView.setOnClickListener(v1 -> {
				final int position = (int) v1.getTag();
				final WeldCountFile weldCountFile = mDataset.get(position);
				if (weldCountFile.getJobInfo().getTotal() == 0) {
					Helper.UiHelper.textViewActivity(mActivity,
							weldCountFile.getName(),
							weldCountFile.getRowText());
				} else {
					final float scale = 1.2f;
					AnimationSet animationSet = new AnimationSet(true);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
						animationSet.addAnimation(Helper.UiHelper
								.getCenterTranslateAnimation(mView, holder.mItemView, scale));
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
//						final WeldCountFile weldCountFile = mDataset.get((int) v12.getTag());
//						Helper.UiHelper.textViewActivity(mActivity, weldCountFile.getName(),
//								weldCountFile.getRowText());
//					}
//				});
//				builder.create().show();

				final WeldCountFile weldCountFile = mDataset.get((int) v12.getTag());
				Helper.UiHelper.textViewActivity(mActivity, weldCountFile.getName(),
						weldCountFile.getRowText());

				return true;
			});
			return holder;
		}

		@Override
		public void onBindViewHolder(final RecyclerView.ViewHolder rh, final int position) {
			final ViewHolder holder = (ViewHolder) rh;
			final WeldCountFile jobFile = mDataset.get(position);
			holder.tvFileName.setText(jobFile.getName());
			holder.tvTime.setText(Helper.TimeHelper.getLasModified(jobFile));
			holder.tvSize.setText(String.format(Locale.KOREA, "%dB", jobFile.length()));

			final String countString = jobFile.getJobInfo().getString();
			if (countString == null || countString.isEmpty()) {
				holder.tvCount.setVisibility(View.GONE);
			} else {
				holder.tvCount.setVisibility(View.VISIBLE);
				holder.tvCount.setText(countString);
			}
			final String previewString = jobFile.getJobInfo().getPreview();
			if (previewString == null || previewString.isEmpty()) {
				holder.tvPreview.setVisibility(View.GONE);
			} else {
				holder.tvPreview.setVisibility(View.VISIBLE);
				holder.tvPreview.setText(previewString);
			}
			final String CNString = jobFile.getCNList();
			if (CNString == null || CNString.isEmpty()) {
				holder.tvCN.setVisibility(View.GONE);
			} else {
				holder.tvCN.setVisibility(View.VISIBLE);
				holder.tvCN.setText(CNString);
			}
			final String MoveString = jobFile.getMoveList();
			if (MoveString == null || MoveString.isEmpty()) {
				holder.tvMove.setVisibility(View.GONE);
			} else {
				holder.tvMove.setVisibility(View.VISIBLE);
				holder.tvMove.setText(MoveString);
			}
			holder.mItemView.setTag(position);
		}

		@Override
		public int getItemCount() {
			return mDataset.size();
		}

		WeldCountFile getItem(int position) {
			return mDataset.get(position);
		}

		private class ViewHolder extends RecyclerView.ViewHolder {
			final View mItemView;
			final TextView tvFileName;
			final TextView tvTime;
			final TextView tvSize;
			final TextView tvCount;
			final TextView tvPreview;
			final TextView tvCN;
			final TextView tvMove;

			ViewHolder(View itemView) {
				super(itemView);
				mItemView = itemView;
				tvFileName = (TextView) itemView.findViewById(R.id.tvFileName);
				tvTime = (TextView) itemView.findViewById(R.id.tvFileTime);
				tvSize = (TextView) itemView.findViewById(R.id.tvFileSize);
				tvCount = (TextView) itemView.findViewById(R.id.tvJobCount);
				tvPreview = (TextView) itemView.findViewById(R.id.tvPreview);
				tvCN = (TextView) itemView.findViewById(R.id.tvCN);
				tvMove = (TextView) itemView.findViewById(R.id.tvMove);
			}
		}
	}

	public class WeldCountFileEditorAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		final WeldCountFile mFile;
		final List<WeldCountFile.Job> mDataset;
		final Activity mActivity;
		final View mSnackbarView;
		Context mContext;

		WeldCountFileEditorAdapter(Activity activity, View snackbarView,
		                           WeldCountFile weldCountFile) {
			mFile = weldCountFile;
			mDataset = new ArrayList<>();
			for (int i = 0; i < mFile.size(); i++) {
				final WeldCountFile.Job job = mFile.get(i);
				if (job.getRowType() == WeldCountFile.Job.JOB_SPOT)
					mDataset.add(job);
			}
			mActivity = activity;
			mSnackbarView = snackbarView;
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

		int updateA(String value) {
			return mFile.updateA(value);
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
			mContext = parent.getContext();
			final View v = LayoutInflater.from(mContext)
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
							mWeldCountAdapter.notifyItemChanged((int) v1.getTag(R.string.tag_position));
					}
				} catch (NumberFormatException e) {
					logD(e.getLocalizedMessage());
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

	private class LooperHandler extends Handler {
		LooperHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MSG_REFRESH:
					logD("MSG_REFRESH");
					refresh(true);
					break;
				default:
					break;
			}
		}
	}
}
