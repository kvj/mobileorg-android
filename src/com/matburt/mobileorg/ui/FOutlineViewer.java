package com.matburt.mobileorg.ui;

import java.io.IOException;
import java.io.StringWriter;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;
import com.matburt.mobileorg.service.DataWriter;
import com.matburt.mobileorg.service.NoteNG;
import com.matburt.mobileorg.settings.SettingsActivity;
import com.matburt.mobileorg.ui.OutlineViewerFragment.DataListener;

public class FOutlineViewer extends FragmentActivity implements ControllerReceiver<DataController>, DataListener {

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
		Log.i(TAG, "We are on: "+android.os.Build.VERSION.SDK_INT);
		if (android.os.Build.VERSION.SDK_INT<11) {//<3.0
			requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		setContentView(R.layout.f_outline_viewer);
		left = (OutlineViewerFragment) getSupportFragmentManager().findFragmentById(R.id.viewer_left_pane);
		right = (OutlineViewerFragment) getSupportFragmentManager().findFragmentById(R.id.viewer_right_pane);
		if (data.getBoolean("slave", false) && null != right) {
			//Slave activity and have right - finish this
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
		menu.findItem(R.id.menu_sync).setVisible(!data.getBoolean("slave", false));
		menu.findItem(R.id.menu_options).setVisible(!data.getBoolean("slave", false));
		if (null != currentFragment) {
			onSelect(currentFragment, currentSelectedPosition);
		}
		return true;
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		conn = new ControllerConnector<App, DataController, DataService>(this, this);
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
		if (null != note.originalID) {
			NoteNG n = controller.findNoteByNoteID(note.originalID);
			if (null != n) {
				note = n;
			}
		}
		if (null == note.noteID) {
			if (!NoteNG.TYPE_AGENDA.equals(note.type)) {
				SuperActivity.notifyUser(this, "Invalid outline");
				return;
			}
		}
		data.putInt("right_id", note.id);
		data.putIntegerArrayList("right_sel", null);
		if (null != right) {
			//Load to right pane
			right.loadData("right", controller, data);
		} else {
			Intent intent = new Intent(this, FOutlineViewer.class);
			intent.putExtra("left_id", note.id);
			intent.putExtra("slave", true);
			startActivity(intent);
		}
	}
	
	@Override
	public void onOpen(OutlineViewerFragment fragment, int position) {
		NoteNG note = fragment.adapter.data.get(position);
		if (null != note) {
			openNote(note);
		}
	}
	
	private void runSynchronizer() {
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setCancelable(false);
		dialog.setMessage("Updating...");
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.show();
		new AsyncTask<Void, Void, String> () {

			@Override
			protected String doInBackground(Void... params) {
				String error = controller.refresh();
				
				return error;
			}
			
			protected void onPostExecute(String result) {
				dialog.dismiss();
				if (null != result) {
					SuperActivity.notifyUser(FOutlineViewer.this, result);
				} else {
					SuperActivity.notifyUser(FOutlineViewer.this, "Updated");
				}
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
			runEditCurrent(NoteNG.TYPE_TEXT);
			break;
		case R.id.menu_edit:
			runEditCurrent(null);
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
		if (null == currentFragment || null == currentFragment.adapter.clicked) {
			SuperActivity.notifyUser(this, "No item selected");
			return;
		}
		NoteNG note = currentFragment.adapter.clicked; 
		//Current selected
		NoteNG nearest = currentFragment.adapter.findNearestNote(note);
		if (null == nearest || null == nearest.noteID) {
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

	private void runEditCurrent(String type) {
//		Log.i(TAG, "current: "+currentFragment+", "+currentFragment.adapter.clicked);
		if (null == currentFragment || null == currentFragment.adapter.clicked) {
			SuperActivity.notifyUser(this, "No item selected");
			return;
		}
		NoteNG note = currentFragment.adapter.clicked; 
		//Current selected
		NoteNG nearest = currentFragment.adapter.findNearestNote(note);
		Integer agendaNote = null;
		//Nearest note with ID - we'll put it's new body to DB
		//NoteNG nearestNote = currentFragment.adapter.findNearestNote(note, false);
		
		if (NoteNG.TYPE_AGENDA_OUTLINE.equals(note.type) && null == type) {
			//Edit item in agenda - jump to real item
			agendaNote = note.id;
			note = controller.findNoteByNoteID(note.originalID);
			nearest = note;
			//nearestNote = note;
		}
		Log.i(TAG, "Add/Edit: "+note.title+", "+note.noteID+", "+nearest);
		if (null == nearest) {
			SuperActivity.notifyUser(this, "Selected item is readonly");
			return;
		}
		if (null == nearest.noteID) {
			SuperActivity.notifyUser(this, "Selected item is readonly");
			return;
		}
        Intent dispIntent = new Intent(this, DataEditActivity.class);
        int editType = ADD_NOTE;
        if (null == type) {
        	editType = EDIT_NOTE;
			//Edit current
    		dispIntent.putExtra("noteID", note.id.intValue());//This is where to save
    		dispIntent.putExtra("changeID", nearest.id.intValue());//This is body to modify
    		if (null != agendaNote) {
    			dispIntent.putExtra("agendaID", agendaNote.intValue());//Modify this also
			}
        	if (NoteNG.TYPE_OUTLINE.equals(note.type)) {
				//Edit outline
        		dispIntent.putExtra("text", note.title);
        		dispIntent.putExtra("type", "title");
        		dispIntent.putExtra("todo", note.todo);
        		dispIntent.putExtra("priority", note.priority);
        		dispIntent.putExtra("tags", note.tags);
			}
        	if (NoteNG.TYPE_SUBLIST.equals(note.type)) {
        		dispIntent.putExtra("text", note.before+" "+note.raw);
        		dispIntent.putExtra("type", "sublist");
        		dispIntent.putExtra("before", note.before);
			}
        	if (NoteNG.TYPE_TEXT.equals(note.type)) {
        		dispIntent.putExtra("text", note.raw);
        		dispIntent.putExtra("type", "text");
			}
		} else {
			//Add new entry
			dispIntent.putExtra("changeID", nearest.id.intValue());
    		dispIntent.putExtra("parentID", note.id.intValue()); //Where to save/refresh - add to me
        	if (NoteNG.TYPE_SUBLIST.equals(note.type)) {
        		dispIntent.putExtra("text", note.before+" ");
        		dispIntent.putExtra("type", "sublist");
        		dispIntent.putExtra("before", note.before);
			} else {
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
		Log.i(TAG, "onSelect: "+position+", "+menu);
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
//				Log.i(TAG, "Current: "+current.type+", "+current.title);
				if (NoteNG.TYPE_OUTLINE.equals(current.type) || 
						NoteNG.TYPE_AGENDA_OUTLINE.equals(current.type) ||
						NoteNG.TYPE_TEXT.equals(current.type) || 
						NoteNG.TYPE_SUBLIST.equals(current.type)) {
					canAdd = true;
					canEdit = true;
					canRemove = false;
					if (NoteNG.TYPE_TEXT.equals(current.type) || 
							NoteNG.TYPE_SUBLIST.equals(current.type)) {
						canRemove = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
//		Log.i(TAG, "Select: "+position+", "+canAdd);
		menu.findItem(R.id.menu_add_text).setEnabled(canAdd);
		menu.findItem(R.id.menu_edit).setEnabled(canEdit);
		menu.findItem(R.id.menu_remove).setEnabled(canRemove);
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
				//This is top note
				Log.i(TAG, "Reload full view");
				currentFragment.adapter.setController(note.id, controller, null);
			} else {
				Log.i(TAG, "Reload parent");
				currentFragment.adapter.setExpanded(currentFragment.adapter.findItem(parent.id), NoteNG.EXPAND_ONE);
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
		Log.i(TAG, "onActivityResult: "+req+", "+res+", "+currentFragment.getSelectedItemPosition());
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
