package com.matburt.mobileorg.ng.ui;

import java.util.ArrayList;
import java.util.List;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;

import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Spinner;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.DataController;
import com.matburt.mobileorg.ng.service.DataService;
import com.matburt.mobileorg.ng.service.NoteNG;
import com.matburt.mobileorg.ng.ui.OutlineViewerFragment.DataListener;

public class OutlineLinkPicker extends FragmentActivity implements
		ControllerReceiver<DataController>, DataListener {

	ControllerConnector<App, DataController, DataService> connector = null;
	OutlineViewerFragment fragment = null;
	Spinner type = null;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.outline_link_picker);
		fragment = (OutlineViewerFragment) getSupportFragmentManager()
				.findFragmentById(R.id.select_outline_list);
		fragment.setDataListener(this);
		List<String> items = new ArrayList<String>();
		items.add("By ID");
		items.add("By Name");
		items.add("By Index");
		type = (Spinner) findViewById(R.id.select_outline_type);
		type.setAdapter(new DataEditOptionsPanel.StringListAdapter(this, items));
		type.setSelection(0);
	};

	@Override
	protected void onStart() {
		super.onStart();
		connector = new ControllerConnector<App, DataController, DataService>(
				this, this);
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
		if (NoteNG.TYPE_TEXT.equals(note.type)
				|| NoteNG.TYPE_SUBLIST.equals(note.type)) {
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
		// Intent result = new Intent();
		// String expandType = "e";
		// result.putExtra("data",
		// note.createNotePath(expandType, type.getSelectedItemPosition()));
		// setResult(RESULT_OK, result);
		// finish();
		ShortcutIconResource icon = Intent.ShortcutIconResource.fromContext(
				this, R.drawable.logo_link);

		Intent intent = new Intent();

		Intent launchIntent = new Intent(this, FOutlineViewer.class);
		launchIntent.putExtra("noteLink",
				note.createNotePath(null, type.getSelectedItemPosition()));

		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, note.title);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

		setResult(RESULT_OK, intent);
		finish();
	}

	@Override
	public void onSelect(OutlineViewerFragment fragment, int position) {
	}

	@Override
	public boolean onKeyPress(int keyCode, OutlineViewerFragment fragment,
			int position) {
		return false;
	}

	@Override
	public void loadStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadFinished() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reportError(String message) {
		// TODO Auto-generated method stub

	}
}
