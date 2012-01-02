package com.matburt.mobileorg.service;

import java.util.ArrayList;
import java.util.List;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController.TodoState;
import com.matburt.mobileorg.ui.adapter.OutlineViewerAdapter;
import com.matburt.mobileorg.ui.adapter.OutlineViewerAdapter.TextViewParts;

public class SearchProvider extends ContentProvider {

	public static final Uri CONTENT_URI = Uri
			.parse("content://com.matburt.mobileorg.search");
	private static final String TAG = "SearchProvider";

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.dir/vnd.mobileorg.outline";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public boolean onCreate() {
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		String query = uri.getLastPathSegment();
		Log.i(TAG, "query: " + uri + ", " + projection + ", " + selection
				+ ", " + selectionArgs + ", " + sortOrder + ", " + query);
		DataController controller = App.getInstance().getBean(
				DataController.class);
		MatrixCursor cursor = new MatrixCursor(new String[] { BaseColumns._ID,
				SearchManager.SUGGEST_COLUMN_TEXT_1,
				SearchManager.SUGGEST_COLUMN_TEXT_2,
				SearchManager.SUGGEST_COLUMN_ICON_1,
				SearchManager.SUGGEST_COLUMN_SHORTCUT_ID,
				SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA });
		try {
			List<String> priorities = controller.getPrioritiesNG();
			List<TodoState> todoStates = controller.getTodoTypes();
			List<String> todos = new ArrayList<String>();
			for (TodoState todoState : todoStates) {
				todos.add(todoState.name);
			}
			int limit = 10;
			if (null != uri.getQueryParameter("limit")) {
				limit = Integer.parseInt(uri.getQueryParameter("limit"));
			}
			List<NoteNG> result = controller.search(query, limit, todos,
					priorities);
			OutlineViewerAdapter adapter = new OutlineViewerAdapter(
					getContext());
			adapter.setController(-2, controller, null);
			for (int i = 0; i < result.size(); i++) {
				NoteNG note = result.get(i);
				List<Object> values = new ArrayList<Object>();

				TextViewParts parts = adapter.customizeTextView(result.get(i),
						false);
				values.add(note.id);
				values.add(parts.leftPart.toString());
				if (null != parts.subPart) {
					values.add(parts.subPart.toString());
				} else {
					values.add(note.tags);
				}
				values.add(getContext().getResources().getDrawable(
						R.drawable.logo_72));
				values.add(SearchManager.SUGGEST_NEVER_MAKE_SHORTCUT);
				if (null != note.noteID) {
					values.add("id:" + note.noteID);
				} else {
					values.add("_id:" + note.id);
				}
				cursor.addRow(values);
			}
			Log.i(TAG, "Now cursor: " + cursor.getCount());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

}
