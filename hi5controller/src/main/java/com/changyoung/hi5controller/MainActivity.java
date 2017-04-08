package com.changyoung.hi5controller;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
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
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import static android.os.Build.VERSION_CODES.M;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener,
		FileListFragment.OnPathChangedListener, WorkPathFragment.OnWorkPathListener,
		WeldCountFragment.OnWorkPathListener, WeldConditionFragment.OnWorkPathListener {

	private final static String TAG = "HI5:MainActivity";
	private final int PERMISSION_REQUEST_EXTERNAL_STORAGE = 100;
	private ViewPager mViewPager;
	private TabLayout mTabLayout;
	private DrawerLayout mDrawer;
	private int mBackPressedCount;

	private static void logD(String msg) {
		try {
			Log.i(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	MainActivity getContext() {
		return this;
	}

	@TargetApi(25)
	private void createAccessIntent() {
/*
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
*/
	}

	@TargetApi(M)
	private void checkPermissionExternalStorage() {
		int writeExtPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		int readExtPerm = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
		logD("WRITE_EXTERNAL_STORAGE : " + writeExtPerm);
		logD("READ_EXTERNAL_STORAGE : " + readExtPerm);

		if (writeExtPerm != PackageManager.PERMISSION_GRANTED || readExtPerm != PackageManager.PERMISSION_GRANTED) {
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				Toast.makeText(this, "Read/Write external storage", Toast.LENGTH_SHORT).show();
			}
			ActivityCompat.requestPermissions(this,
					new String[]{
							Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.WRITE_EXTERNAL_STORAGE
					},
					PERMISSION_REQUEST_EXTERNAL_STORAGE);
		} else {
			logD("permission has been granted");
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       @NonNull String[] permissions,
	                                       @NonNull int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_EXTERNAL_STORAGE:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED
						&& grantResults[1] == PackageManager.PERMISSION_GRANTED) {
					logD("Permission granted");
				} else {
					logD("Permission always deny");
				}
				break;
		}
	}

	private void show(String msg) {
		try {
			if (msg == null)
				return;
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter()).getItem(mTabLayout.getSelectedTabPosition());
			if (refresh != null)
				refresh.show(msg);
		} catch (Exception e) {
			e.printStackTrace();
			Snackbar.make(mViewPager, msg, Snackbar.LENGTH_SHORT)
					.setAction("Action", null).show();
			logD(msg);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		logD("onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		checkPermissionExternalStorage();
		createAccessIntent();

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (toolbar != null) {
			toolbar.setOnClickListener(v -> onBackPressed());
//			toolbar.setOnLongClickListener(v -> onExitDialog());
			logD("TITLE:" + toolbar.getTitle().toString());
		} else {
			logD("TITLE:" + "toolbar is null");
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
//					startActivity(new Intent(MainActivity.this, BackupActivity.class));
//				}
//			});
//		}

		mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (mDrawer != null) {
			ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
					MainActivity.this, mDrawer, toolbar,
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
				String s = getText(R.string.app_name) + " (v" + BuildConfig.VERSION_NAME + ") ";
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
			CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
			Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

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
					case PagerAdapter.WORK_PATH_FRAGMENT:
						toolBarLayoutColorId = R.color.tab1_tablayout_background;
						toolBarColorId = R.color.tab1_actionbar_background;
						tabLayoutColorId = R.color.tab1_tablayout_background;
						tabIndicatorColorId = R.color.tab1_tabindicator_background;
						break;
					case PagerAdapter.WELD_COUNT_FRAGMENT:
						toolBarLayoutColorId = R.color.tab2_tablayout_background;
						toolBarColorId = R.color.tab2_actionbar_background;
						tabLayoutColorId = R.color.tab2_tablayout_background;
						tabIndicatorColorId = R.color.tab2_tabindicator_background;
						break;
					case PagerAdapter.WELD_CONDITION_FRAGMENT:
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

	public boolean onExitDialog() {
		Helper.UiHelper.adMobExitDialog(this);
		return true;
		//super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		mBackPressedCount = 0;
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
			show(getResources().getString(R.string.action_settings));
			return true;
		}
		if (id == R.id.action_backup) {
			final FloatingActionButton fab = (FloatingActionButton) getFab();
			if (fab != null && android.os.Build.VERSION.SDK_INT
					>= android.os.Build.VERSION_CODES.LOLLIPOP) {
				if (mTabLayout.getSelectedTabPosition() == PagerAdapter.WORK_PATH_FRAGMENT) {
					ActivityOptions options = ActivityOptions
							.makeSceneTransitionAnimation(this, fab, "fab");
					startActivity(new Intent(this, BackupActivity.class), options.toBundle());
				} else {
					final Intent intent = new Intent(this, BackupActivity.class);
					final ActivityOptions options = ActivityOptions
							.makeSceneTransitionAnimation(this, fab, "fab");
					TranslateAnimation translateAnimation = new TranslateAnimation(
							TranslateAnimation.RELATIVE_TO_SELF, 0f,
							TranslateAnimation.ABSOLUTE, (mTabLayout.getRight() - fab.getRight()) - fab.getLeft(),
							TranslateAnimation.RELATIVE_TO_SELF, 0f,
							TranslateAnimation.RELATIVE_TO_SELF, 0f);
					translateAnimation.setDuration(400);
					translateAnimation.setInterpolator(new DecelerateInterpolator());
					translateAnimation.setAnimationListener(new Animation.AnimationListener() {
						@Override
						public void onAnimationStart(Animation animation) {

						}

						@Override
						public void onAnimationEnd(Animation animation) {
							startActivity(intent, options.toBundle());
						}

						@Override
						public void onAnimationRepeat(Animation animation) {

						}
					});
					fab.startAnimation(translateAnimation);
				}
			} else {
				startActivity(new Intent(this, BackupActivity.class));
			}
			return true;
		}
//		if (id == R.id.action_exit) {
//			finish();
//			return true;
//		}

		return super.onOptionsItemSelected(item);
	}

	private View getFab() {
		View fab = null;
		try {
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter())
					.getItem(mTabLayout.getSelectedTabPosition());
			if (refresh != null)
				fab = refresh.getFab();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fab;
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		mBackPressedCount = 0;

		// Handle navigation view item clicks here.
		int id = item.getItemId();
		if (id == R.id.nav_weld_count) {
			mViewPager.setCurrentItem(PagerAdapter.WELD_COUNT_FRAGMENT, true);
		} else if (id == R.id.nav_weld_condition) {
			mViewPager.setCurrentItem(PagerAdapter.WELD_CONDITION_FRAGMENT, true);
		} else if (id == R.id.nav_storage) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_sdcard) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_extsdcard) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_usbstorage) {
			mViewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_backup) {
			startActivity(new Intent(MainActivity.this, BackupActivity.class));
		} else if (id == R.id.nav_exit) {
			finish();
		}
		mDrawer.closeDrawer(GravityCompat.START);

		try {
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter())
					.getItem(mTabLayout.getSelectedTabPosition());
			if (refresh != null)
				show(refresh.refresh(id));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void onPathChanged(File path) {
		logD("onPathChanged");
		WorkPathFragment fragment = (WorkPathFragment) ((PagerAdapter) mViewPager.getAdapter())
				.getItem(PagerAdapter.WORK_PATH_FRAGMENT);
		if (fragment != null)
			fragment.onPathChanged(path.getPath());
	}

	@Override
	public String onGetWorkPath() {
		return Helper.Pref.getWorkPath(getContext());
	}

	@Override
	public void onSetWorkPath(String path) {
		logD("onSetWorkPath");
		Helper.Pref.setWorkPath(getContext(), path);

		final int[] tabs = { PagerAdapter.WELD_COUNT_FRAGMENT, PagerAdapter.WELD_CONDITION_FRAGMENT };
		for (int tab : tabs) {
			Refresh refresh = (Refresh) ((PagerAdapter) mViewPager.getAdapter()).getItem(tab);
			if (refresh != null)
				refresh.refresh(true);
		}
	}

	public static class PagerAdapter extends FragmentStatePagerAdapter {
		final static int WORK_PATH_FRAGMENT = 0;
		final static int WELD_COUNT_FRAGMENT = 1;
		final static int WELD_CONDITION_FRAGMENT = 2;
		final static int NUM_OF_TABS = 3;

		private Context mContext;
		private Fragment[] mFragments;

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
					case WORK_PATH_FRAGMENT:
						mFragments[position] = new WorkPathFragment();
						break;
					case WELD_COUNT_FRAGMENT:
						mFragments[position] = new WeldCountFragment();
						break;
					case WELD_CONDITION_FRAGMENT:
						mFragments[position] = new WeldConditionFragment();
						break;

				}
			}
			return mFragments[position];
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case WORK_PATH_FRAGMENT:
					return mContext.getResources().getString(R.string.work_path_fragment);
				case WELD_COUNT_FRAGMENT:
					return mContext.getResources().getString(R.string.weld_count_fragment);
				case WELD_CONDITION_FRAGMENT:
					return mContext.getResources().getString(R.string.weld_condition_fragment);
			}
			return super.getPageTitle(position);
		}
	}
}
