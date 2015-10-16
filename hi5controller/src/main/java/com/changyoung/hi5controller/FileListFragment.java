package com.changyoung.hi5controller;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnPathChangedListener} interface
 * to handle interaction events.
 * Use the {@link FileListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileListFragment extends android.support.v4.app.Fragment {
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String TAG = "FileListFragment";
	private static final String ARG_DIR_PATH = "dirPath";
	public View snackbarView;
	View mView;
	private OnPathChangedListener mListener;

	private ListView listView;
	private FileListAdapter adapter;
	private File dirPath;

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

	private void logDebug(String msg) {
		try {
			Log.d(getActivity().getPackageName(), "FileListFragment: " + msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void show(String msg) {
		try {
			if (snackbarView != null)
				Snackbar.make(snackbarView, msg, Snackbar.LENGTH_SHORT).show();
			logDebug(msg);
		} catch (Exception e) {
			logDebug(msg);
		}
	}

	public String getDirPath() {
		return dirPath.getPath();
	}

	public void setDirPath(String value) {
		this.dirPath = new File(value);
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
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
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
					Util.UiUtil.textViewActivity(getContext(), file.getName(), Util.FileUtil.readFileString(file.getPath()));
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
				String fileType = file.isDirectory() ? "이 폴더를 " : "이 파일을 ";
				String msg = fileType + "완전히 삭제 하시겠습니까?\n\n" + file.getName() + "\n\n수정한 날짜: " + Util.TimeUtil.getLasModified(file);

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(actionName)
						.setMessage(msg)
						.setNegativeButton("취소", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								refreshFilesList(getDirPath());
								show("삭제가 취소 되었습니다");

							}
						})
						.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try {
									Util.FileUtil.delete(file, true);
									refreshFilesList(getDirPath());
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
		} catch (NullPointerException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getDirPath();
	}

	public String refreshFilesList(File dir) {
		try {
			if (dir == null)
				dir = dirPath;

//			if (adapter == null)
//				return null;

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
			this.dirPath = dir;
			adapter.insert(dir, 0);
			adapter.notifyDataSetChanged();
			listView.refreshDrawableState();
			onDirPathChanged(dirPath);
		} catch (NullPointerException e) {
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
//			throw new ClassCastException(context.toString()
//					+ " must implement OnPathChangedListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
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
}
