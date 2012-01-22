package com.matburt.mobileorg.ng.service;

import android.text.TextUtils;

public class UploadBean {

	public Integer dataID = null;
	public String name = null;
	public String fileName = null;

	public UploadBean() {
	}

	public UploadBean(NoteNG note, String name) {
		dataID = note.id;
		this.name = name;
		if (!TextUtils.isEmpty(note.noteID)) {
			fileName = note.noteID + "-" + name;
		}
	}
}
