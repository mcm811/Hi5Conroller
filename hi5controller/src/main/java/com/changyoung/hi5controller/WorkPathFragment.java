package com.changyoung.hi5controller;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.File;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnWorkPathListener} interface
 * to handle interaction events.
 * Use the {@link WorkPathFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WorkPathFragment extends Fragment implements Refresh {
	private static final String TAG = "WorkPathFragment";
	private static final String ARG_WORK_PATH = "workPath";
	FileListFragment fragment;
	private View mView;
	private String mWorkPath;
	private FloatingActionButton mFab;

	private OnWorkPathListener mListener;

	public WorkPathFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param workPath Parameter 1.
	 * @return A new instance of fragment WorkPathFragment.
	 */
	@SuppressWarnings("unused")
	public static WorkPathFragment newInstance(String workPath) {
		WorkPathFragment fragment = new WorkPathFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		fragment.setArguments(args);
		return fragment;
	}

	private void logD(String msg) {
		try {
			Log.d(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void refresh(boolean forced) {
		try {
			if (forced && isAdded()) {
				EditText etPath = (EditText) mView.findViewById((R.id.etWorkPath));
				FileListFragment workPathFragment = (FileListFragment) getChildFragmentManager().findFragmentById(R.id.work_path_fragment);
				etPath.setText(onGetWorkPath());
				workPathFragment.refreshFilesList(etPath.getText().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean refresh(String path) {
		if (path != null && isAdded()) {
			try {
				File dir = new File(path);
				if (dir.isDirectory()) {
					FileListFragment fragment = (FileListFragment) getChildFragmentManager().findFragmentById(R.id.work_path_fragment);
					fragment.refreshFilesList(dir);
				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				logD("refresh");
			}
		} else {
			logD("isAdded() == false, refresh:" + path);
		}
		return false;
	}

	public String refresh(int menuId) {
		String ret = null;
		switch (menuId) {
			case R.id.toolbar_work_path_menu_home:
				if (!refresh(onGetWorkPath()))
					ret = "경로 이동 실패: " + onGetWorkPath();
				break;
			case R.id.nav_storage:
//			case R.id.toolbar_work_path_menu_storage:
				if (!refresh(Helper.Pref.STORAGE_PATH))
					ret = "경로 이동 실패: " + Helper.Pref.STORAGE_PATH;
				break;
			case R.id.nav_sdcard:
//			case R.id.toolbar_work_path_menu_sdcard:
				if (!refresh(Helper.Pref.EXTERNAL_STORAGE_PATH))
					ret = "경로 이동 실패: " + Helper.Pref.EXTERNAL_STORAGE_PATH;
				break;
			case R.id.nav_extsdcard:
//			case R.id.toolbar_work_path_menu_extsdcard:
				ret = "경로 이동 실패: " + "SD 카드";
				try {
					File dir = new File(Helper.Pref.STORAGE_PATH);
					for (File file : dir.listFiles()) {
						if (file.getName().toLowerCase().startsWith("ext") || file.getName().toLowerCase().startsWith("sdcard1")) {
							try {
								for (File subItem : file.listFiles()) {
									if (subItem.exists() && refresh(file.getPath())) {
										ret = null;
										break;
									}
								}
							} catch (NullPointerException e) {
								Log.d(TAG, e.getLocalizedMessage());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					logD("경로 이동 실패");
				}
				break;
			case R.id.nav_usbstorage:
			case R.id.toolbar_work_path_menu_usbstorage:
				ret = "경로 이동 실패: " + "USB 저장소";
				try {
					File dir = new File(Helper.Pref.STORAGE_PATH);
					Log.d(TAG, String.format("STORAGE: %s", dir.getPath().toLowerCase()));
					for (File file : dir.listFiles()) {
						if (file.getName().toLowerCase().startsWith("usb")) {
							try {
								for (File subItem : file.listFiles()) {
									if (subItem.exists() && refresh(file.getPath())) {
										Log.d(TAG, String.format("USB: %s", file.getPath().toLowerCase()));
										ret = null;
										break;
									}
								}
							} catch (NullPointerException e) {
								Log.d(TAG, e.getLocalizedMessage());
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					logD("경로 이동 실패");
				}
				break;
		}
		return ret;
	}

	@Override
	public String onBackPressedFragment() {
		return isAdded() ? fragment.refreshParent() : null;
	}

	@Override
	public void show(String msg) {
		try {
			if (msg == null)
				return;
			if (mView != null) {
				Snackbar.make(mView.findViewById(R.id.coordinator_layout),
						msg, Snackbar.LENGTH_SHORT)
						.setAction("Action", null).show();
			}
			logD(msg);
		} catch (Exception e) {
			e.printStackTrace();
			logD(msg);
		}
	}

	public void onPathChanged(String path) {
		try {
			if (isAdded()) {
				mWorkPath = onGetWorkPath();
				EditText etPath = (EditText) mView.findViewById(R.id.etWorkPath);
				etPath.setText(path);
				FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.fab);
				if (mWorkPath.compareTo(path) == 0) {
					fab.setImageResource(R.drawable.ic_archive_white);
				} else {
					fab.setImageResource(R.drawable.ic_done_white);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public View getFab() {
		return mFab;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mWorkPath = getArguments().getString(ARG_WORK_PATH);
		} else {
			mWorkPath = onGetWorkPath();
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		mView = inflater.inflate(R.layout.fragment_work_path, container, false);

		String path = mWorkPath;
		fragment = (FileListFragment) getChildFragmentManager().findFragmentById(R.id.work_path_fragment);
		if (fragment == null) {
			Log.d(TAG, "fragment == null");
			fragment = FileListFragment.newInstance(path);
			FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
			transaction.replace(R.id.work_path_fragment, fragment);
			transaction.addToBackStack(null);
			transaction.commit();
		}

		fragment.snackbarView = mView.findViewById(R.id.coordinator_layout);
		fragment.refreshFilesList(path);
		EditText etPath = (EditText) mView.findViewById(R.id.etWorkPath);
		etPath.setText(path);
		etPath.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				FileListFragment fragment = (FileListFragment) getChildFragmentManager().findFragmentById(R.id.work_path_fragment);
				EditText etPath = (EditText) mView.findViewById(R.id.etWorkPath);
				try {
					File file = new File(etPath.getText().toString());
					if (file.isDirectory()) {
						onSetWorkPath(etPath.getText().toString());
					} else {
						throw new Exception();
					}
				} catch (NullPointerException e) {
					logD(e.getLocalizedMessage());
				} catch (Exception e) {
					e.printStackTrace();
					show("잘못된 경로: " + etPath.getText().toString());
					etPath.setText(onGetWorkPath());
				}
				fragment.refreshFilesList(etPath.getText().toString());
			}
		});
		etPath.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Helper.UiHelper.hideSoftKeyboard(getActivity(), v, event);
				return false;
			}
		});

		mFab = (FloatingActionButton) mView.findViewById(R.id.fab);
		if (mFab != null) {
			mFab.setOnClickListener(new View.OnClickListener() {
				private void scaleAnimationFab(final float from, final float to) {
					ScaleAnimation shrink = new ScaleAnimation(from, to, from, to,
							Animation.RELATIVE_TO_SELF, 0.5f,
							Animation.RELATIVE_TO_SELF, 0.5f);
					shrink.setDuration(250);
					shrink.setInterpolator(new AccelerateInterpolator());
					shrink.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {

						}

						@Override
						public void onAnimationEnd(Animation animation) {
							ScaleAnimation expand = new ScaleAnimation(to, from, to, from,
									Animation.RELATIVE_TO_SELF, 0.5f,
									Animation.RELATIVE_TO_SELF, 0.5f);
							expand.setDuration(250);
							expand.setInterpolator(new DecelerateInterpolator());
							mFab.startAnimation(expand);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {

						}
					});
					mFab.startAnimation(shrink);
				}

				@Override
				public void onClick(View v) {
					scaleAnimationFab(1.0f, 1.5f);
					Helper.UiHelper.hideSoftKeyboard(getActivity(), null, null);
					FileListFragment fragment = (FileListFragment) getChildFragmentManager()
							.findFragmentById(R.id.work_path_fragment);
					EditText etPath = (EditText) mView.findViewById(R.id.etWorkPath);
					String path = fragment.getDirPath();
					mWorkPath = onGetWorkPath();
					if (mWorkPath.compareTo(path) == 0) {
//						ActivityOptions options = ActivityOptions
//								.makeSceneTransitionAnimation(getActivity(), fab, "fab");
//						startActivity(new Intent(getContext(), BackupActivity.class), options.toBundle());
						String ret = Helper.FileHelper.backup(getContext(),
								mView.findViewById(R.id.coordinator_layout));
						if (ret != null)
							show(ret);
					} else {
						etPath.setText(path);
						onSetWorkPath(path);
						fragment.refreshFilesList();
						show("경로 설정 완료: " + path);
					}
				}
			});
		}

		Toolbar toolbar = (Toolbar) mView.findViewById(R.id.work_path_toolbar);
		toolbar.inflateMenu(R.menu.menu_toolbar_work_path);
		toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				String ret = refresh(item.getItemId());
				if (ret != null)
					show(ret);
				return true;
			}
		});

		AdView adView = new AdView(getContext());
		adView.setAdSize(AdSize.BANNER);
		adView.setScaleX(0.4f);
		adView.setScaleY(0.4f);
		adView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
				FrameLayout.LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM | Gravity.START));
		if (BuildConfig.DEBUG)
			adView.setAdUnitId(getActivity().getString(R.string.banner_ad_unit_id_debug));
		else
			adView.setAdUnitId(getActivity().getString(R.string.banner_ad_unit_id_release));
		AdRequest adRequest = new AdRequest.Builder()
				.setRequestAgent("android_studio:ad_template").build();
		adView.loadAd(adRequest);
		FrameLayout frameLayout = (FrameLayout) mView.findViewById(R.id.frame_layout);
		frameLayout.addView(adView, frameLayout.getChildCount() - 1);

		return mView;
	}

	public void onSetWorkPath(String path) {
		if (mListener != null) {
			mListener.onSetWorkPath(path);
		}
	}

	public String onGetWorkPath() {
		if (mListener != null) {
			return mListener.onGetWorkPath();
		}
		return null;
	}

	@Override
	public void onAttach(Context activity) {
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
	public void onDetach() {
		super.onDetach();
		mListener = null;
		mView = null;
		mWorkPath = null;
		fragment = null;
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

		void onSetWorkPath(String path);
	}
}
