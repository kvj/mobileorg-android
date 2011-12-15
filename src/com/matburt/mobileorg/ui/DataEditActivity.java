package com.matburt.mobileorg.ui;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;
import com.matburt.mobileorg.service.NoteNG;

public class DataEditActivity extends FragmentActivity implements ControllerReceiver<DataController> {

	private static final String TAG = "DataEdit";
	EditText edit = null;
	ImageButton togglePanel = null;
	Button save = null;
	DataEditOptionsPanel panel = null;
	DataController controller = null;
	ControllerConnector<App, DataController, DataService> conn = null;
	Bundle data = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "onCreate: "+savedInstanceState);
		setContentView(R.layout.data_edit);
		edit = (EditText) findViewById(R.id.data_edit_text);
		togglePanel = (ImageButton) findViewById(R.id.data_edit_button);
		save = (Button) findViewById(R.id.data_edit_save);
		save.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				onSave();
			}
		});
		panel = (DataEditOptionsPanel) getSupportFragmentManager().findFragmentById(R.id.data_edit_panel);
		if (null == savedInstanceState) {
			data = getIntent().getExtras();
			if (null == data) {
				data = new Bundle();
				data.putString("type", "title");
			}
			data.putBoolean("panel", false);
		} else {
			data = savedInstanceState;
		}
		togglePanel.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (panel.getView().getVisibility() == View.VISIBLE) {
					panel.getView().setVisibility(View.GONE);
				} else {
					panel.getView().setVisibility(View.VISIBLE);
				}
			}
		});
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
		Log.i(TAG, "Restoring editor state here "+data.getString("text"));
		if (!"title".equals(data.getString("type"))) {
			togglePanel.setVisibility(View.GONE);
			edit.setSingleLine(false);
		} else {
			edit.setSingleLine(true);
		}
		edit.setText(data.getString("text"));
		panel.getView().setVisibility(
				data.getBoolean("panel", false)
				? View.VISIBLE
				: View.GONE);
		panel.loadData(controller, data);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i(TAG, "Saving editor state here "+edit.getText());
		data.putBoolean("panel", panel.getView().getVisibility() == View.VISIBLE);
		data.putString("text", edit.getText().toString().trim());
		outState.putAll(data);
		panel.saveData(outState);
	}
	
	private void onSave() {
		panel.saveData(data);
		String text = edit.getText().toString().trim();
		if ("".equals(text)) {
			SuperActivity.notifyUser(this, "Text is empty");
			return;
		}
		int noteID = data.getInt("id", -1);
		if (-1 == noteID) {
			//New note - create
			NoteNG note = new NoteNG();
			note.level = 1;
			note.priority = data.getString("priority");
			note.tags = data.getString("tags");
			note.title = text;
			note.todo = data.getString("todo");
			note.type = NoteNG.TYPE_OUTLINE;
			if (null == controller.createNewNote(note)) {
				SuperActivity.notifyUser(this, "Error creating note");
				return;
			}
			noteID = note.id;
			controller.addChange(noteID, "data", null, null);
		}
		finish();
	}
}
