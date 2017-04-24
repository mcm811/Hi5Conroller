package com.changyoung.hi5controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v13.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener,
		WeldFileListFragment.OnPathChangedListener, WeldFileFragment.OnWorkPathListener,
		WeldCountFragment.OnWorkPathListener, WeldConditionFragment.OnWorkPathListener {

	private final static String TAG = "HI5:MainActivity";

	private final int PERMISSION_REQUEST_EXTERNAL_STORAGE = 100;
//	private final int OPEN_DIRECTORY_REQUEST_CODE = 1000;

	private Toolbar mAppbarToolbar;
	private ViewPager mViewPager;
	private TabLayout mTabLayout;
	private DrawerLayout mDrawer;
	private int mBackPressedCount;

	private AdView mAdView;

	private static void logD(String msg) {
		try {
			Log.i(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private MainActivity getContext() {
		return this;
	}

/*
	private void createAccessIntent() {
		if (BuildConfig.DEBUG) {
			try {
				StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
				List<StorageVolume> volumes = sm.getStorageVolumes();
				for (StorageVolume volume : volumes) {
					if (volume.isRemovable()) {
						logD(volume.getState());
						logD("Storage:" + volume.getDescription(getContext()));
						logD("Dir:" + Environment.getExternalStorageDirectory().getPath());
						Intent intent = volume.createAccessIntent(null);
//						Intent intent = volume.createAccessIntent(Environment.DIRECTORY_DOCUMENTS);
						int request_code = 0;
						startActivityForResult(intent, request_code);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
*/

	private void checkPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			int writeExtPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
			int readExtPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
			int recordAudioPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
			if (recordAudioPerm != PackageManager.PERMISSION_GRANTED
					|| writeExtPerm != PackageManager.PERMISSION_GRANTED
					|| readExtPerm != PackageManager.PERMISSION_GRANTED) {
				if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					Toast.makeText(this, "Read/Write external storage", Toast.LENGTH_SHORT).show();
				}
				ActivityCompat.requestPermissions(this,
						new String[]{
								Manifest.permission.WRITE_EXTERNAL_STORAGE,
								Manifest.permission.READ_EXTERNAL_STORAGE,
								Manifest.permission.RECORD_AUDIO
						},
						PERMISSION_REQUEST_EXTERNAL_STORAGE);
			} else {
				Log.e(TAG, "permission has been granted");
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_EXTERNAL_STORAGE:
				if (grantResults.length > 0) {
					if (grantResults[0] == PackageManager.PERMISSION_GRANTED
							&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
						Log.e(TAG, "External storage permission granted");
					} else {
						Log.e(TAG, "External storage permission always deny");
					}
					if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
						Log.e(TAG, "Record audio permission granted");
					} else {
						Log.e(TAG, "Record audio permission always deny");
					}
				}
				break;
		}
	}

//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
//		int OPEN_DIRECTORY_REQUEST_CODE = 1000;
//		logD("onActivityResult");
//		if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//			if (resultData != null) {
//				Uri pickedDirUri = resultData.getData();
//				if (pickedDirUri != null) {
//					String path = Helper.UriHelper.getFullPathFromTreeUri(pickedDirUri, getContext());
//					String uri = pickedDirUri.toString();
//					onSetWorkUri(uri, path);
//					show("경로 설정 완료: " + path);
//				}
//			}
//		}
//	}

	private void show(String msg) {
		try {
			if (msg == null)
				return;
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter()).getItem(mTabLayout.getSelectedTabPosition());
			if (refresh != null)
				refresh.show(msg);
		} catch (Exception e) {
			e.printStackTrace();
			Snackbar.make(mViewPager, msg, Snackbar.LENGTH_LONG)
					.setAction("Action", null).show();
			logD(msg);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logD("onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		checkPermission();

		mAppbarToolbar = (Toolbar) findViewById(R.id.appbar_toolbar);
		setSupportActionBar(mAppbarToolbar);
		if (mAppbarToolbar != null) {
			mAppbarToolbar.setOnClickListener(v -> onBackPressed());
			logD("TITLE:" + mAppbarToolbar.getTitle().toString());
		} else {
			logD("TITLE:" + "mAppbarToolbar is null");
		}

//		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//		if (fab != null) {
//			fab.setOnClickListener(new View.OnClickListener() {
//				@Override
//				public void onClick(View view) {
//					Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//							.setAction("Action", null).show();
//					mBackPressedCount = 0;
//				}
//			});
//			findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
//				@Override
//				public void onClick(View v) {
//					startActivity(new Intent(MainActivity.this, WeldRestoreActivity.class));
//				}
//			});
//		}

//		FloatingActionButton mFabStorage = (FloatingActionButton) findViewById(R.id.fab_main_storage);
//		if (mFabStorage != null) {
//			mFabStorage.setOnClickListener(v -> {
//				logD("FabStorage:Main");
//				int OPEN_DIRECTORY_REQUEST_CODE = 1000;
//				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//					startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
//					logD("FabStorage:Main:OPEN_DIRECTORY_REQUEST_CODE");
//				}
//			});
//		}

		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (mDrawer != null) {
			ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
					MainActivity.this, mDrawer, mAppbarToolbar,
					R.string.navigation_drawer_open,
					R.string.navigation_drawer_close);
			//noinspection deprecation
			mDrawer.setDrawerListener(toggle);
			toggle.syncState();
		}

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		if (navigationView != null) {
			navigationView.setNavigationItemSelectedListener(MainActivity.this);
			try {
				String s = getText(R.string.app_name) + " (v" + BuildConfig.VERSION_NAME + ")";
				logD("App Name:" + s);
				View v = navigationView.getHeaderView(0);
				if (v != null) {
					TextView tvAppName = (TextView) v.findViewById(R.id.tvAppName);
					if (tvAppName != null)
						tvAppName.setText(s);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
		if (mTabLayout != null) {
			mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
			mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
		}

		PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), getApplicationContext());
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		if (mViewPager != null) {
			mViewPager.setAdapter(pagerAdapter);
		}
		mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
		mTabLayout.setupWithViewPager(mViewPager);

		mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				Helper.UiHelper.hideSoftKeyboard(getContext(), null, null);
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
				Helper.UiHelper.hideSoftKeyboard(getContext(), null, null);
			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				Helper.UiHelper.hideSoftKeyboard(getContext(), null, null);
			}
		});
/*
		mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			final CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
			final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

			int toolBarLayoutColorId = R.color.tab1_tablayout_background;
			int toolBarColorId = R.color.tab1_actionbar_background;
			int tabLayoutColorId = R.color.tab1_tablayout_background;
			int tabIndicatorColorId = R.color.tab1_tabindicator_background;

			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
				if (toolBarLayout != null)
					toolBarLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), toolBarLayoutColorId));
				if (toolbar != null)
					toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), toolBarColorId));
				if (mTabLayout != null) {
					mTabLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
					mTabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), tabIndicatorColorId));
				}
			}

			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				// 탭을 터치 했을때 뷰페이지 이동
				mViewPager.setCurrentItem(tab.getPosition(), true);
				mBackPressedCount = 0;
				switch (tab.getPosition()) {
					case PagerAdapter.WELD_COUNT_FRAGMENT:
						toolBarLayoutColorId = R.color.tab1_tablayout_background;
						toolBarColorId = R.color.tab1_actionbar_background;
						tabLayoutColorId = R.color.tab1_tablayout_background;
						tabIndicatorColorId = R.color.tab1_tabindicator_background;
						break;
					case PagerAdapter.WELD_CONDITION_FRAGMENT:
						toolBarLayoutColorId = R.color.tab2_tablayout_background;
						toolBarColorId = R.color.tab2_actionbar_background;
						tabLayoutColorId = R.color.tab2_tablayout_background;
						tabIndicatorColorId = R.color.tab2_tabindicator_background;
						break;
					case PagerAdapter.WORK_PATH_FRAGMENT:
						toolBarLayoutColorId = R.color.tab3_tablayout_background;
						toolBarColorId = R.color.tab3_actionbar_background;
						tabLayoutColorId = R.color.tab3_tablayout_background;
						tabIndicatorColorId = R.color.tab3_tabindicator_background;
						break;
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
				if (toolBarLayout != null)
					toolBarLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), toolBarLayoutColorId));
				if (toolbar != null)
					toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), toolBarColorId));
				if (mTabLayout != null) {
					mTabLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
					mTabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), tabIndicatorColorId));
				}
				Helper.UiHelper.hideSoftKeyboard(getContext(), null, null);
//				try {
//					Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter()).getItem(tab.getPosition());
//					if (refresh != null) {
//						refresh.refresh(false);
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {

			}
		});
*/

		MobileAds.initialize(getApplicationContext(), "R.string.banner_ad_unit_id");

		AdRequest adRequest;
		int adResId;
		if (BuildConfig.DEBUG) {
			adRequest = new AdRequest.Builder().addTestDevice("hi5Controller test device").build();
			adResId = R.string.banner_ad_unit_id_debug;
		} else {
			adRequest = new AdRequest.Builder().build();
			adResId = R.string.banner_ad_unit_id;
		}
		mAdView = new AdView(getContext());
		mAdView.setAdSize(AdSize.MEDIUM_RECTANGLE);
		mAdView.setAdUnitId(getContext().getString(adResId));
		mAdView.loadAd(adRequest);

/*
		AdView adViewBanner = (AdView) findViewById(R.id.adViewBanner);
		adViewBanner.setAlpha(0.5f);
		adViewBanner.loadAd(adRequest);
*/
	}

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		logD("onSaveInstanceState");
		super.onSaveInstanceState(outState, outPersistentState);
	}

	@Override
	public void onBackPressed() {
		final int EXIT_COUNT = 0;
		if (mDrawer.isDrawerOpen(GravityCompat.START)) {
			mDrawer.closeDrawer(GravityCompat.START);
			mBackPressedCount = 0;
		} else {
			try {
				Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter())
						.getItem(mTabLayout.getSelectedTabPosition());
				if (refresh != null) {
					String ret = refresh.onBackPressedFragment();
					if (ret == null || ret.compareTo("/") == 0)
						throw new Exception();
					else
						mBackPressedCount = 0;
				}
			} catch (Exception e) {
				if (++mBackPressedCount > EXIT_COUNT) {
					onExitDialog();
				} else {
					show(String.format(getResources().getString(R.string.main_activity_exit_format),
							Integer.toString(EXIT_COUNT - mBackPressedCount + 1)));
				}
			}
		}
	}

	private void onExitDialog() {
		Helper.UiHelper.adMobExitDialog(this, mAdView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_toolbar_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		mBackPressedCount = 0;

		switch (item.getItemId()) {
			case R.id.action_main_toolbar_exit:
				onExitDialog();
//				finish();
				break;
			case R.id.action_main_toolbar_restore:
//				final FloatingActionButton fab = (FloatingActionButton) getFab();
//				if (fab != null && android.os.Build.VERSION.SDK_INT
//						>= android.os.Build.VERSION_CODES.LOLLIPOP) {
//					if (mTabLayout.getSelectedTabPosition() == PagerAdapter.WORK_PATH_FRAGMENT) {
//						ActivityOptions options = ActivityOptions
//								.makeSceneTransitionAnimation(this, fab, "fab");
//						startActivity(new Intent(this, WeldRestoreActivity.class), options.toBundle());
//					} else {
//						final Intent intent = new Intent(this, WeldRestoreActivity.class);
//						final ActivityOptions options = ActivityOptions
//								.makeSceneTransitionAnimation(this, fab, "fab");
//						TranslateAnimation translateAnimation = new TranslateAnimation(
//								TranslateAnimation.RELATIVE_TO_SELF, 0f,
//								TranslateAnimation.ABSOLUTE, (mTabLayout.getRight() - fab.getRight()) - fab.getLeft(),
//								TranslateAnimation.RELATIVE_TO_SELF, 0f,
//								TranslateAnimation.RELATIVE_TO_SELF, 0f);
//						translateAnimation.setDuration(400);
//						translateAnimation.setInterpolator(new DecelerateInterpolator());
//						translateAnimation.setAnimationListener(new Animation.AnimationListener() {
//							@Override
//							public void onAnimationStart(Animation animation) {
//
//							}
//
//							@Override
//							public void onAnimationEnd(Animation animation) {
//								startActivity(intent, options.toBundle());
//							}
//
//							@Override
//							public void onAnimationRepeat(Animation animation) {
//
//							}
//						});
//						fab.startAnimation(translateAnimation);
//					}
//				} else {
//					startActivity(new Intent(this, WeldRestoreActivity.class));
//				}
				startActivity(new Intent(this, WeldRestoreActivity.class));
				break;
			case R.id.action_main_toolbar_backup:
				String ret = Helper.FileHelper.backupDocumentFile(getContext(), mTabLayout);
				if (ret != null)
					show(ret);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

//	private View getFab() {
//		View fab = null;
//		try {
//			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter())
//					.getItem(mTabLayout.getSelectedTabPosition());
//			if (refresh != null)
//				fab = refresh.getFab();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return fab;
//	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		mBackPressedCount = 0;

		// Handle navigation view item clicks here.
		int id = item.getItemId();
		logD("ID:" + id);

		if (id == R.id.nav_weld_count) {
			mViewPager.setCurrentItem(PagerAdapter.WELD_COUNT_FRAGMENT, true);
		} else if (id == R.id.nav_weld_condition) {
			mViewPager.setCurrentItem(PagerAdapter.WELD_CONDITION_FRAGMENT, true);
		} else if (id == R.id.nav_home) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_storage) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_sdcard) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_extsdcard) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_usbstorage) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_backup) {
			startActivity(new Intent(MainActivity.this, WeldRestoreActivity.class));
		} else if (id == R.id.nav_exit) {
			onExitDialog();
//			finish();
		}
		mDrawer.closeDrawer(GravityCompat.START);

		try {
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter())
					.getItem(mTabLayout.getSelectedTabPosition());
			if (refresh != null) {
				show(refresh.refresh(id));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void onPathChanged(File path) {
		logD("onPathChanged");
		WeldFileFragment fragment = (WeldFileFragment) ((PagerAdapter) mViewPager.getAdapter())
				.getItem(PagerAdapter.WORK_PATH_FRAGMENT);
		if (fragment != null)
			fragment.onPathChanged(path.getPath());
	}

	@Override
	public String onGetWorkPath() {
		String path;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			Uri uri = Uri.parse(onGetWorkUri());
			path = Helper.UriHelper.getFullPathFromTreeUri(uri, getContext());
		} else {
			path = Helper.Pref.getWorkPath(getContext());
		}

/*
		try {
			File file = new File(path);
			if (file.getName().length() > 0) {
				String s = getText(R.string.app_name) + " (" + file.getName() + ")";
				mAppbarToolbar.setTitle(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
*/

		return path;
	}

	@Override
	public void onSetWorkPath(String path) {
		logD("onSetWorkPath");
		Helper.Pref.setWorkPath(getContext(), path);

		final int[] tabs = {
				PagerAdapter.WELD_COUNT_FRAGMENT,
				PagerAdapter.WELD_CONDITION_FRAGMENT,
				PagerAdapter.WORK_PATH_FRAGMENT
		};
		for (int tab : tabs) {
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter()).getItem(tab);
			if (refresh != null)
				refresh.refresh(true);
		}
	}

	@Override
	public String onGetWorkUri() {
		return Helper.Pref.getWorkPathUriString(getContext());
	}

	@Override
	public void onSetWorkUri(String uri, String path) {
		logD("onSetWorkUri");
		Helper.Pref.setWorkPathUri(getContext(), uri);
		Helper.Pref.setWorkPath(getContext(), path);

		final int[] tabs = {
				PagerAdapter.WELD_COUNT_FRAGMENT,
				PagerAdapter.WELD_CONDITION_FRAGMENT,
				PagerAdapter.WORK_PATH_FRAGMENT
		};
		for (int tab : tabs) {
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter()).getItem(tab);
			if (refresh != null)
				refresh.refresh(true);
		}

		try {
			File file = new File(path);
			if (file.getName().length() > 0) {
//				String s = getText(R.string.app_name) + " (" + file.getName() + ")";
				String s = file.getName();
				mAppbarToolbar.setTitle(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class PagerAdapter extends FragmentStatePagerAdapter {
		final static int WELD_COUNT_FRAGMENT = 0;
		final static int WELD_CONDITION_FRAGMENT = 1;
		final static int WORK_PATH_FRAGMENT = 2;
		final static int NUM_OF_TABS = 3;

		private final Context mContext;
		private final Fragment[] mFragments;

		PagerAdapter(FragmentManager fm, Context context) {
			super(fm);
			mContext = context;
			mFragments = new Fragment[NUM_OF_TABS];
		}

		@Override
		public int getCount() {
			return NUM_OF_TABS;
		}

		@Override
		public Fragment getItem(int position) {
			if (mFragments[position] == null) {
				switch (position) {
					case WELD_COUNT_FRAGMENT:
						mFragments[position] = new WeldCountFragment();
						break;
					case WELD_CONDITION_FRAGMENT:
						mFragments[position] = new WeldConditionFragment();
						break;
					case WORK_PATH_FRAGMENT:
						mFragments[position] = new WeldFileFragment();
						break;
				}
			}
			return mFragments[position];
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case WELD_COUNT_FRAGMENT:
					return mContext.getResources().getString(R.string.weldcount_fragment);
				case WELD_CONDITION_FRAGMENT:
					return mContext.getResources().getString(R.string.weldcondition_fragment);
				case WORK_PATH_FRAGMENT:
					return mContext.getResources().getString(R.string.weldfile_fragment);
			}
			return super.getPageTitle(position);
		}
	}
}
