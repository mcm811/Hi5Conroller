package com.changyoung.hi5controller;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chang on 2015-10-13.
 */
class Job extends java.lang.Object {
	public static final int ROWTYPES_COMMENT = 1;
	public static final int ROWTYPES_SPOT = 2;
	public static final int ROWTYPES_MOVE = 3;
	public static final int ROWTYPES_WAIT = 4;
	public static final int ROWTYPES_DO = 5;
	public static final int ROWTYPES_HEADER = 0;
	public static final int ROWTYPES_CALL = 6;
	public static final int ROWTYPES_END = 7;
	public static final int ROWTYPES_ETC = 8;
	RowJob row;

	public Job(Integer rowNumber, String rowString) {
		Integer rowType = getRowType(rowString);

		switch (rowType) {
			case ROWTYPES_HEADER:
				row = new HeaderJob(rowNumber, rowString);
				break;
			case ROWTYPES_COMMENT:
				row = new CommentJob(rowNumber, rowString);
				break;
			case ROWTYPES_SPOT:
				row = new SpotJob(rowNumber, rowString);
				break;
			case ROWTYPES_MOVE:
				row = new MoveJob(rowNumber, rowString);
				break;
			case ROWTYPES_WAIT:
				row = new WaitJob(rowNumber, rowString);
				break;
			case ROWTYPES_DO:
				row = new DoJob(rowNumber, rowString);
				break;
			case ROWTYPES_CALL:
				row = new CallJob(rowNumber, rowString);
				break;
			case ROWTYPES_END:
				row = new EndJob(rowNumber, rowString);
				break;
			case ROWTYPES_ETC:
				row = new EtcJob(rowNumber, rowString);
				break;
		}
	}

	public static void logDebug(String msg) {
		try {
			Log.d("Job", "[" + msg + "]");
		} catch (Exception e) {
		}
	}

	private Integer getRowType(String rowString) {
		Integer rowType = ROWTYPES_ETC;
		String[] s = rowString.trim().split(" ");
		if (s.length > 0) {
			if (s[0].equals("Program"))
				rowType = ROWTYPES_HEADER;
			else if (s[0].startsWith("'"))
				rowType = ROWTYPES_COMMENT;
			else if (s[0].startsWith("SPOT"))
				rowType = ROWTYPES_SPOT;
			else if (s[0].startsWith("S"))
				rowType = ROWTYPES_MOVE;
			else if (s[0].startsWith("WAIT"))
				rowType = ROWTYPES_WAIT;
			else if (s[0].startsWith("DO"))
				rowType = ROWTYPES_DO;
			else if (s[0].startsWith("END"))
				rowType = ROWTYPES_END;
		}
		return rowType;
	}

	public String getCN() {
		if (getRowType() == ROWTYPES_SPOT)
			return ((SpotJob) row).getCN();
		else
			return null;
	}

	public void setCN(String value) {
		if (getRowType() == ROWTYPES_SPOT)
			((SpotJob) row).setCN(value);
	}

	public String getGN() {
		if (getRowType() == ROWTYPES_SPOT)
			return ((SpotJob) row).getGN();
		else
			return null;
	}

	public String getG() {
		if (getRowType() == ROWTYPES_SPOT)
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

		public void setType(String type) {
			this.mType = type;
		}

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

		public void setRowType(Integer value) {
			mRowType = value;
		}

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
		String version;
		String mechType;
		String totalAxis;
		String auxAxis;

		public HeaderJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_HEADER, rowNumber, rowString);
		}
	}

	public class CommentJob extends RowJob {
		public CommentJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_COMMENT, rowNumber, rowString);
		}
	}

	public class SpotJob extends RowJob {
		ArrayList<JobValue> mJobValueList;

		public SpotJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_SPOT, rowNumber, rowString);
			mJobValueList = new ArrayList<>();
			String[] s = rowString.trim().split(" ");
			if (s.length == 2) {
				String[] f = s[1].split(",");
				for (Integer i = 0; i < f.length; i++) {
					mJobValueList.add(new JobValue(f[i]));
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
		Integer step;

		public MoveJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_MOVE, rowNumber, rowString);
		}
	}

	public class WaitJob extends RowJob {
		public WaitJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_WAIT, rowNumber, rowString);
		}
	}

	public class DoJob extends RowJob {
		public DoJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_DO, rowNumber, rowString);
		}
	}

	public class CallJob extends RowJob {
		public CallJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_CALL, rowNumber, rowString);
		}
	}

	public class EndJob extends RowJob {
		public EndJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_END, rowNumber, rowString);
		}
	}

	public class EtcJob extends RowJob {
		public EtcJob(Integer rowNumber, String rowString) {
			super(ROWTYPES_ETC, rowNumber, rowString);
		}
	}
}

class JobCount {
	public File fi;
	private Integer total;              // SPOT 의 총 카운트
	private ArrayList<Integer> gn;      // SPOT 단어 다음에 나오는 첫번째 단어 분석 해서 종류 결정(GN1, GN2, GN3, G1, G2)
	private ArrayList<Integer> g;
	private Integer step;               // S1 S2 S3 붙은 것들 젤 마지막 S번호 값
	private String preview;

	public JobCount(String fileName) {
		try {
			fi = new File(fileName);
		} catch (Exception e) {
			fi = null;
		}
		total = 0;
		gn = new ArrayList<>();
		g = new ArrayList<>();
		step = 0;
	}

	public void IncreaseGN(String strIndex) {
		Integer index = Integer.parseInt(strIndex);
		if (gn.size() < index) {
			for (Integer i = gn.size(); i < index; i++) {
				gn.add(0);
			}
		}
		gn.set(index - 1, gn.get(index - 1) + 1);
	}

	public void IncreaseG(String strIndex) {
		Integer index = Integer.parseInt(strIndex);
		if (g.size() < index) {
			for (Integer i = g.size(); i < index; i++) {
				g.add(0);
			}
		}
		g.set(index - 1, g.get(index - 1) + 1);
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
			sb.append("Total: " + total.toString());

		Integer n = 1;
		for (Integer item : gn) {
			sb.append(",  GN" + (n++).toString() + ": " + item.toString());
		}

		n = 1;
		for (Integer item : g) {
			sb.append(",  G" + (n++).toString() + ": " + item.toString());
		}

		if (step > 0) {
			if (total > 0)
				sb.append(",  ");
			sb.append("Step: " + step.toString());
		}

		return sb.toString();
	}
}

public class JobFile {
	private ArrayList<Job> jobList;
	private JobCount jobCount;

	public JobFile(String fileName) {
		jobList = new ArrayList<>();
		jobCount = new JobCount(fileName);
		readFile(fileName, jobList);
		buildJobCount(jobList, jobCount);
	}

	public Job get(Integer index) {
		return jobList.get(index);
	}

	public void set(Integer index, Job value) {
		jobList.set(index, value);
	}

	public Integer size() {
		return jobList.size();
	}

	public JobCount getJobCount() {
		return jobCount;
	}

	private List<Job> readFile(String fileName, ArrayList<Job> items) {
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
		} catch (Exception e) {
		}
		return items;
	}

	private void saveFile(String fileName, ArrayList<Job> items) {
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
			Job.logDebug("저장 완료:" + fileName);

			jobCount = new JobCount(fileName);
			buildJobCount(jobList, jobCount);
		} catch (Exception e) {
			Job.logDebug("저장 실패:" + fileName);
		}
	}

	public void saveFile() {
		saveFile(jobCount.fi.getPath(), jobList);
	}

	private void buildJobCount(ArrayList<Job> jobList, JobCount jobCount) {
		for (Job job : jobList) {
			String cn = job.getCN();
			if (cn != null) {
				jobCount.setTotal(jobCount.getTotal() + 1);
			}
			String gn = job.getGN();
			if (gn != null) {
				jobCount.IncreaseGN(gn);
			}
			String g = job.getG();
			if (g != null) {
				jobCount.IncreaseG(g);
			}
			if (job.getRowType() == Job.ROWTYPES_MOVE) {
				jobCount.setStep(jobCount.getStep() + 1);
			}
			if (job.getRowType() == Job.ROWTYPES_COMMENT) {
				if (jobCount.getPreview() == null)
					jobCount.setPreview(job.getRowString().trim());
			}
		}
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
				sb.append(cn + "  ");
			}
		}
		if (sb.length() > 0)
			sb.insert(0, "CN: ");
		return sb.toString();
	}

	public String getCNTest() {
		StringBuilder sb = new StringBuilder();

		for (Job job : jobList) {
			String cn = job.getCN();
			if (cn != null)
				sb.append(job.getRowNumber().toString() + ": CN=" + cn + "\n");
		}

		return sb.toString();
	}

	public String getRowText() {
		StringBuilder sb = new StringBuilder();
		for (Job Job : jobList) {
			sb.append(Job.getRowString() + "\n");
		}
		return sb.toString();
	}

	public void updateCN(Integer start) {
		for (Job job : jobList) {
			if (job.getRowType() == Job.ROWTYPES_SPOT) {
				job.setCN((start++).toString());
			}
		}
	}

	public void updateCN(Integer index, String value) {
		jobList.get(index).setCN(value);
	}
}
