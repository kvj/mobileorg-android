package com.matburt.mobileorg.service;

import java.io.BufferedReader;
import java.io.StringWriter;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.matburt.mobileorg.synchronizers.ReportableError;
import com.matburt.mobileorg.synchronizers.Synchronizer;

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
			"(((\\:[a-z]+)*\\:)+)?" +
			"(<before>(.*)</before>)?"+
			"(<after>(.*)</after>)?"+
			"$");
	private static Pattern drawerPattern = Pattern.compile("^\\s*:([A-Z_-]+):\\s*(.*)\\s*$");
	private static Pattern paramPattern = Pattern.compile("^\\s*([A-Z_-]+):\\s*(.*)\\s*$");
	
	//***: 1, 2: DONE, 3: [#A], 4: A, 5: title
	private static Pattern controlPattern = Pattern.compile("^\\#\\+([A-Z_-]+)(\\:\\s(.*))?$");
	//[[file:main.org][Main]] file: 3, main.org: 4 main.org: Main: 5
	private static Pattern linkPattern = Pattern.compile("\\[((\\[([a-zA-Z_-]+)\\:(.*)\\]\\[(.+)\\])|(\\[(.*)\\]))\\]");
	public static Pattern listPattern = Pattern.compile("^(\\s*)(\\+|\\-|\\*|(\\d+\\.))\\s(.+)$", Pattern.DOTALL);
	
	public OrgNGParser(DataController controller, Synchronizer synchronizer) {
		this.controller = controller;
		this.synchronizer = synchronizer;
	}
	
	interface ItemListener {
		
		void onSharpLine(String name, String value);
		void onItem(NoteNG note);
	}
	
	private void debugExp(Matcher m) {
		for (int i = 1; i <= m.groupCount(); i++) {
			Log.i(TAG, "Matcher "+i+", ["+m.group(i)+"]");
		}
	}
	
	class ParseFileOptions {
		boolean agenda = false;
		int todoGroup = 0;
		boolean sharpOnly = false;
	}
	
	public String parseFile(String name, ItemListener listener, NoteNG _parent, ParseFileOptions options) {
		try {
			BufferedReader reader = synchronizer.fetchOrgFile(name);
			String line = null;
			Marker marker = Marker.Unknown;
			Stack<NoteNG> parents = new Stack<NoteNG>();
			NoteNG parent = _parent;
			StringBuilder drawerBuilder = new StringBuilder();
			Map<String, String> drawerValues = new HashMap<String, String>();
			NoteNG listNote = null;
			Stack<NoteNG> listParents = new Stack<NoteNG>();
			while ((line = reader.readLine()) != null) {
				if (marker == Marker.Drawer) {
					drawerBuilder.append('\n');
					drawerBuilder.append(line.trim());
					Matcher m = drawerPattern.matcher(line);
					if (m.find()) {
						if ("END".equals(m.group(1).trim())) {
							marker = Marker.Outline;
//							Log.i(TAG, "Drawer OK: "+drawerBuilder+", "+drawerValues);
							NoteNG newNote = new NoteNG();
							newNote.raw = drawerBuilder.toString();
							newNote.type = NoteNG.TYPE_DRAWER;
							newNote.fileID = parent.fileID;
							newNote.parentID = parent.id;
							newNote.level = parent.level;
							controller.addData(newNote);
							String originalID = drawerValues.get("ORIGINAL_ID");
							if (null != originalID) {
								controller.updateData(parent, "original_id", originalID);
							}
							String noteID = drawerValues.get("ID");
							if (null != noteID) {
								controller.updateData(parent, "note_id", noteID);
							}
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
//					Log.i(TAG, "Outline: "+line);
					marker = Marker.Outline;
//					debugExp(m);
					NoteNG note = new NoteNG();
					note.type = NoteNG.TYPE_OUTLINE;
					if (options.agenda) {
						note.type = NoteNG.TYPE_AGENDA_OUTLINE;
					}
					note.raw = line;
					note.title = m.group(5);
					note.priority = m.group(4);
					note.todo = m.group(2);
					if (null != note.todo) {
						note.todo = note.todo.trim();
					} else {
						if (options.agenda) {
							note.type = NoteNG.TYPE_AGENDA;
						}
					}
					int level = m.group(1).trim().length();
					note.level = level;
					if (level<=parent.level) {
						//Search for parent
						while(!parents.isEmpty()) {
							parent = parents.pop();
							if (parent.level<level) {
								break;
							}
						}
					}
					note.fileID = parent.fileID;
					note.parentID = parent.id;
					Matcher m2 = outlineTailPattern.matcher(note.title);
					if (m2.find()) {
//						debugExp(m2);
						//before: 5, after: 7
						StringBuffer buffer = new StringBuffer();
						m2.appendReplacement(buffer, "");
						note.title = buffer.toString().trim();
						note.tags = m2.group(1);
						note.before = m2.group(5);
						note.after = m2.group(7);
					}
					controller.addData(note);
					parents.push(parent);
					parent = note;
					if (null != listNote) {
						controller.addData(listNote);
						listNote = null;
					}
					listener.onItem(note);
//					Log.i(TAG, "Note["+note.parentID+"] ["+note.title+"] ["+note.before+"] ["+note.after+"]");
					continue;
				}
				m = drawerPattern.matcher(line);
				if (m.find()) {
					marker = Marker.Drawer;
					drawerBuilder = new StringBuilder();
					drawerValues.clear();
					drawerBuilder.append(line.trim());
//					Log.i(TAG, "Start drawer: "+m.group(1));
					if (null != listNote) {
						controller.addData(listNote);
						listNote = null;
					}
					continue;
				}
				m = controlPattern.matcher(line);
				if (m.find()) {
//					Log.i(TAG, "Control: "+m+", "+line);
					//1 TODO 3 TODO DONE
					//debugExp(m);
					if (null != listNote) {
						controller.addData(listNote);
						listNote = null;
					}
					listener.onSharpLine(m.group(1), m.group(3));
					continue;
				}
				m = paramPattern.matcher(line);
				if (m.find()) {
					if (null != listNote) {
						controller.addData(listNote);
						listNote = null;
					}
					NoteNG n = new NoteNG();
					n.editable = false;
					n.fileID = parent.fileID;
					n.parentID = parent.id;
					n.raw = line.trim();
					n.title = line.trim();
					n.type = NoteNG.TYPE_PROPERTY;
					controller.addData(n);
					continue;
				}
//				Log.i(TAG, "Unknown line: "+line);
				m = listPattern.matcher(line);
				if (m.find()) {
					//1: spaces 2: -/+/*/1. 4: Text
					//debugExp(m);
					if (null != listNote) {
						controller.addData(listNote);
					}
					int newIndent = m.group(1).replace("\t", "        ").length();
					NoteNG n = new NoteNG();
					n.before = m.group(2);
					n.editable = true;
					n.raw = m.group(4).trim();
					n.title = m.group(4).trim();
					n.type = NoteNG.TYPE_SUBLIST;
					n.indent = newIndent;
					if (null != listNote) {
						//Item before was sublist
						while (listNote.indent >= newIndent) {
							//Same level or left
							if (listParents.isEmpty()) {
								//No more list parents
								listNote = parent;
								break;
							}
							listNote = listParents.pop();
						}
					} else {
						listParents.clear();
						// First list item in outline
						listNote = parent;
					}
					n.fileID = listNote.fileID;
					n.parentID = listNote.id;
//					Log.i(TAG, "Next sublist: "+listNote.type+", "+newIndent+", "+
//							listNote.indent+", "+
//							n.title+" - "+listNote.title+", "+
//							listParents.size()+", "+listNote.id);
					if (NoteNG.TYPE_SUBLIST == listNote.type) {
						//Sub item - save to parents
						listParents.push(listNote);
					}
					listNote = n;
				} else {
					if (options.agenda && "".equals(line)) {
						//Empty line - step up
						if (null != listNote) {
							controller.addData(listNote);
							listNote = null;
						}
						if (!parents.empty()) {
							parent = parents.pop();
						}
						marker = Marker.Outline;
						continue;
					}
					if (null != listNote) {
						listNote.raw += '\n'+line.trim();
					} else {
						NoteNG n = new NoteNG();
						n.editable = true;
						n.fileID = parent.fileID;
						n.parentID = parent.id;
						n.raw = line.trim();
						n.title = line.trim();
						n.type = NoteNG.TYPE_TEXT;
						controller.addData(n);
					}
				}
			}
			if (null != listNote) {
				controller.addData(listNote);
				listNote = null;
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
	
	public String parse() {
		try {
			if (controller.hasChanges()) {
				StringWriter writer = new StringWriter();
				DataWriter dataWriter = new DataWriter(controller);
				if(!dataWriter.writeChanges(writer)) {
					return "Problem writing data";
				}
				Log.i(TAG, "Controller has changes: "+writer);
				if (!synchronizer.putFile(true, "mobileorg.org", writer.toString())) {
					return "Error sending data";
				}
				controller.clearChanges();
			} else {
				Log.i(TAG, "No changes to send");
			}
			String prevSession = controller.appContext.getStringPreference("prevSyncSession", "");
			Log.i(TAG, "Start sync");
			String newSession = synchronizer.getFileHash("checksums.dat");
			if (null != newSession && newSession.equals(prevSession)) {
				Log.i(TAG, "No changes detected - exiting");
				return null;
			}
			if (!controller.clearCaptured()) {
				return "DB error";
			}
			String checksums = synchronizer.fetchOrgFileString("checksums.dat");
			final Map<String, String> sums = synchronizer.getChecksums(checksums);
			//file1 s1, file2 s2, file3 s3
			Map<String, String> nowSums = controller.getChecksums();
			Log.i(TAG, "Comparing["+sums.size()+"] = ["+nowSums.size()+"]: "+sums+" and "+nowSums);
			if (sums.size() != nowSums.size()) {
				Log.i(TAG, "Full sync");
			} else if (!sums.get("index.org").equals(nowSums.get("index.org"))) {
				Log.i(TAG, "Index changed - full sync");
			} else {
				for (String name : nowSums.keySet()) {
					String chsum = nowSums.get(name);
					String otherSum = sums.get(name);
					if (chsum.equals(otherSum)) {
						sums.remove(name);
					}
				}
				Log.i(TAG, "Parse only["+sums.size()+"]: "+sums);
				for (String fileName : sums.keySet()) {
					String sum = sums.get(fileName);
					NoteNG root = controller.findAndRefreshFile(fileName);
					if (null == root) {
						return "DB error";
					}
					ParseFileOptions options = new ParseFileOptions();
					final boolean isAgenda = "agendas.org".equals(fileName);
					options.agenda = isAgenda;
					String error = parseFile(fileName, new ItemListener() {
						
						@Override
						public void onItem(NoteNG note) {
						}

						@Override
						public void onSharpLine(String name, String value) {
						}
					}, root, options);
					if (null != error) {
						return error;
					}
					if (!controller.updateFile(fileName, sum)) {
						return "DB error";
					}
				}
				controller.appContext.setStringPreference("prevSyncSession", newSession);
				return null;
			}
			if (!controller.cleanupDB(true)) {
				return "DB error";
			}
			//Next step - parse index.org
			NoteNG root = new NoteNG();
			Integer indexFileID = controller.addFile("index.org", sums.get("index.org"), null);
			root.fileID = indexFileID;
			if (null ==indexFileID) {
				return "DB error";
			}
			final ParseFileOptions indexOptions = new ParseFileOptions();
			String error = parseFile("index.org", new ItemListener() {
				
				@Override
				public void onItem(NoteNG note) {
//					Log.i(TAG, "See file note: "+note.title);
					Matcher m = linkPattern.matcher(note.title);
					if (!m.find()) {
						return;
					}
//					debugExp(m);
					note.title = m.group(5);
					if (!controller.updateData(note, "type", NoteNG.TYPE_FILE)) {
						return;
					}
					if (!controller.updateData(note, "title", note.title)) {
						return;
					}
					String fileName = m.group(4);
					Integer fileID = controller.addFile(fileName, sums.get(fileName), note.id);
					if (null == fileID) {
						return;
					}
					NoteNG n = new NoteNG();
					n.id = note.id;
					n.fileID = fileID;
					final boolean isAgenda = "agendas.org".equals(fileName);
					if (!isAgenda) {
						note.editable = true;
					}
					ParseFileOptions options = new ParseFileOptions();
					options.agenda = isAgenda;
					String error = parseFile(fileName, new ItemListener() {
						
						@Override
						public void onItem(NoteNG note) {
						}

						@Override
						public void onSharpLine(String name, String value) {
						}
					}, n, options);
					if (null != error) {
						Log.e(TAG, "Error parsing: "+error);
					}
				}

				@Override
				public void onSharpLine(String name, String value) {
//					Log.i(TAG, "# line: "+name+", "+value);
					if ("TODO".equals(name)) {
						String[] items = value.split("\\s");
						boolean done = false;
						for (int i = 0; i < items.length; i++) {
							if ("|".equals(items[i])) {
								done = true;
								continue;
							}
							controller.addTodoType(indexOptions.todoGroup, items[i], done);
						}
						indexOptions.todoGroup++;
					}
					if ("ALLPRIORITIES".equals(name)) {
						String[] items = value.split("\\s");
						for (int i = 0; i < items.length; i++) {
							controller.addPriorityType(items[i]);
						}
					}
				}
			}, root, indexOptions);
			NoteNG capturedNotes = new NoteNG();
			capturedNotes.type = NoteNG.TYPE_FILE;
			capturedNotes.fileID = -1;
			capturedNotes.level = 1;
			capturedNotes.title = "Captured";
			controller.addData(capturedNotes);
			Log.i(TAG, "Stop sync");
			if (null == error) {
				controller.appContext.setStringPreference("prevSyncSession", newSession);
			}
			return error;
		} catch (Throwable e) {
			Log.e(TAG, "Error while downloading", e);
			return e.getMessage();
		}
	}
}
