package com.matburt.mobileorg.ng.settings;

import java.util.HashMap;
import java.util.List;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;
import org.kvj.bravo7.SuperActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.DataController;
import com.matburt.mobileorg.ng.service.DataService;

public class SettingsActivity extends PreferenceActivity implements
		ControllerReceiver<DataController> {

	private HashMap<String, Intent> syncIntents = new HashMap<String, Intent>();

	ControllerConnector<App, DataController, DataService> cc = null;
	private DataController controller = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent prefsIntent = getIntent();
		int resourceID = prefsIntent.getIntExtra("prefs", R.xml.preferences);
		addPreferencesFromResource(resourceID);
		populateSyncSources();
		findPreference("clearDB").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						new AlertDialog.Builder(SettingsActivity.this)
								.setIcon(android.R.drawable.ic_dialog_alert)
								.setTitle("Clear DB?")
								.setMessage("Are you sure want to clear DB?")
								.setPositiveButton("Yes",
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												if (!controller.cleanupDB(true)) {
													SuperActivity
															.notifyUser(
																	SettingsActivity.this,
																	"DB error");
												} else {
													SuperActivity
															.notifyUser(
																	SettingsActivity.this,
																	"Done");
												}
											}

										}).setNegativeButton("No", null).show();
						return false;
					}
				});
		findPreference("syncConfig").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						String synchroMode = getPreferenceManager()
								.getSharedPreferences().getString("syncSource",
										"");
						if (syncIntents.containsKey(synchroMode)) {
							startActivity(syncIntents.get(synchroMode));
						} else {
							SuperActivity.notifyUser(SettingsActivity.this,
									"No synchronizer set");
						}
						return true;
					}
				});
	}

	@Override
	protected void onStart() {
		super.onStart();
		cc = new ControllerConnector<App, DataController, DataService>(this,
				this);
		cc.connectController(DataService.class);
	}

	@Override
	protected void onStop() {
		super.onStop();
		cc.disconnectController();
	}

	protected void populateSyncSources() {
		syncIntents.clear();
		List<PackageItemInfo> synchronizers = App
				.discoverSynchronizerPlugins(this);

		ListPreference syncSource = (ListPreference) findPreference("syncSource");

		// save the items for built-in synchronizer originally
		// retrieved from xml resources
		CharSequence[] entries = new CharSequence[synchronizers.size()
				+ syncSource.getEntries().length];
		CharSequence[] values = new CharSequence[synchronizers.size()
				+ syncSource.getEntryValues().length];
		System.arraycopy(syncSource.getEntries(), 0, entries, 0,
				syncSource.getEntries().length);
		System.arraycopy(syncSource.getEntryValues(), 0, values, 0,
				syncSource.getEntryValues().length);

		// populate the sync source list and prepare Intents for
		// discovered synchronizers
		int offset = syncSource.getEntries().length;
		for (PackageItemInfo info : synchronizers) {
			entries[offset] = info.nonLocalizedLabel;
			values[offset] = info.packageName;
			Intent syncIntent = new Intent(this, SettingsActivity.class);
			syncIntents.put(info.packageName, syncIntent);
			offset++;
		}

		// fill in the Intents for built-in synchronizers
		Intent synchroIntent = new Intent(this, WebDAVSettingsActivity.class);
		syncIntents.put("webdav", synchroIntent);

		synchroIntent = new Intent(this, SDCardSettingsActivity.class);
		syncIntents.put("sdcard", synchroIntent);

		synchroIntent = new Intent(this, DropboxSettingsActivity.class);
		syncIntents.put("dropbox", synchroIntent);

		// populate the sync source list with updated data
		syncSource.setEntries(entries);
		syncSource.setEntryValues(values);
	}

	@Override
	public void onController(DataController controller) {
		this.controller = controller;
	}
}