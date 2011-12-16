package com.matburt.mobileorg.ui;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;
import com.matburt.mobileorg.service.NoteNG;
import com.matburt.mobileorg.ui.OutlineViewerFragment.DataListener;

public class FOutlineViewer extends FragmentActivity implements ControllerReceiver<DataController>, DataListener {

	private static final String TAG = "OutlineViewer";
	public static final int ADD_EDIT_NOTE = 101;
	public static final int ADD_EDIT_NOTE_OK = 102;
	Bundle data = null;
	OutlineViewerFragment left = null;
	OutlineViewerFragment right = null;
	ControllerConnector<App, DataController, DataService> conn = null;
	DataController controller = null;
	Menu menu = null;
	OutlineViewerFragment currentFragment = null;
	
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
		case R.id.menu_add_outline:
			runEditCurrent(NoteNG.TYPE_OUTLINE);
			break;
		case R.id.menu_add_text:
			runEditCurrent(NoteNG.TYPE_TEXT);
			break;
		case R.id.menu_edit:
			runEditCurrent(null);
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
	
	private void runEditCurrent(String type) {
		if (null == currentFragment || null == currentFragment.adapter.clicked) {
			SuperActivity.notifyUser(this, "No item selected");
			return;
		}
		NoteNG note = currentFragment.adapter.clicked; 
		//Current selected
		NoteNG nearest = currentFragment.adapter.findNearestNote(note, true);
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
		if (null == nearest) {
			SuperActivity.notifyUser(this, "Selected item is readonly");
			return;
		}
        Intent dispIntent = new Intent(this, DataEditActivity.class);
        if (null == type) {
			//Edit current
    		dispIntent.putExtra("noteID", note.id.intValue());//This is where to save
    		if (note.id != nearest.id) {
        		dispIntent.putExtra("changeID", nearest.id.intValue());//This is body to modify
			}
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
    		NoteNG nearestNote = currentFragment.adapter.findNearestNote(note, false);//Should be always not null
    		//This is nearest note to add/refresh
			dispIntent.putExtra("changeID", nearest.id.intValue());
			if (NoteNG.TYPE_OUTLINE.equals(type)) {
				//Trying to add outline
				dispIntent.putExtra("parentID", nearestNote.id.intValue()); //Where to save/refresh
				dispIntent.putExtra("type", "title");
			}
			if (NoteNG.TYPE_TEXT.equals(type)) {
				//Trying to add text
	        	if (NoteNG.TYPE_SUBLIST.equals(note.type)) {
	        		dispIntent.putExtra("text", note.before+" ");
	        		dispIntent.putExtra("type", "sublist");
	        		dispIntent.putExtra("before", note.before);
	        		dispIntent.putExtra("parentID", note.id.intValue()); //Where to save/refresh - add to me
				} else {
					dispIntent.putExtra("type", "text");
					dispIntent.putExtra("parentID", nearestNote.id.intValue()); //Where to save/refresh
				}
			}
		}
        startActivityForResult(dispIntent, ADD_EDIT_NOTE);
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
		if (null == menu) {
			return;
		}
		boolean canAdd = false;
		boolean canEdit = false;
		boolean canRemove = false;
		try {
			if (-1 != position) {
				NoteNG current = fragment.adapter.getItem(position);
				Log.i(TAG, "Current: "+current.type+", "+current.title);
				if (NoteNG.TYPE_OUTLINE.equals(current.type) || 
						NoteNG.TYPE_AGENDA_OUTLINE.equals(current.type) ||
						NoteNG.TYPE_TEXT.equals(current.type) || 
						NoteNG.TYPE_SUBLIST.equals(current.type)) {
					canAdd = true;
					canEdit = true;
					canRemove = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i(TAG, "Select: "+position+", "+canAdd);
		menu.findItem(R.id.menu_add_outline).setEnabled(canAdd);
		menu.findItem(R.id.menu_add_text).setEnabled(canAdd);
		menu.findItem(R.id.menu_edit).setEnabled(canEdit);
		menu.findItem(R.id.menu_remove).setEnabled(canRemove);
	}
	
	@Override
	protected void onActivityResult(int req, int res, Intent intent) {
		super.onActivityResult(req, res, intent);
		if (ADD_EDIT_NOTE == req && ADD_EDIT_NOTE_OK == res) {
			//Need to refresh
		}
	}
}
