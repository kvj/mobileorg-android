package com.matburt.mobileorg.settings.widget;

import android.content.Intent;
import android.os.Bundle;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;

public class OutlineWidgetConfig extends WidgetPreferenceActivity {

	public OutlineWidgetConfig() {
		super("outline", R.xml.widget_preference);
	}

	OutlineListPreference itemsPreference = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		itemsPreference = (OutlineListPreference) findPreference("items");
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
	public void onController(DataController controller) {
		itemsPreference.onController(controller);
	}
}
