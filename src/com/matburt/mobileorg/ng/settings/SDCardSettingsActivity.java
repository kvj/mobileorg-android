package com.matburt.mobileorg.ng.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.ng.R;

public class SDCardSettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.sdsync_preferences);
	}
}