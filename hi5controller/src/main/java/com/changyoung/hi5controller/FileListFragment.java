package com.changyoung.hi5controller;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.Uri;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FileListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FileListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FileListFragment extends android.support.v4.app.Fragment {
	// TODO: Rename parameter arguments, choose names that match
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_PARAM1 = "dirPath";
	private static final String ARG_PARAM2 = "param2";

	public String prefKey;
	public View snackbarView;

	// TODO: Rename and change types of parameters
	private String mParam1;
	private String mParam2;
	private OnFragmentInteractionListener mListener;

	private View view;
	private ListView listView;
	private FileListAdapter adapter;
	private File dirPath;

	public FileListFragment() {
		// Required empty public constructor
		logDebug("constructor");
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param param1 Parameter 1.
	 * @param param2 Parameter 2.
	 * @return A new instance of fragment FileListFragment.
	 */
	// TODO: Rename and change types and number of parameters
	public static FileListFragment newInstance(String param1, String param2) {
		FileListFragment fragment = new FileListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_PARAM1, param1);
		args.putString(ARG_PARAM2, param2);
		fragment.setArguments(args);
		return fragment;
	}

	private void logDebug(String msg) {
		try {
			Log.d(getActivity().getPackageName(), "FileListFragment: " + msg);
		} catch (Exception e) {
		}
	}

	private void show(String msg) {
		try {
			if (snackbarView != null)
				Snackbar.make(snackbarView, msg, Snackbar.LENGTH_SHORT).show();
			logDebug(msg);
		} catch (Exception e) {
		}
	}

	public String getDirPath() {
		return dirPath.getPath();
	}

	public void setDirPath(String value) {
		this.dirPath = new File(value);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mParam1 = getArguments().getString(ARG_PARAM1);
			mParam2 = getArguments().getString(ARG_PARAM2);
		}
		setDirPath(Environment.getExternalStorageDirectory().getAbsolutePath());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		view = inflater.inflate(R.layout.fragment_file_list, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) view.findViewById(R.id.srl);
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
		listView = (ListView) view.findViewById(R.id.listView);
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
//					Pref.textViewDialog(getContext(), Pref.readFileString(file.getPath()));
					Pref.textViewActivity(getContext(), file.getName(), Pref.readFileString(file.getPath()));
				}
			}
		});
		listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (position == 0)
					return false;

				// TODO: 디렉토리 삭제 구현 해야됨 (제귀 호출)
				final File file = (File) parent.getAdapter().getItem(position);
				String actionName = file.isDirectory() ? "폴더 삭제" : "파일 삭제";
				String fileType = file.isDirectory() ? "이 폴더를 " : "이 파일을 ";
				String msg = fileType + "완전히 삭제 하시겠습니까?\n\n" + file.getName() + "\n\n수정한 날짜: " + new SimpleDateFormat("yyy-dd-MM a hh-mm-ss").format(new Date(file.lastModified()));

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
									file.delete();
									refreshFilesList(getDirPath());
								} catch (Exception e) {
									show("삭제할 수 없습니다");
								}
							}
						});
				builder.create().show();

				return true;
			}
		});

		return view;
	}

	public void refreshFilesList(String path) {
		try {
			if (path == null)
				path = getDirPath();
			File dir = new File(path);

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
			Pref.setPath(getContext(), prefKey, dir.getPath());

			adapter.insert(dir, 0);
			adapter.notifyDataSetChanged();
			listView.refreshDrawableState();
		} catch (Exception e) {
			refreshFilesList(getDirPath());
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshFilesList(getDirPath());
	}

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (OnFragmentInteractionListener) activity;
		} catch (ClassCastException e) {
//			throw new ClassCastException(activity.toString()
//					+ " must implement OnFragmentInteractionListener");
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
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		void onFragmentInteraction(Uri uri);
	}
}
