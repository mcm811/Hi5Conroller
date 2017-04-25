package com.changyoung.hi5controller.weldfile;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.changyoung.hi5controller.R;

import java.io.File;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnPathChangedListener} interface
 * to handle interaction events.
 * Use the {@link WeldFileListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WeldFileListFragment extends Fragment {
	public static final int MSG_DEFAULT = 100;
	public static final int MSG_REFRESH_DIR = 101;
	private static final int MSG_REFRESH_PARENT_DIR = 102;

	private static final String TAG = "WeldFileListFragment";
	private static final String ARG_DIR_PATH = "dirPath";
	public View snackbarView;
	LooperHandler looperHandler;
	private View mView;
	private OnPathChangedListener mListener;
	private RecyclerView mRecyclerView;
	private WeldFileListAdapter mAdapter;
	private File dirPath;
	private WeldFileListObserver weldFileListObserver;

	public WeldFileListFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param path Parameter 1.
	 * @return A new instance of fragment WeldFileListFragment.
	 */
	public static WeldFileListFragment newInstance(String path) {
		WeldFileListFragment fragment = new WeldFileListFragment();
		Bundle args = new Bundle();
		args.putString(ARG_DIR_PATH, path);
		fragment.setArguments(args);
		return fragment;
	}

	static void logD(String msg) {
		try {
			Log.i("HI5", TAG + ":" + msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void show(String msg) {
		try {
			if (snackbarView != null)
				Snackbar.make(snackbarView, msg, Snackbar.LENGTH_LONG).show();
			logD(msg);
		} catch (Exception e) {
			logD(msg);
		}
	}

	public String getDirPath() {
		if (dirPath == null)
			return null;
		return dirPath.getPath();
	}

	private void setDirPath(String value) {
		this.dirPath = new File(value);
	}

	private void setDirPath(File value) {
		this.dirPath = value;
	}

	public File getDirFile() {
		return dirPath;
	}

/*
	public Uri getDirUri() {
		return dirUri;
	}

	public void setDirUri(Uri uri) {
		this.dirUri = uri;
	}
*/

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
		mView = inflater.inflate(R.layout.weldfile_list_fragment, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) mView.findViewById(R.id.srl);
		if (refresher != null) {
			refresher.setOnRefreshListener(() -> {
				refreshFilesList(getDirPath());
				refresher.setRefreshing(false);
			});
		}

		mRecyclerView = (RecyclerView) mView.findViewById(R.id.recycler_view);
		mRecyclerView.setHasFixedSize(true);
		RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
		mRecyclerView.setLayoutManager(mLayoutManager);
		mAdapter = new WeldFileListAdapter(this, new ArrayList<>());
		mRecyclerView.setAdapter(mAdapter);
		RecyclerView.ItemDecoration itemDecoration =
				new android.support.v7.widget.DividerItemDecoration(getContext(),
						DividerItemDecoration.VERTICAL);
		mRecyclerView.addItemDecoration(itemDecoration);
		mRecyclerView.setItemAnimator(new DefaultItemAnimator());

		looperHandler = new LooperHandler(Looper.getMainLooper());

		return mView;
	}

	public String refreshParent() {
		return refreshFilesList(dirPath.getParent());
	}

	public void refreshFilesList() {
		final File file = null;
		refreshFilesList(file);
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

			logD(dir.getName());

			mAdapter.clear();
			File[] files = dir.listFiles();
			if (files != null) {
				for (File file : files) {
					if (file != null) {
						if (file.isFile() || file.isDirectory()) {
							mAdapter.add(file);
						}
					}
				}
			}
			if (mAdapter.getItemCount() > 0) {
				mAdapter.sort((obj1, obj2) -> {
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
				});
			}

			setDirPath(dir);
			mAdapter.insert(dir, 0);
			mAdapter.notifyDataSetChanged();
			onDirPathChanged(dir);

			weldFileListObserver = new WeldFileListObserver(dir, looperHandler);
			weldFileListObserver.startWatching();
		} catch (NullPointerException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			refreshFilesList(getDirPath());
		}

		return getDirPath();
	}

/*
	void refreshFilesList(Uri uri) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//			uri = Uri.parse("content://com.android.externalstorage.documents/tree/A345-1F0E%3Ajob");
			Log.e(TAG, uri.toString());

			ContentResolver contentResolver = getActivity().getContentResolver();
			Uri docUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
			Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
			DocumentFile pickedTree = DocumentFile.fromTreeUri(getContext(), childrenUri);
			for (DocumentFile file : pickedTree.listFiles()) {
				Log.e(TAG, file.getUri().toString());
			}

			Cursor docCursor = contentResolver.query(docUri,
					new String[]{
							DocumentsContract.Document.COLUMN_DISPLAY_NAME,
							DocumentsContract.Document.COLUMN_MIME_TYPE,
							DocumentsContract.Document.COLUMN_DOCUMENT_ID
					},
					null, null, null);
			if (docCursor != null) {
				try {
					while (docCursor.moveToNext()) {
						Log.e(TAG, "name =" + docCursor.getString(0)
								+ ", mime=" + docCursor.getString(1)
								+ ", id=" + docCursor.getString(2));
//				    mCurrentDirectoryUri = uri;
//				    mCurrentDirectoryTextView.setText(docCursor.getString(0));
					}
				} finally {
//			    	closeQuietly(docCursor);
					docCursor.close();
				}
			}

			Cursor childCursor = contentResolver.query(childrenUri,
					new String[]{
							DocumentsContract.Document.COLUMN_DISPLAY_NAME,
							DocumentsContract.Document.COLUMN_MIME_TYPE,
							DocumentsContract.Document.COLUMN_DOCUMENT_ID
					}, null, null, null);
			if (childCursor != null) {
				try {
//					List<DirectoryEntry> directoryEntries = new ArrayList<>();
					while (childCursor.moveToNext()) {
						Log.i(TAG, "name =" + childCursor.getString(0)
								+ ", mime=" + childCursor.getString(1)
								+ ", id=" + childCursor.getString(2));

//				        DirectoryEntry entry = new DirectoryEntry();
//				        entry.fileName = childCursor.getString(0);
//				        entry.mimeType = childCursor.getString(1);
//			        	directoryEntries.add(entry);
					}
//			        mAdapter.setDirectoryEntries(directoryEntries);
//			        mAdapter.notifyDataSetChanged();
				} finally {
					childCursor.close();
				}
			}
		}
	}
*/

	@Override
	public void onResume() {
		super.onResume();
		refreshFilesList(getDirPath());
	}

	private void onDirPathChanged(File path) {
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
		mRecyclerView = null;
		mAdapter = null;
		dirPath = null;
		looperHandler = null;
		if (weldFileListObserver != null) {
			weldFileListObserver.stopWatching();
			weldFileListObserver = null;
		}
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
				case MSG_REFRESH_DIR:
					logD("MSG_REFRESH_DIR");
					if (msg.obj == null)
						refreshFilesList();
					else
						refreshFilesList((File) msg.obj);
					break;
				case MSG_REFRESH_PARENT_DIR:
					logD("MSG_REFRESH_PARENT_DIR");
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
