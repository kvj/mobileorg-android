package com.matburt.mobileorg.service;

import org.kvj.bravo7.DBHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class MobileOrgDBHelper extends DBHelper{

	public MobileOrgDBHelper(Context context, String path) {
		super(context, path, 2);
	}

	@Override
	public void migrate(SQLiteDatabase db, int version) {
		switch (version) {
		case 1:
			db.execSQL("CREATE TABLE IF NOT EXISTS files"
                    + " (file VARCHAR, name VARCHAR,"
                    + " checksum VARCHAR)");
			db.execSQL("CREATE TABLE IF NOT EXISTS todos"
                    + " (tdgroup int, name VARCHAR,"
                    + " isdone INT)");
			db.execSQL("CREATE TABLE IF NOT EXISTS priorities"
                    + " (tdgroup int, name VARCHAR,"
                    + " isdone INT)");
			break;
		case 2:
			db.execSQL("drop table if exists files");
			db.execSQL("create table if not exists files (id integer primary key autoincrement, file text, checksum text)");
			db.execSQL("create table if not exists data (id integer primary key autoincrement, parent_id integer, " +
					"indent integer default 0, editable integer default 0, note_id text, original_id text, " +
					"type text, priority text, todo text, title text, raw text, tags text, level integer default 0, before text, after text)");
			break;
		}
	}

}
