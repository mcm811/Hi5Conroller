package com.changyoung.hi5controller.weldcondition;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;

import com.changyoung.hi5controller.R;
import com.changyoung.hi5controller.common.FileHelper;
import com.changyoung.hi5controller.common.RefreshHandler;
import com.changyoung.hi5controller.common.UiHelper;
import com.changyoung.hi5controller.common.UriHelper;
import com.changyoung.hi5controller.weldfile.WeldFileListFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by chang on 2015-10-14.
 * changmin811@gmail.com
 */
public class WeldConditionFragment extends Fragment
		implements RefreshHandler,
		LoaderManager.LoaderCallbacks<List<WeldConditionItem>> {
	static final int MSG_DEFAULT = 100;
	static final int MSG_REFRESH = 101;
	static final int[] valueMax = { 2000, 100, 350, 500, 500, 500, 500, 1000, 1000 };
	private static final String TAG = "HI5:WeldConditionFrag";
	RecyclerView mRecyclerView;
	WeldConditionAdapter mWeldConditionAdapter;
	RecyclerView.LayoutManager mLayoutManager;
	Snackbar mSnackbar;
	WeldConditionSqueezeForceAdapter mWeldConditionSqueezeForceAdapter;
	WeldConditionObserver mWeldConditionObserver;
	TextToSpeech mTextToSpeech;
	SpeechRecognizer mSpeechRecognizer;
	int mLastPosition = 0;
	boolean mSaveFlag;
	private View mView;
	private int mFabImageId = R.drawable.ic_view_module;
	private FloatingActionButton mFabMain;
	private FloatingActionButton mFabStorage;
	private LooperHandler mLooperHandler;
	private OnWorkPathListener mListener;

	public WeldConditionFragment() {
		// Required empty public constructor
	}

/*
	public static WeldConditionFragment newInstance(String workPath) {
		WeldConditionFragment fragment = new WeldConditionFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		fragment.setArguments(args);
		return fragment;
	}
*/

	static void logD(String msg) {
		try {
			Log.i(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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

							String path = UriHelper.getFullPathFromTreeUri(uri, getContext());
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
		// onCreated -> onCreatedView -> onActivityCreated
		logD("onCreated");
		super.onCreate(savedInstanceState);

		try {
			mListener = (OnWorkPathListener) getActivity();
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ClassCastException(getActivity().toString() + " must implement OnPathChangedListener");
		}

//		if (getArguments() != null) {
//			mWorkPath = getArguments().getString(ARG_WORK_PATH) + "/ROBOT.SWD";
//		} else {
//			mWorkPath = onGetWorkPath();
//		}
	}

	@SuppressLint("CutPasteId")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		logD("onCreatedView");
		mView = inflater.inflate(R.layout.weldcondition_fragment, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) mView.findViewById(R.id.srl);
		refresher.setOnRefreshListener(() -> {
			onRefresh(true);
			refresher.setRefreshing(false);
		});

		mFabMain = (FloatingActionButton) mView.findViewById(R.id.fab_weld_condition_main);
		mFabMain.setOnClickListener(v -> {
			if (mSaveFlag) {
				mSaveFlag = false;
				setImageFab();
				mWeldConditionAdapter.update(onGetWorkPath());
				show("저장 완료: " + onGetWorkPath());
			} else if (mWeldConditionAdapter.getSelectedItemCount() == 0) {
				mWeldConditionSqueezeForceAdapter.showSqueezeForceEditorDialog();
			} else {
				mWeldConditionAdapter.showEditorDialog(0);
			}
		});
		mFabMain.setOnLongClickListener(v -> {
			UiHelper.textViewActivity(getActivity(), "ROBOT.SWD",
					FileHelper.readFileString(onGetWorkPath()));
			return true;
		});

		mFabStorage = (FloatingActionButton) mView.findViewById(R.id.fab_weld_condition_storage);
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
					show(onRefresh(R.id.nav_usbstorage));
				}
			});
		}

		mRecyclerView = (RecyclerView) mView.findViewById(R.id.recycler_view);
		mRecyclerView.setHasFixedSize(true);
		mLayoutManager = new LinearLayoutManager(getContext());
		mRecyclerView.setLayoutManager(mLayoutManager);
		mWeldConditionAdapter = new WeldConditionAdapter(this, getActivity(), new ArrayList<>());
		mWeldConditionAdapter.onLoadInstanceState(savedInstanceState);
		mRecyclerView.setAdapter(mWeldConditionAdapter);
		RecyclerView.ItemDecoration itemDecoration =
				new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
		mRecyclerView.addItemDecoration(itemDecoration);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());
		mWeldConditionSqueezeForceAdapter = new WeldConditionSqueezeForceAdapter(this, getActivity(), mWeldConditionAdapter.getData());

		mLooperHandler = new LooperHandler(Looper.getMainLooper());
		mWeldConditionObserver = new WeldConditionObserver(onGetWorkPath(), mLooperHandler);
		mWeldConditionObserver.startWatching();

		try {
			mTextToSpeech = new TextToSpeech(getContext(), status -> {
			});
//			mTextToSpeech.setLanguage(Locale.KOREAN);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mWeldConditionAdapter.onSaveInstanceState(outState);
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
		mWeldConditionAdapter = null;
		mView = null;
		mRecyclerView = null;
		mSnackbar = null;
		getLoaderManager().destroyLoader(0);
		mLooperHandler = null;
		if (mTextToSpeech != null) {
			mTextToSpeech.shutdown();
			mTextToSpeech = null;
		}
		if (mSpeechRecognizer != null) {
			mSpeechRecognizer.stopListening();
			mSpeechRecognizer.destroy();
			mSpeechRecognizer = null;
		}
	}

	@Override
	public void onRefresh(boolean forced) {
		try {
			if (isAdded()) {
				logD("onRefresh:restartLoader:" + onGetWorkPath());
				getLoaderManager().restartLoader(0, null, this);
				if (mWeldConditionObserver != null) {
					mWeldConditionObserver.stopWatching();
					mWeldConditionObserver = null;
				}
				mWeldConditionObserver = new WeldConditionObserver(onGetWorkPath(), mLooperHandler);
				mWeldConditionObserver.startWatching();
			}
		} catch (IllegalStateException e) {
			logD(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String onRefresh(int menuId) {
		return null;
	}

	@Override
	public String onBackPressedFragment() {
		if (mRecyclerView != null) {
			if (mWeldConditionAdapter.getSelectedItemCount() > 0) {
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
				Snackbar.make(mView, msg, Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
				logD(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

/*
	@Override
	public View getFab() {
		return mFabMain;
	}
*/

	void setCheckedItemSnackbar() {
		try {
			if (mView != null && isAdded()) {
				final int selectedItemCount = mWeldConditionAdapter.getSelectedItemCount();
				if (mSnackbar == null || !mSnackbar.isShown()) {
					mSnackbar = Snackbar
							.make(mView, String.valueOf(selectedItemCount) + "개 항목 선택됨", Snackbar.LENGTH_INDEFINITE)
							.setAction("선택 취소", v -> {
								UiHelper.hideSoftKeyboard(getActivity(), null, null);
								mWeldConditionAdapter.clearSelections();
								setImageFab();
							});
				}
				if (selectedItemCount > 0 && mWeldConditionAdapter.getItemCount() > 0) {
					if (mSnackbar.isShown())
						mSnackbar.setText(String.valueOf(selectedItemCount) + "개 항목 선택됨");
					else {
						new Handler().postDelayed(() -> {
							if (mSnackbar != null)
								mSnackbar.show();
						}, 500);
					}
				} else {
					mSnackbar.dismiss();
					mSnackbar = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void setCheckedItem(boolean value) {
		try {
			if (isAdded()) {
				if (!value)
					mWeldConditionAdapter.clearSelections();
				setImageFab();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		setCheckedItemSnackbar();
	}

	void setImageFab() {
		long fabDelay = 0;
		int fabImageId = R.drawable.ic_edit;
		if (mSaveFlag)
			fabImageId = R.drawable.ic_save;
		else if (mWeldConditionAdapter.getItemCount() == 0)
			fabImageId = R.drawable.ic_refresh;
		else if (mWeldConditionAdapter.getSelectedItemCount() == 0) {
			fabImageId = R.drawable.ic_view_module;
			fabDelay = 350;
		}
		if (fabImageId == mFabImageId)
			return;
		mFabImageId = fabImageId;

		mFabMain.clearAnimation();
		final RotateAnimation animation = new RotateAnimation(0f, 180f,
				Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		animation.setDuration(150);
		animation.setInterpolator(new AccelerateInterpolator());
		animation.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				try {
					if (isAdded())
						mFabMain.setImageResource(mFabImageId);
				} catch (Exception e) {
					e.printStackTrace();
				}
				RotateAnimation expand = new RotateAnimation(180f, 0f,
						Animation.RELATIVE_TO_SELF, 0.5f,
						Animation.RELATIVE_TO_SELF, 0.5f);
				expand.setDuration(350);
				expand.setInterpolator(new DecelerateInterpolator());
				mFabMain.startAnimation(expand);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}
		});
		mFabMain.postDelayed(() -> mFabMain.startAnimation(animation), fabDelay);
	}

/*
	private void onSetWorkPath(String path) {
		if (mListener != null) {
			mListener.onSetWorkPath(path);
		}
	}
*/

	String onGetWorkPath() {
		if (mListener != null) {
			return mListener.onGetWorkPath() + File.separator + "ROBOT.SWD";
		}
		return null;
	}

	private void onSetWorkUri(String uri, String path) {
		if (mListener != null) {
			mListener.onSetWorkUri(uri, path);
		}
	}

/*
	private String onGetWorkUri() {
		if (mListener != null) {
			return mListener.onGetWorkUri();
		}
		return null;
	}
*/

	@Override
	public Loader<List<WeldConditionItem>> onCreateLoader(int id, Bundle args) {
		logD(String.format(Locale.KOREA, "ID_%d onCreateLoader()", id));
		return new WeldConditionLoader(getActivity(), mListener);
	}

	@Override
	public void onLoadFinished(Loader<List<WeldConditionItem>> loader, List<WeldConditionItem> data) {
		logD(String.format(Locale.KOREA, "onLoadFinished: id:%d, size:%d", loader.getId(), data.size()));
		mWeldConditionAdapter.setData(data);
		if (mRecyclerView != null)
			mRecyclerView.refreshDrawableState();
		if (mView != null) {
			if (mWeldConditionAdapter.getItemCount() > 0) {
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

		if (mFabMain != null) {
			final RotateAnimation animation = new RotateAnimation(0, 360 * 2,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(500);
			animation.setFillAfter(true);
			animation.setInterpolator(new AccelerateDecelerateInterpolator());
			mFabMain.startAnimation(animation);
		}

		if (mFabStorage != null) {
			final RotateAnimation animation = new RotateAnimation(360 * 2, 0,
					Animation.RELATIVE_TO_SELF, 0.5f,
					Animation.RELATIVE_TO_SELF, 0.5f);
			animation.setDuration(500);
			animation.setFillAfter(true);
			animation.setInterpolator(new AccelerateDecelerateInterpolator());
			mFabStorage.startAnimation(animation);
		}
	}

	@Override
	public void onLoaderReset(Loader<List<WeldConditionItem>> loader) {
		logD(String.format(Locale.KOREA, "ID_%d onLoaderReset()", loader.getId()));
		mWeldConditionAdapter.setData(null);
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

		void onSetWorkUri(String uri, String path);
	}

	/*
	public static class DividerItemDecoration extends RecyclerView.ItemDecoration {

		private static final int[] ATTRS = new int[]{
				android.R.attr.listDivider
		};

		public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

		public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

		private Drawable mDivider;

		private int mOrientation;

		public DividerItemDecoration(Context context, int orientation) {
			final TypedArray a = context.obtainStyledAttributes(ATTRS);
			mDivider = a.getDrawable(0);
			a.recycle();
			setOrientation(orientation);
		}

		public void setOrientation(int orientation) {
			if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
				throw new IllegalArgumentException("invalid orientation");
			}
			mOrientation = orientation;
		}

		@Override
		public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
			if (mOrientation == VERTICAL_LIST) {
				drawVertical(c, parent);
			} else {
				drawHorizontal(c, parent);
			}
		}

		public void drawVertical(Canvas c, RecyclerView parent) {
			final int left = parent.getPaddingLeft();
			final int right = parent.getWidth() - parent.getPaddingRight();

			final int childCount = parent.getChildCount();
			for (int i = 0; i < childCount; i++) {
				final View child = parent.getChildAt(i);
				final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
						.getLayoutParams();
				final int top = child.getBottom() + params.bottomMargin;
				final int bottom = top + mDivider.getIntrinsicHeight();
				mDivider.setBounds(left, top, right, bottom);
				mDivider.draw(c);
			}
		}

		public void drawHorizontal(Canvas c, RecyclerView parent) {
			final int top = parent.getPaddingTop();
			final int bottom = parent.getHeight() - parent.getPaddingBottom();

			final int childCount = parent.getChildCount();
			for (int i = 0; i < childCount; i++) {
				final View child = parent.getChildAt(i);
				final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
						.getLayoutParams();
				final int left = child.getRight() + params.rightMargin;
				final int right = left + mDivider.getIntrinsicHeight();
				mDivider.setBounds(left, top, right, bottom);
				mDivider.draw(c);
			}
		}

		@Override
		public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
		                           RecyclerView.State state) {
			if (mOrientation == VERTICAL_LIST) {
				outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
			} else {
				outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
			}
		}
	}
*/

	private class LooperHandler extends Handler {
		LooperHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MSG_DEFAULT:
					break;
				case MSG_REFRESH:
					logD("MSG_REFRESH");
					onRefresh(true);
					break;
				default:
					break;
			}
		}
	}
}