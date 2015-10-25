package com.changyoung.hi5controller;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnPathChangedListener} interface
 * to handle interaction events.
 * Use the {@link FileListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileListFragment extends Fragment {
	private static final int MSG_REFRESH_DIR = 0;
	private static final int MSG_REFRESH_PARENT_DIR = 1;

	private static final String TAG = "FileListFragment";
	private static final String ARG_DIR_PATH = "dirPath";
	public View snackbarView;
	private View mView;
	private OnPathChangedListener mListener;

	private ListView listView;
	private FileListAdapter adapter;

	private File dirPath;
	private FileListObserver fileListObserver;
	private LooperHandler looperHandler;

	public FileListFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param path Parameter 1.
	 * @return A new instance of fragment FileListFragment.
	 */
	public static FileListFragment newInstance(String path) {
		FileListFragment fragment = new FileListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_DIR_PATH, path);
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

	private void show(String msg) {
		try {
			if (snackbarView != null)
				Snackbar.make(snackbarView, msg, Snackbar.LENGTH_SHORT).show();
			logD(msg);
		} catch (Exception e) {
			logD(msg);
		}
	}

	public String getDirPath() {
		return dirPath.getPath();
	}

	public void setDirPath(String value) {
		this.dirPath = new File(value);
	}

	public void setDirPath(File value) {
		this.dirPath = value;
	}

	public File getDirFile() {
		return dirPath;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			setDirPath(getArguments().getString(ARG_DIR_PATH));
		} else {
			setDirPath(Environment.getExternalStorageDirectory().getAbsolutePath());
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, final ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		mView = inflater.inflate(R.layout.fragment_file_list, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) mView.findViewById(R.id.srl);
		if (refresher != null) {
			refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
				@Override
				public void onRefresh() {
					refreshFilesList(getDirPath());
					refresher.setRefreshing(false);
				}
			});
		}

		adapter = new FileListAdapter(getActivity(), new ArrayList<File>());
		listView = (ListView) mView.findViewById(R.id.listView);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File file = (File) parent.getAdapter().getItem(position);
				if (position == 0) {
					String p = file.getParent();
					if (p == null)
						p = File.pathSeparator;
					refreshFilesList(p);
				} else if (file.isDirectory()) {
					refreshFilesList(file.getPath());
				} else {
					Helper.UiHelper.textViewActivity(getContext(), file.getName(),
							Helper.FileHelper.readFileString(file.getPath()));
				}
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0)
					return false;

				final File file = (File) parent.getAdapter().getItem(position);
				String actionName = file.isDirectory() ? "폴더 삭제" : "파일 삭제";
				String fileType = file.isDirectory() ? "이 폴더를" : "이 파일을";
				String msg = String.format("%s 완전히 삭제 하시겠습니까?\n\n%s\n\n수정한 날짜: %s",
						fileType, file.getName(), Helper.TimeHelper.getLasModified(file));

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(actionName)
						.setMessage(msg)
						.setNegativeButton("취소", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								show("삭제가 취소 되었습니다");

							}
						})
						.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									new Helper.AsyncTaskFileDialog(getContext(), snackbarView, "삭제", looperHandler)
											.execute(file);
								} catch (Exception e) {
									e.printStackTrace();
									show("삭제할 수 없습니다");
								}
							}
						});
				builder.create().show();

				return true;
			}
		});

		looperHandler = new LooperHandler(Looper.getMainLooper());

		return mView;
	}

	public String refreshParent() {
		return refreshFilesList(dirPath.getParent());
	}

	public String refreshFilesList() {
		final File file = null;
		return refreshFilesList(file);
	}

	public String refreshFilesList(String path) {
		try {
			if (path == null)
				path = getDirPath();
			return refreshFilesList(new File(path));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getDirPath();
	}

	public String refreshFilesList(File dir) {
		try {
			if (dir == null)
				dir = dirPath;

			Log.d(TAG, dir.getName());

			adapter.clear();
			for (File item : dir.listFiles()) {
				if (item.isFile() || item.isDirectory())
					adapter.add(item);
			}
			adapter.sort(new Comparator<File>() {
				public int compare(File obj1, File obj2) {
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
				}
			});

			setDirPath(dir);
			adapter.insert(dir, 0);
			adapter.notifyDataSetChanged();
			listView.refreshDrawableState();
			onDirPathChanged(dir);

			fileListObserver = new FileListObserver(dir, looperHandler);
			fileListObserver.startWatching();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			refreshFilesList(getDirPath());
		}

		return getDirPath();
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshFilesList(getDirPath());
	}

	public void onDirPathChanged(File path) {
		if (mListener != null) {
			mListener.onPathChanged(path);
		}
	}

	@Override
	public void onAttach(Context context) {
		super.onAttach(context);
		try {
			mListener = (OnPathChangedListener) context;
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ClassCastException(context.toString()
					+ " must implement OnPathChangedListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
		mView = null;
		snackbarView = null;
		listView = null;
		adapter.clear();
		adapter = null;
		dirPath = null;
		looperHandler = null;
		fileListObserver.stopWatching();
		fileListObserver = null;
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
	public interface OnPathChangedListener {
		void onPathChanged(File path);
	}

	public static class FileListObserver extends FileObserver {
		static final String TAG = "FileListObserver";
		static final int mask = CREATE | DELETE | DELETE_SELF |
				MOVED_FROM | MOVED_TO | MOVE_SELF | CLOSE_WRITE;
		File file;
		private Handler handler;

		public FileListObserver(File file, Handler handler) {
			super(file.getPath(), mask);
			this.file = file;
			this.handler = handler;
			Log.d(TAG, "FILE_OBSERVER: " + file.getPath());
		}

		public void onEvent(int event, String path) {
			if ((event & CREATE) == CREATE)
				Log.d(TAG, String.format("CREATE: %s/%s", file.getPath(), path));
			else if ((event & DELETE) == DELETE)
				Log.d(TAG, String.format("DELETE: %s/%s", file.getPath(), path));
			else if ((event & DELETE_SELF) == DELETE_SELF)
				Log.d(TAG, String.format("DELETE_SELF: %s/%s", file.getPath(), path));
			else if ((event & MOVED_FROM) == MOVED_FROM)
				Log.d(TAG, String.format("MOVED_FROM: %s/%s", file.getPath(), path));
			else if ((event & MOVED_TO) == MOVED_TO)
				Log.d(TAG, String.format("MOVED_TO: %s", path == null ? file.getPath() : path));
			else if ((event & MOVE_SELF) == MOVE_SELF)
				Log.d(TAG, String.format("MOVE_SELF: %s", path == null ? file.getPath() : path));
			else if ((event & CLOSE_WRITE) == CLOSE_WRITE)
				Log.d(TAG, String.format("CLOSE_WRITE: %s", path == null ? file.getPath() : path));
			else
				return;

			stopWatching();
			Message msg = handler.obtainMessage();
			msg.what = MSG_REFRESH_DIR;
			msg.obj = file;
			handler.sendMessage(msg);
		}
	}

	public class FileListAdapter extends ArrayAdapter<File> {
		private Activity mContext;

		public FileListAdapter(Activity context, List<File> objects) {
			super(context, R.layout.list_item_file, objects);
			mContext = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;
			View row = convertView;

			if (row == null) {
				row = mContext.getLayoutInflater().inflate(R.layout.list_item_file, parent, false);
				viewHolder = new ViewHolder((TextView) row.findViewById(R.id.file_picker_time),
						(TextView) row.findViewById(R.id.file_picker_text),
						(ImageView) row.findViewById(R.id.file_picker_image),
						(FloatingActionButton) row.findViewById(R.id.file_picker_fab));
				row.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) row.getTag();
			}

			File file = getItem(position);
			if (position == 0) {
				String p = file.getParent();
				if (p == null) {
					viewHolder.update(null, ".", R.drawable.ic_android);
				} else {
					viewHolder.update(null, file.getParentFile().getName() + "/..", R.drawable.ic_file_upload);
				}
			} else {
				viewHolder.update(Helper.TimeHelper.getLasModified(file), file.getName(), file.isFile() ? R.drawable.ic_description : R.drawable.ic_folder_open);
			}

			return row;
		}

		public class ViewHolder {
			private TextView TimeTextView;
			private android.widget.TextView TextView;
			private android.widget.ImageView ImageView;
			private FloatingActionButton Fab;

			public ViewHolder(TextView timeTextView, TextView textView, ImageView imageView, FloatingActionButton fab) {
				TimeTextView = timeTextView;
				TextView = textView;
				ImageView = imageView;
				Fab = fab;
			}

			public void update(String fileTime, String fileName, int fileImageResourceId) {
				if (fileTime == null) {
					TimeTextView.setText("");
					TimeTextView.setVisibility(View.GONE);
				} else {
					TimeTextView.setText(fileTime);
					TimeTextView.setVisibility(View.VISIBLE);
				}
				TextView.setText(fileName);
				ImageView.setImageResource(fileImageResourceId);
				Fab.setImageResource(fileImageResourceId);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					Fab.setVisibility(View.VISIBLE);
					ImageView.setVisibility(View.GONE);
				} else {
					Fab.setVisibility(View.GONE);
					ImageView.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	private class LooperHandler extends Handler {
		public LooperHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MSG_REFRESH_DIR:
					Log.d(TAG, "MSG_REFRESH_DIR");
					if (msg.obj == null)
						refreshFilesList();
					else
						refreshFilesList((File) msg.obj);
					break;
				case MSG_REFRESH_PARENT_DIR:
					Log.d(TAG, "MSG_REFRESH_PARENT_DIR");
					if (msg.obj == null)
						refreshParent();
					else
						refreshFilesList(((File) msg.obj).getParentFile());
					break;
				default:
					break;
			}
		}
	}
}
