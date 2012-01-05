package com.matburt.mobileorg.ui;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.SuperService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.markupartist.android.widget.ActionBar;
import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;
import com.matburt.mobileorg.service.DataWriter;
import com.matburt.mobileorg.service.NoteNG;
import com.matburt.mobileorg.service.OrgNGParser;
import com.matburt.mobileorg.service.OrgNGParser.ParseProgressListener;
import com.matburt.mobileorg.settings.SettingsActivity;
import com.matburt.mobileorg.ui.OutlineViewerFragment.DataListener;

public class FOutlineViewer extends FragmentActivity implements
		ControllerReceiver<DataController>, DataListener {

	private static final String TAG = "OutlineViewer";
	public static final int ADD_NOTE = 101;
	public static final int EDIT_NOTE = 102;
	Bundle data = null;
	OutlineViewerFragment left = null;
	OutlineViewerFragment right = null;
	ControllerConnector<App, DataController, DataService> conn = null;
	DataController controller = null;
	Menu menu = null;
	OutlineViewerFragment currentFragment = null;
	int currentSelectedPosition = -1;
	int currentMenuPosition = -1;
	ActionBar actionBar = null;
	List<MenuItemInfo> contextMenu = new ArrayList<MenuItemInfo>();

	private static final int MENU_LINK = 0;
	private static final int MENU_OPEN = 1;
	private static final int MENU_CHECKBOX = 2;

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		Intent serviceIntent = new Intent(this, DataService.class);
		startService(serviceIntent);
		if (null != savedState) {
			data = savedState;
		} else {
			data = getIntent().getExtras();
			if (null == data) {
				data = new Bundle();
			}
		}
		// Log.i(TAG, "We are on: " + android.os.Build.VERSION.SDK_INT);
		setContentView(R.layout.f_outline_viewer);
		if (null != findViewById(R.id.actionbar)) {
			actionBar = (ActionBar) findViewById(R.id.actionbar);
			// actionBar.setHomeLogo(R.drawable.logo_72);
			getMenuInflater().inflate(R.menu.action_bar_menu,
					actionBar.asMenu());
			actionBar.findAction(R.id.menu_sync).setVisible(
					!data.getBoolean("slave", false));
		}
		left = (OutlineViewerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.viewer_left_pane);
		right = (OutlineViewerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.viewer_right_pane);
		if (data.getBoolean("slave", false) && null != right) {
			// Slave activity and have right - finish this
			finish();
			return;
		}
		left.setDataListener(this);
		if (null != right) {
			right.setDataListener(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main_menu, menu);
		this.menu = menu;
		menu.findItem(R.id.menu_sync).setVisible(
				!data.getBoolean("slave", false));
		menu.findItem(R.id.menu_options).setVisible(
				!data.getBoolean("slave", false));
		if (null != currentFragment) {
			onSelect(currentFragment, currentSelectedPosition);
		}
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		conn = new ControllerConnector<App, DataController, DataService>(this,
				this);
		conn.connectController(DataService.class);
	}

	@Override
	protected void onStop() {
		super.onStop();
		conn.disconnectController();
	}

	@Override
	public void onController(DataController controller) {
		if (null != this.controller) {
			return;
		}
		this.controller = controller;
		left.loadData("left", controller, data);
		if (null != right && -1 != data.getInt("right_id", -1)) {
			right.loadData("right", controller, data);
		}
		if (null != getIntent().getExtras() && getIntent().hasExtra("noteLink")) {
			NoteNG n = controller.findNoteByLink(getIntent().getStringExtra(
					"noteLink"));
			if (null != n) {
				openNote(n);
			}
		}
		if (null != getIntent().getExtras() && getIntent().hasExtra("noteID")) {
			NoteNG n = controller.findNoteByID(getIntent().getIntExtra(
					"noteID", -1));
			if (null != n) {
				openNote(n);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		left.saveData();
		if (null != right) {
			right.saveData();
		}
		outState.putAll(data);
	}

	private void openNote(NoteNG note) {
		Log.i(TAG, "Open note: " + note.title);
		if (null != note.originalID) {
			NoteNG n = controller.findNoteByNoteID(note.originalID);
			if (null != n) {
				note = n;
			}
		}
		if (null == note.noteID) {
			if (NoteNG.TYPE_TEXT.equals(note.type)
					|| NoteNG.TYPE_SUBLIST.equals(note.type)) {
				// runEditCurrent(null, note);
				SuperActivity.notifyUser(this, "Invalid outline");
				return;
			}
		}
		data.putInt("right_id", note.id);
		data.putIntegerArrayList("right_sel", null);
		if (null != right) {
			// Load to right pane
			right.loadData("right", controller, data);
		} else {
			Intent intent = new Intent(this, FOutlineViewer.class);
			intent.putExtra("left_id", note.id);
			intent.putExtra("slave", true);
			startActivity(intent);
		}
	}

	private String changeCheckbox(NoteNG note) {
		NoteNG nearest = currentFragment.adapter.findNearestNote(note);
		if (null == nearest) {
			return "Outline is readonly";
		}
		Matcher m = OrgNGParser.checkboxPattern.matcher(note.title);
		if (!m.find()) {
			return "Invalid text";
		}
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("X".equals(m.group(1)) ? ' ' : 'X');
		sb.append("]");
		sb.append(note.title.substring(m.group().length()));
		String newText = sb.toString();
		String changeBody = null;
		StringWriter sw = new StringWriter();
		DataWriter dw = new DataWriter(controller);
		try {
			dw.writeOutlineWithChildren(nearest, sw, false);
			changeBody = sw.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "Error writing";
		}
		if (!controller.updateData(note, "title", newText)
				|| !controller.updateData(note, "raw", newText)) {
			return "DB error";
		}
		sw = new StringWriter();
		try {
			dw.writeOutlineWithChildren(nearest, sw, false);
			controller.addChange(nearest.id, "body", changeBody, sw.toString());
		} catch (IOException e) {
			e.printStackTrace();
			return "Error writing";
		}
		note.title = newText;
		note.raw = newText;
		return null;
	}

	class MenuItemInfo {
		int type = 0;
		String title = "";
		Object data = null;
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		menu.clear();
		for (int i = 0; i < contextMenu.size(); i++) {
			MenuItemInfo info = contextMenu.get(i);
			menu.add(0, i, i, info.title);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.i(TAG, "Clicked on " + item.getItemId());
		if (item.getItemId() < contextMenu.size()) {
			onContextMenu(contextMenu.get(item.getItemId()));
		}
		return true;
	}

	private void onContextMenu(MenuItemInfo item) {
		if (null == currentFragment) {
			return;
		}
		NoteNG note = currentFragment.adapter.getItem(currentMenuPosition);
		if (null == note) {
			return;
		}
		switch (item.type) {
		case MENU_OPEN:
			openNote(note);
			break;
		case MENU_CHECKBOX:
			String error = changeCheckbox(note);
			if (null != error) {
				SuperActivity.notifyUser(this, error);
				break;
			}
			currentFragment.adapter.notifyChanged();
			controller.notifyChangesHaveBeenMade();
			break;
		case MENU_LINK:
			openLink((String) item.data);
			break;
		}
	}

	private void openLink(String link) {
		if (!link.startsWith("http://") && !link.startsWith("https://")) {
			link = "http://" + link;
		}
		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
		startActivity(browserIntent);
	}

	@Override
	public void onOpen(OutlineViewerFragment fragment, int position) {
		currentFragment = fragment;
		currentMenuPosition = position;
		NoteNG note = fragment.adapter.getItem(position);
		contextMenu.clear();
		if (null != note) {
			Matcher m = OrgNGParser.linkPattern.matcher(note.title);
			while (m.find()) {
				// OrgNGParser.debugExp(m);
				MenuItemInfo menu = new MenuItemInfo();
				menu.type = MENU_LINK;
				menu.title = "Open " + m.group(1);
				menu.data = m.group(1);
				contextMenu.add(menu);
			}
			if (note.checkboxState != NoteNG.CBOX_NONE) {
				// Have checkbox
				MenuItemInfo menu = new MenuItemInfo();
				menu.type = MENU_CHECKBOX;
				menu.title = "Change checkbox state";
				contextMenu.add(menu);
			} else {
				MenuItemInfo menu = new MenuItemInfo();
				menu.type = MENU_OPEN;
				menu.title = "Open outline";
				contextMenu.add(menu);
				// openNote(note);
			}
			if (1 == contextMenu.size()) {
				onContextMenu(contextMenu.get(0));
			}
			if (contextMenu.size() > 1) {// Show menu
				registerForContextMenu(fragment.getListView());
				openContextMenu(fragment.getListView());
				unregisterForContextMenu(fragment.getListView());
			}
		}
	}

	class ProgressInfo {
		int progress1Total, progress1Pos, progress2Total, progress2Pos;
		String message;
	}

	private void runSynchronizer() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(false);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.progress_dialog,
				left.getListView(), false);
		final ProgressBar progress1 = (ProgressBar) view
				.findViewById(R.id.progress_bar1);
		final ProgressBar progress2 = (ProgressBar) view
				.findViewById(R.id.progress_bar2);
		final TextView progressText = (TextView) view
				.findViewById(R.id.progress_text);
		builder.setView(view);
		final Dialog dialog = builder.show();
		SuperService.powerLock(this);
		new AsyncTask<Void, ProgressInfo, String>() {

			@Override
			protected String doInBackground(Void... params) {
				String error = controller.refresh(new ParseProgressListener() {

					@Override
					public void progress(int total, int totalPos, int current,
							int currentPos, String message) {
						ProgressInfo info = new ProgressInfo();
						info.progress1Total = total;
						info.progress1Pos = totalPos;
						info.progress2Total = current;
						info.progress2Pos = currentPos;
						info.message = message;
						publishProgress(info);
					}
				});

				return error;
			}

			@Override
			public void onProgressUpdate(ProgressInfo... values) {
				ProgressInfo info = values[0];
				progress1.setMax(info.progress1Total);
				progress1.setProgress(info.progress1Pos);
				progress2.setMax(info.progress2Total);
				progress2.setProgress(info.progress2Pos);
				progressText.setText(info.message);
			};

			@Override
			protected void onPostExecute(String result) {
				dialog.dismiss();
				if (null != result) {
					SuperActivity.notifyUser(FOutlineViewer.this, result);
				} else {
					SuperActivity.notifyUser(FOutlineViewer.this, "Updated");
				}
				SuperService.powerUnlock(FOutlineViewer.this);
				left.reload();
				if (null != right) {
					right.reload();
				}
			};

		}.execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_sync:
			runSynchronizer();
			break;
		case R.id.menu_add_text:
			runEditCurrent(NoteNG.TYPE_TEXT, null);
			break;
		case R.id.menu_edit:
			runEditCurrent(null, null);
			break;
		case R.id.menu_search:
			onSearchRequested();
			break;
		case R.id.menu_remove:
			runRemoveCurrent();
			break;
		case R.id.menu_capture:
			runCapture();
			break;
		case R.id.menu_options:
			runOptions();
			break;
		}
		return true;
	}

	private String doRemove(NoteNG note, NoteNG change) {
		StringWriter sw = new StringWriter();
		DataWriter dw = new DataWriter(controller);
		String changeBody = null;
		try {
			dw.writeOutlineWithChildren(change, sw, false);
			changeBody = sw.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "Error writing";
		}
		if (!controller.removeData(note.id)) {
			return "DB error";
		}
		sw = new StringWriter();
		try {
			dw.writeOutlineWithChildren(change, sw, false);
			controller.addChange(change.id, "body", changeBody, sw.toString());
		} catch (IOException e) {
			e.printStackTrace();
			return "Error writing";
		}
		return null;
	}

	private void runRemoveCurrent() {
		if (null == currentFragment
				|| null == currentFragment.adapter.getClicked()) {
			SuperActivity.notifyUser(this, "No item selected");
			return;
		}
		NoteNG note = currentFragment.adapter.getClicked();
		// Current selected
		NoteNG nearest = currentFragment.adapter.findNearestNote(note);
		if (null == nearest) {
			SuperActivity.notifyUser(this, "Outline is readonly");
			return;
		}
		String error = doRemove(note, nearest);
		if (null != error) {
			SuperActivity.notifyUser(this, "No item selected");
			return;
		}
		refresh(currentFragment.adapter.findItem(note.parentNote.id), false);
	}

	private void runEditCurrent(String type, NoteNG note) {
		// Log.i(TAG,
		// "current: "+currentFragment+", "+currentFragment.adapter.clicked);
		if (null == currentFragment) {
			SuperActivity.notifyUser(this, "No item selected");
			return;
		}
		if (null == note) {
			if (null == currentFragment.adapter.getClicked()) {
				SuperActivity.notifyUser(this, "No item selected");
				return;
			}
			note = currentFragment.adapter.getClicked();
		}
		// Current selected
		NoteNG nearest = currentFragment.adapter.findNearestNote(note);
		// Nearest note with ID - we'll put it's new body to DB
		// NoteNG nearestNote = currentFragment.adapter.findNearestNote(note,
		// false);

		if (NoteNG.TYPE_AGENDA_OUTLINE.equals(note.type) && null == type) {
			// Edit item in agenda - jump to real item
			note = controller.findNoteByNoteID(note.originalID);
			nearest = note;
			// nearestNote = note;
		}
		Log.i(TAG, "Add/Edit: " + note.title + ", " + note.noteID + ", "
				+ nearest);
		if (null == nearest) {
			SuperActivity.notifyUser(this, "Selected item is readonly");
			return;
		}
		if (NoteNG.TYPE_AGENDA.equals(nearest.type)
				|| NoteNG.TYPE_FILE.equals(nearest.type)) {
			SuperActivity.notifyUser(this, "Selected item is readonly");
			return;
		}
		// if (null == nearest.noteID) {
		// SuperActivity.notifyUser(this, "Selected item is readonly");
		// return;
		// }
		Intent dispIntent = new Intent(this, DataEditActivity.class);
		int editType = ADD_NOTE;
		if (null == type) {
			editType = EDIT_NOTE;
			// Edit current
			dispIntent.putExtra("noteID", note.id.intValue());// This is where
																// to save
			dispIntent.putExtra("changeID", nearest.id.intValue());// This is
																	// body to
																	// modify
			if (NoteNG.TYPE_OUTLINE.equals(note.type)) {
				// Edit outline
				dispIntent.putExtra("panel", true);
				dispIntent.putExtra("text", note.title);
				dispIntent.putExtra("type", "title");
				dispIntent.putExtra("todo", note.todo);
				dispIntent.putExtra("priority", note.priority);
				dispIntent.putExtra("tags", note.tags);
			}
			if (NoteNG.TYPE_SUBLIST.equals(note.type)) {
				dispIntent.putExtra("text", note.before + " " + note.raw);
				dispIntent.putExtra("type", "sublist");
				dispIntent.putExtra("before", note.before);
			}
			if (NoteNG.TYPE_TEXT.equals(note.type)) {
				if (null != nearest.tags
						&& nearest.tags.contains(controller.getCryptTag())) {
					dispIntent.putExtra("crypt", true);
				}
				dispIntent.putExtra("text", note.raw);
				dispIntent.putExtra("type", "text");
			}
		} else {
			// Add new entry
			dispIntent.putExtra("changeID", nearest.id.intValue());
			dispIntent.putExtra("parentID", note.id.intValue());
			if (NoteNG.TYPE_SUBLIST.equals(note.type)) {
				dispIntent.putExtra("text", note.before + " ");
				dispIntent.putExtra("type", "sublist");
				dispIntent.putExtra("before", note.before);
			} else {
				currentFragment.setSelected(currentFragment.adapter
						.findItem(nearest.id));
				dispIntent.putExtra("parentID", nearest.id.intValue());
				dispIntent.putExtra("type", "text");
			}
		}
		startActivityForResult(dispIntent, editType);
	}

	private void runOptions() {
		Intent settingsIntent = new Intent(this, SettingsActivity.class);
		startActivity(settingsIntent);
	}

	private void runCapture() {
		Intent dispIntent = new Intent(this, DataEditActivity.class);
		startActivity(dispIntent);
	}

	@Override
	public void onSelect(OutlineViewerFragment fragment, int position) {
		Log.i(TAG, "onSelect: " + position + ", " + menu);
		currentFragment = fragment;
		currentSelectedPosition = position;
		if (null == menu) {
			return;
		}
		boolean canAdd = false;
		boolean canEdit = false;
		boolean canRemove = false;
		try {
			if (-1 != position) {
				NoteNG current = fragment.adapter.getItem(position);
				// Log.i(TAG, "Current: "+current.type+", "+current.title);
				if (NoteNG.TYPE_OUTLINE.equals(current.type)
						|| NoteNG.TYPE_AGENDA_OUTLINE.equals(current.type)
						|| NoteNG.TYPE_TEXT.equals(current.type)
						|| NoteNG.TYPE_SUBLIST.equals(current.type)) {
					canAdd = true;
					canEdit = true;
					canRemove = false;
					if (null != current.tags
							&& current.tags.contains(controller.getCryptTag())) {
						canAdd = false;
					}
					if (NoteNG.TYPE_TEXT.equals(current.type)
							|| NoteNG.TYPE_SUBLIST.equals(current.type)) {
						canRemove = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Log.i(TAG, "Select: "+position+", "+canAdd);
		menu.findItem(R.id.menu_add_text).setEnabled(canAdd);
		menu.findItem(R.id.menu_edit).setEnabled(canEdit);
		menu.findItem(R.id.menu_remove).setEnabled(canRemove);
		if (null != actionBar) {
			actionBar.findAction(R.id.menu_add_text).setEnabled(canAdd);
			actionBar.findAction(R.id.menu_edit).setEnabled(canEdit);
		}
	}

	private void refresh(int refreshPos, boolean refreshParent) {
		if (null == currentFragment) {
			Log.e(TAG, "No fragment");
			return;
		}
		if (-1 == refreshPos) {
			Log.e(TAG, "No item");
			return;
		}
		NoteNG note = currentFragment.adapter.getItem(refreshPos);
		if (refreshParent) {
			NoteNG parent = note.parentNote;
			if (null == parent) {
				// This is top note
				Log.i(TAG, "Reload full view");
				currentFragment.adapter
						.setController(note.id, controller, null);
			} else {
				Log.i(TAG, "Reload parent");
				currentFragment.adapter.setExpanded(
						currentFragment.adapter.findItem(parent.id),
						NoteNG.EXPAND_ONE);
			}
		} else {
			Log.i(TAG, "Refresh this note");
			currentFragment.adapter.setExpanded(refreshPos, NoteNG.EXPAND_ONE);
		}
		currentFragment.setSelected(currentFragment.adapter.findItem(note.id));
	}

	@Override
	protected void onActivityResult(int req, int res, Intent intent) {
		super.onActivityResult(req, res, intent);
		if (null == currentFragment) {
			return;
		}
		Log.i(TAG, "onActivityResult: " + req + ", " + res + ", "
				+ currentFragment.getSelectedItemPosition());
		if (res != Activity.RESULT_OK) {
			return;
		}
		if (ADD_NOTE == req) {
			refresh(currentSelectedPosition, false);
		}
		if (EDIT_NOTE == req) {
			refresh(currentSelectedPosition, true);
		}
	}
}
