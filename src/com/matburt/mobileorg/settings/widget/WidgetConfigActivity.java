package com.matburt.mobileorg.settings.widget;

import org.kvj.bravo7.ControllerConnector;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class WidgetConfigActivity extends PreferenceActivity {

	OutlineListPreference itemsPreference = null;
	
	ControllerConnector<App, DataController, DataService> connector = null;
	
@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName("widget_"+getIntent().getExtras().getInt("id"));
		addPreferencesFromResource(R.xml.widget_preference);
		itemsPreference = (OutlineListPreference) findPreference("items");
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (RESULT_OK == resultCode && OutlineListPreference.SELECT_OUTLINE == requestCode) {
			itemsPreference.itemModified(data.getStringExtra("data"));
		}
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		connector = new ControllerConnector<App, DataController, DataService>(this, itemsPreference);
		connector.connectController(DataService.class);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		connector.disconnectController();
	}
}
