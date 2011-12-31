package com.matburt.mobileorg.service;

public class NoteNG {

	public static final String TYPE_FILE = "file";
	public static final String TYPE_AGENDA = "agenda";
	public static final String TYPE_OUTLINE = "outline";
	public static final String TYPE_AGENDA_OUTLINE = "aoutline";
	public static final String TYPE_PROPERTY = "prop";
	public static final String TYPE_DRAWER = "drawer";
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_SUBLIST = "sub";
	public static final String TYPE_BLOCK = "block";

	public static final int EXPAND_COLLAPSED = 0;
	public static final int EXPAND_ONE = 1;
	public static final int EXPAND_MANY = 2;

	public static final int REF_ID = 0;
	public static final int REF_OLP = 1;
	public static final int REF_INDEX = 2;

	public static final int CBOX_NONE = 0;
	public static final int CBOX_UNCHECKED = 1;
	public static final int CBOX_CHECKED = 2;

	public Integer id = null;
	public Integer parentID = null;
	public int indent = 0;
	public boolean editable = false;
	public String noteID = null;
	public String originalID = null;
	public String type = null;
	public String priority = null;
	public String todo = null;
	public String title = null;
	public String raw = null;
	public String tags = null;
	public int level = 0;
	public String before = null;
	public String after = null;
	public Integer fileID = null;
	public NoteNG parentNote = null;
	public int checkboxState = CBOX_NONE;

	public int expanded = EXPAND_COLLAPSED;
	public int index = 0;

	public boolean isExpandable() {
		return TYPE_AGENDA.equals(type) || TYPE_AGENDA_OUTLINE.equals(type)
				|| TYPE_FILE.equals(type) || TYPE_OUTLINE.equals(type)
				|| TYPE_SUBLIST.equals(type);
	}

	public String createNotePath(String expand, int refType) {
		String link = "";
		if (REF_ID == refType) {
			String id = noteID;
			if (null == id) {
				id = originalID;
			}
			link = "id:" + id;
		} else {
			StringBuilder sb = new StringBuilder(REF_OLP == refType ? title
					: Integer.toString(index));
			NoteNG note = parentNote;
			while (null != note) {
				sb.insert(
						0,
						(REF_OLP == refType ? note.title : Integer
								.toString(note.index)) + "/");
				note = note.parentNote;
			}
			link = (REF_OLP == refType ? "olp:" : "index:") + sb;
		}
		return (null != expand ? expand + "::" : "") + link;
	}
}
