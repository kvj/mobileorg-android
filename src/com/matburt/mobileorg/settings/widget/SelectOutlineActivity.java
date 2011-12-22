package com.matburt.mobileorg.settings.widget;

import java.util.ArrayList;
import java.util.List;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;
import com.matburt.mobileorg.service.NoteNG;
import com.matburt.mobileorg.ui.DataEditOptionsPanel;
import com.matburt.mobileorg.ui.OutlineViewerFragment;
import com.matburt.mobileorg.ui.OutlineViewerFragment.DataListener;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Spinner;

public class SelectOutlineActivity extends FragmentActivity implements ControllerReceiver<DataController>, DataListener {

	ControllerConnector<App, DataController, DataService> connector = null;
	OutlineViewerFragment fragment = null;
	Spinner type = null;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.select_outline_dialog);
		fragment = (OutlineViewerFragment) getSupportFragmentManager().findFragmentById(R.id.select_outline_list);
		fragment.setDataListener(this);
		List<String> items = new ArrayList<String>();
		items.add("By ID");
		items.add("By Name");
		items.add("By Index");
		type = (Spinner) findViewById(R.id.select_outline_type);
		type.setAdapter(new DataEditOptionsPanel.StringListAdapter(this, items));
		type.setSelection(0);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		connector = new ControllerConnector<App, DataController, DataService>(this, this);
		connector.connectController(DataService.class);
	}

	@Override
	public void onController(DataController controller) {
		fragment.loadData("list", controller, new Bundle());
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		connector.disconnectController();
	}

	@Override
	public void onOpen(OutlineViewerFragment fragment, int position) {
		NoteNG note = fragment.getSelectedItem(position);
		if (NoteNG.TYPE_TEXT.equals(note.type) || NoteNG.TYPE_SUBLIST.equals(note.type)) {
			SuperActivity.notifyUser(this, "Invalid outline selected");
			return;
		}
		String id = note.noteID;
		if (null == id) {
			id = note.originalID;
		}
		if (0 == type.getSelectedItemPosition() && null == id) {
			SuperActivity.notifyUser(this, "Outline has no ID");
			return;
		}
		Intent result = new Intent();
		result.putExtra("data", note.createNotePath(type.getSelectedItemPosition()));
		setResult(RESULT_OK, result);
		finish();
	}

	@Override
	public void onSelect(OutlineViewerFragment fragment, int position) {
	}
}
