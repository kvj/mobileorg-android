package com.matburt.mobileorg.service;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.matburt.mobileorg.Error.ReportableError;
import com.matburt.mobileorg.Synchronizers.Synchronizer;

public class OrgNGParser {

	private static final String TAG = "OrgNGParser";
	DataController controller;
	Synchronizer synchronizer;
	
	private enum Marker {Unknown, Outline, Drawer, List, Block};
	private static Pattern outlinePattern = Pattern.compile("^(\\*+\\s)([A-Z0-9]+\\s+)?(\\[\\#([A-Z])\\])?(\\s*.+\\s*)(((\\:[^\\s]+)+\\:)*)\\s*$");
	private static Pattern controlPattern = Pattern.compile("^\\#\\+([A-Z]+)(\\:\\s(.*))?$");
	
	public OrgNGParser(DataController controller, Synchronizer synchronizer) {
		this.controller = controller;
		this.synchronizer = synchronizer;
	}
	
	interface ItemListener {
		void onItem(NoteNG note);
	}
	
	private void debugExp(Matcher m) {
		for (int i = 0; i <= m.groupCount(); i++) {
			Log.i(TAG, "Matcher "+i+", "+m.group(i));
		}
	}
	
	public String parseFile(String folder, String name, String checksum, ItemListener listener) {
		try {
			BufferedReader reader = synchronizer.fetchOrgFile(folder+name);
			String line = null;
			Marker marker = Marker.Unknown;
			while ((line = reader.readLine()) != null) {
				if (marker == Marker.Unknown) {
					Matcher m = controlPattern.matcher(line);
					Log.i(TAG, "Control: "+m+", "+line);
					if (m.find()) {
						//1 TODO 3 TODO DONE
						debugExp(m);
						continue;
					}
				}
				Matcher m = outlinePattern.matcher(line);
				if (m.find()) {
					marker = Marker.Outline;
					// 1 *** 2 ? 3 ? 4 ? 5 title 6 ? 7 ? 8 ?
					debugExp(m);
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Error parsing file:", e);
			return e.getMessage();
		} catch (ReportableError e) {
			Log.e(TAG, "Error downloading file:", e);
			return e.getMessage();
		}
		return null;
	}
	
	public String parse(String path) {
		try {
			String indexPath = path;
			if (indexPath.indexOf("/") != -1) {
				indexPath = indexPath.substring(0, indexPath.indexOf("/")+1);
			}
			String checksums = synchronizer.fetchOrgFileString(indexPath+"checksums.dat");
			Map<String, String> sums = synchronizer.getChecksums(checksums);
			//file1 s1, file2 s2, file3 s3
			Map<String, String> nowSums = controller.getChecksums();
			Log.i(TAG, "Comparing: "+sums+" and "+nowSums);
			//file1 s1, file2 s2`
//			List<String> filesToClear = new ArrayList<String>(nowSums.keySet());
//			filesToClear.removeAll(sums.keySet());
//			//empty
//			for (String file : sums.keySet()) {
//				String hash = sums.get(file);
//				String oldHash = nowSums.get(file);
//				if (hash.equals(oldHash) && "index.org" != "file") {
//					
//				}
//			}
			
			//Next step - parse index.org
			String error = parseFile(indexPath, "index.org", sums.get("index.org"), new ItemListener() {
				
				@Override
				public void onItem(NoteNG note) {
				}
			});
		} catch (ReportableError e) {
			Log.e(TAG, "Error while downloading", e);
			return e.getMessage();
		}
		return null;
	}
}
