package com.matburt.mobileorg.ui;

import java.util.List;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
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

	Bundle data = null;
	OutlineViewerFragment left = null;
	OutlineViewerFragment right = null;
	ControllerConnector<App, DataController, DataService> conn = null;
	DataController controller = null;
	
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
		if (data.getBoolean("slave", false) && 
				getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			//Slave activity  - not show in landscape
            finish();
            return;
        }
		setContentView(R.layout.f_outline_viewer);
		left = (OutlineViewerFragment) getSupportFragmentManager().findFragmentById(R.id.viewer_left_pane);
		left.setDataListener(this);
		right = (OutlineViewerFragment) getSupportFragmentManager().findFragmentById(R.id.viewer_right_pane);
		if (null != right) {
			right.setDataListener(this);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main_menu, menu);
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
		case R.id.menu_capture:
			runCapture();
			break;
		case R.id.menu_options:
			runOptions();
			break;
		}
		return true;
	}

	private void runOptions() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
	}

	private void runCapture() {
        Intent dispIntent = new Intent(this, DataEditActivity.class);
        startActivity(dispIntent);
	}
}
