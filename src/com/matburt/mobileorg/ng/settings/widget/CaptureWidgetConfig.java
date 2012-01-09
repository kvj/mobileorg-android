package com.matburt.mobileorg.ng.settings.widget;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.preference.ListPreference;

import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.DataController;
import com.matburt.mobileorg.ng.service.DataController.TodoState;

public class CaptureWidgetConfig extends WidgetPreferenceActivity {

	public CaptureWidgetConfig() {
		super("capture", R.xml.capture_widget_preference);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onController(DataController controller) {
		super.onController(controller);
		ListPreference todo = (ListPreference) findPreference("template_todo");
		ListPreference priority = (ListPreference) findPreference("template_priority");
		List<TodoState> todoStates = controller.getTodoTypes();
		List<CharSequence> ids = new ArrayList<CharSequence>();
		List<CharSequence> values = new ArrayList<CharSequence>();
		ids.add("");
		values.add("Empty");
		for (TodoState todoState : todoStates) {
			ids.add(todoState.name);
			values.add(todoState.name);
		}
		todo.setEntries(values.toArray(new CharSequence[0]));
		todo.setEntryValues(ids.toArray(new CharSequence[0]));

		List<String> prStates = controller.getPrioritiesNG();
		ids = new ArrayList<CharSequence>();
		values = new ArrayList<CharSequence>();
		ids.add("");
		values.add("Empty");
		for (String pr : prStates) {
			ids.add(pr);
			values.add(pr);
		}
		priority.setEntries(values.toArray(new CharSequence[0]));
		priority.setEntryValues(ids.toArray(new CharSequence[0]));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
}
