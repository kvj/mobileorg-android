package com.matburt.mobileorg.ng.settings;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.ng.R;

public class WebDAVSettingsActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.webdav_preferences);
	}
}