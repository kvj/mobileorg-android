package com.matburt.mobileorg.ui;

import java.util.ArrayList;

import org.kvj.bravo7.SuperActivity;

import com.matburt.mobileorg.MobileOrgActivity;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.Settings.SettingsActivity;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;
import com.matburt.mobileorg.service.NoteNG;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class OutlineViewer extends SuperActivity<DataController, DataService> {

    private static final int OP_MENU_SETTINGS = 1;
    private static final int OP_MENU_SYNC = 2;
    private static final int OP_MENU_OUTLINE = 3;
    private static final int OP_MENU_CAPTURE = 4;
	protected static final String TAG = "OutlineViewer";
    
	public OutlineViewer() {
		super(DataService.class);
	}
	
	ListView list;
	OutlineViewerAdapter listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.outline_viewer);
		listAdapter = new OutlineViewerAdapter();
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
				if (null != noteID) {
					Log.i(TAG, "Showing activity: "+noteID);
					Intent intent = new Intent(OutlineViewer.this, OutlineViewer.class);
					intent.putExtra("notePath", noteID);
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
		listAdapter.setController(noteID, controller);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, OP_MENU_OUTLINE, 0, R.string.menu_outline);
        menu.add(0, OP_MENU_CAPTURE, 0, R.string.menu_capture);
        menu.add(0, OP_MENU_SYNC, 0, R.string.menu_sync);
        menu.add(0, OP_MENU_SETTINGS, 0, R.string.menu_settings);
        return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case OP_MENU_SYNC:
            runSynchronizer();
            return true;
        case OP_MENU_SETTINGS:
            return showSettings();
//        case MobileOrgActivity.OP_MENU_OUTLINE:
//            Intent dispIntent = new Intent(this, MobileOrgActivity.class);
//            dispIntent.putIntegerArrayListExtra( "nodePath", new ArrayList<Integer>() );
//            startActivity(dispIntent);
//            return true;
//        case MobileOrgActivity.OP_MENU_CAPTURE:
//            return this.runCapture();
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
				listAdapter.reload();
			};
			
		}.execute();
	}
	
}
