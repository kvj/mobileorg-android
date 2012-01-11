package com.matburt.mobileorg.ng.settings;

import org.kvj.bravo7.SuperActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;
import com.matburt.mobileorg.ng.R;

public class SDCardSettingsActivity extends PreferenceActivity {
	private static final int REQUEST_OPEN = 105;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.sdsync_preferences);
		findPreference("indexFilePathSelect").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						openOpenDialog();
						return true;
					}
				});
	}

	private void openOpenDialog() {
		Intent intent = new Intent(this, FileDialog.class);
		String state = Environment.getExternalStorageState();
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			intent.putExtra(FileDialog.START_PATH, Environment
					.getExternalStorageDirectory().getAbsolutePath());
		}
		startActivityForResult(intent, REQUEST_OPEN);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == REQUEST_OPEN) {
				getPreferenceManager()
						.getSharedPreferences()
						.edit()
						.putString("indexFilePath",
								data.getStringExtra(FileDialog.RESULT_PATH))
						.commit();
				SuperActivity.notifyUser(
						SDCardSettingsActivity.this,
						"File path saved: "
								+ data.getStringExtra(FileDialog.RESULT_PATH));
			}
		}
	}
}