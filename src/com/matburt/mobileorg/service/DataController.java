package com.matburt.mobileorg.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.kvj.bravo7.ApplicationContext;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.matburt.mobileorg.synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.synchronizers.Synchronizer;
import com.matburt.mobileorg.synchronizers.WebDAVSynchronizer;

public class DataController {

	private static final String TAG = "DataController";
	MobileOrgDBHelper db = null;
	ApplicationContext appContext = null;
	public static DateFormat timeFormat = new SimpleDateFormat("HH:mm");
	public static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd EEE");
	
	public DataController(ApplicationContext appContext, Context context) {
		this.appContext = appContext;
		db = new MobileOrgDBHelper(context, "MobileOrg");
		if (!db.open()) {
			Log.e(TAG, "Error opening DB");
			db = null;
		}
	}
	
    public void wrapExecSQL(String sqlText) {
    	if (null == db) {
			return;
		}
        try {
            db.getDatabase().execSQL(sqlText);
        }
        catch (Exception e) {
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
                    allFiles.put(result.getString(0),
                                 result.getString(1));
                } while(result.moveToNext());
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
                    fchecks.put(result.getString(0),
                                result.getString(1));
                } while (result.moveToNext());
            }
            result.close();
        }
        return fchecks;
    }

    public void removeFile(String filename) {
        this.wrapExecSQL("DELETE FROM files " +
                           "WHERE file = '"+filename+"'");
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
        Cursor result = this.wrapRawQuery("SELECT * FROM files " +
                                       "WHERE file = '"+filename+"'");
        if (result != null) {
            if (result.getCount() > 0) {
                this.wrapExecSQL("UPDATE files set name = '"+name+"', "+
                              "checksum = '"+ checksum + "' where file = '"+filename+"'");
            }
            else {
                this.wrapExecSQL("INSERT INTO files (file, name, checksum) " +
                              "VALUES ('"+filename+"','"+name+"','"+checksum+"')");
            }
            result.close();
        }
    }

    public ArrayList<HashMap<String, Integer>> getTodos() {
        ArrayList<HashMap<String, Integer>> allTodos = new ArrayList<HashMap<String, Integer>>();
        Cursor result = this.wrapRawQuery("SELECT tdgroup, name, isdone " +
                                            "FROM todos order by tdgroup");
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
                    grouping.put(result.getString(1),
                                 result.getInt(2));
                } while(result.moveToNext());
                allTodos.add(grouping);
            }
            result.close();
        }
        return allTodos;
    }

    public ArrayList<ArrayList<String>> getPriorities() {
        ArrayList<ArrayList<String>> allPriorities = new ArrayList<ArrayList<String>>();
        Cursor result = this.wrapRawQuery("SELECT tdgroup, name FROM priorities order by tdgroup");
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
                } while(result.moveToNext());
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
                this.wrapExecSQL("INSERT INTO todos (tdgroup, name, isdone) " +
                                   "VALUES (" + grouping + "," +
                                   "        '" + key + "'," +
                                   "        " + isDone + ")");
            }
            grouping++;
        }
    }

    public void setPriorityList(ArrayList<ArrayList<String>> newList) {
        this.clearPriorities();
        for (int idx = 0; idx < newList.size(); idx++) {
            for (int jdx = 0; jdx < newList.get(idx).size(); jdx++) {
                this.wrapExecSQL("INSERT INTO priorities (tdgroup, name, isdone) " +
                                   "VALUES (" + Integer.toString(idx) + "," +
                                   "        '" + newList.get(idx).get(jdx) + "'," +
                                   "        0)");
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
	
	public String refresh() {
        String userSynchro = appContext.getStringPreference("syncSource","");
        final Synchronizer appSync;
        if (userSynchro.equals("webdav")) {
            appSync = new WebDAVSynchronizer(appContext, this);
        }
        else if (userSynchro.equals("sdcard")) {
            appSync = new SDCardSynchronizer(appContext, this);
        }
        else if (userSynchro.equals("dropbox")) {
            appSync = new DropboxSynchronizer(appContext, this);
        }
        else {
            return "No configuration";
        }
        OrgNGParser parser = new OrgNGParser(this, appSync);
        return parser.parse();
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
			db.getDatabase().update("files", values, "file=?", new String[] {name});
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
			values.put("isdone", done? 1: 0);
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
					new String[] {"groupnum", "name", "isdone"}, 
					null, 
					null, 
					null, null, "groupnum, isdone, id");
			if (c.moveToFirst()) {
				do {
					TodoState state = new TodoState();
					state.group = c.getInt(0);
					state.name = c.getString(1);
					state.done = c.getInt(2) == 1? true: false;
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
					new String[] {"name"}, 
					null, 
					null, 
					null, null, "id");
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
			values.put("priority", note.priority);
			values.put("todo", note.todo);
			values.put("raw", note.raw);
			values.put("tags", note.tags);
			values.put("title", note.title);
			values.put("type", note.type);
			values.put("editable", note.editable? 1: 0);
			values.put("level", note.level);
			int result = (int) db.getDatabase().insert("data", null, values);
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
			db.getDatabase().update("data", values, "id=?", new String[] {note.id.toString()});
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
//		note.indent = c.getInt(1);
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
		return note;
	}
	
	static String[] dataFields = new String[] {
		"id", "indent", "editable", "note_id", "original_id", 
		"type", "priority", "todo", "title", "tags", "level", 
		"before","after", "raw"};
	
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
			Cursor c = db.getDatabase().query("data", 
					dataFields, 
					whereStart+" and type<>? and type<>?", 
					whereArgs.toArray(new String[] {}),//parent == null? null: parent.toString() 
					null, null, "id");
			if (c.moveToFirst()) {
				do {
					NoteNG note = cursorToNote(c);
					if (NoteNG.TYPE_TEXT.equals(note.type) 
							&& null != note.title 
							&& note.title.trim().isEmpty()) {
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
			Cursor c = db.getDatabase().query("data", 
					dataFields, 
					"note_id=?", 
					new String[] {noteID}, 
					null, null, "id");
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
	
	public NoteNG findNoteByID(Integer id) {
		try {
			if (null == id) {
				return null;
			}
			Cursor c = db.getDatabase().query("data", 
					dataFields, 
					"id=?", 
					new String[] {id.toString()}, 
					null, null, "id");
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
	public boolean addChange(Integer noteID, String type, String oldValue, String newValue) {
		if (null == db) {
			return false;
		}
		Log.i(TAG, "addChange: "+type+", "+oldValue+", "+newValue);
		NoteNG note = findNoteByID(noteID);
		if (null == note) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			if (!"data".equals(type)) {
				//Not new item - try to update existing
				Cursor c = db.getDatabase().query("changes", 
						new String [] {"id", "type"}, 
						"(type=? or type=?) and data_id=?", 
						new String[] {type, "data", noteID.toString()}, 
						null, null, "id");
				if (c.moveToFirst()) {
					if ("data".equals(c.getString(1))) {
						//We have data means new outline - don't need to modify
						Log.i(TAG, "New outline - finish");
						c.close();
						db.getDatabase().setTransactionSuccessful();
						return true;
					}
					//Found - update
					ContentValues values = new ContentValues();
					values.put("new_value", newValue);
					Log.i(TAG, "Existing - only update");
					db.getDatabase().update("changes", values, "id=?", new String[] {c.getString(0)});
					c.close();
					db.getDatabase().setTransactionSuccessful();
					return true;
				}
				c.close();
			}
			Log.i(TAG, "New change - insert");
			//Otherwise - insert new entry
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
			Cursor c = db.getDatabase().query("changes", new String[] {"id"}, null, null, null, null, null);
			int result = c.getCount();
			c.close();
			return result>0;
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
			db.getDatabase().delete("data", "file_id=? and type<>?", new String[] {"-1", NoteNG.TYPE_FILE});
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
		char[] chars = {'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', 'a', 
				's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 
				'z', 'x', 'c', 'v', 'b', 'n', 'm', '1', '2', '3', 
				'4', '5', '6', '7', '8', '9', '0'};
		StringBuilder sb = new StringBuilder();
		Random r = new Random(new Date().getTime());
		for (int i = 0; i < size; i++) {
			sb.append(chars[r.nextInt(chars.length)]);
		}
		return sb.toString();
	}
	
	public Integer createNewNote(NoteNG note) {
		if (null == db) {
			return null;
		}
		try {
			db.getDatabase().beginTransaction();
			if (null == note.parentID) {
				//No parentID - captured note - search for "Captured"
				Cursor c = db.getDatabase().query("data", 
						new String[] {"id"}, "file_id=-1 and parent_id is null", 
						null, null, null, null);
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
			Integer newNoteID = addData(note, true);//Note created
			if (null == newNoteID) {
				return null;
			}
			//Create 2 notes: properties with ID and text with date created
			NoteNG drawerNote = new NoteNG();
			drawerNote.parentID = note.id;
			drawerNote.fileID = note.fileID;
			drawerNote.raw = ":PROPERTIES:\n:ID: "+note.noteID+"\n:END:";
			drawerNote.type = NoteNG.TYPE_DRAWER;
			if (null == addData(drawerNote)) {
				return null;
			}
			NoteNG dateNote = new NoteNG();
			dateNote.parentID = note.id;
			dateNote.fileID = note.fileID;
			dateNote.raw = "["+dateFormat.format(new Date())+"]";
			dateNote.title = dateNote.raw;
			dateNote.type = NoteNG.TYPE_TEXT;
			if (null == addData(dateNote)) {
				return null;
			}
			db.getDatabase().setTransactionSuccessful();
			Log.i(TAG, "Note created: "+note.noteID+", "+note.title);
			return note.id;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return null;
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
			Cursor c = db.getDatabase().query("data", new String [] {"id"}, 
					"parent_id=?", new String[] {id.toString()}, null, null, null);
			if (c.moveToFirst()) {
				do {
					if (!removeData(c.getInt(0), true)) {
						c.close();
						return false;
					}
				} while (c.moveToNext());
			}
			c.close();
			db.getDatabase().delete("data", "id=?", new String[] {id.toString()});
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
					new String[] {"id", "data_id"}, "file=?", new String[] {fileName}, null, null, null);
			if (!file.moveToFirst()) {
				Log.e(TAG, "File not found: "+fileName);
				file.close();
				return null;
			}
			int fileID = file.getInt(0);
			int noteID = file.getInt(1);
			file.close();
			NoteNG note = findNoteByID(noteID);
			note.fileID = fileID;
			db.getDatabase().delete("data", "file_id=?", new String[] {Integer.toString(fileID)});
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

}
