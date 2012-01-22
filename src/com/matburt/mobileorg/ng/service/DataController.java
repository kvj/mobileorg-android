package com.matburt.mobileorg.ng.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.SuperActivity;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils.InsertHelper;
import android.text.TextUtils;
import android.util.Log;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.OrgNGParser.ParseProgressListener;
import com.matburt.mobileorg.ng.synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.ng.synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.ng.synchronizers.Synchronizer;
import com.matburt.mobileorg.ng.synchronizers.WebDAVSynchronizer;

public class DataController {

	public static interface ControllerListener extends ParseProgressListener {

		public void dataModified();

		public void syncStarted();

		public void syncFinished(boolean success);
	}

	private static final String TAG = "DataController";
	MobileOrgDBHelper db = null;
	ApplicationContext appContext = null;
	ControllerListener listener = null;
	boolean inSync = false;
	int inEdit = 0;
	public static DateFormat timeFormat = new SimpleDateFormat("HH:mm");
	public static DateFormat dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd EEE", Locale.ENGLISH);
	InsertHelper dataInsertHelper = null;

	public DataController(ApplicationContext appContext, Context context) {
		this.appContext = appContext;
		db = new MobileOrgDBHelper(context, "MobileOrg");
		if (!db.open()) {
			Log.e(TAG, "Error opening DB");
			db = null;
		} else {
			dataInsertHelper = new InsertHelper(db.getDatabase(), "data");
		}
	}

	public void wrapExecSQL(String sqlText) {
		if (null == db) {
			return;
		}
		try {
			db.getDatabase().execSQL(sqlText);
		} catch (Exception e) {
			Log.e(TAG, "SQL error:", e);
		}
	}

	public Cursor wrapRawQuery(String sqlText) {
		Cursor result = null;
		if (null == db) {
			return result;
		}
		try {
			result = db.getDatabase().rawQuery(sqlText, null);
		} catch (Exception e) {
			Log.e(TAG, "SQL error:", e);
		}
		return result;
	}

	public HashMap<String, String> getOrgFiles() {
		HashMap<String, String> allFiles = new HashMap<String, String>();
		if (null == db) {
			return allFiles;
		}
		Cursor result = wrapRawQuery("SELECT file, name FROM files");
		if (result != null) {
			if (result.getCount() > 0) {
				result.moveToFirst();
				do {
					allFiles.put(result.getString(0), result.getString(1));
				} while (result.moveToNext());
			}
			result.close();
		}
		return allFiles;
	}

	public Map<String, String> getChecksums() {
		HashMap<String, String> fchecks = new HashMap<String, String>();
		Cursor result = this.wrapRawQuery("SELECT file, checksum FROM files");
		if (result != null) {
			if (result.getCount() > 0) {
				result.moveToFirst();
				do {
					fchecks.put(result.getString(0), result.getString(1));
				} while (result.moveToNext());
			}
			result.close();
		}
		return fchecks;
	}

	public void removeFile(String filename) {
		this.wrapExecSQL("DELETE FROM files " + "WHERE file = '" + filename
				+ "'");
		Log.i(TAG, "Finished deleting from files");
	}

	public void clearData() {
		this.wrapExecSQL("DELETE FROM data");
	}

	public void clearTodos() {
		this.wrapExecSQL("DELETE from todos");
	}

	public void clearPriorities() {
		this.wrapExecSQL("DELETE from priorities");
	}

	public void addOrUpdateFile(String filename, String name, String checksum) {
		Cursor result = this.wrapRawQuery("SELECT * FROM files "
				+ "WHERE file = '" + filename + "'");
		if (result != null) {
			if (result.getCount() > 0) {
				this.wrapExecSQL("UPDATE files set name = '" + name + "', "
						+ "checksum = '" + checksum + "' where file = '"
						+ filename + "'");
			} else {
				this.wrapExecSQL("INSERT INTO files (file, name, checksum) "
						+ "VALUES ('" + filename + "','" + name + "','"
						+ checksum + "')");
			}
			result.close();
		}
	}

	public ArrayList<HashMap<String, Integer>> getTodos() {
		ArrayList<HashMap<String, Integer>> allTodos = new ArrayList<HashMap<String, Integer>>();
		Cursor result = this.wrapRawQuery("SELECT tdgroup, name, isdone "
				+ "FROM todos order by tdgroup");
		if (result != null) {
			HashMap<String, Integer> grouping = new HashMap<String, Integer>();
			int resultgroup = 0;
			if (result.getCount() > 0) {
				result.moveToFirst();
				do {
					if (result.getInt(0) != resultgroup) {
						allTodos.add(grouping);
						grouping = new HashMap<String, Integer>();
						resultgroup = result.getInt(0);
					}
					grouping.put(result.getString(1), result.getInt(2));
				} while (result.moveToNext());
				allTodos.add(grouping);
			}
			result.close();
		}
		return allTodos;
	}

	public ArrayList<ArrayList<String>> getPriorities() {
		ArrayList<ArrayList<String>> allPriorities = new ArrayList<ArrayList<String>>();
		Cursor result = this
				.wrapRawQuery("SELECT tdgroup, name FROM priorities order by tdgroup");
		if (result != null) {
			ArrayList<String> grouping = new ArrayList();
			int resultgroup = 0;
			if (result.getCount() > 0) {
				result.moveToFirst();
				do {
					if (result.getInt(0) != resultgroup) {
						allPriorities.add(grouping);
						grouping = new ArrayList();
						resultgroup = result.getInt(0);
					}
					grouping.add(result.getString(1));
				} while (result.moveToNext());
				allPriorities.add(grouping);
			}
			result.close();
		}
		return allPriorities;
	}

	public void setTodoList(ArrayList<HashMap<String, Boolean>> newList) {
		this.clearTodos();
		int grouping = 0;
		for (HashMap<String, Boolean> entry : newList) {
			for (String key : entry.keySet()) {
				String isDone = "0";
				if (entry.get(key))
					isDone = "1";
				this.wrapExecSQL("INSERT INTO todos (tdgroup, name, isdone) "
						+ "VALUES (" + grouping + "," + "        '" + key
						+ "'," + "        " + isDone + ")");
			}
			grouping++;
		}
	}

	public void setPriorityList(ArrayList<ArrayList<String>> newList) {
		this.clearPriorities();
		for (int idx = 0; idx < newList.size(); idx++) {
			for (int jdx = 0; jdx < newList.get(idx).size(); jdx++) {
				this.wrapExecSQL("INSERT INTO priorities (tdgroup, name, isdone) "
						+ "VALUES ("
						+ Integer.toString(idx)
						+ ","
						+ "        '"
						+ newList.get(idx).get(jdx)
						+ "',"
						+ "        0)");
			}
		}
	}

	public long insert(String string, ContentValues recValues) {
		if (null == db) {
			return 0;
		}
		return db.getDatabase().insert(string, null, recValues);
	}

	public void update(String string, ContentValues recValues, String string2,
			String[] strings) {
		if (null == db) {
			return;
		}
		db.getDatabase().update(string, recValues, string2, strings);
	}

	public String refresh(final ParseProgressListener parseListener) {
		boolean success = false;
		try {
			if (inSync || inEdit > 0) {
				return "Sync is in progress";
			}
			inSync = true;
			if (null != listener) {
				listener.syncStarted();
			}
			String userSynchro = appContext.getStringPreference("syncSource",
					"");
			final Synchronizer appSync;
			if (userSynchro.equals("webdav")) {
				appSync = new WebDAVSynchronizer(appContext, this);
			} else if (userSynchro.equals("sdcard")) {
				appSync = new SDCardSynchronizer(appContext, this);
			} else if (userSynchro.equals("dropbox")) {
				appSync = new DropboxSynchronizer(appContext, this);
			} else {
				return "No configuration";
			}
			OrgNGParser parser = new OrgNGParser(this, appSync);
			String result = parser.parse(new ParseProgressListener() {

				@Override
				public void progress(int total, int totalPos, int current,
						int currentPos, String message) {
					if (null != listener) {
						listener.progress(total, totalPos, current, currentPos,
								message);
					}
					if (null != parseListener) {
						parseListener.progress(total, totalPos, current,
								currentPos, message);
					}
				}
			});
			success = result == null;
			if (success) {
				// Update all widgets
				App.getInstance().updateWidgets(-1);
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			inSync = false;
			if (null != listener) {
				listener.syncFinished(success);
			}
		}
		return "Configuration error";
	}

	public File getAttachmentFolder() {
		File cache = SuperActivity.getExternalCacheFolder(appContext);
		if (null != cache) {
			cache = new File(cache, "attachments");
			if (!cache.exists()) {
				if (cache.mkdir()) {
					return cache;
				}
			} else {
				return cache;
			}
		}
		return null;
	}

	public boolean cleanupDB(boolean full) {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			if (full) {
				db.getDatabase().delete("files", null, null);
				db.getDatabase().delete("data", null, null);
				db.getDatabase().delete("changes", null, null);
				db.getDatabase().delete("uploads", null, null);
				appContext.setStringPreference("prevSyncSession", "");
			}
			db.getDatabase().delete("todos", null, null);
			db.getDatabase().delete("priorities", null, null);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public Integer addFile(String name, String checksum, Integer dataID) {
		if (null == db) {
			return null;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			values.put("file", name);
			values.put("checksum", checksum);
			values.put("data_id", dataID);
			int result = (int) db.getDatabase().insert("files", null, values);
			db.getDatabase().setTransactionSuccessful();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return null;
	}

	public boolean updateFile(String name, String checksum) {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			values.put("checksum", checksum);
			db.getDatabase().update("files", values, "file=?",
					new String[] { name });
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public boolean addTodoType(int group, String name, boolean done) {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			values.put("groupnum", group);
			values.put("name", name);
			values.put("isdone", done ? 1 : 0);
			db.getDatabase().insert("todos", null, values);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public boolean addPriorityType(String name) {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			values.put("name", name);
			db.getDatabase().insert("priorities", null, values);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public class TodoState {
		public int group;
		public String name;
		public boolean done;
	}

	public List<TodoState> getTodoTypes() {
		if (null == db) {
			return new ArrayList<TodoState>();
		}
		List<TodoState> result = new ArrayList<TodoState>();
		try {
			Cursor c = db.getDatabase().query("todos",
					new String[] { "groupnum", "name", "isdone" }, null, null,
					null, null, "groupnum, isdone, id");
			if (c.moveToFirst()) {
				do {
					TodoState state = new TodoState();
					state.group = c.getInt(0);
					state.name = c.getString(1);
					state.done = c.getInt(2) == 1 ? true : false;
					result.add(state);
				} while (c.moveToNext());
			}
			c.close();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<TodoState>();
	}

	public List<String> getPrioritiesNG() {
		if (null == db) {
			return new ArrayList<String>();
		}
		List<String> result = new ArrayList<String>();
		try {
			Cursor c = db.getDatabase().query("priorities",
					new String[] { "name" }, null, null, null, null, "id");
			if (c.moveToFirst()) {
				do {
					result.add(c.getString(0));
				} while (c.moveToNext());
			}
			c.close();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}

	public Integer addData(NoteNG note) {
		return addData(note, false);
	}

	public Integer addData(NoteNG note, boolean haveTransaction) {
		if (null == db) {
			return null;
		}
		try {
			if (!haveTransaction) {
				db.getDatabase().beginTransaction();
			}
			ContentValues values = new ContentValues();
			values.put("after", note.after);
			values.put("before", note.before);
			values.put("parent_id", note.parentID);
			values.put("note_id", note.noteID);
			values.put("file_id", note.fileID);
			if (!TextUtils.isEmpty(note.priority)) {
				values.put("priority", note.priority);
			}
			if (!TextUtils.isEmpty(note.tags)) {
				values.put("tags", note.tags);
			}
			if (!TextUtils.isEmpty(note.todo)) {
				values.put("todo", note.todo);
			}
			values.put("raw", note.raw);
			values.put("title", note.title);
			values.put("type", note.type);
			values.put("editable", note.editable ? 1 : 0);
			values.put("level", note.level);
			values.put("habit", note.habit);
			int result = (int) dataInsertHelper.insert(values);
			note.id = result;
			if (!haveTransaction) {
				db.getDatabase().setTransactionSuccessful();
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!haveTransaction) {
				db.getDatabase().endTransaction();
			}
		}
		return null;
	}

	public boolean updateData(NoteNG note, String field, Object value) {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			if (null == value || value instanceof String) {
				values.put(field, (String) value);
			} else if (value instanceof Number) {
				values.put(field, (Integer) value);
			}
			db.getDatabase().update("data", values, "id=?",
					new String[] { note.id.toString() });
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public List<UploadBean> getUploads() {
		if (null == db) {
			return new ArrayList<UploadBean>();
		}
		List<UploadBean> result = new ArrayList<UploadBean>();
		try {
			Cursor c = db.getDatabase().query("uploads",
					new String[] { "name", "filename", "data_id" }, null, null,
					null, null, "id");
			if (c.moveToFirst()) {
				do {
					UploadBean bean = new UploadBean();
					bean.name = c.getString(0);
					bean.fileName = c.getString(1);
					bean.dataID = new Integer(c.getInt(2));
					result.add(bean);
				} while (c.moveToNext());
			}
			c.close();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<UploadBean>();
	}

	public boolean clearUploads() {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			db.getDatabase().delete("uploads", null, null);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public boolean addUpload(UploadBean upload) {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			values.put("name", upload.name);
			values.put("filename", upload.fileName);
			values.put("data_id", upload.dataID.intValue());
			db.getDatabase().insert("uploads", null, values);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	NoteNG cursorToNote(Cursor c) {
		NoteNG note = new NoteNG();
		note.id = c.getInt(0);
		// note.indent = c.getInt(1);
		note.editable = 1 == c.getInt(2);
		note.noteID = c.getString(3);
		note.originalID = c.getString(4);
		note.type = c.getString(5);
		note.priority = c.getString(6);
		note.todo = c.getString(7);
		note.title = c.getString(8);
		note.tags = c.getString(9);
		note.level = c.getInt(10);
		note.before = c.getString(11);
		note.after = c.getString(12);
		note.raw = c.getString(13);
		note.parentID = safeInt(c, 14);
		note.habit = c.getString(15);
		return note;
	}

	private Integer safeInt(Cursor c, int index) {
		try {
			return Integer.parseInt(c.getString(index));
		} catch (Exception e) {
		}
		return null;
	}

	static String[] dataFields = new String[] { "id", "indent", "editable",
			"note_id", "original_id", "type", "priority", "todo", "title",
			"tags", "level", "before", "after", "raw", "parent_id", "habit" };

	public List<NoteNG> getData(Integer parent) {
		if (null == db) {
			return new ArrayList<NoteNG>();
		}
		List<NoteNG> result = new ArrayList<NoteNG>();
		try {
			String whereStart = "parent_id is null";
			List<String> whereArgs = new ArrayList<String>();
			if (null != parent) {
				whereStart = "parent_id=?";
				whereArgs.add(parent.toString());
			}
			whereArgs.add(NoteNG.TYPE_DRAWER);
			whereArgs.add(NoteNG.TYPE_PROPERTY);
			Cursor c = db.getDatabase().query("data", dataFields,
					whereStart + " and type<>? and type<>?",
					whereArgs.toArray(new String[] {}),// parent == null? null:
														// parent.toString()
					null, null, "id");
			if (c.moveToFirst()) {
				do {
					NoteNG note = cursorToNote(c);
					if (NoteNG.TYPE_TEXT.equals(note.type)
							&& null != note.title
							&& "".equals(note.title.trim())) {
						continue;
					}
					result.add(note);
				} while (c.moveToNext());
			}
			c.close();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public NoteNG findNoteByNoteID(String noteID) {
		try {
			Cursor c = db.getDatabase().query("data", dataFields, "note_id=?",
					new String[] { noteID }, null, null, "id");
			NoteNG note = null;
			if (c.moveToFirst()) {
				note = cursorToNote(c);
			}
			c.close();
			return note;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public int getExpand(String link) {
		Matcher m = OrgNGParser.noteRefPattern.matcher(link);
		if (!m.find()) {
			return 1;
		}
		Log.i(TAG, "getExpand: " + m.group(2));
		if ("a".equals(m.group(2))) {
			return -1;
		}
		if ("e".equals(m.group(2))) {
			return 1;
		}
		if (m.group(2).startsWith("e")) {
			return Integer.parseInt(m.group(2).substring(1));
		}
		return 1;
	}

	public NoteNG findNoteByLink(String link) {
		if (null == db) {
			return null;
		}
		Matcher m = OrgNGParser.noteRefPattern.matcher(link);
		if (!m.find()) {
			return null;
		}
		// OrgNGParser.debugExp(m);
		String type = m.group(3);
		if ("id".equals(type)) {
			return findNoteByNoteID(m.group(4));
		}
		String[] parts = m.group(4).split("/");
		Integer parent = null;
		NoteNG result = null;
		for (int i = 0; i < parts.length; i++) {
			List<NoteNG> notes = getData(parent);
			result = null;
			for (int j = 0; j < notes.size(); j++) {
				if ("index".equals(type)) {
					if (Integer.toString(j).equals(parts[i])) {
						result = notes.get(j);
						break;
					}
				}
				if ("olp".equals(type)) {
					if (parts[i].equals(notes.get(j).title)) {
						result = notes.get(j);
						break;
					}
				}
			}
			if (null == result) {
				return null;
			}
			parent = result.id;
		}
		return result;
	}

	public NoteNG findNoteByID(Integer id) {
		try {
			if (null == id) {
				return null;
			}
			Cursor c = db.getDatabase().query("data", dataFields, "id=?",
					new String[] { id.toString() }, null, null, "id");
			NoteNG note = null;
			if (c.moveToFirst()) {
				note = cursorToNote(c);
			}
			c.close();
			return note;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/* Type: body, data, heading, todo, priority, tags */
	public boolean addChange(Integer noteID, String type, String oldValue,
			String newValue) {
		if (null == db) {
			return false;
		}
		Log.i(TAG, "addChange: " + type + ", " + oldValue + ", " + newValue);
		NoteNG note = findNoteByID(noteID);
		if (null == note) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			if (!"data".equals(type)) {
				// Not new item - try to update existing
				Cursor c = db.getDatabase().query("changes",
						new String[] { "id", "type" },
						"(type=? or type=?) and data_id=?",
						new String[] { type, "data", noteID.toString() }, null,
						null, "id");
				if (c.moveToFirst()) {
					if ("data".equals(c.getString(1))) {
						// We have data means new outline - don't need to modify
						Log.i(TAG, "New outline - finish");
						c.close();
						db.getDatabase().setTransactionSuccessful();
						return true;
					}
					// Found - update
					ContentValues values = new ContentValues();
					values.put("new_value", newValue);
					Log.i(TAG, "Existing - only update");
					db.getDatabase().update("changes", values, "id=?",
							new String[] { c.getString(0) });
					c.close();
					db.getDatabase().setTransactionSuccessful();
					return true;
				}
				c.close();
			}
			Log.i(TAG, "New change - insert");
			// Otherwise - insert new entry
			ContentValues values = new ContentValues();
			values.put("type", type);
			values.put("data_id", noteID);
			values.put("old_value", oldValue);
			values.put("new_value", newValue);
			db.getDatabase().insert("changes", null, values);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public boolean hasChanges() {
		if (null == db) {
			return false;
		}
		try {
			Cursor c = db.getDatabase().query("changes", new String[] { "id" },
					null, null, null, null, null);
			int result = c.getCount();
			c.close();
			return result > 0;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean clearChanges() {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			db.getDatabase().delete("changes", null, null);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public boolean clearCaptured() {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			db.getDatabase().delete("data", "file_id=? and type<>?",
					new String[] { "-1", NoteNG.TYPE_FILE });
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public String generateNoteID(int size) {
		char[] chars = { 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a',
				's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'z', 'x', 'c', 'v',
				'b', 'n', 'm', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' };
		StringBuilder sb = new StringBuilder();
		Random r = new Random(new Date().getTime());
		for (int i = 0; i < size; i++) {
			sb.append(chars[r.nextInt(chars.length)]);
		}
		return sb.toString();
	}

	public static class NewNoteData {
		public final Map<String, String> properties = new LinkedHashMap<String, String>();
		public final List<NoteNG> children = new ArrayList<NoteNG>();
		public String attachment = null;
	}

	public Integer createNewNote(NoteNG note, NewNoteData newNoteData) {
		if (null == db) {
			return null;
		}
		try {
			db.getDatabase().beginTransaction();
			if (null == note.parentID) {
				// No parentID - captured note - search for "Captured"
				Cursor c = db.getDatabase().query("data",
						new String[] { "id" },
						"file_id=-1 and parent_id is null", null, null, null,
						null);
				if (c.moveToFirst()) {
					Log.i(TAG, "Found parent for note");
					note.parentID = c.getInt(0);
					note.fileID = -1;
				} else {
					Log.w(TAG, "Parent ID for new note is not found");
					c.close();
					return null;
				}
				c.close();
			}
			note.noteID = generateNoteID(12);
			note.level = 1;
			Integer newNoteID = addData(note, true);// Note created
			if (null == newNoteID) {
				return null;
			}
			if (null != newNoteData.attachment) {
				Log.i(TAG, "Have att: " + newNoteData.attachment);
				File cache = getAttachmentFolder();
				if (null != cache) {
					Log.i(TAG, "Cache is OK: " + cache.getAbsolutePath());
					File inFile = new File(newNoteData.attachment);

					if (inFile.exists()) {
						UploadBean upload = new UploadBean(note,
								inFile.getName());
						if (copyStream(new FileInputStream(inFile),
								new FileOutputStream(new File(cache,
										upload.fileName)))) {
							if (addUpload(upload)) {
								Log.i(TAG, "Att copied: " + upload.fileName);
								newNoteData.properties.put("Attachments",
										upload.name);
								String newTags = note.tags;
								if (null == newTags) {
									newTags = ":";
								}
								newTags += "ATTACH:";
								updateData(note, "tags", newTags);
							}
						}
					}
				}
			}
			// Create 2 notes: properties with ID and text with date created
			NoteNG drawerNote = new NoteNG();
			drawerNote.parentID = note.id;
			drawerNote.fileID = note.fileID;
			StringBuilder propertiesText = new StringBuilder(":PROPERTIES:\n");
			propertiesText.append(":ID: " + note.noteID + "\n");
			for (String key : newNoteData.properties.keySet()) {
				String value = newNoteData.properties.get(key);
				propertiesText.append(":" + key + ": " + value + "\n");
			}
			propertiesText.append(":END:");
			drawerNote.raw = propertiesText.toString();
			drawerNote.title = drawerNote.raw;
			drawerNote.type = NoteNG.TYPE_DRAWER;
			if (null == addData(drawerNote)) {
				return null;
			}
			for (int i = 0; i < newNoteData.children.size(); i++) {
				NoteNG n = newNoteData.children.get(i);
				n.parentID = note.id;
				n.fileID = note.fileID;
				Integer childNoteID = addData(n);
				Log.i(TAG, "Created child: " + childNoteID + ", " + n.type);
			}
			NoteNG dateNote = new NoteNG();
			dateNote.parentID = note.id;
			dateNote.fileID = note.fileID;
			dateNote.raw = "[" + dateFormat.format(new Date()) + "]";
			dateNote.title = dateNote.raw;
			dateNote.type = NoteNG.TYPE_TEXT;
			if (null == addData(dateNote)) {
				return null;
			}
			db.getDatabase().setTransactionSuccessful();
			Log.i(TAG, "Note created: " + note.noteID + ", " + note.title);
			return note.id;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return null;
	}

	private boolean copyStream(InputStream inStream, OutputStream outStream) {
		try {
			byte[] buffer = new byte[4096];
			int bytes = -1;
			while ((bytes = inStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, bytes);
			}
			inStream.close();
			outStream.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean removeData(Integer id) {
		return removeData(id, false);
	}

	private boolean removeData(Integer id, boolean inTransaction) {
		if (null == db) {
			return false;
		}
		try {
			if (!inTransaction) {
				db.getDatabase().beginTransaction();
			}
			Cursor c = db.getDatabase().query("data", new String[] { "id" },
					"parent_id=?", new String[] { id.toString() }, null, null,
					null);
			if (c.moveToFirst()) {
				do {
					if (!removeData(c.getInt(0), true)) {
						c.close();
						return false;
					}
				} while (c.moveToNext());
			}
			c.close();
			db.getDatabase().delete("data", "id=?",
					new String[] { id.toString() });
			if (!inTransaction) {
				db.getDatabase().setTransactionSuccessful();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (!inTransaction) {
				db.getDatabase().endTransaction();
			}
		}
		return false;
	}

	public NoteNG findAndRefreshFile(String fileName) {
		if (null == db) {
			return null;
		}
		try {
			db.getDatabase().beginTransaction();
			Cursor file = db.getDatabase().query("files",
					new String[] { "id", "data_id" }, "file=?",
					new String[] { fileName }, null, null, null);
			if (!file.moveToFirst()) {
				Log.e(TAG, "File not found: " + fileName);
				file.close();
				return null;
			}
			int fileID = file.getInt(0);
			int noteID = file.getInt(1);
			file.close();
			NoteNG note = findNoteByID(noteID);
			note.fileID = fileID;
			db.getDatabase().delete("data", "file_id=?",
					new String[] { Integer.toString(fileID) });
			db.getDatabase().setTransactionSuccessful();
			return note;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return null;
	}

	public Context getContext() {
		return appContext;
	}

	public void setListener(ControllerListener listener) {
		this.listener = listener;
	}

	public synchronized void setInEdit(boolean inEdit) {
		if (inEdit) {
			this.inEdit++;
		} else {
			if (this.inEdit > 0) {
				this.inEdit--;
			}
		}
	}

	public void notifyChangesHaveBeenMade() {
		if (null != listener) {
			listener.dataModified();
		}
		// App.getInstance().updateWidgets(-1);
	}

	public boolean updateData(String cfield, String cvalue, String field,
			Object value) {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			if (null == value || value instanceof String) {
				values.put(field, (String) value);
			} else if (value instanceof Number) {
				values.put(field, (Integer) value);
			}
			db.getDatabase().update("data", values, cfield + "=?",
					new String[] { cvalue });
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}

	public List<NoteNG> search(String query, int size,
			Collection<String> todos, Collection<String> priorities) {
		if (null == db || null == query || "".equals(query.trim())) {
			return new ArrayList<NoteNG>();
		}
		List<NoteNG> result = new ArrayList<NoteNG>();
		String[] parts = query.split("\\s");
		try {
			StringBuilder where = new StringBuilder();
			List<String> whereParams = new ArrayList<String>();
			where.append("type=?");
			whereParams.add(NoteNG.TYPE_OUTLINE);
			List<String> titleSearches = new ArrayList<String>();
			for (int i = 0; i < parts.length; i++) {
				String part = parts[i];
				if (todos.contains(part)) {
					where.append(" and todo=?");
					whereParams.add(part);
					continue;
				}
				if (part.startsWith("#")
						&& priorities.contains(part.substring(1))) {
					where.append(" and priority=?");
					whereParams.add(part.substring(1));
					continue;
				}
				if (part.startsWith("id:")) {
					where.append(" and note_id=?");
					whereParams.add(part.substring(3));
					continue;
				}
				if (part.startsWith(":")) {
					String[] tags = part.split("\\:");
					for (int j = 0; j < tags.length; j++) {
						if ("".equals(tags[j])) {
							continue;
						}
						where.append(" and tags like ?");
						whereParams.add("%:" + tags[j] + ":%");
					}
					continue;
				}
				titleSearches.add(part);
				where.append(" and title like ?");
				whereParams.add("%" + part + "%");
			}
			// Log.i(TAG, "Search: " + where + ", " + whereParams);
			Cursor c = db.getDatabase().query("data", dataFields,
					where.toString(), whereParams.toArray(new String[] {}),
					null, null, "id", size > 0 ? Integer.toString(size) : null);
			if (c.moveToFirst()) {
				do {
					NoteNG note = cursorToNote(c);
					result.add(note);
				} while (c.moveToNext());
			}
			c.close();
			if (size > 0 && result.size() >= size) {
				return result;
			}
			if (titleSearches.size() == 0) {
				return result;
			}
			where = new StringBuilder();
			whereParams.clear();
			where.append("(type=? or type=?)");
			whereParams.add(NoteNG.TYPE_TEXT);
			whereParams.add(NoteNG.TYPE_SUBLIST);
			for (int i = 0; i < titleSearches.size(); i++) {
				String part = titleSearches.get(i);
				where.append(" and title like ?");
				whereParams.add("%" + part + "%");
			}
			c = db.getDatabase().query("data",
					new String[] { "parent_id", "title" }, where.toString(),
					whereParams.toArray(new String[0]), null, null, "id");
			// Log.i(TAG,
			// "Search2: " + where + ", " + whereParams + ", "
			// + c.getCount() + ", " + size);
			if (c.moveToFirst()) {
				do {
					String parentID = c.getString(0);
					while (null != parentID) {
						Cursor c2 = db.getDatabase().query("data", dataFields,
								"id=?", new String[] { parentID }, null, null,
								null);
						if (!c2.moveToFirst()) {
							// Log.i(TAG, "No parent");
							c2.close();
							break;
						}
						if (NoteNG.TYPE_OUTLINE.equals(c2.getString(5))) {
							// Log.i(TAG, "Found parent");
							NoteNG note = cursorToNote(c2);
							note.subtitle = c.getString(1);
							result.add(note);
							if (size > 0 && result.size() >= size) {
								c2.close();
								c.close();
								return result;
							}
							break;
						}
						if (NoteNG.TYPE_SUBLIST.equals(c2.getString(5))) {
							// Log.i(TAG, "Found parent sublist");
							parentID = c2.getString(14);
							c2.close();
							continue;
						}
						Log.i(TAG, "Invalid parent");
						c2.close();
						break;
					}
				} while (c.moveToNext());
			}
			c.close();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<NoteNG>();
	}

	public String getCryptTag() {
		String cryptTag = ":"
				+ appContext.getStringPreference(R.string.cryptTag,
						R.string.cryptTagDefault) + ":";
		return cryptTag;
	}

	public String getID(NoteNG note) {
		if (null != note.originalID) {
			return "id:" + note.originalID;
		}
		if (null != note.noteID) {
			return "id:" + note.noteID;
		}
		if (null == db) {
			return null;
		}
		String olp = note.title;
		Integer parent = note.parentID;
		// Log.i(TAG, "Start olp: " + olp + ", " + parent);
		try {
			while (null != parent) {
				NoteNG n = findNoteByID(parent);
				// Log.i(TAG, "Olp: " + olp + ", " + n);
				if (null == n) {
					parent = null;
				} else {
					if (NoteNG.TYPE_FILE.equals(n.type)) {
						// Log.i(TAG, "Olp: " + olp + "file");
						Cursor c = db.getDatabase().query("files",
								new String[] { "file" }, "data_id=?",
								new String[] { n.id.toString() }, null, null,
								null);
						if (c.moveToFirst()) {
							olp = c.getString(0) + ":" + olp;
							// Log.i(TAG, "Olp - file: " + olp);
						}
						c.close();
						parent = null;
						continue;
					}
					olp = n.title + "/" + olp;
					parent = n.parentID;
					// Log.i(TAG, "Olp - normal: " + olp + ", " + parent);
				}
			}
			return "olp:" + olp;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
