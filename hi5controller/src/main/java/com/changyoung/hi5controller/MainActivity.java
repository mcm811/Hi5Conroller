package com.changyoung.hi5controller;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
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

import java.io.File;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener,
		FileListFragment.OnPathChangedListener, WorkPathFragment.OnWorkPathListener,
		WeldCountFragment.OnWorkPathListener, WeldConditionFragment.OnWorkPathListener {

	private final static String TAG = "MainActivity";

	private int mBackPressedCount;

	MainActivity getContext() {
		return this;
	}

	private void logD(String msg) {
		try {
			Log.d(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void show(String msg) {
		try {
			if (msg == null)
				return;
			ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
			TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
			Refresh refresh = (Refresh) ((PagerAdapter) viewPager.getAdapter()).getItem(tabLayout.getSelectedTabPosition());
			if (refresh != null)
				refresh.show(msg);
		} catch (Exception e) {
			e.printStackTrace();
			ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
			Snackbar.make(viewPager, msg, Snackbar.LENGTH_SHORT)
					.setAction("Action", null).show();
			logD(msg);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		if (fab != null) {
			fab.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
							.setAction("Action", null).show();
					mBackPressedCount = 0;
				}
			});
			findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(MainActivity.this, BackupActivity.class));
				}
			});
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer != null) {
			ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
					MainActivity.this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
			drawer.setDrawerListener(toggle);
			toggle.syncState();
		}

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(MainActivity.this);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), getApplicationContext());
		final ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		viewPager.setAdapter(pagerAdapter);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
		tabLayout.setupWithViewPager(viewPager);
		tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
			Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
			TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

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
				if (tabLayout != null) {
					tabLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
					tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), tabIndicatorColorId));
				}
			}

			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				// 탭을 터치 했을때 뷰페이지 이동
				((ViewPager) findViewById(R.id.view_pager)).setCurrentItem(tab.getPosition(), true);
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
				if (tabLayout != null) {
					tabLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
					tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), tabIndicatorColorId));
				}
				Helper.UiHelper.hideSoftKeyboard(getContext(), null, null);
//				try {
//					Refresh refresh = (Refresh) ((PagerAdapter) viewPager.getAdapter()).getItem(tab.getPosition());
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
		Log.d(TAG, "onSaveInstanceState");
		super.onSaveInstanceState(outState, outPersistentState);
	}

	@Override
	public void onBackPressed() {
		final int EXIT_COUNT = 0;
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
			mBackPressedCount = 0;
		} else {
			try {
				ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
				TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
				Refresh refresh = (Refresh) ((PagerAdapter) viewPager.getAdapter()).getItem(tabLayout.getSelectedTabPosition());
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
					show(String.format(getResources().getString(R.string.main_activity_exit_format), Integer.toString(EXIT_COUNT - mBackPressedCount + 1)));
				}
			}
		}
	}

	public void onExitDialog() {
		Helper.UiHelper.adMobExitDialog(this);
		//super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mBackPressedCount = 0;

		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
			show(getResources().getString(R.string.action_settings));
			return true;
		}
		if (id == R.id.action_backup) {
			startActivity(new Intent(MainActivity.this, BackupActivity.class));
			return true;
		}
//		if (id == R.id.action_exit) {
//			finish();
//			return true;
//		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		mBackPressedCount = 0;

		// Handle navigation view item clicks here.
		int id = item.getItemId();
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		if (id == R.id.nav_weld_count) {
			viewPager.setCurrentItem(PagerAdapter.WELD_COUNT_FRAGMENT, true);
		} else if (id == R.id.nav_weld_condition) {
			viewPager.setCurrentItem(PagerAdapter.WELD_CONDITION_FRAGMENT, true);
		} else if (id == R.id.nav_storage) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_sdcard) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_extsdcard) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_usbstorage) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
		} else if (id == R.id.nav_backup) {
			startActivity(new Intent(MainActivity.this, BackupActivity.class));
		} else if (id == R.id.nav_exit) {
			finish();
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		try {
			TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
			Refresh refresh = (Refresh) ((PagerAdapter) viewPager.getAdapter()).getItem(tabLayout.getSelectedTabPosition());
			if (refresh != null)
				show(refresh.refresh(id));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void onPathChanged(File path) {
		Log.d(TAG, "onPathChanged");
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		WorkPathFragment fragment = (WorkPathFragment) ((PagerAdapter) viewPager.getAdapter()).getItem(PagerAdapter.WORK_PATH_FRAGMENT);
		if (fragment != null)
			fragment.onPathChanged(path.getPath());
	}

	@Override
	public String onGetWorkPath() {
		return Helper.Pref.getWorkPath(getContext());
	}

	@Override
	public void onSetWorkPath(String path) {
		Log.d(TAG, "onSetWorkPath");
		Helper.Pref.setWorkPath(getContext(), path);
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);

		final int[] tabs = {PagerAdapter.WELD_COUNT_FRAGMENT, PagerAdapter.WELD_CONDITION_FRAGMENT};
		for (int tab : tabs) {
			Refresh refresh = (Refresh) ((PagerAdapter) viewPager.getAdapter()).getItem(tab);
			if (refresh != null)
				refresh.refresh(true);
		}
	}

	public static class PagerAdapter extends FragmentStatePagerAdapter {
		public final static int WORK_PATH_FRAGMENT = 0;
		public final static int WELD_COUNT_FRAGMENT = 1;
		public final static int WELD_CONDITION_FRAGMENT = 2;
		public final static int NUM_OF_TABS = 3;

		private Context mContext;
		private Fragment[] mFragments;

		public PagerAdapter(FragmentManager fm, Context context) {
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
