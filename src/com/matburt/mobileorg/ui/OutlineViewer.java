package com.matburt.mobileorg.ui;

import org.kvj.bravo7.SuperActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;
import com.matburt.mobileorg.service.NoteNG;
import com.matburt.mobileorg.settings.SettingsActivity;
import com.matburt.mobileorg.ui.adapter.OutlineViewerAdapter;

public class OutlineViewer extends SuperActivity<App, DataController, DataService> {

    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;
    private static final int OP_MENU_OUTLINE = 3;
    private static final int OP_MENU_CAPTURE = 4;
	protected static final String TAG = "OutlineViewer";
    
	public OutlineViewer() {
		super(DataService.class);
		Log.i(TAG, "Activity constructor");
	}
	
	ListView list;
	OutlineViewerAdapter listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "Activity create: "+savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
        Intent serviceIntent = new Intent(this, DataService.class);
        startService(serviceIntent);
		setContentView(R.layout.outline_viewer);
		listAdapter = new OutlineViewerAdapter(this);
		list = (ListView) findViewById(R.id.outline_viewer_list);
		list.setAdapter(listAdapter);
		list.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				listAdapter.collapseExpand(pos, true);
			}
		});
		list.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				String noteID = listAdapter.getIntent(pos);
				NoteNG note = listAdapter.getItem(pos);
				if (null != noteID) {
					Log.i(TAG, "Showing activity: "+noteID);
//					Intent intent = new Intent(OutlineViewer.this, OutlineViewer.class);
//					intent.putExtra("notePath", noteID);
//					startActivity(intent);
					Intent intent = new Intent(OutlineViewer.this, DataEditActivity.class);
					intent.putExtra("text", note.title);
					intent.putExtra("type", "title");
					intent.putExtra("todo", note.todo);
					intent.putExtra("priority", note.priority);
					intent.putExtra("tags", note.tags);
					startActivity(intent);
					return true;
				}
				return false;
			}
		});
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.i(TAG, "Activity config change");
		if (controller == null) {
			return;
		}
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			listAdapter.setShowWide(true);
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			listAdapter.setShowWide(false);
	    }
	}

	@Override
	public void onController(DataController controller) {
		super.onController(controller);
		Log.i(TAG, "Activity controller");
		Integer noteID = null;
		String notePath = null;
		if (null != getIntent().getExtras()) {
			notePath = getIntent().getExtras().getString("notePath");
		}
		Log.i(TAG, "Start from: "+notePath);
		if (null != notePath) {
			if (notePath.startsWith("id:")) {
				NoteNG note = controller.findNoteByNoteID(notePath.substring(3));
				if (null != note) {
					noteID = note.id;
				}
			}
		}
		Log.i(TAG, "Show from: "+noteID);
		listAdapter.setController(noteID, controller, null);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Clicked: "+item.getItemId()+", "+R.id.menu_capture+", "+R.id.menu_sync);
        switch (item.getItemId()) {
        case R.id.menu_sync:
        	Log.i(TAG, "Synchronizer");
            runSynchronizer();
            return true;
        case R.id.menu_options:
        	Log.i(TAG, "Settings");
            return showSettings();
        case R.id.menu_capture:
        	Log.i(TAG, "Capture");
            Intent dispIntent = new Intent(this, DataEditActivity.class);
            startActivity(dispIntent);
            return true;
        }
        return false;
	}

	private boolean showSettings() {
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);
		return true;
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
					notifyUser(result);
				} else {
					notifyUser("Updated");
				}
				listAdapter.reload(null);
			};
			
		}.execute();
	}
	
}
