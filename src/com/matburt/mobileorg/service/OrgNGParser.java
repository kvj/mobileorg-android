package com.matburt.mobileorg.service;

import java.io.BufferedReader;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
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
	private static Pattern outlinePattern = Pattern.compile("^(\\*+\\s+)" +
			"([A-Z0-9]+\\s+)?" +
			"(\\[\\#([A-Z])\\]\\s+)?" +
			"(.+)$");
	private static Pattern outlineTailPattern = Pattern.compile(
			"(((\\:[a-z]+)+\\:)+)?" +
			"(<before>(.*)</before>)?"+
			"(<after>(.*)</after>)?"+
			"$");
	private static Pattern drawerPattern = Pattern.compile("^\\s*:([A-Z_-]+):\\s*(.*)\\s*$");
	
	//***: 1, 2: DONE, 3: [#A], 4: A, 5: title
	private static Pattern controlPattern = Pattern.compile("^\\#\\+([A-Z]+)(\\:\\s(.*))?$");
	//[[file:main.org][Main]] file: 3, main.org: 4 main.org: Main: 5
	private static Pattern linkPattern = Pattern.compile("\\[((\\[([a-zA-Z_-]+)\\:(.*)\\]\\[(.+)\\])|(\\[(.*)\\]))\\]");
	
	public OrgNGParser(DataController controller, Synchronizer synchronizer) {
		this.controller = controller;
		this.synchronizer = synchronizer;
	}
	
	interface ItemListener {
		boolean onItem(NoteNG note);
	}
	
	private void debugExp(Matcher m) {
		for (int i = 1; i <= m.groupCount(); i++) {
			Log.i(TAG, "Matcher "+i+", ["+m.group(i)+"]");
		}
	}
	
	public String parseFile(String folder, String name, ItemListener listener, NoteNG _parent) {
		try {
			BufferedReader reader = synchronizer.fetchOrgFile(folder+name);
			String line = null;
			Marker marker = Marker.Unknown;
			Stack<NoteNG> parents = new Stack<NoteNG>();
			parents.push(_parent);
			NoteNG parent = _parent;
			StringBuilder drawerBuilder = new StringBuilder();
			Map<String, String> drawerValues = new HashMap<String, String>();
			while ((line = reader.readLine()) != null) {
				if (marker == Marker.Unknown) {
					Matcher m = controlPattern.matcher(line);
					Log.i(TAG, "Control: "+m+", "+line);
					if (m.find()) {
						//1 TODO 3 TODO DONE
						//debugExp(m);
						continue;
					}
				}
				if (marker == Marker.Drawer) {
					drawerBuilder.append('\n');
					drawerBuilder.append(line);
					Matcher m = drawerPattern.matcher(line);
					if (m.find()) {
						if ("END".equals(m.group(1).trim())) {
							marker = Marker.Outline;
							Log.i(TAG, "Drawer OK: "+drawerBuilder+", "+drawerValues);
							NoteNG newNote = new NoteNG();
							newNote.raw = drawerBuilder.toString();
							newNote.type = NoteNG.TYPE_DRAWER;
							newNote.fileID = parent.fileID;
							newNote.parentID = parent.id;
							newNote.level = parent.level;
							controller.addData(newNote);
						} else {
							if (null != m.group(2)) {
								drawerValues.put(m.group(1).trim(), m.group(2).trim());
							}
						}
					}
					continue;
				}
				Matcher m = outlinePattern.matcher(line);
				if (m.find()) {
					Log.i(TAG, "Outline: "+line);
					marker = Marker.Outline;
//					debugExp(m);
					NoteNG note = new NoteNG();
					note.raw = line;
					note.title = m.group(5);
					Matcher m2 = outlineTailPattern.matcher(note.title);
					if (m2.find()) {
						//debugExp(m2);
						//before: 5, after: 7
						StringBuffer buffer = new StringBuffer();
						m2.appendReplacement(buffer, "");
						note.title = buffer.toString().trim();
						note.before = m2.group(1);
						note.before = m2.group(5);
						note.after = m2.group(7);
					}
					controller.addData(note);
					if(!listener.onItem(note)) {
						continue;
					}
					Log.i(TAG, "Note ["+note.title+"] ["+note.before+"] ["+note.after+"]");
					continue;
				}
				m = drawerPattern.matcher(line);
				if (m.find()) {
					marker = Marker.Drawer;
					drawerBuilder = new StringBuilder();
					drawerValues.clear();
					drawerBuilder.append(line);
					Log.i(TAG, "Start drawer: "+m.group(1));
					continue;
				}
				Log.i(TAG, "Unknown line: "+line);
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
			if (!controller.cleanupDB()) {
				return "DB error";
			}
			String _indexPath = path;
			if (_indexPath.indexOf("/") != -1) {
				_indexPath = _indexPath.substring(0, _indexPath.indexOf("/")+1);
			}
			final String indexPath = _indexPath;
			String checksums = synchronizer.fetchOrgFileString(indexPath+"checksums.dat");
			final Map<String, String> sums = synchronizer.getChecksums(checksums);
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
			Integer indexFileID = controller.updateFile("index.org", sums.get("index.org"));
			NoteNG root = new NoteNG();
			root.fileID = indexFileID;
			if (null ==indexFileID) {
				return "DB error";
			}
			String error = parseFile(indexPath, "index.org", new ItemListener() {
				
				@Override
				public boolean onItem(NoteNG note) {
					Log.i(TAG, "See file note: "+note.title);
					Matcher m = linkPattern.matcher(note.title);
					if (!m.find()) {
						return false;
					}
//					debugExp(m);
					note.title = m.group(5);
					if (!controller.updateData(note, "type", NoteNG.TYPE_FILE)) {
						return false;
					}
					String fileName = m.group(4);
					Integer fileID = controller.updateFile(fileName, sums.get(fileName));
					if (null == fileID) {
						return false;
					}
					note.fileID = fileID;
					final boolean isAgenda = "agendas.org".equals(fileName);
					if (!isAgenda) {
						note.editable = true;
					}
					String error = parseFile(indexPath, fileName, new ItemListener() {
						
						@Override
						public boolean onItem(NoteNG note) {
							if (isAgenda && note.level == 1) {
								controller.updateData(note, "type", NoteNG.TYPE_AGENDA);
							}
							return true;
						}
					}, note);
					if (null != error) {
						Log.e(TAG, "Error parsing: "+error);
					}
					return true;
				}
			}, root);
			return error;
		} catch (ReportableError e) {
			Log.e(TAG, "Error while downloading", e);
			return e.getMessage();
		}
	}
}
