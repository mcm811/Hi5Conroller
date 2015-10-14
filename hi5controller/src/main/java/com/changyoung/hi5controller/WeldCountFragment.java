package com.changyoung.hi5controller;

import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnWorkPathListener} interface
 * to handle interaction events.
 * Use the {@link WeldCountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WeldCountFragment extends android.support.v4.app.Fragment implements Refresh {
	// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
	private static final String ARG_WORK_PATH = "workPath";

	private View mView;
	private ListView mListView;
	private WeldCountAdapter<JobFile> adapter;

	private String mWorkPath;

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
	public static WeldCountFragment newInstance(String workPath) {
		WeldCountFragment fragment = new WeldCountFragment();
		Bundle args = new Bundle();
		args.putString(ARG_WORK_PATH, workPath);
		fragment.setArguments(args);
		return fragment;
	}

	private void logDebug(String msg) {
		try {
			Log.d(getActivity().getPackageName(), "WeldCountFragment: " + msg);
		} catch (Exception e) {
			Log.d(Pref.TAG_NAME, "WeldCountFragment: " + msg);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getArguments() != null) {
			mWorkPath = getArguments().getString(ARG_WORK_PATH);
		} else {
			mWorkPath = onGetWorkPath();
		}
		refresh(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		mView = inflater.inflate(R.layout.fragment_weld_count, container, false);

		final SwipeRefreshLayout refresher = (SwipeRefreshLayout) mView.findViewById(R.id.srl);
		refresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				refresh(true);
				refresher.setRefreshing(false);
			}
		});

		mListView = (ListView) mView.findViewById(R.id.listView);
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
				final JobFile jobFile = adapter.getItem(position);
				if (jobFile.getJobCount().getTotal() == 0) {
					Util.UiUtil.textViewActivity(getContext(), jobFile.getJobCount().fi.getName(), jobFile.getRowText());
				} else {
					listView_click(position);
				}
			}
		});
		mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
				builder.setItems(R.array.select_dialog_items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//String[] items = getResources().getStringArray(R.array.select_dialog_items);
						//String item = items[which];
						if (which == 0) {
							listView_click(position);
						} else if (which == 1) {
							final JobFile jobFile = adapter.getItem(position);
							Util.UiUtil.textViewActivity(getContext(), jobFile.getJobCount().fi.getName(), jobFile.getRowText());
						}
					}
				});
				builder.create().show();
				return true;
			}
		});

		return mView;
	}

	private void listView_click(final int position) {
		final JobFile jobFile = adapter.getItem(position);
		if (jobFile.getJobCount().getTotal() == 0) {
			show("CN 항목이 없습니다");
			return;
		}

		View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_weld_count, null);
		AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
		dialog.setView(dialogView);

		TextView statusText = (TextView) dialogView.findViewById(R.id.statusText);
		statusText.setText("계열 수정 (CN: " + jobFile.getJobCount().getTotal().toString() + "개)");

		LinearLayout linearLayout = (LinearLayout) dialogView.findViewById(R.id.linearLayout);
		final EditText etBeginNumber = (EditText) dialogView.findViewById(R.id.etBeginNumber);
		final SeekBar sbBeginNumber = (SeekBar) dialogView.findViewById(R.id.sampleSeekBar);

		final ArrayList<EditText> etList = new ArrayList<>();
		for (int i = 0; i < jobFile.size(); i++) {
			if (jobFile.get(i).getRowType() == Job.ROWTYPES_SPOT) {
				TextInputLayout textInputLayout = new TextInputLayout(getContext());
				final EditText etCN = new EditText(getContext());
				etCN.setSingleLine();
				etCN.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
				etCN.setInputType(etBeginNumber.getInputType());
				etCN.setSelectAllOnFocus(true);
				etCN.setHint("CN[줄번호:" + jobFile.get(i).getRowNumber() + "]");
				etCN.setText(jobFile.get(i).getCN());
				etCN.setGravity(android.view.Gravity.CENTER);
				etCN.setTag(jobFile.get(i));
				etCN.setOnFocusChangeListener(new View.OnFocusChangeListener() {
					@Override
					public void onFocusChange(View v, boolean hasFocus) {
						Integer beginNumber = Integer.parseInt(etCN.getText().toString());
						if (beginNumber > 255) {
							beginNumber = 255;
							etCN.setText(beginNumber.toString());
						}
					}
				});
				etCN.setOnKeyListener(new View.OnKeyListener() {
					@Override
					public boolean onKey(View v, int keyCode, KeyEvent event) {
						Util.UiUtil.hideSoftKeyboard(getActivity(), event);
						return false;
					}
				});
				textInputLayout.addView(etCN);
				linearLayout.addView(textInputLayout);
				etList.add(etCN);
			}
		}

		etBeginNumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				Integer beginNumber = Integer.parseInt(etBeginNumber.getText().toString());
				if (beginNumber > 255) {
					beginNumber = 255;
					etBeginNumber.setText(beginNumber.toString());
				}
				sbBeginNumber.setProgress(beginNumber - 1);

				for (EditText et : etList) {
					et.setText((beginNumber++).toString());
					if (beginNumber > 255)
						beginNumber = 255;
				}
			}
		});
		etBeginNumber.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				Util.UiUtil.hideSoftKeyboard(getActivity(), event);
				return false;
			}
		});

		sbBeginNumber.setMax(254);
		sbBeginNumber.setProgress(0);
		sbBeginNumber.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Integer beginNumber = sbBeginNumber.getProgress() + 1;
				etBeginNumber.setText(beginNumber.toString());
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Integer beginNumber = sbBeginNumber.getProgress() + 1;
				etBeginNumber.setText(beginNumber.toString());
				for (EditText et : etList) {
					et.setText((beginNumber++).toString());
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
					Job job = (Job) et.getTag();
					job.setCN(et.getText().toString());
				}
				if (jobFile.getJobCount().getTotal() > 0) {
					jobFile.saveFile();
					adapter.notifyDataSetChanged();
					mListView.refreshDrawableState();
					show("저장 완료: " + jobFile.getJobCount().fi.getName());
				}
			}
		});

		dialog.show();
	}

	public String onGetWorkPath() {
		if (mListener != null) {
			return mListener.onGetWorkPath();
		}
		return null;
	}

	@Override
	public void onAttach(Context activity) {
		super.onAttach(activity);
		try {
			mListener = (OnWorkPathListener) activity;
		} catch (ClassCastException e) {
/*
			throw new ClassCastException(activity.toString()
					+ " must implement OnPathChangedListener");
*/
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	}

	@Override
	public void refresh(boolean forced) {
		if (forced || adapter.getCount() == 0) {
			mWorkPath = onGetWorkPath();
			if (adapter == null)
				adapter = new WeldCountAdapter<>(getActivity());
			else
				adapter.clear();

			try {
				File dir = new File(mWorkPath);
				for (File file : dir.listFiles()) {
					if (file.getName().toUpperCase().endsWith(".JOB") || file.getName().toUpperCase().startsWith("HX"))
						adapter.add(new JobFile(file.getPath()));
				}
				adapter.notifyDataSetChanged();
				mListView.refreshDrawableState();
			} catch (Exception e) {

			}
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
			if (msg == null)
				return;
			Snackbar.make(mView.findViewById(R.id.listView), msg, Snackbar.LENGTH_SHORT)
					.setAction("Action", null).show();
			logDebug(msg);
		} catch (Exception e) {
			logDebug(msg);
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
	public interface OnWorkPathListener {
		String onGetWorkPath();
	}
}
