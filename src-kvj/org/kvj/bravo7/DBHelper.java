package org.kvj.bravo7;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

abstract public class DBHelper extends SQLiteOpenHelper {

	private static final String TAG = "DBHelper";
	private SQLiteDatabase database = null;
	private int version = -1;

	public DBHelper(Context context, String path, int version) {
		super(context, path, null, version);
		this.version = version;
	}
	
	public boolean open() {
		try {
			Log.i(TAG, "Opening DB...");
			SQLiteDatabase db = this.getWritableDatabase();
			if (db != null) {
				return true;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error opening DB", e);
		}
		return false;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i("DBHelper", "onCreate called - create table");
		onUpgrade(db, 0, version);
	}

	abstract public void migrate(SQLiteDatabase db, int version);
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "onUpgrade from "+oldVersion+" to "+newVersion);
		for (int i = oldVersion+1; i <= newVersion; i++) {
			migrate(db, i);
//			switch (i) {
//			case 1:
//				db.execSQL("create table data (id integer primary key autoincrement, " +
//						"mdate integer not null, level integer default 0, status integer default 0)");
//				break;
//			}
		}
	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		super.onOpen(db);
		Log.i("DBHelper", "onOpen");
		this.database = db;
	}
	
	public SQLiteDatabase getDatabase() {
		return database;
	}
	
	@Override
	protected void finalize() throws Throwable {
		Log.i(TAG, "Closing DB...");
		super.finalize();
		if (database != null) {
			database.close();
		}
	}
}
