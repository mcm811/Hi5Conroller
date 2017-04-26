package com.changyoung.hi5controller.weldcount;

import android.app.Activity;

import com.changyoung.hi5controller.common.PrefHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

class WeldCountJobFile extends File {
	static final int VALUE_MAX = 255;
	private List<JobFileItem> jobFileItemList;
	private JobFileInfo jobFileInfo;

	WeldCountJobFile(String path) {
		super(path);
		readFile();
	}

	JobFileItem get(int index) {
		return jobFileItemList.get(index);
	}

	int size() {
		return jobFileItemList.size();
	}

	JobFileInfo getJobFileInfo() {
		return jobFileInfo;
	}

	void readFile() {
		jobFileItemList = readFile(getPath(), new ArrayList<>());
		jobFileInfo = createJobInfo(jobFileItemList, new JobFileInfo());
	}

	private List<JobFileItem> readFile(String fileName, List<JobFileItem> items) {
		try {
			FileInputStream fileInputStream = new FileInputStream(fileName);
			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, "EUC-KR");
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

			String rowString;
			int rowNumber = 0;
			while ((rowString = bufferedReader.readLine()) != null) {
				items.add(new JobFileItem(rowNumber++, rowString));
			}

			bufferedReader.close();
			inputStreamReader.close();
			fileInputStream.close();
		} catch (FileNotFoundException e) {
			WeldCountFragment.logD(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return items;
	}

	void saveFile(Activity activity) {
		jobFileInfo = saveFile(getPath(), jobFileItemList, activity);
	}

	private JobFileInfo
	saveFile(String fileName, List<JobFileItem> items, Activity activity) {
		try {
			WeldCountFragment.logD("fileName:" + fileName);
			OutputStream outputStream = PrefHelper.getWorkPathOutputStream(activity, fileName);

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "EUC-KR");
			BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
			for (JobFileItem item : items) {
				bufferedWriter.write(item.getJobString());
				bufferedWriter.newLine();
			}
			bufferedWriter.close();
			outputStreamWriter.close();
			outputStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return createJobInfo(jobFileItemList, new JobFileInfo());
	}

	private JobFileInfo createJobInfo(List<JobFileItem> jobFileItemList, JobFileInfo jobFileInfo) {
		for (JobFileItem jobFileItem : jobFileItemList) {
			String cn = jobFileItem.getCN();
			if (cn != null) {
				jobFileInfo.setTotal(jobFileInfo.getTotal() + 1);
			}
			String gn = jobFileItem.getGN();
			if (gn != null) {
				jobFileInfo.IncreaseGN(gn);
			}
			String g = jobFileItem.getG();
			if (g != null) {
				jobFileInfo.IncreaseG(g);
			}
			if (jobFileItem.getJobType() == JobFileItem.JOB_MOVE) {
				jobFileInfo.setStep(jobFileInfo.getStep() + 1);
			}
			if (jobFileItem.getJobType() == JobFileItem.JOB_COMMENT) {
				if (jobFileInfo.getPreview() == null)
					jobFileInfo.setPreview(jobFileItem.getJobString().trim());
			}
		}
		return jobFileInfo;
	}

	String getCNList() {
		StringBuilder sb = new StringBuilder();
		int n = 0;
		for (JobFileItem jobFileItem : jobFileItemList) {
			String cn = jobFileItem.getCN();
			if (cn != null) {
				final int MAX_CN_COUNT = 500;
				if (++n > MAX_CN_COUNT) {
					sb.append("...");
					break;
				}
				sb.append(cn).append(" ");
			}
		}
		if (sb.length() > 0)
			sb.insert(0, "CN: ");
		return sb.toString();
	}

//		public String getCNTest() {
//			StringBuilder sb = new StringBuilder();
//			for (JobFileItem job : jobFileItemList) {
//				String cn = job.getCN();
//				if (cn != null) {
//					sb.append(String.format(Locale.KOREA, "%d: CN=%s\n", job.getJobNumber(), cn));
//				}
//			}
//			return sb.toString();
//		}

	String getRowText() {
		StringBuilder sb = new StringBuilder();
		for (JobFileItem JobFileItem : jobFileItemList) {
			sb.append(JobFileItem.getJobString());
			sb.append("\n");
		}
		return sb.toString();
	}

	String getMoveList() {
		StringBuilder sb = new StringBuilder();
		JobFileItem prevJobFileItem = null;
		for (JobFileItem jobFileItem : jobFileItemList) {
			if (prevJobFileItem != null && jobFileItem.isSpot()) {
				String mv = prevJobFileItem.getA();
				if (mv != null) {
					if (!mv.contains("A=0")) {
						sb.append(mv).append(" ");
					}
				}
			}
			prevJobFileItem = jobFileItem;
		}
		if (sb.length() > 0)
			sb.insert(0, "MV: ");
		return sb.toString();
	}

	int updateZeroA() {
		int ret = 0;
		JobFileItem prevJobFileItem = null;
		for (JobFileItem jobFileItem : jobFileItemList) {
			if (prevJobFileItem != null && jobFileItem.isSpot()) {
				String mv = prevJobFileItem.getA();
				if (mv != null) {
					if (!mv.contains("A=0")) {
						if (prevJobFileItem.setZeroA())
							ret++;
					}
				}
			}
			prevJobFileItem = jobFileItem;
		}
		return ret;
	}

//		public void updateCN(int start) {
//			for (JobFileItem job : jobFileItemList) {
//				if (job.getJobType() == JobFileItem.JOB_SPOT) {
//					job.setCN((start++).toString());
//				}
//			}
//		}

//		public void updateCN(int index, String value) {
//			jobFileItemList.get(index).setCN(value);
//		}

	class JobFileInfo {
		private final List<Integer> gnList;  // SPOT 단어 다음에 나오는 첫번째 단어 분석 해서 종류 결정(GN1, GN2, GN3, G1, G2)
		private final List<Integer> gList;
		private int total;         // SPOT 의 총 카운트
		private int step;          // S1 S2 S3 붙은 것들 젤 마지막 S번호 값
		private String preview;

		JobFileInfo() {
			total = 0;
			step = 0;
			gnList = new ArrayList<>();
			gList = new ArrayList<>();
		}

		void IncreaseGN(String strIndex) {
			int index = Integer.parseInt(strIndex);
			if (gnList.size() < index) {
				for (int i = gnList.size(); i < index; i++) {
					gnList.add(0);
				}
			}
			gnList.set(index - 1, gnList.get(index - 1) + 1);
		}

		void IncreaseG(String strIndex) {
			int index = Integer.parseInt(strIndex);
			if (gList.size() < index) {
				for (int i = gList.size(); i < index; i++) {
					gList.add(0);
				}
			}
			gList.set(index - 1, gList.get(index - 1) + 1);
		}

		int getTotal() {
			return total;
		}

		void setTotal(int value) {
			total = value;
		}

		int getStep() {
			return step;
		}

		void setStep(int value) {
			step = value;
		}

		String getPreview() {
			return preview;
		}

		void setPreview(String value) {
			preview = value;
		}

		public String getString() {
			StringBuilder sb = new StringBuilder();

			if (total > 0)
				sb.append("Total: ").append(total);

			int n = 1;
			for (int gnItem : gnList) {
				sb.append(",  GN").append(n++).append(": ").append(gnItem);
			}

			n = 1;
			for (int gItem : gList) {
				sb.append(",  G").append(n++).append(": ").append(gItem);
			}

			if (step > 0) {
				if (total > 0)
					sb.append(",  ");
				sb.append("Step: ").append(step);
			}

			return sb.toString();
		}
	}

	class JobFileItem {
		static final int JOB_HEADER = 0;
		static final int JOB_COMMENT = 1;
		static final int JOB_SPOT = 2;
		static final int JOB_MOVE = 3;
		static final int JOB_WAIT = 4;
		static final int JOB_DO = 5;
		static final int JOB_CALL = 6;
		static final int JOB_END = 7;
		static final int JOB_ETC = 8;
		Job job;

		JobFileItem(int jobNumber, String jobString) {
			int jobType = getJobType(jobString);

			switch (jobType) {
				case JOB_HEADER:
					job = new JobHeader(jobNumber, jobString);
					break;
				case JOB_COMMENT:
					job = new JobComment(jobNumber, jobString);
					break;
				case JOB_SPOT:
					job = new JobSpot(jobNumber, jobString);
					break;
				case JOB_MOVE:
					job = new JobMove(jobNumber, jobString);
					break;
				case JOB_WAIT:
					job = new JobWait(jobNumber, jobString);
					break;
				case JOB_DO:
					job = new JobDo(jobNumber, jobString);
					break;
				case JOB_CALL:
					job = new JobCall(jobNumber, jobString);
					break;
				case JOB_END:
					job = new JobEnd(jobNumber, jobString);
					break;
				case JOB_ETC:
					job = new JobEtc(jobNumber, jobString);
					break;
			}
		}

		private int getJobType(String jobString) {
			int jobType = JOB_ETC;
			String[] s = jobString.trim().split(" ");
			if (s.length > 0) {
				if (s[0].equals("Program"))
					jobType = JOB_HEADER;
				else if (s[0].startsWith("'"))
					jobType = JOB_COMMENT;
				else if (s[0].startsWith("SPOT"))
					jobType = JOB_SPOT;
				else if (s[0].startsWith("S"))
					jobType = JOB_MOVE;
				else if (s[0].startsWith("WAIT"))
					jobType = JOB_WAIT;
				else if (s[0].startsWith("DO"))
					jobType = JOB_DO;
				else if (s[0].startsWith("END"))
					jobType = JOB_END;
			}
			return jobType;
		}

		boolean isSpot() {
			return getJobType() == JOB_SPOT;
		}

		String getCN() {
			if (getJobType() == JOB_SPOT)
				return ((JobSpot) job).getCN();
			else
				return null;
		}

		void setCN(String value) {
			if (getJobType() == JOB_SPOT)
				((JobSpot) job).setCN(value);
		}

		String getGN() {
			if (getJobType() == JOB_SPOT)
				return ((JobSpot) job).getGN();
			else
				return null;
		}

		String getG() {
			if (getJobType() == JOB_SPOT)
				return ((JobSpot) job).getG();
			else
				return null;
		}

		public String getA() {
			if (getJobType() == JOB_MOVE)
				return ((JobMove) job).getA();
			else
				return null;
		}

		boolean setZeroA() {
			return getJobType() == JOB_MOVE && ((JobMove) job).setZeroA();
		}

		int getJobType() {
			return job.getJobType();
		}

		int getJobNumber() {
			return job.getJobNumber();
		}

		String getJobString() {
			return job.getJobString();
		}

		class JobTypeValue {
			private String mType;
			private String mValue;

			JobTypeValue(String str) {
				setUpdate(str);
			}

			String getValue() {
				return mValue;
			}

			void setValue(String value) {
				this.mValue = value;
			}

			boolean equalType(String s) {
				return !(mType == null || s == null) && mType.equals(s);
			}

			String getUpdate() {
				return mType == null || mType.isEmpty() ? mValue : mType + "=" + mValue;
			}

			void setUpdate(String value) {
				if (value != null) {
					String[] s = value.trim().split("=");
					if (s.length == 2) {
						mType = s[0];
						mValue = s[1];
					} else {
						mType = "";
						mValue = value;
					}
				}
			}
		}

		class Job {
			private final int mJobType;
			private final int mJobNumber;
			private String mJobString;

			Job(int jobType, int jobNumber, String jobString) {
				mJobType = jobType;
				mJobNumber = jobNumber;
				mJobString = jobString;
			}

			int getJobType() {
				return mJobType;
			}

			int getJobNumber() {
				return mJobNumber;
			}

			String getJobString() {
				return mJobString;
			}

			void setJobString(String jobString) {
				this.mJobString = jobString;
			}
		}

		class JobHeader extends Job {
			JobHeader(int rowNumber, String rowString) {
				super(JOB_HEADER, rowNumber, rowString);
			}
		}

		class JobComment extends Job {
			JobComment(int rowNumber, String rowString) {
				super(JOB_COMMENT, rowNumber, rowString);
			}
		}

		class JobSpot extends Job {
			final List<JobTypeValue> mJobTypeValueList;
			StringBuilder mComment;

			JobSpot(int rowNumber, String rowString) {
				super(JOB_SPOT, rowNumber, rowString);
				mJobTypeValueList = new ArrayList<>();

				// split commands, comments
				String rs = rowString;
				try {
					String[] cs = rs.trim().split("'");
					if (cs.length == 2) {
						rs = cs[0];
						mComment = new StringBuilder("'");
						mComment.append(cs[1]);
					} else if (cs.length > 2) {
						rs = cs[0];
						mComment = new StringBuilder("'");
						mComment.append(cs[1]);
						for (int i = 2; i < cs.length; i++) {
							if (!cs[1].trim().equals(cs[i].trim())) {
								mComment.append("'");
								mComment.append(cs[i]);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				String[] s = rs.trim().split(" +");
				if (s.length == 2) {
					String[] f = s[1].split(",");
					for (String aF : f) {
						mJobTypeValueList.add(new JobTypeValue(aF));
					}
				}
			}

			void Update() {
				StringBuilder rs = new StringBuilder("     SPOT ");
				for (JobTypeValue jv : mJobTypeValueList) {
					rs.append(jv.getUpdate());
					rs.append(",");
				}
				int n = rs.lastIndexOf(",");
				if (n != -1)
					rs.deleteCharAt(n);
				if (mComment != null && mComment.length() > 0) {
					rs.append(" ");
					rs.append(mComment);
				}
				setJobString(rs.toString());
				WeldCountFragment.logD("UPDATE:" + rs);
			}

			String getCN() {
				for (JobTypeValue s : mJobTypeValueList) {
					if (s.equalType("CN"))
						return s.getValue();
				}
				return null;
			}

			void setCN(String value) {
				for (JobTypeValue s : mJobTypeValueList) {
					if (s.equalType("CN")) {
						s.setValue(value);
						Update();
					}
				}
			}

			String getGN() {
				for (JobTypeValue s : mJobTypeValueList) {
					if (s.equalType("GN"))
						return s.getValue();
				}
				return null;
			}

			String getG() {
				for (JobTypeValue s : mJobTypeValueList) {
					if (s.equalType("G"))
						return s.getValue();
				}
				return null;
			}
		}

		public class JobMove extends Job {
			final List<JobTypeValue> mJobTypeValueList;
			StringBuilder mComment;
			String mStep;
			String mParam;

			JobMove(int rowNumber, String rowString) {
				super(JOB_MOVE, rowNumber, rowString);
				mJobTypeValueList = new ArrayList<>();

				// split commands, comments
				String rs = rowString;
				try {
					String[] cs = rs.trim().split("'");
					if (cs.length == 2) {
						rs = cs[0];
						mComment = new StringBuilder("'" + cs[1]);
					} else if (cs.length > 2) {
						rs = cs[0];
						mComment = new StringBuilder("'" + cs[1]);
						for (int i = 2; i < cs.length; i++) {
							if (!cs[1].trim().equals(cs[i].trim())) {
								mComment.append("'");
								mComment.append(cs[i]);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				String[] s = rs.trim().split(" +");
				if (s.length == 4) {
					mStep = s[0].substring(1);
					String[] f = s[2].split(",");
					for (String aF : f) {
						mJobTypeValueList.add(new JobTypeValue(aF));
					}
					mParam = s[3].trim();
				}
			}

			void Update() {
				StringBuilder rs = new StringBuilder("S" + getStep() + (getStep().length() == 1 ? "   " : "  ") + "MOVE ");
				for (JobTypeValue jv : mJobTypeValueList) {
					rs.append(jv.getUpdate());
					rs.append(",");
				}
				int n = rs.lastIndexOf(",");
				if (n != -1)
					rs.deleteCharAt(n);
				if (mParam != null && !mParam.isEmpty()) {
					rs.append("  ");
					rs.append(mParam);
				}
				if (mComment != null && mComment.length() > 0) {
					rs.append(" ");
					rs.append(mComment);
				}
				setJobString(rs.toString());
				WeldCountFragment.logD("UPDATE:" + rs);
			}

			public String getA() {
				for (JobTypeValue s : mJobTypeValueList) {
					if (s.equalType("A")) {
						return "S" + getStep() + ":A=" + s.getValue();
					}
				}
				return null;
			}

			boolean setZeroA() {
				boolean ret = false;
				for (JobTypeValue s : mJobTypeValueList) {
					if (s.equalType("A")) {
						WeldCountFragment.logD("Step:" + getStep() + " value:" + s.getValue());
						s.setValue("0");
						WeldCountFragment.logD("Step:" + getStep() + " value:" + s.getValue());
						Update();
						ret = true;
					}
				}
				return ret;
			}

			String getStep() {
				return mStep;
			}
		}

		class JobWait extends Job {
			JobWait(int rowNumber, String rowString) {
				super(JOB_WAIT, rowNumber, rowString);
			}
		}

		class JobDo extends Job {
			JobDo(int rowNumber, String rowString) {
				super(JOB_DO, rowNumber, rowString);
			}
		}

		class JobCall extends Job {
			JobCall(int rowNumber, String rowString) {
				super(JOB_CALL, rowNumber, rowString);
			}
		}

		class JobEnd extends Job {
			JobEnd(int rowNumber, String rowString) {
				super(JOB_END, rowNumber, rowString);
			}
		}

		class JobEtc extends Job {
			JobEtc(int rowNumber, String rowString) {
				super(JOB_ETC, rowNumber, rowString);
			}
		}
	}
}
