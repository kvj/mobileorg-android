package com.matburt.mobileorg.service;

import org.kvj.bravo7.DBHelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public class MobileOrgDBHelper extends DBHelper{

	public MobileOrgDBHelper(Context context, String path) {
		super(context, path, 1);
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
		}
	}

}
