package com.matburt.mobileorg.synchronizers;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.text.TextUtils;

import com.matburt.mobileorg.service.DataController;

abstract public class Synchronizer {
	public DataController controller = null;
	public SharedPreferences appSettings = null;
	public Context rootContext = null;
	public static final String LT = "MobileOrg";
	public Resources r;
	final private int BUFFER_SIZE = 23 * 1024;

	public static class FileInfo {

		public FileInfo(BufferedReader reader) {
			this.reader = reader;
		}

		public BufferedReader reader = null;
		public long size = -1;
	}

	public FileInfo fetchOrgFile(String orgPath) throws NotFoundException,
			ReportableError {
		return null;
	}

	public String fetchOrgFileString(String orgPath) throws ReportableError {
		BufferedReader reader = this.fetchOrgFile(orgPath).reader;
		if (reader == null) {
			return "";
		}
		String fileContents = "";
		String thisLine = "";
		try {
			while ((thisLine = reader.readLine()) != null) {
				fileContents += thisLine + "\n";
			}
		} catch (java.io.IOException e) {
			throw new ReportableError("Error reading file", e);
		}
		return fileContents;
	}

	public Map<String, String> getChecksums(String master) {
		HashMap<String, String> chksums = new HashMap<String, String>();
		for (String eachLine : master.split("[\\n\\r]+")) {
			if (TextUtils.isEmpty(eachLine))
				continue;
			String[] chksTuple = eachLine.split("\\s+");
			String name = chksTuple[1];
			String value = chksTuple[0];
			if ("mobileorg.org".equals(name)) {
				continue;
			}
			chksums.put(name, value);
		}
		return chksums;
	}

	/* Use this to detect changes */
	public String getFileHash(String name) throws ReportableError {
		return null;
	}

	public boolean putFile(boolean append, String fileName, String data) {
		return false;
	}
}
