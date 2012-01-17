package com.matburt.mobileorg.ng.widgets;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.DataController;
import com.matburt.mobileorg.ng.service.NoteNG;
import com.matburt.mobileorg.ng.ui.FOutlineViewer;
import com.matburt.mobileorg.ng.ui.adapter.OutlineViewerAdapter;
import com.matburt.mobileorg.ng.ui.adapter.OutlineViewerAdapter.TextViewParts;

public class OutlineWidget extends AppWidgetProvider {
	private static final String TAG = "OutlineWidget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		try {
			DataController controller = App.getInstance().getBean(
					DataController.class);
			for (int i = 0; i < appWidgetIds.length; i++) {
				int id = appWidgetIds[i];
				SharedPreferences prefs = App.getInstance().getWidgetConfig(id);
				if (null == prefs) {
					Log.w(TAG, "No config for " + id);
					continue;
				}
				RemoteViews views = new RemoteViews(context.getPackageName(),
						R.layout.outline_widget);
				Intent intent = new Intent(context, FOutlineViewer.class);
				PendingIntent pendingIntent = PendingIntent.getActivity(
						context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
				views.setOnClickPendingIntent(R.id.outline_widget_root,
						pendingIntent);
				views.removeAllViews(R.id.outline_widget_list);
				int bg = Integer.parseInt(prefs.getString("background", "4"));
				Log.i(TAG,
						"bg: " + bg + ", " + prefs.getString("background", "4"));
				int bgResource = android.R.drawable.screen_background_dark;
				switch (bg) {
				case 0:
					bgResource = android.R.color.transparent;
					break;
				case 1:
					bgResource = R.drawable.opacity0;
					break;
				case 2:
					bgResource = R.drawable.opacity1;
					break;
				case 3:
					bgResource = R.drawable.opacity2;
					break;
				}
				views.setInt(R.id.outline_widget_list, "setBackgroundResource",
						bgResource);
				String[] items = prefs.getString("items", "\n").split("\n");
				for (int j = 0; j < items.length; j++) {
					putOutline(context, views, controller, items[j],
							prefs.getBoolean("long", false));
				}
				appWidgetManager.updateAppWidget(id, views);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void addText(Context context, RemoteViews views,
			CharSequence sequence, CharSequence rightPart) {
		RemoteViews text = new RemoteViews(context.getPackageName(),
				R.layout.outline_widget_item);
		text.setTextViewText(R.id.outline_widget_item_text, sequence);
		if (null != rightPart) {
			text.setTextViewText(R.id.outline_widget_item_tags, rightPart);
		}
		views.addView(R.id.outline_widget_list, text);
	}

	private void putOutline(int expand, boolean longFormat, NoteNG note,
			Context context, RemoteViews views, DataController controller) {
		if (null == note) {
			addText(context, views, "Error!", null);
			return;
		}
		OutlineViewerAdapter adapter = new OutlineViewerAdapter(context, null);
		adapter.setWide(longFormat);
		adapter.setController(note.id, controller, new ArrayList<Integer>());
		adapter.expandNote(adapter.getItem(0), 0, expand == -1 ? true : false);
		if (expand == 2) {
			int pos = 0;
			int size = adapter.getCount();
			for (int i = 1; i < size; i++) {
				pos += adapter.expandNote(adapter.getItem(i + pos), i + pos,
						false);
			}
		}
		// Log.i(TAG, "Number of items: " + adapter.getCount());
		for (int i = 0; i < adapter.getCount(); i++) {
			TextViewParts parts = adapter.customizeTextView(adapter.getItem(i),
					false);
			addText(context, views, parts.leftPart, parts.rightPart);
		}
	}

	private void putOutline(Context context, RemoteViews views,
			DataController controller, String link, boolean longFormat) {
		NoteNG note = controller.findNoteByLink(link);
		putOutline(controller.getExpand(link), longFormat, note, context,
				views, controller);
	}
}
