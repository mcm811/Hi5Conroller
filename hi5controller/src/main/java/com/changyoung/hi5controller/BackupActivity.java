package com.changyoung.hi5controller;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.IOException;

public class BackupActivity extends AppCompatActivity implements Refresh, FileListFragment.OnPathChangedListener {
	private int mBackPressedCount;

	private void logDebug(String msg) {
		try {
			Log.d(getPackageName(), "BackupActivity: " + msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	BackupActivity getContext() {
		return this;
	}

	BackupActivity getActivity() {
		return this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_backup);
		BackupActivity view = this;

		String path = Pref.getBackupPath(getContext());
		final FileListFragment fragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.backup_path_fragment);
		if (fragment != null) {
			fragment.refreshFilesList(path);
			fragment.snackbarView = view.findViewById(R.id.coordinator_layout);
			final EditText etPath = (EditText) view.findViewById((R.id.etBackupPath));
			if (etPath != null) {
				etPath.setText(path);
				etPath.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						try {
							File file = new File(etPath.getText().toString());
							if (file.isDirectory()) {
								Pref.setBackupPath(getContext(), etPath.getText().toString());
							} else {
								throw new Exception();
							}
						} catch (Exception e) {
							e.printStackTrace();
							show("잘못된 경로: " + etPath.getText().toString());
							etPath.setText(Pref.getBackupPath(getContext()));
						}
						fragment.refreshFilesList(etPath.getText().toString());
					}
				});
				etPath.setOnKeyListener(new View.OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						Util.UiUtil.hideSoftKeyboard(getActivity(), v, event);
						return false;
					}
				});
			}
		}

		Toolbar toolbar = (Toolbar) view.findViewById(R.id.backup_path_toolbar);
		toolbar.inflateMenu(R.menu.menu_toolbar_back_path);
		toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				String ret = refresh(item.getItemId());
				if (ret != null)
					show(ret);
				mBackPressedCount = 0;
				return true;
			}
		});

		AdView adView = (AdView) view.findViewById(R.id.adView);
		AdRequest adRequest = new AdRequest.Builder()
				.setRequestAgent("android_studio:ad_template").build();
		adView.loadAd(adRequest);

		try {
			findViewById(R.id.action_bar).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onBackPressed();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			logDebug("onBackPressed()");
		}

		FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
		if (fab != null) {
			fab.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					Util.UiUtil.hideSoftKeyboard(getActivity(), null, null);
					mBackPressedCount = 0;
					show(backup());
				}
			});
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_backup, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		if (id == R.id.action_restore) {
			show(restore());
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void refresh(boolean forced) {
		try {
			if (forced) {
				final EditText etPath = (EditText) findViewById((R.id.etBackupPath));
				final FileListFragment fragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.backup_path_fragment);
				etPath.setText(Pref.getBackupPath(getContext()));
				fragment.refreshFilesList(etPath.getText().toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean refresh(String path) {
		if (path != null) {
			try {
				File dir = new File(path);
				if (dir.isDirectory()) {
					final FileListFragment fragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.backup_path_fragment);
					fragment.refreshFilesList(dir);
				}
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	public String refresh(int menuId) {
		switch (menuId) {
			case R.id.toolbar_backup_path_menu_home:
				if (!refresh(Pref.getBackupPath(getContext())))
					show("백업 폴더가 없습니다");
				break;
			case R.id.toolbar_backup_path_menu_done:
				finish();
				break;
		}

		return null;
	}

	private String restore() {
		String ret = null;

		try {
			final FileListFragment fragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.backup_path_fragment);
			File source = fragment.getDirFile();
			File dest = new File(Pref.getWorkPath(getContext()));

			// 복원할 파일을 먼저 확인
			boolean sourceChecked = false;
			if (source.exists()) {
				for (File file : source.listFiles()) {
					String fileName = file.getName().toUpperCase();
					if (fileName.startsWith("HX") || fileName.endsWith("JOB") || fileName.startsWith("ROBOT")) {
						sourceChecked = true;
						break;
					}
				}
				if (sourceChecked) {
					if (dest.exists())
						Util.FileUtil.delete(dest, false);
					if (dest.mkdirs())
						logDebug("dest.mkdir");
					Util.FileUtil.copy(source, dest, false);
					ret = "복원 완료: " + source.getName();
				}
			}
			if (ret == null)
				throw new Exception();
		} catch (IOException e) {
			e.printStackTrace();
			ret = "복원 실패: 파일 복사 오류";
		} catch (Exception e) {
			e.printStackTrace();
			ret = "복원 실패";
		}

		return ret;
	}

	public String backup() {
		String ret = null;

		try {
			File source = new File(Pref.getWorkPath(getContext()));
			File dest = new File(source.getPath() + "/Backup/" + source.getName() + Util.TimeUtil.getTimeString("_yyyyMMdd_HHmmss", System.currentTimeMillis()));

			if (source.exists() && source.isDirectory()) {
				if (dest.exists())
					Util.FileUtil.delete(dest, false);
				if (dest.mkdirs())
					logDebug("dest.mkdir");
				Util.FileUtil.copy(source, dest, false);
				ret = "백업 완료: " + dest.getName();
			}
			if (ret == null)
				throw new Exception();
			refresh(Pref.getBackupPath(getContext()));
		} catch (IOException e) {
			e.printStackTrace();
			ret = "백업 실패: 파일 복사 오류";
		} catch (Exception e) {
			e.printStackTrace();
			ret = "백업 실패";
		}

		return ret;
	}

	@Override
	public String onBackPressedFragment() {
		final FileListFragment fragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.backup_path_fragment);
		return fragment.refreshParent();
	}

	@Override
	public void onBackPressed() {
		final int EXIT_COUNT = 0;
		try {
			String ret = onBackPressedFragment();
			if (ret == null || ret.compareTo("/") == 0)
				throw new Exception();
			else
				mBackPressedCount = 0;
		} catch (Exception e) {
			if (++mBackPressedCount > EXIT_COUNT)
				super.onBackPressed();
			else
				show(String.format(getResources().getString(R.string.main_activity_exit_format), Integer.toString(EXIT_COUNT - mBackPressedCount + 1)));
		}
	}

	@Override
	public void show(String msg) {
		try {
			if (msg == null)
				return;
			Snackbar.make(findViewById(R.id.coordinator_layout), msg, Snackbar.LENGTH_SHORT)
					.setAction("Action", null).show();
			logDebug(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onPathChanged(File path) {
		EditText etPath = (EditText) findViewById(R.id.etBackupPath);
		etPath.setText(path.getPath());
/*
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		mWorkPath = onGetWorkPath();
		if (mWorkPath.compareTo(path) == 0) {
			fab.setImageResource(R.drawable.ic_home_white);
		} else {
			fab.setImageResource(R.drawable.ic_done_white);
		}
*/
	}
}
