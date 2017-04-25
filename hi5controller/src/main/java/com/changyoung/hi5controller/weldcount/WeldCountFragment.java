package com.changyoung.hi5controller.weldcount;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.Helper;
import com.changyoung.hi5controller.common.Refresh;
import com.changyoung.hi5controller.weldfile.WeldFileListFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by changmin on 2015-10-13.
 * changmin811@gmail.com
 */
public class WeldCountFragment extends Fragment
		implements Refresh, LoaderManager.LoaderCallbacks<List<WeldCountFile>> {

	static final int MSG_REFRESH = 0;
	static final int ORDER_TYPE_DESCEND = 1;
	//	private static final String ARG_WORK_PATH = "workPath";
	private static final String TAG = "HI5:WeldCountFrag";
	private static final int ORDER_TYPE_ASCEND = 0;
	private static final int LAYOUT_TYPE_LINEAR = 0;
	private static final int LAYOUT_TYPE_GRID = 1;
	//	static final int LAYOUT_TYPE_STAGGERRED = 2;
	public WeldCountAdapter mWeldCountAdapter;
	WeldCountFileEditorAdapter mWeldCountFileEditorAdapter;
	View mView;
	WeldCountFileObserver observer;
	TextToSpeech mTts;
	SpeechRecognizer mRecognizer;
	private RecyclerView mRecyclerView;
	private RecyclerView.LayoutManager mLinearLayoutManager;
	private RecyclerView.LayoutManager mGridLayoutManager;
	private FloatingActionButton mFabSort;
	private FloatingActionButton mFabUpdate;
	private FloatingActionButton mFabStorage;
	private LooperHandler looperHandler;
	private int mOrderType = ORDER_TYPE_ASCEND;
	private int mLayoutType = LAYOUT_TYPE_LINEAR;

	//	private String mWorkPath;

	private OnWorkPathListener mListener;

	public WeldCountFragment() {
		// Required empty public constructor
	}

	public static void logD(String msg) {
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

			mWeldCountAdapter = new WeldCountAdapter(this, getActivity(), mView, new ArrayList<>());
			mRecyclerView.setAdapter(mWeldCountAdapter);
		}

		looperHandler = new LooperHandler(Looper.getMainLooper());
		observer = new WeldCountFileObserver(onGetWorkPath(), looperHandler);
		observer.startWatching();

		try {
			mTts = new TextToSpeech(getContext(), status -> {
			});
//			mTts.setLanguage(Locale.KOREAN);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mTts.speak("우측하단 버튼을 눌러 작업경로를 설정하세요", TextToSpeech.QUEUE_FLUSH, null, null);

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
				observer = new WeldCountFileObserver(onGetWorkPath(), looperHandler);
				observer.startWatching();
			}
		} catch (IllegalStateException e) {
			logD(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
				Snackbar.make(mView, msg, Snackbar.LENGTH_LONG)
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
			mWeldCountAdapter.updateZeroA();
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
		return new WeldCountAsyncTaskLoader(getActivity(), mListener);
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
		void onSetWorkUri(String uri, String path);
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
