package com.matburt.mobileorg.service;

import java.io.IOException;
import java.io.Writer;

import android.database.Cursor;

public class DataWriter {
	
	DataController controller = null;

	public DataWriter(DataController controller) {
		this.controller = controller;
	}
	
	private void writeIndent(int indent, Writer writer) {
		
	}
	
	private void writeDataItem(NoteNG note, int indent, Writer writer) {
	}
	
	public boolean writeOutlineWithChildren(NoteNG note, Writer writer, boolean writeItself) throws IOException {
		if (null == controller.db) {
			return false;
		}
		if (!NoteNG.TYPE_OUTLINE.equals(note.type)) {
			return false;
		}
		int indent = 0;
		if (writeItself) {
			for (int i = 0; i < note.level; i++) {
				writer.write("*");
			}
			indent = 1+note.level;
			if (null != note.todo) {
				writer.write(" "+note.todo);
			}
			if (null != note.priority) {
				writer.write(" [#"+note.priority+"]");
			}
			writer.write(" "+note.title);
			if (null != note.tags) {
				writer.write("\\t"+note.tags);
			}
			writer.write("\n");
		}
		try {
			Cursor c = controller.db.getDatabase().query("data", 
					DataController.dataFields, 
					"parent_id=?", 
					new String[] {note.id.toString()}, 
					null, null, "id");
			if (c.moveToFirst()) {
				do {
					NoteNG n = controller.cursorToNote(c);
					if (NoteNG.TYPE_AGENDA.equals(n.type)) {
						writeOutlineWithChildren(n, writer, true);
					} else {
						writeDataItem(n, indent, writer);
					}
				} while (c.moveToNext());
			}
			c.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean writeChanges(Writer writer) {
		if (null == controller.db) {
			return false;
		}
		try {
			Cursor c = controller.db.getDatabase().query("changes", 
					new String [] {"data_id", "type", "new_value", "old_value"}, 
					null, null,
					null, null, "id");
			if (c.moveToFirst()) {
				do {
					if ("data".equals(c.getString(1))) {
						//Type is data - means capture
						
						continue;
					}
					NoteNG note = controller.findNoteByID(c.getInt(0));
					if (null == note) {
						continue;
					}
					String id = note.originalID;
					if (null == id) {
						id = note.noteID;
					}
				} while (c.moveToNext());
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
