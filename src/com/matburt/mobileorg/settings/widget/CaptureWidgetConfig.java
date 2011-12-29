package com.matburt.mobileorg.settings.widget;

import android.os.Bundle;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;

public class CaptureWidgetConfig extends WidgetPreferenceActivity {

	CaptureTemplatePreference captureTemplatePreference = null;

	public CaptureWidgetConfig() {
		super("capture", R.xml.capture_widget_preference);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		captureTemplatePreference = (CaptureTemplatePreference) findPreference("template");
	}

	@Override
	public void onController(DataController controller) {
		super.onController(controller);
		captureTemplatePreference.onController(controller);
	}

	@Override
	protected void onDestroy() {
		captureTemplatePreference.saveData();
		super.onDestroy();
	}
}
