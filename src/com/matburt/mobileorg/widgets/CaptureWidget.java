package com.matburt.mobileorg.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.ui.DataEditActivity;

public class CaptureWidget extends AppWidgetProvider {

	private static final String TAG = "CaptureWidget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		try {
			for (int i = 0; i < appWidgetIds.length; i++) {
				int id = appWidgetIds[i];
				SharedPreferences prefs = App.getInstance().getWidgetConfig(id);
				if (null == prefs) {
					Log.w(TAG, "No config for " + id);
					continue;
				}
				RemoteViews views = new RemoteViews(context.getPackageName(),
						R.layout.capture_widget);
				views.setTextViewText(R.id.capture_widget_text,
						prefs.getString("name", "???"));
				Intent intent = new Intent(context, DataEditActivity.class);
				intent.putExtra("type", "title");
				intent.putExtra("panel", true);
				intent.putExtra("text", prefs.getString("template", ""));
				intent.putExtra("todo", prefs.getString("template_todo", ""));
				intent.putExtra("priority",
						prefs.getString("template_priority", ""));
				intent.putExtra("tags", prefs.getString("template_tags", ""));
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
				views.setOnClickPendingIntent(R.id.capture_widget_root,
						pendingIntent);
				appWidgetManager.updateAppWidget(id, views);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
