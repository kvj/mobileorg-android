package com.matburt.mobileorg.service;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import android.database.Cursor;

public class DataWriter {

	DataController controller = null;

	public DataWriter(DataController controller) {
		this.controller = controller;
	}

	public static void writeIndent(int indent, Writer writer)
			throws IOException {
		for (int i = 0; i < indent; i++) {
			writer.write(' ');
		}
	}

	private void writeDataItem(NoteNG note, int indent, Writer writer)
			throws IOException {
		if (NoteNG.TYPE_SUBLIST.equals(note.type)) {
			// Write line by line
			int itemIndent = 1 + note.before.length();
			String[] lines = note.raw.split("\\n");
			for (int i = 0; i < lines.length; i++) {
				if (i == 0) {
					writeIndent(indent, writer);
					writer.write(note.before + ' ');
				} else {
					writeIndent(indent + itemIndent, writer);
				}
				writer.write(lines[i] + '\n');
			}
			// Write other notes
			List<NoteNG> subNotes = controller.getData(note.id);
			for (int i = 0; i < subNotes.size(); i++) {
				writeDataItem(subNotes.get(i), indent + itemIndent, writer);
			}
			return;
		}
		String[] lines = note.raw.split("\n");
		for (int i = 0; i < lines.length; i++) {
			writeIndent(indent, writer);
			writer.write(lines[i].trim() + '\n');
		}
	}

	public boolean writeOutlineWithChildren(NoteNG note, Writer writer,
			boolean writeItself) throws IOException {
		if (null == controller.db) {
			return false;
		}
		if (!NoteNG.TYPE_OUTLINE.equals(note.type)) {
			return false;
		}
		int indent = 1 + note.level;
		boolean crypt = null != note.tags
				&& note.tags.contains(controller.getCryptTag());
		if (writeItself) {
			for (int i = 0; i < note.level; i++) {
				writer.write('*');
			}
			if (null != note.todo) {
				writer.write(" " + note.todo);
			}
			if (null != note.priority) {
				writer.write(" [#" + note.priority + "]");
			}
			writer.write(" " + note.title);
			if (null != note.tags) {
				writer.write("\t" + note.tags);
			}
			writer.write('\n');
		}
		try {
			Cursor c = controller.db.getDatabase().query("data",
					DataController.dataFields, "parent_id=?",
					new String[] { note.id.toString() }, null, null, "id");
			if (c.moveToFirst()) {
				do {
					NoteNG n = controller.cursorToNote(c);
					if (crypt) {
						if (NoteNG.TYPE_TEXT.equals(n.type)) {
							writeDataItem(n, 0, writer);
							break;
						}
						continue;
					}
					if (!NoteNG.TYPE_OUTLINE.equals(n.type)) {
						// writeOutlineWithChildren(n, writer, true);
						// } else {
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
			Cursor c = controller.db
					.getDatabase()
					.query("changes",
							new String[] { "data_id", "type", "new_value",
									"old_value" }, null, null, null, null, "id");
			if (c.moveToFirst()) {
				do {
					NoteNG note = controller.findNoteByID(c.getInt(0));
					if (null == note) {
						continue;
					}
					if ("data".equals(c.getString(1))) {
						// Type is data - means capture
						writeOutlineWithChildren(note, writer, true);
					} else {

						String id = controller.getID(note);
						if (null == id) {
							continue;
						}
						writer.write(String.format("* F(edit:%s) [[%s][%s]]\n",
								c.getString(1), id, note.title));
						writer.write(String.format("** Old value\n%s\n",
								c.getString(3) == null ? "" : c.getString(3)));
						writer.write(String.format("** New value\n%s\n",
								c.getString(2) == null ? "" : c.getString(2)));
						writer.write("** End of edit\n");
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
