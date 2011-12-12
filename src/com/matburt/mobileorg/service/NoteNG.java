package com.matburt.mobileorg.service;

public class NoteNG {
	
	public static final String TYPE_FILE = "file";
	public static final String TYPE_AGENDA = "agenda";
	public static final String TYPE_OUTLINE = "outline";
	public static final String TYPE_PROPERTY = "prop";
	public static final String TYPE_DRAWER = "drawer";
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_SUBLIST = "sub";
	public static final String TYPE_BLOCK = "block";
	
	public static final int EXPAND_COLLAPSED = 0;
	public static final int EXPAND_ONE = 1;
	public static final int EXPAND_MANY = 2;
	
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
	
	public int expanded = EXPAND_COLLAPSED;
	
	public boolean isExpandable() {
		return TYPE_AGENDA.equals(type) 
			|| TYPE_FILE.equals(type) 
			|| TYPE_OUTLINE.equals(type)
			|| TYPE_SUBLIST.equals(type)
			;
	}
}
