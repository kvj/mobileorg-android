package com.matburt.mobileorg.settings.widget;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;

public class WidgetPreferenceActivity extends PreferenceActivity implements
		ControllerReceiver<DataController> {

	ControllerConnector<App, DataController, DataService> connector = null;
	Integer widgetID = null;
	private String widgetType;
	private int prefID;

	public WidgetPreferenceActivity(String widgetType, int prefID) {
		this.widgetType = widgetType;
		this.prefID = prefID;
	}

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
		addPreferencesFromResource(prefID);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (null != widgetID) {
			App.getInstance().setWidgetConfig(widgetID, widgetType);
			App.getInstance().updateWidgets(widgetID);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		connector = new ControllerConnector<App, DataController, DataService>(
				this, this);
		connector.connectController(DataService.class);
	}

	@Override
	protected void onStop() {
		super.onStop();
		connector.disconnectController();
	}

	@Override
	public void onController(DataController controller) {
		// TODO Auto-generated method stub

	}
}
