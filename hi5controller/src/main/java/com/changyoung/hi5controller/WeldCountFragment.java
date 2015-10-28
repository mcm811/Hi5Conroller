package com.changyoung.hi5controller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by changmin on 2015-10-13.
 * changmin811@gmail.com
 */
public class WeldCountFragment extends Fragment
		implements Refresh, LoaderManager.LoaderCallbacks<List<WeldCountFragment.WeldCountFile>> {

	private static final int MSG_REFRESH = 0;

	private static final String ARG_WORK_PATH = "workPath";
	private static final String TAG = "WeldCountFragment";

	private static final int ORDER_TYPE_ASCEND = 0;
	private static final int ORDER_TYPE_DESCEND = 1;

	private View mView;
	private RecyclerView mRecyclerView;
	private RecyclerView.LayoutManager mLayoutManager;
	private WeldCountAdapter mAdapter;

	private LooperHandler looperHandler;
	private WeldCountObserver observer;
	private TextToSpeech mTts;

	private int orderType = ORDER_TYPE_ASCEND;

	//	private String mWorkPath;

	private OnWorkPathListener mListener;

	public WeldCountFragment() {
		// Required empty public constructor
	}

	/**
	 * Use this factory method to create a new instance of
	 * this fragment using the provided parameters.
	 *
	 * @param workPath Parameter 1.
	 * @return A new instance of fragment WeldCountFragment.
	 */
	@SuppressWarnings("unused")
	public WeldCountFragment newInstance(String workPath) {
		WeldCountFragment fragment = new WeldCountFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		fragment.setArguments(args);
		return fragment;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		Log.d(TAG, "onAttach");
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
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
//		if (getArguments() != null) {
//			mWorkPath = getArguments().getString(ARG_WORK_PATH);
//		} else {
//			mWorkPath = onGetWorkPath();
//		}
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView");
		mView = inflater.inflate(R.layout.fragment_weld_count, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) mView.findViewById(R.id.srl);
		refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refresh(true);
				refresher.setRefreshing(false);
			}
		});

		final FloatingActionButton fab = (FloatingActionButton) mView.findViewById(R.id.weld_count_fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				orderType = orderType == ORDER_TYPE_ASCEND ? ORDER_TYPE_DESCEND : ORDER_TYPE_ASCEND;
				mAdapter.sortName(orderType);
				fab.setScaleY(fab.getScaleY() * -1);
			}
		});
		fab.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				if (mLayoutManager instanceof GridLayoutManager)
					mLayoutManager = new StaggeredGridLayoutManager(2,
							StaggeredGridLayoutManager.VERTICAL);
				else if (mLayoutManager instanceof StaggeredGridLayoutManager)
					mLayoutManager = new LinearLayoutManager(getContext());
				else if (mLayoutManager instanceof LinearLayoutManager)
					mLayoutManager = new GridLayoutManager(getContext(), 2);
				mRecyclerView.setLayoutManager(mLayoutManager);
				return true;
			}
		});

		mRecyclerView = (RecyclerView) mView.findViewById(R.id.recycler_view);
		mRecyclerView.setHasFixedSize(true);
		mLayoutManager = new GridLayoutManager(getContext(), 2);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mAdapter = new WeldCountAdapter(getActivity(), fab, new ArrayList<WeldCountFile>());
		mRecyclerView.setAdapter(mAdapter);

		looperHandler = new LooperHandler(Looper.getMainLooper());
		observer = new WeldCountObserver(onGetWorkPath(), looperHandler);
		observer.startWatching();

		mTts = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {

			}
		});
//		mTts.setLanguage(Locale.KOREAN);

		return mView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		logD("onActivityCreated");
		super.onActivityCreated(savedInstanceState);
		try {
			getLoaderManager().initLoader(0, null, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
	}

	@Override
	public void onDetach() {
		Log.d(TAG, "onDetach");
		super.onDetach();
		mListener = null;
		mAdapter = null;
		mView = null;
		mRecyclerView = null;
		getLoaderManager().destroyLoader(0);
	}

	@Override
	public void refresh(boolean forced) {
		try {
			if (isAdded()) {
				Log.d(TAG, "refresh: restartLoader");
				getLoaderManager().restartLoader(0, null, this);
				observer = new WeldCountObserver(onGetWorkPath(), looperHandler);
				observer.startWatching();
			}
		} catch (IllegalStateException e) {
			Log.d(TAG, e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean refresh(String path) {
		return false;
	}

	@Override
	public String refresh(int menuId) {
		return null;
	}

	@Override
	public String onBackPressedFragment() {
		return null;
	}

	@Override
	public void show(String msg) {
		try {
			if (msg != null && isAdded()) {
				Snackbar.make(mRecyclerView, msg, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
				logD(msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String onGetWorkPath() {
		if (mListener != null) {
			return mListener.onGetWorkPath();
		}
		return null;
	}

	private void logD(String msg) {
		try {
			Log.d(TAG, msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Loader<List<WeldCountFile>> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, String.format("id:%d, onCreateLoader()", id));
		return new WeldCountLoader(getActivity(), mListener);
	}

	@Override
	public void onLoadFinished(Loader<List<WeldCountFile>> loader, List<WeldCountFile> data) {
		Log.d(TAG, String.format("id:%d, onLoadFinished() size:%d", loader.getId(), data.size()));
		mAdapter.setData(data, orderType);
		if (mRecyclerView != null)
			mRecyclerView.refreshDrawableState();
		if (mView != null) {
			if (mAdapter.getItemCount() > 0) {
				mView.findViewById(R.id.imageView).setVisibility(View.GONE);
				mView.findViewById(R.id.textView).setVisibility(View.GONE);
			} else {
				mView.findViewById(R.id.imageView).setVisibility(View.VISIBLE);
				mView.findViewById(R.id.textView).setVisibility(View.VISIBLE);
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<List<WeldCountFile>> loader) {
		Log.d(TAG, String.format("id: %d, onLoaderReset()", loader.getId()));
		mAdapter.setData(null, ORDER_TYPE_ASCEND);
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
	}

	public static class WeldCountObserver extends FileObserver {
		static final String TAG = "WeldCountObserver";
		static final int mask = CREATE | DELETE | DELETE_SELF |
				MOVED_FROM | MOVED_TO | MOVE_SELF | CLOSE_WRITE;
		File file;
		private Handler handler;

		@SuppressWarnings("unused")
		public WeldCountObserver(File file, Handler handler) {
			super(file.getPath(), mask);
			this.file = file;
			this.handler = handler;
			Log.d(TAG, "FILE_OBSERVER: " + file.getPath());
		}

		public WeldCountObserver(String path, Handler handler) {
			super(path, mask);
			this.file = new File(path);
			this.handler = handler;
			Log.d(TAG, "FILE_OBSERVER: " + path);
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
			msg.what = MSG_REFRESH;
			msg.obj = file;
			handler.sendMessage(msg);
		}
	}

	public static class WeldCountFile extends File {
//		private static final String TAG = "WeldCountFile";

		private List<Job> jobList;
		private JobInfo jobInfo;

		public WeldCountFile(String path) {
			super(path);
			readFile();
		}

		public Job get(Integer index) {
			return jobList.get(index);
		}

//		public void set(Integer index, Job value) {
//			jobList.set(index, value);
//		}

		public Integer size() {
			return jobList.size();
		}

		public JobInfo getJobInfo() {
			return jobInfo;
		}

		public void readFile() {
			jobList = readFile(getPath(), new ArrayList<Job>());
			jobInfo = createJobInfo(jobList, new JobInfo());
		}

		private List<Job> readFile(String fileName, List<Job> items) {
			try {
				FileInputStream fileInputStream = new FileInputStream(fileName);
				InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

				String rowString;
				Integer rowNumber = 0;
				while ((rowString = bufferedReader.readLine()) != null) {
					items.add(new Job(rowNumber++, rowString));
				}

				bufferedReader.close();
				inputStreamReader.close();
				fileInputStream.close();
			} catch (FileNotFoundException e) {
				Log.d(TAG, e.getLocalizedMessage());
			} catch (Exception e) {
				e.printStackTrace();
			}
			return items;
		}

		public void saveFile() {
			jobInfo = saveFile(getPath(), jobList);
		}

		private JobInfo saveFile(String fileName, List<Job> items) {
			try {
				FileOutputStream fileOutputStream = new FileOutputStream(fileName);
				OutputStreamWriter outputStreamReader = new OutputStreamWriter(fileOutputStream, "EUC-KR");
				BufferedWriter bufferedWriter = new BufferedWriter(outputStreamReader);

				for (Job item : items) {
					bufferedWriter.write(item.getRowString());
					bufferedWriter.newLine();
				}
				bufferedWriter.close();
				outputStreamReader.close();
				fileOutputStream.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return createJobInfo(jobList, new JobInfo());
		}

		private JobInfo createJobInfo(List<Job> jobList, JobInfo jobInfo) {
			for (Job job : jobList) {
				String cn = job.getCN();
				if (cn != null) {
					jobInfo.setTotal(jobInfo.getTotal() + 1);
				}
				String gn = job.getGN();
				if (gn != null) {
					jobInfo.IncreaseGN(gn);
				}
				String g = job.getG();
				if (g != null) {
					jobInfo.IncreaseG(g);
				}
				if (job.getRowType() == Job.JOB_MOVE) {
					jobInfo.setStep(jobInfo.getStep() + 1);
				}
				if (job.getRowType() == Job.JOB_COMMENT) {
					if (jobInfo.getPreview() == null)
						jobInfo.setPreview(job.getRowString().trim());
				}
			}
			return jobInfo;
		}

		public String getCNList() {
			StringBuilder sb = new StringBuilder();
			Integer n = 0;
			for (Job job : jobList) {
				String cn = job.getCN();
				if (cn != null) {
					if (++n > 200) {    // 200개 까지만 보여줌
						sb.append("...");
						break;
					}
					sb.append(cn).append("  ");
				}
			}
			if (sb.length() > 0)
				sb.insert(0, "CN: ");
			return sb.toString();
		}

//		public String getCNTest() {
//			StringBuilder sb = new StringBuilder();
//			for (Job job : jobList) {
//				String cn = job.getCN();
//				if (cn != null) {
//					sb.append(String.format("%d: CN=%s\n", job.getRowNumber(), cn));
//				}
//			}
//			return sb.toString();
//		}

		public String getRowText() {
			StringBuilder sb = new StringBuilder();
			for (Job Job : jobList) {
				sb.append(Job.getRowString());
				sb.append("\n");
			}
			return sb.toString();
		}

//		public void updateCN(Integer start) {
//			for (Job job : jobList) {
//				if (job.getRowType() == Job.JOB_SPOT) {
//					job.setCN((start++).toString());
//				}
//			}
//		}

//		public void updateCN(Integer index, String value) {
//			jobList.get(index).setCN(value);
//		}

		class JobInfo {
			//			private static final String TAG = "JobInfo";
			private Integer total;         // SPOT 의 총 카운트
			private Integer step;          // S1 S2 S3 붙은 것들 젤 마지막 S번호 값
			private List<Integer> gnList;  // SPOT 단어 다음에 나오는 첫번째 단어 분석 해서 종류 결정(GN1, GN2, GN3, G1, G2)
			private List<Integer> gList;
			private String preview;

			public JobInfo() {
				total = 0;
				step = 0;
				gnList = new ArrayList<>();
				gList = new ArrayList<>();
			}

			public void IncreaseGN(String strIndex) {
				Integer index = Integer.parseInt(strIndex);
				if (gnList.size() < index) {
					for (Integer i = gnList.size(); i < index; i++) {
						gnList.add(0);
					}
				}
				gnList.set(index - 1, gnList.get(index - 1) + 1);
			}

			public void IncreaseG(String strIndex) {
				Integer index = Integer.parseInt(strIndex);
				if (gList.size() < index) {
					for (Integer i = gList.size(); i < index; i++) {
						gList.add(0);
					}
				}
				gList.set(index - 1, gList.get(index - 1) + 1);
			}

			public Integer getTotal() {
				return total;
			}

			public void setTotal(Integer value) {
				total = value;
			}

			public Integer getStep() {
				return step;
			}

			public void setStep(Integer value) {
				step = value;
			}

			public String getPreview() {
				return preview;
			}

			public void setPreview(String value) {
				preview = value;
			}

			public String getString() {
				StringBuilder sb = new StringBuilder();

				if (total > 0)
					sb.append("Total: ").append(total.toString());

				Integer n = 1;
				for (Integer item : gnList) {
					sb.append(",  GN").append((n++).toString()).append(": ").append(item.toString());
				}

				n = 1;
				for (Integer item : gList) {
					sb.append(",  G").append((n++).toString()).append(": ").append(item.toString());
				}

				if (step > 0) {
					if (total > 0)
						sb.append(",  ");
					sb.append("Step: ").append(step.toString());
				}

				return sb.toString();
			}
		}

		class Job {
			public static final int JOB_COMMENT = 1;
			public static final int JOB_SPOT = 2;
			public static final int JOB_MOVE = 3;
			public static final int JOB_WAIT = 4;
			public static final int JOB_DO = 5;
			public static final int JOB_HEADER = 0;
			public static final int JOB_CALL = 6;
			public static final int JOB_END = 7;
			public static final int JOB_ETC = 8;
			RowJob row;

			public Job(Integer rowNumber, String rowString) {
				Integer rowType = getRowType(rowString);

				switch (rowType) {
					case JOB_HEADER:
						row = new HeaderJob(rowNumber, rowString);
						break;
					case JOB_COMMENT:
						row = new CommentJob(rowNumber, rowString);
						break;
					case JOB_SPOT:
						row = new SpotJob(rowNumber, rowString);
						break;
					case JOB_MOVE:
						row = new MoveJob(rowNumber, rowString);
						break;
					case JOB_WAIT:
						row = new WaitJob(rowNumber, rowString);
						break;
					case JOB_DO:
						row = new DoJob(rowNumber, rowString);
						break;
					case JOB_CALL:
						row = new CallJob(rowNumber, rowString);
						break;
					case JOB_END:
						row = new EndJob(rowNumber, rowString);
						break;
					case JOB_ETC:
						row = new EtcJob(rowNumber, rowString);
						break;
				}
			}

			private Integer getRowType(String rowString) {
				Integer rowType = JOB_ETC;
				String[] s = rowString.trim().split(" ");
				if (s.length > 0) {
					if (s[0].equals("Program"))
						rowType = JOB_HEADER;
					else if (s[0].startsWith("'"))
						rowType = JOB_COMMENT;
					else if (s[0].startsWith("SPOT"))
						rowType = JOB_SPOT;
					else if (s[0].startsWith("S"))
						rowType = JOB_MOVE;
					else if (s[0].startsWith("WAIT"))
						rowType = JOB_WAIT;
					else if (s[0].startsWith("DO"))
						rowType = JOB_DO;
					else if (s[0].startsWith("END"))
						rowType = JOB_END;
				}
				return rowType;
			}

			public String getCN() {
				if (getRowType() == JOB_SPOT)
					return ((SpotJob) row).getCN();
				else
					return null;
			}

			public void setCN(String value) {
				if (getRowType() == JOB_SPOT)
					((SpotJob) row).setCN(value);
			}

			public String getGN() {
				if (getRowType() == JOB_SPOT)
					return ((SpotJob) row).getGN();
				else
					return null;
			}

			public String getG() {
				if (getRowType() == JOB_SPOT)
					return ((SpotJob) row).getG();
				else
					return null;
			}

			public Integer getRowType() {
				return row.getRowType();
			}

			public Integer getRowNumber() {
				return row.getRowNumber();
			}

			public String getRowString() {
				return row.getRowString();
			}

			public class JobValue {
				private String mValue;
				private String mType;

				public JobValue(String str) {
					setUpdate(str);
				}

				public String getValue() {
					return mValue;
				}

				public void setValue(String value) {
					this.mValue = value;
				}

				public String getType() {
					return mType;
				}

//				public void setType(String type) {
//					this.mType = type;
//				}

				public String getUpdate() {
					return mType + "=" + mValue;
				}

				public void setUpdate(String value) {
					if (value != null) {
						String[] s = value.trim().split("=");
						if (s.length == 2) {
							mType = s[0];
							mValue = s[1];
						}
					}
				}
			}

			public class RowJob {
				private Integer mRowType;
				private Integer mRowNumber;
				private String mRowString;

				public RowJob(Integer rowType, Integer rowNumber, String rowString) {
					mRowType = rowType;
					mRowNumber = rowNumber;
					mRowString = rowString;
				}

				public Integer getRowType() {
					return mRowType;
				}

//				public void setRowType(Integer value) {
//					mRowType = value;
//				}

				public Integer getRowNumber() {
					return mRowNumber;
				}

				public String getRowString() {
					return mRowString;
				}

				public void setRowString(String rowString) {
					this.mRowString = rowString;
				}
			}

			public class HeaderJob extends RowJob {
//			String version;
//			String mechType;
//			String totalAxis;
//			String auxAxis;

				public HeaderJob(Integer rowNumber, String rowString) {
					super(JOB_HEADER, rowNumber, rowString);
				}
			}

			public class CommentJob extends RowJob {
				public CommentJob(Integer rowNumber, String rowString) {
					super(JOB_COMMENT, rowNumber, rowString);
				}
			}

			public class SpotJob extends RowJob {
				List<JobValue> mJobValueList;

				public SpotJob(Integer rowNumber, String rowString) {
					super(JOB_SPOT, rowNumber, rowString);
					mJobValueList = new ArrayList<>();
					String[] s = rowString.trim().split(" ");
					if (s.length == 2) {
						String[] f = s[1].split(",");
						for (String aF : f) {
							mJobValueList.add(new JobValue(aF));
						}
					}
				}

				public void Update() {
					setRowString("     SPOT " + mJobValueList.get(0).getUpdate() + "," + mJobValueList.get(1).getUpdate() + "," + mJobValueList.get(2).getUpdate());
				}

				public String getCN() {
					for (JobValue s : mJobValueList) {
						if (s.getType().equals("CN"))
							return s.getValue();
					}
					return null;
				}

				public void setCN(String value) {
					for (JobValue s : mJobValueList) {
						if (s.getType().equals("CN")) {
							s.setValue(value);
							Update();
						}
					}
				}

				public String getGN() {
					for (JobValue s : mJobValueList) {
						if (s.getType().equals("GN"))
							return s.getValue();
					}
					return null;
				}

				public String getG() {
					for (JobValue s : mJobValueList) {
						if (s.getType().equals("G"))
							return s.getValue();
					}
					return null;
				}
			}

			public class MoveJob extends RowJob {
//			Integer step;

				public MoveJob(Integer rowNumber, String rowString) {
					super(JOB_MOVE, rowNumber, rowString);
				}
			}

			public class WaitJob extends RowJob {
				public WaitJob(Integer rowNumber, String rowString) {
					super(JOB_WAIT, rowNumber, rowString);
				}
			}

			public class DoJob extends RowJob {
				public DoJob(Integer rowNumber, String rowString) {
					super(JOB_DO, rowNumber, rowString);
				}
			}

			public class CallJob extends RowJob {
				public CallJob(Integer rowNumber, String rowString) {
					super(JOB_CALL, rowNumber, rowString);
				}
			}

			public class EndJob extends RowJob {
				public EndJob(Integer rowNumber, String rowString) {
					super(JOB_END, rowNumber, rowString);
				}
			}

			public class EtcJob extends RowJob {
				public EtcJob(Integer rowNumber, String rowString) {
					super(JOB_ETC, rowNumber, rowString);
				}
			}
		}
	}

	public static class WeldCountLoader extends AsyncTaskLoader<List<WeldCountFile>> {
		private static final String TAG = "WeldCountLoader";

		WeldCountFragment.OnWorkPathListener mCallBack;
		List<WeldCountFile> mList;
		WeldCountReceiver mReceiver;

		public WeldCountLoader(Context context,
		                       WeldCountFragment.OnWorkPathListener callBack) {
			super(context);
			mCallBack = callBack;
		}

		@Override
		public List<WeldCountFile> loadInBackground() {
			Log.d(TAG, "loadInBackground");
			if (mCallBack == null)
				return null;
			String path = mCallBack.onGetWorkPath();
			List<WeldCountFile> list = new ArrayList<>();
			try {
				File dir = new File(path);
				for (File file : dir.listFiles()) {
					if (file.getName().toUpperCase().endsWith(".JOB")
							|| file.getName().toUpperCase().startsWith("HX"))
						list.add(new WeldCountFile(file.getPath()));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Log.d(TAG, "loadInBackground() list.size: " + list.size());
			return list;
		}

		@Override
		public void deliverResult(List<WeldCountFile> data) {
			Log.d(TAG, "deliverResult");

			if (isReset()) {
				if (data != null)
					onReleaseResources(data);
			}

			List<WeldCountFile> oldList = mList;
			mList = data;

			if (isStarted()) {
				super.deliverResult(data);
			}

			if (oldList != null)
				onReleaseResources(oldList);
			if (mList != null)
				Log.d(TAG, "deliverResult() mList.size: " + mList.size());
		}

		protected void onReleaseResources(List<WeldCountFile> data) {
			// For a simple List<> there is nothing to do.  For something
			// like a Cursor, we would close it here.
			if (data != null)
				Log.d(TAG, "dataSize: " + data.size());
		}

		@Override
		protected void onStartLoading() {
			Log.d(TAG, "onStartLoading");

			if (mList != null) {
				deliverResult(mList);
			}

			if (mReceiver == null) {
				mReceiver = new WeldCountReceiver(this);
			}

			if (takeContentChanged() || mList == null) {
				forceLoad();
			}
		}

		@Override
		protected void onStopLoading() {
			Log.d(TAG, "onStopLoading");
			cancelLoad();
		}

		@Override
		public void onCanceled(List<WeldCountFile> data) {
			Log.d(TAG, "onCanceled");
			super.onCanceled(data);
			onReleaseResources(data);
		}

		@Override
		protected void onReset() {
			Log.d(TAG, "onReset");

			super.onReset();

			onStopLoading();

			if (mList != null) {
				onReleaseResources(mList);
				mList = null;
			}

			if (mReceiver != null) {
				getContext().unregisterReceiver(mReceiver);
				mReceiver = null;
			}
		}

		public static class WeldCountReceiver extends BroadcastReceiver {
			public static final String WELD_COUNT_LOAD = "com.changyoung.hi5controller.weld_count_load";
			public static final String WELD_COUNT_UPDATE = "com.changyoung.hi5controller.weld_count_update";

			final WeldCountLoader mLoader;

			public WeldCountReceiver(WeldCountLoader loader) {
				this.mLoader = loader;
				IntentFilter filter = new IntentFilter(WELD_COUNT_LOAD);
				filter.addAction(WELD_COUNT_UPDATE);
				mLoader.getContext().registerReceiver(this, filter);
			}

			@Override
			public void onReceive(Context context, Intent intent) {
				mLoader.onContentChanged();
			}
		}
	}

	public class WeldCountAdapter extends RecyclerView.Adapter<WeldCountAdapter.ViewHolder> {
		private List<WeldCountFile> mDataset;
		private Context mContext;
		private Activity mActivity;
		private View mSnackbarView;

		public WeldCountAdapter(Activity activity, View snackbarView, List<WeldCountFile> dataSet) {
			mDataset = dataSet;
			mActivity = activity;
			mSnackbarView = snackbarView;
		}

		public void show(String msg) {
			try {
				if (msg != null) {
					Snackbar.make(mSnackbarView, msg, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void setData(List<WeldCountFile> data, int orderType) {
			mDataset.clear();
			if (data != null) {
				Collections.sort(data, new Comparator<File>() {
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
				if (orderType == ORDER_TYPE_DESCEND)
					Collections.reverse(data);
				mDataset.addAll(data);
			}
			notifyDataSetChanged();
		}

		public void sortName(final int orderType) {
			Collections.sort(mDataset, new Comparator<File>() {
				public int compare(File obj1, File obj2) {
					int ret = 0;
					if (obj1.isDirectory() && obj2.isDirectory())
						ret = obj1.getName().compareToIgnoreCase(obj2.getName());
					else if (obj1.isFile() && obj2.isFile())
						ret = obj1.getName().compareToIgnoreCase(obj2.getName());
					else if (obj1.isDirectory() && obj2.isFile())
						ret = -1;
					else if (obj1.isFile() && obj2.isDirectory())
						ret = 1;
					return orderType == 0 ? ret : -ret;
				}
			});
			notifyDataSetChanged();
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			mContext = parent.getContext();
			final View v = LayoutInflater.from(mContext)
					.inflate(R.layout.list_item_weld_count, parent, false);
			ViewHolder holder = new ViewHolder(v);
			holder.mItemView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final WeldCountFile weldCountFile = mDataset.get((int) v.getTag());
					if (weldCountFile.getJobInfo().getTotal() == 0) {
						Helper.UiHelper.textViewActivity(mContext, weldCountFile.getName(),
								weldCountFile.getRowText());
					} else {
						showDialog((int) v.getTag());
					}
				}
			});
			holder.mItemView.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(final View v) {
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
					builder.setItems(R.array.dialog_items, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							//String[] items = getResources().getStringArray(R.array.dialog_items);
							if (which == 0) {
								showDialog((int) v.getTag());
							} else if (which == 1) {
								final WeldCountFile weldCountFile = mDataset.get((int) v.getTag());
								Helper.UiHelper.textViewActivity(mContext, weldCountFile.getName(),
										weldCountFile.getRowText());
							}
						}
					});
					builder.create().show();
					return true;
				}
			});
			return holder;
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int position) {
			final WeldCountFile jobFile = mDataset.get(position);
			holder.tvFileName.setText(jobFile.getName());
			holder.tvTime.setText(Helper.TimeHelper.getLasModified(jobFile));
			holder.tvSize.setText(String.format("%dB", jobFile.length()));

			final String countString = jobFile.getJobInfo().getString();
			if (countString == null || countString.isEmpty()) {
				holder.tvCount.setVisibility(View.GONE);
			} else {
				holder.tvCount.setVisibility(View.VISIBLE);
				holder.tvCount.setText(countString);
			}
			final String previewString = jobFile.getJobInfo().getPreview();
			if (previewString == null || previewString.isEmpty()) {
				holder.tvPreview.setVisibility(View.GONE);
			} else {
				holder.tvPreview.setVisibility(View.VISIBLE);
				holder.tvPreview.setText(previewString);
			}
			final String CNString = jobFile.getCNList();
			if (CNString == null || CNString.isEmpty()) {
				holder.tvCN.setVisibility(View.GONE);
			} else {
				holder.tvCN.setVisibility(View.VISIBLE);
				holder.tvCN.setText(CNString);
			}
			holder.mItemView.setTag(position);
		}

		@Override
		public int getItemCount() {
			return mDataset.size();
		}

		public WeldCountFile getItem(int position) {
			return mDataset.get(position);
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public View mItemView;
			public TextView tvFileName;
			public TextView tvTime;
			public TextView tvSize;
			public TextView tvCount;
			public TextView tvPreview;
			public TextView tvCN;

			public ViewHolder(View itemView) {
				super(itemView);
				mItemView = itemView;
				tvFileName = (TextView) itemView.findViewById(R.id.tvFileName);
				tvTime = (TextView) itemView.findViewById(R.id.tvTime);
				tvSize = (TextView) itemView.findViewById(R.id.tvSize);
				tvCount = (TextView) itemView.findViewById(R.id.tvCount);
				tvPreview = (TextView) itemView.findViewById(R.id.tvPreview);
				tvCN = (TextView) itemView.findViewById(R.id.tvCN);
			}
		}

		private void showDialog(final int position) {
			final WeldCountFile weldCountFile = getItem(position);
			if (weldCountFile.getJobInfo().getTotal() == 0) {
				show("CN 항목이 없습니다");
				return;
			}

			@SuppressLint("InflateParams") View dialogView = LayoutInflater.from(mContext)
					.inflate(R.layout.dialog_weld_count, null);
			AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
			dialog.setView(dialogView);

			AdView adView = new AdView(mContext);
			adView.setAdSize(AdSize.BANNER);
			adView.setScaleX(0.85f);
			adView.setScaleY(0.85f);
			if (BuildConfig.DEBUG)
				adView.setAdUnitId(mContext.getString(R.string.banner_ad_unit_id_debug));
			else
				adView.setAdUnitId(mContext.getString(R.string.banner_ad_unit_id_release));
			AdRequest adRequest = new AdRequest.Builder()
					.setRequestAgent("android_studio:ad_template").build();
			adView.loadAd(adRequest);
			LinearLayout linearLayoutWeldCount = (LinearLayout)
					dialogView.findViewById(R.id.linearLayout_WeldCount);
			linearLayoutWeldCount.addView(adView, 2);

			TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
			statusText.setText(String.format("계열 수정 (CN: %d개)",
					weldCountFile.getJobInfo().getTotal()));

			LinearLayout linearLayout = (LinearLayout) dialogView.findViewById(R.id.linearLayout1);
			final EditText etBeginNumber = (EditText) dialogView.findViewById(R.id.etBeginNumber);
			final SeekBar sbBeginNumber = (SeekBar) dialogView.findViewById(R.id.sampleSeekBar);

			final List<EditText> etList = new ArrayList<>();
			for (int i = 0; i < weldCountFile.size(); i++) {
				if (weldCountFile.get(i).getRowType() == WeldCountFile.Job.JOB_SPOT) {
					final TextInputLayout til = new TextInputLayout(mContext);
					til.setHint(String.format("줄 번호: %03d (CN = %s)",
							weldCountFile.get(i).getRowNumber(),
							weldCountFile.get(i).getCN()));
					final EditText editText = new EditText(mContext);
					editText.setTag(weldCountFile.get(i));
					editText.setSingleLine();
					editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
					editText.setGravity(Gravity.CENTER_HORIZONTAL);
					editText.setInputType(etBeginNumber.getInputType());
//					editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
					editText.setSelectAllOnFocus(true);
					editText.setText(weldCountFile.get(i).getCN());
					editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
						@Override
						public void onFocusChange(View v, boolean hasFocus) {
							try {
								if (!hasFocus) {
									Integer beginNumber = Integer.parseInt(editText.getText().toString());
									if (beginNumber > 255)
										beginNumber = 255;
									editText.setText(String.valueOf(beginNumber));
								}
							} catch (NumberFormatException e) {
								Log.d(TAG, e.getLocalizedMessage());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
					editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						@Override
						public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
							if (actionId == 6)
								Helper.UiHelper.hideSoftKeyboard(getActivity(), v, event);
//							Log.d("onEditorAction", "actionId: " + String.valueOf(actionId));
							return false;
						}
					});
					til.addView(editText);
					linearLayout.addView(til);
					etList.add(editText);
				}
			}

			etBeginNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					try {
						if (!hasFocus) {
							Integer beginNumber = Integer.parseInt(etBeginNumber.getText().toString());
							if (beginNumber > 255)
								beginNumber = 255;
							etBeginNumber.setText(String.valueOf(beginNumber));
							sbBeginNumber.setProgress(beginNumber - 1);
							for (EditText et : etList) {
								et.setText(String.valueOf(beginNumber++));
								if (beginNumber > 255)
									beginNumber = 255;
							}
						}
					} catch (NumberFormatException e) {
						Log.d(TAG, e.getLocalizedMessage());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			etBeginNumber.setOnKeyListener(new View.OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					Helper.UiHelper.hideSoftKeyboard(mActivity, v, event);
					return false;
				}
			});

			final int etListSize = etList.size();
			sbBeginNumber.setMax(254);
			sbBeginNumber.setProgress(0);
			sbBeginNumber.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					Integer beginNumber = sbBeginNumber.getProgress() + 1;
					etBeginNumber.setText(String.valueOf(beginNumber));
					if (etListSize < 30) {
						for (EditText et : etList) {
							et.setText(String.valueOf(beginNumber++));
							if (beginNumber > 255)
								beginNumber = 255;
						}
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					Integer beginNumber = sbBeginNumber.getProgress() + 1;
					etBeginNumber.setText(String.valueOf(beginNumber));
					for (EditText et : etList) {
						et.setText(String.valueOf(beginNumber++));
						if (beginNumber > 255)
							beginNumber = 255;
					}
				}
			});

			dialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});

			dialog.setPositiveButton("저장", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					for (EditText et : etList) {
						WeldCountFile.Job job = (WeldCountFile.Job) et.getTag();
						job.setCN(et.getText().toString());
					}
					if (weldCountFile.getJobInfo().getTotal() > 0) {
						weldCountFile.saveFile();
						notifyDataSetChanged();
						show("저장 완료: " + weldCountFile.getName());
					}
				}
			});

			dialog.show();

			final String ttsMsg = mContext.getString(R.string.tts_begin_number);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mTts.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null, null);
			} else {
				//noinspection deprecation
				mTts.speak(ttsMsg, TextToSpeech.QUEUE_FLUSH, null);
			}

			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
					intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getActivity().getPackageName());
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR");

					SpeechRecognizer mRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
					mRecognizer.setRecognitionListener(new RecognitionListener() {
						@Override
						public void onReadyForSpeech(Bundle params) {

						}

						@Override
						public void onBeginningOfSpeech() {

						}

						@Override
						public void onRmsChanged(float rmsdB) {

						}

						@Override
						public void onBufferReceived(byte[] buffer) {

						}

						@Override
						public void onEndOfSpeech() {

						}

						@Override
						public void onError(int error) {

						}

						@Override
						public void onResults(Bundle results) {
							String key = SpeechRecognizer.RESULTS_RECOGNITION;
							ArrayList<String> list = results.getStringArrayList(key);
							if (list != null) {
								for (String item : list) {
									try {
										sbBeginNumber.setProgress(Integer.parseInt(item) - 1);
										break;
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}
						}

						@Override
						public void onPartialResults(Bundle partialResults) {

						}

						@Override
						public void onEvent(int eventType, Bundle params) {

						}
					});
					mRecognizer.startListening(intent);
				}
			}, 2000);
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
				case MSG_REFRESH:
					Log.d(TAG, "MSG_REFRESH");
					refresh(true);
					break;
				default:
					break;
			}
		}
	}
}
