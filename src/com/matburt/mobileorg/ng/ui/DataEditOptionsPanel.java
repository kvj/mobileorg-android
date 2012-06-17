package com.matburt.mobileorg.ng.ui;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.DataController;
import com.matburt.mobileorg.ng.service.DataController.TodoState;

public class DataEditOptionsPanel extends Fragment {

	public static class StringListAdapter extends ArrayAdapter<String> {

		public StringListAdapter(Context activity, List<String> data) {
			super(activity, android.R.layout.simple_spinner_item,
					new ArrayList<String>(data));
			setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		}

	}

	Spinner todoSpinner = null;
	Spinner prioritySpinner = null;
	EditText tagsText = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.data_options, container);
		todoSpinner = (Spinner) view.findViewById(R.id.data_panel_todo);
		prioritySpinner = (Spinner) view.findViewById(R.id.data_panel_priority);
		tagsText = (EditText) view.findViewById(R.id.data_panel_tags);
		tagsText.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);
		tagsText.setTextSize(
				TypedValue.COMPLEX_UNIT_DIP,
				App.getInstance().getIntPreference(R.string.docFontSize,
						R.string.docFontSizeDefault));
		return view;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public static void loadPanel(Context activity, DataController controller,
			Spinner todoSpinner, String todo, Spinner prioritySpinner,
			String priority, EditText tagsText, String tags) {
		List<String> items = new ArrayList<String>();
		items.add("Empty");
		int selectedTODO = 0;
		List<TodoState> todoStates = controller.getTodoTypes();
		for (int i = 0; i < todoStates.size(); i++) {
			items.add(todoStates.get(i).name);
			if (todoStates.get(i).name.equals(todo)) {
				selectedTODO = i + 1;
			}
		}
		todoSpinner.setAdapter(new StringListAdapter(activity, items));
		todoSpinner.setSelection(selectedTODO);
		items.clear();
		items.add("Empty");
		int selectedPriority = 0;
		List<String> prStates = controller.getPrioritiesNG();
		for (int i = 0; i < prStates.size(); i++) {
			items.add(prStates.get(i));
			if (prStates.get(i).equals(priority)) {
				selectedPriority = i + 1;
			}
		}
		prioritySpinner.setAdapter(new StringListAdapter(activity, items));
		prioritySpinner.setSelection(selectedPriority);
		if (null == tags) {
			tags = ":";
		}
		tagsText.setText(tags);

	}

	public void loadData(DataController controller, Bundle data) {
		loadPanel(this.getActivity(), controller, todoSpinner,
				data.getString("todo"), prioritySpinner,
				data.getString("priority"), tagsText, data.getString("tags"));
	}

	public static String getSpinnerValue(Spinner spinner) {
		if (0 == spinner.getSelectedItemPosition()) {
			return null;
		} else {
			return spinner.getSelectedItem().toString();
		}
	}

	public void saveData(Bundle data) {
		data.putString("todo", getSpinnerValue(todoSpinner));
		data.putString("priority", getSpinnerValue(prioritySpinner));
		String tagsString = tagsText.getText().toString().trim();
		if (!tagsString.startsWith(":")) {
			tagsString = ":" + tagsString;
		}
		if (!tagsString.endsWith(":")) {
			tagsString = tagsString + ":";
		}
		if (":".equals(tagsString)) {
			tagsString = null;
		}
		data.putString("tags", tagsString);
	}

}
