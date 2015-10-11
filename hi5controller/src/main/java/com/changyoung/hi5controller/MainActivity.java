package com.changyoung.hi5controller;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private int mBackPressedCount;

	private void Show(String msg) {
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		Snackbar.make(viewPager, msg, Snackbar.LENGTH_SHORT)
				.setAction("Action", null).show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
				mBackPressedCount = 0;
			}
		});
		fab.setVisibility(View.GONE);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				MainActivity.this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.setDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(MainActivity.this);

		TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
		tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
		tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

		PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), getApplicationContext());
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		viewPager.setAdapter(pagerAdapter);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
		tabLayout.setupWithViewPager(viewPager);
		tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
			TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);

			int toolBarColorId = R.color.tab1_actionbar_background;
			int tabLayoutColorId = R.color.tab1_tablayout_background;
			int tabIndicatorColorId = R.color.tab1_tabindicator_background;

			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
				}
				toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), toolBarColorId));
				tabLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
				tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), tabIndicatorColorId));
			}

			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				mBackPressedCount = 0;
				switch (tab.getPosition()) {
					case PagerAdapter.WORK_PATH_FRAGMENT:
						toolBarColorId = R.color.tab1_actionbar_background;
						tabLayoutColorId = R.color.tab1_tablayout_background;
						tabIndicatorColorId = R.color.tab1_tabindicator_background;
						break;
					case PagerAdapter.WELD_COUNT_FRAGMENT:
						toolBarColorId = R.color.tab2_actionbar_background;
						tabLayoutColorId = R.color.tab2_tablayout_background;
						tabIndicatorColorId = R.color.tab2_tabindicator_background;
						break;
					case PagerAdapter.WELD_CONDITION_FRAGMENT:
						toolBarColorId = R.color.tab3_actionbar_background;
						tabLayoutColorId = R.color.tab3_tablayout_background;
						tabIndicatorColorId = R.color.tab3_tabindicator_background;
						break;
					case PagerAdapter.BACKUP_PATH_FRAGMENT:
						toolBarColorId = R.color.tab4_actionbar_background;
						tabLayoutColorId = R.color.tab4_tablayout_background;
						tabIndicatorColorId = R.color.tab4_tabindicator_background;
						break;
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
				}
				toolbar.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), toolBarColorId));
				tabLayout.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), tabLayoutColorId));
				tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(getApplicationContext(), tabIndicatorColorId));
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {

			}
		});
		findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(MainActivity.this, BackupActivity.class));
			}
		});
	}

	@Override
	public void onBackPressed() {
		final int EXIT_COUNT = 2;
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
			mBackPressedCount = 0;
		} else {
			if (mBackPressedCount <= EXIT_COUNT) {
				Show(String.format(getResources().getString(R.string.main_activity_exit_format), Integer.toString(EXIT_COUNT - mBackPressedCount + 1)));
			}
			if (mBackPressedCount++ > EXIT_COUNT)
				super.onBackPressed();
		}
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
			Show(getResources().getString(R.string.action_settings));
			return true;
		} else if (id == R.id.action_backup) {
			startActivity(new Intent(MainActivity.this, BackupActivity.class));
//            viewPager.setCurrentItem(PagerAdapter.BACKUP_PATH_FRAGMENT, true);
//            Show(getResources().getString(R.string.action_backup));
			return true;
		} else if (id == R.id.action_exit) {
			finish();
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		mBackPressedCount = 0;

		// Handle navigation view item clicks here.
		int id = item.getItemId();
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		if (id == R.id.nav_weld_count) {
			viewPager.setCurrentItem(PagerAdapter.WELD_COUNT_FRAGMENT, true);
			Show(getResources().getString(R.string.nav_weld_count));
		} else if (id == R.id.nav_weld_condition) {
			viewPager.setCurrentItem(PagerAdapter.WELD_CONDITION_FRAGMENT, true);
			Show(getResources().getString(R.string.nav_weld_condition));
		} else if (id == R.id.nav_storage) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
			Show(getResources().getString(R.string.nav_storage));
		} else if (id == R.id.nav_sdcard) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
			Show(getResources().getString(R.string.nav_sdcard));
		} else if (id == R.id.nav_extsdcard) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
			Show(getResources().getString(R.string.nav_extsdcard));
		} else if (id == R.id.nav_usbstorage) {
			viewPager.setCurrentItem(PagerAdapter.WORK_PATH_FRAGMENT, true);
			Show(getResources().getString(R.string.nav_usbstorage));
		} else if (id == R.id.nav_backup) {
			startActivity(new Intent(MainActivity.this, BackupActivity.class));
		} else if (id == R.id.nav_exit) {
			finish();
		}

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		return true;
	}

	public class PagerAdapter extends FragmentStatePagerAdapter {
		public final static int WORK_PATH_FRAGMENT = 0;
		public final static int WELD_COUNT_FRAGMENT = 1;
		public final static int WELD_CONDITION_FRAGMENT = 2;
		public final static int BACKUP_PATH_FRAGMENT = 3;
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
				Log.d("fragment: ", Integer.toString(position));
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
					case BACKUP_PATH_FRAGMENT:
						mFragments[position] = new BackupPathFragment();
						break;

				}
			}
			Log.d("return: ", Integer.toString(position));
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
//                case BACKUP_PATH_FRAGMENT:
//                    return mContext.getResources().getString(R.string.backup_path_fragment);
			}

			return super.getPageTitle(position);
		}
	}
}
