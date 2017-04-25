package com.changyoung.hi5controller.weldcount;

import android.app.Activity;

import com.changyoung.hi5controller.common.Helper;

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

class WeldCountFile extends File {
	static final int VALUE_MAX = 255;
	private List<Job> jobList;
	private JobInfo jobInfo;

	WeldCountFile(String path) {
		super(path);
		readFile();
	}

	Job get(Integer index) {
		return jobList.get(index);
	}

//		public void set(Integer index, Job value) {
//			jobList.set(index, value);
//		}

	Integer size() {
		return jobList.size();
	}

	JobInfo getJobInfo() {
		return jobInfo;
	}

	void readFile() {
		jobList = readFile(getPath(), new ArrayList<>());
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
			WeldCountFragment.logD(e.getLocalizedMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return items;
	}

	void saveFile(Activity activity) {
		jobInfo = saveFile(getPath(), jobList, activity);
	}

	private JobInfo
	saveFile(String fileName, List<Job> items, Activity activity) {
		try {
			WeldCountFragment.logD("fileName:" + fileName);
			OutputStream outputStream = Helper.Pref.getWorkPathOutputStream(activity, fileName);

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "EUC-KR");
			BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
			for (Job item : items) {
				bufferedWriter.write(item.getRowString());
				bufferedWriter.newLine();
			}
			bufferedWriter.close();
			outputStreamWriter.close();
			outputStream.close();

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

	String getCNList() {
		StringBuilder sb = new StringBuilder();
		Integer n = 0;
		for (Job job : jobList) {
			String cn = job.getCN();
			if (cn != null) {
				if (++n > 500) {    // 500개 까지만 보여줌
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
//			for (Job job : jobList) {
//				String cn = job.getCN();
//				if (cn != null) {
//					sb.append(String.format(Locale.KOREA, "%d: CN=%s\n", job.getRowNumber(), cn));
//				}
//			}
//			return sb.toString();
//		}

	String getRowText() {
		StringBuilder sb = new StringBuilder();
		for (Job Job : jobList) {
			sb.append(Job.getRowString());
			sb.append("\n");
		}
		return sb.toString();
	}

	String getMoveList() {
		StringBuilder sb = new StringBuilder();
//			Integer n = 0;
		Job prevJob = null;
		for (Job job : jobList) {
			if (prevJob != null && job.isSpot()) {
				String mv = prevJob.getA();
				if (mv != null) {
//						if (mv.contains("A=0") && ++n < 100) {
//							sb.append(mv).append(" ");
//						} else {
//							sb.insert(0, " ").insert(0, mv);
//						}
					if (!mv.contains("A=0")) {
						sb.append(mv).append(" ");
					}
				}
			}
			prevJob = job;
		}
		if (sb.length() > 0)
			sb.insert(0, "MV: ");
		return sb.toString();
	}

	int updateZeroA() {
		int ret = 0;
		Job prevJob = null;
		for (Job job : jobList) {
			if (prevJob != null && job.isSpot()) {
				String mv = prevJob.getA();
				if (mv != null) {
					if (!mv.contains("A=0")) {
						if (prevJob.setZeroA())
							ret++;
					}
				}
			}
			prevJob = job;
		}
		return ret;
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
		private final List<Integer> gnList;  // SPOT 단어 다음에 나오는 첫번째 단어 분석 해서 종류 결정(GN1, GN2, GN3, G1, G2)
		private final List<Integer> gList;
		private Integer total;         // SPOT 의 총 카운트
		private Integer step;          // S1 S2 S3 붙은 것들 젤 마지막 S번호 값
		private String preview;

		JobInfo() {
			total = 0;
			step = 0;
			gnList = new ArrayList<>();
			gList = new ArrayList<>();
		}

		void IncreaseGN(String strIndex) {
			Integer index = Integer.parseInt(strIndex);
			if (gnList.size() < index) {
				for (Integer i = gnList.size(); i < index; i++) {
					gnList.add(0);
				}
			}
			gnList.set(index - 1, gnList.get(index - 1) + 1);
		}

		void IncreaseG(String strIndex) {
			Integer index = Integer.parseInt(strIndex);
			if (gList.size() < index) {
				for (Integer i = gList.size(); i < index; i++) {
					gList.add(0);
				}
			}
			gList.set(index - 1, gList.get(index - 1) + 1);
		}

		Integer getTotal() {
			return total;
		}

		void setTotal(Integer value) {
			total = value;
		}

		Integer getStep() {
			return step;
		}

		void setStep(Integer value) {
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
		static final int JOB_COMMENT = 1;
		static final int JOB_SPOT = 2;
		static final int JOB_MOVE = 3;
		static final int JOB_WAIT = 4;
		static final int JOB_DO = 5;
		static final int JOB_HEADER = 0;
		static final int JOB_CALL = 6;
		static final int JOB_END = 7;
		static final int JOB_ETC = 8;
		Job.RowJob row;

		Job(Integer rowNumber, String rowString) {
			Integer rowType = getRowType(rowString);

			switch (rowType) {
				case JOB_HEADER:
					row = new Job.HeaderJob(rowNumber, rowString);
					break;
				case JOB_COMMENT:
					row = new Job.CommentJob(rowNumber, rowString);
					break;
				case JOB_SPOT:
					row = new Job.SpotJob(rowNumber, rowString);
					break;
				case JOB_MOVE:
					row = new Job.MoveJob(rowNumber, rowString);
					break;
				case JOB_WAIT:
					row = new Job.WaitJob(rowNumber, rowString);
					break;
				case JOB_DO:
					row = new Job.DoJob(rowNumber, rowString);
					break;
				case JOB_CALL:
					row = new Job.CallJob(rowNumber, rowString);
					break;
				case JOB_END:
					row = new Job.EndJob(rowNumber, rowString);
					break;
				case JOB_ETC:
					row = new Job.EtcJob(rowNumber, rowString);
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

		boolean isSpot() {
			return getRowType() == JOB_SPOT;
		}

		String getCN() {
			if (getRowType() == JOB_SPOT)
				return ((Job.SpotJob) row).getCN();
			else
				return null;
		}

		void setCN(String value) {
			if (getRowType() == JOB_SPOT)
				((Job.SpotJob) row).setCN(value);
		}

		String getGN() {
			if (getRowType() == JOB_SPOT)
				return ((Job.SpotJob) row).getGN();
			else
				return null;
		}

		String getG() {
			if (getRowType() == JOB_SPOT)
				return ((Job.SpotJob) row).getG();
			else
				return null;
		}

		public String getA() {
			if (getRowType() == JOB_MOVE)
				return ((Job.MoveJob) row).getA();
			else
				return null;
		}

		boolean setZeroA() {
			return getRowType() == JOB_MOVE && ((MoveJob) row).setZeroA();
		}

		Integer getRowType() {
			return row.getRowType();
		}

		Integer getRowNumber() {
			return row.getRowNumber();
		}

		String getRowString() {
			return row.getRowString();
		}

		class JobValue {
			private String mType;
			private String mValue;

			JobValue(String str) {
				setUpdate(str);
			}

			String getValue() {
				return mValue;
			}

			void setValue(String value) {
				this.mValue = value;
			}

//			public String getType() {
//				return mType;
//			}

//			public void setType(String type) {
//				this.mType = type;
//			}

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

		class RowJob {
			private final Integer mRowType;
			private final Integer mRowNumber;
			private String mRowString;

			RowJob(Integer rowType, Integer rowNumber, String rowString) {
				mRowType = rowType;
				mRowNumber = rowNumber;
				mRowString = rowString;
			}

			Integer getRowType() {
				return mRowType;
			}

//			public void setRowType(Integer value) {
//				mRowType = value;
//			}

			Integer getRowNumber() {
				return mRowNumber;
			}

			String getRowString() {
				return mRowString;
			}

			void setRowString(String rowString) {
				this.mRowString = rowString;
			}
		}

		class HeaderJob extends Job.RowJob {
//				String version;
//				String mechType;
//				String totalAxis;
//				String auxAxis;

			HeaderJob(Integer rowNumber, String rowString) {
				super(JOB_HEADER, rowNumber, rowString);
			}
		}

		class CommentJob extends Job.RowJob {
			CommentJob(Integer rowNumber, String rowString) {
				super(JOB_COMMENT, rowNumber, rowString);
			}
		}

		class SpotJob extends Job.RowJob {
			final List<Job.JobValue> mJobValueList;
			StringBuilder mComment;

			SpotJob(Integer rowNumber, String rowString) {
				super(JOB_SPOT, rowNumber, rowString);
				mJobValueList = new ArrayList<>();

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
//						if (mComment != null) logD("mComment:" + mComment);
				} catch (Exception e) {
					e.printStackTrace();
				}
//					logD("rs:" + rs);

				String[] s = rs.trim().split(" +");
				if (s.length == 2) {
					String[] f = s[1].split(",");
					for (String aF : f) {
						mJobValueList.add(new Job.JobValue(aF));
					}
				}
			}

			void Update() {
				StringBuilder rs = new StringBuilder("     SPOT ");
				for (Job.JobValue jv : mJobValueList) {
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
				setRowString(rs.toString());
				WeldCountFragment.logD("UPDATE:" + rs);
			}

			String getCN() {
				for (Job.JobValue s : mJobValueList) {
					if (s.equalType("CN"))
						return s.getValue();
				}
				return null;
			}

			void setCN(String value) {
				for (Job.JobValue s : mJobValueList) {
					if (s.equalType("CN")) {
						s.setValue(value);
						Update();
					}
				}
			}

			String getGN() {
				for (Job.JobValue s : mJobValueList) {
					if (s.equalType("GN"))
						return s.getValue();
				}
				return null;
			}

			String getG() {
				for (Job.JobValue s : mJobValueList) {
					if (s.equalType("G"))
						return s.getValue();
				}
				return null;
			}
		}

		public class MoveJob extends Job.RowJob {
			final List<Job.JobValue> mJobValueList;
			StringBuilder mComment;
			String mStep;
			String mParam;

			MoveJob(Integer rowNumber, String rowString) {
				super(JOB_MOVE, rowNumber, rowString);
				mJobValueList = new ArrayList<>();

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
//						if (mComment != null) logD("mComment:" + mComment);
				} catch (Exception e) {
					e.printStackTrace();
				}
//					logD("rs:" + rs);

				String[] s = rs.trim().split(" +");
//					logD("s.Length:" + s.length);
//					for (String ds : s) {
//						logD("s[" + ds + "]");
//					}
				if (s.length == 4) {
					mStep = s[0].substring(1);
					//logD("step:" + mStep + ";");
					String[] f = s[2].split(",");
					for (String aF : f) {
						mJobValueList.add(new Job.JobValue(aF));
					}
					mParam = s[3].trim();
				}
			}

			void Update() {
				StringBuilder rs = new StringBuilder("S" + getStep() + (getStep().length() == 1 ? "   " : "  ") + "MOVE ");
				for (Job.JobValue jv : mJobValueList) {
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
				setRowString(rs.toString());
				WeldCountFragment.logD("UPDATE:" + rs);
			}

			public String getA() {
				for (Job.JobValue s : mJobValueList) {
					if (s.equalType("A")) {
						//logD("value[" + s.getValue() + "] " + getStep());
						//if (!s.getValue().equals("0"))
						return "S" + getStep() + ":A=" + s.getValue();
					}
				}
				return null;
			}

			boolean setZeroA() {
				boolean ret = false;
				for (Job.JobValue s : mJobValueList) {
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

		class WaitJob extends Job.RowJob {
			WaitJob(Integer rowNumber, String rowString) {
				super(JOB_WAIT, rowNumber, rowString);
			}
		}

		class DoJob extends Job.RowJob {
			DoJob(Integer rowNumber, String rowString) {
				super(JOB_DO, rowNumber, rowString);
			}
		}

		class CallJob extends Job.RowJob {
			CallJob(Integer rowNumber, String rowString) {
				super(JOB_CALL, rowNumber, rowString);
			}
		}

		class EndJob extends Job.RowJob {
			EndJob(Integer rowNumber, String rowString) {
				super(JOB_END, rowNumber, rowString);
			}
		}

		class EtcJob extends Job.RowJob {
			EtcJob(Integer rowNumber, String rowString) {
				super(JOB_ETC, rowNumber, rowString);
			}
		}
	}
}
