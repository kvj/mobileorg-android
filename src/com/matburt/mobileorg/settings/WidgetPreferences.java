package com.matburt.mobileorg.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.settings.widget.WidgetConfigActivity;
import com.matburt.mobileorg.settings.widget.WidgetList;
import com.matburt.mobileorg.settings.widget.WidgetList.ClickListener;
import com.matburt.mobileorg.settings.widget.WidgetList.WidgetInfo;

public class WidgetPreferences extends FragmentActivity implements ClickListener {

	WidgetList widgetList = null;
	Fragment editorFragment = null;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.widgets_config);
		widgetList = (WidgetList) getSupportFragmentManager().findFragmentById(R.id.widget_list);
		widgetList.setClickListener(this);
	}

	@Override
	public void click(WidgetInfo info) {
		if (null == editorFragment) {
			Intent intent = new Intent(this, WidgetConfigActivity.class);
			intent.putExtra("id", info.id);
			startActivity(intent);
		}
	}

}
