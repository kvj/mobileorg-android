package com.matburt.mobileorg.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kvj.bravo7.ApplicationContext;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Error.ErrorReporter;
import com.matburt.mobileorg.Synchronizers.DropboxSynchronizer;
import com.matburt.mobileorg.Synchronizers.SDCardSynchronizer;
import com.matburt.mobileorg.Synchronizers.Synchronizer;
import com.matburt.mobileorg.Synchronizers.WebDAVSynchronizer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDiskIOException;
import android.util.Log;

public class DataController {

	private static final String TAG = "DataController";
	private MobileOrgDBHelper db = null;
	ApplicationContext appContext = null;
	
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
        return parser.parse(appContext.getStringPreference("dropboxPath", ""));
	}
	
	public boolean cleanupDB() {
		if (null == db) {
			return false;
		}
		try {
			db.getDatabase().beginTransaction();
			db.getDatabase().delete("files", null, null);
			db.getDatabase().delete("data", null, null);
			db.getDatabase().setTransactionSuccessful();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
		}
		return false;
	}
	
	public Integer updateFile(String name, String checksum) {
		if (null == db) {
			return null;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			values.put("file", name);
			values.put("checksum", checksum);
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
	
	public Integer addData(NoteNG note) {
		if (null == db) {
			return null;
		}
		try {
			db.getDatabase().beginTransaction();
			ContentValues values = new ContentValues();
			values.put("after", note.after);
			values.put("before", note.before);
			values.put("parent_id", note.parentID);
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
			db.getDatabase().setTransactionSuccessful();
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			db.getDatabase().endTransaction();
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
			if (value instanceof String) {
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

	private NoteNG cursorToNote(Cursor c) {
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
		return note;
	}
	
	private static String[] dataFields = new String[] {
		"id", "indent", "editable", "note_id", "original_id", 
		"type", "priority", "todo", "title", "tags", "level", 
		"before","after"};
	
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
	
}
