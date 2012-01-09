package com.matburt.mobileorg.ng.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.settings.widget.CaptureWidgetConfig;
import com.matburt.mobileorg.ng.settings.widget.OutlineWidgetConfig;
import com.matburt.mobileorg.ng.settings.widget.WidgetList;
import com.matburt.mobileorg.ng.settings.widget.WidgetList.ClickListener;
import com.matburt.mobileorg.ng.settings.widget.WidgetList.WidgetInfo;
import com.matburt.mobileorg.ng.settings.widget.WidgetPreferenceActivity;

public class WidgetPreferences extends FragmentActivity implements
		ClickListener {

	WidgetList widgetList = null;
	Fragment editorFragment = null;

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		super.onActivityResult(arg0, arg1, arg2);
		widgetList.reloadData();
	}

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.widgets_config);
		widgetList = (WidgetList) getSupportFragmentManager().findFragmentById(
				R.id.widget_list);
		widgetList.setClickListener(this);
	}

	@Override
	public void click(WidgetInfo info) {
		if (null == editorFragment) {
			Class<? extends WidgetPreferenceActivity> configActivity = getConfigActivity(info);
			if (null == configActivity) {
				return;
			}
			Intent intent = new Intent(this, configActivity);
			intent.putExtra("id", info.id);
			startActivity(intent);
		}
	}

	protected Class<? extends WidgetPreferenceActivity> getConfigActivity(
			WidgetInfo info) {
		if ("outline".equals(info.type)) {
			return OutlineWidgetConfig.class;
		}
		if ("capture".equals(info.type)) {
			return CaptureWidgetConfig.class;
		}
		return null;
	}
}
