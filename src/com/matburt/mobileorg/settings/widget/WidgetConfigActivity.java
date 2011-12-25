package com.matburt.mobileorg.settings.widget;

import org.kvj.bravo7.ControllerConnector;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;

public class WidgetConfigActivity extends PreferenceActivity {

	OutlineListPreference itemsPreference = null;

	ControllerConnector<App, DataController, DataService> connector = null;
	Integer widgetID = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		widgetID = App.getInstance().getWidgetConfigID(getIntent());
		if (null != widgetID) {
			App.getInstance().setWidgetConfigDone(this);
		}
		if (null == widgetID && getIntent().getExtras() != null) {
			widgetID = getIntent().getExtras().getInt("id");
		}
		getPreferenceManager().setSharedPreferencesName("widget_" + widgetID);
		addPreferencesFromResource(R.xml.widget_preference);
		itemsPreference = (OutlineListPreference) findPreference("items");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != widgetID) {
			App.getInstance().setWidgetConfig(widgetID, "outline");
			App.getInstance().updateWidgets(widgetID);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (RESULT_OK == resultCode
				&& OutlineListPreference.SELECT_OUTLINE == requestCode) {
			itemsPreference.itemModified(data.getStringExtra("data"));
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		connector = new ControllerConnector<App, DataController, DataService>(
				this, itemsPreference);
		connector.connectController(DataService.class);
	}

	@Override
	protected void onStop() {
		super.onStop();
		connector.disconnectController();
	}
}
