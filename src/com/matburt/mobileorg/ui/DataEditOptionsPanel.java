package com.matburt.mobileorg.ui;

import java.util.ArrayList;
import java.util.List;

import android.R.color;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataController.TodoState;

public class DataEditOptionsPanel extends Fragment {

	class StringListAdapter implements SpinnerAdapter {

		List<String> data = null;
		
		public StringListAdapter(List<String> data) {
			this.data = new ArrayList<String>(data);
		}
		
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public String getItem(int arg0) {
			return data.get(arg0);
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public int getItemViewType(int arg0) {
			return 0;
		}

		@Override
		public View getView(int pos, View view, ViewGroup parent) {
			if (view == null) {
				view = new TextView(parent.getContext());
				
			}
			TextView textView = (TextView) view;
			textView.setTextColor(Color.BLACK);
			textView.setText(getItem(pos));
			return view;
		}

		@Override
		public int getViewTypeCount() {
			return 1;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public void registerDataSetObserver(DataSetObserver arg0) {
		}

		@Override
		public void unregisterDataSetObserver(DataSetObserver arg0) {
		}

		@Override
		public View getDropDownView(int arg0, View arg1, ViewGroup arg2) {
			arg1 = getView(arg0, arg1, arg2);
			arg1.setPadding(20, 20, 20, 20);
			return arg1;
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
		return view;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	public void loadData(DataController controller, Bundle data) {
		List<String> items = new ArrayList<String>();
		items.add("Empty");
		int selectedTODO = 0;
		List<TodoState> todoStates = controller.getTodoTypes();
		for (int i = 0; i < todoStates.size(); i++) {
			items.add(todoStates.get(i).name);
			if (todoStates.get(i).name.equals(data.getString("todo"))) {
				selectedTODO = i+1;
			}
		}
		todoSpinner.setAdapter(new StringListAdapter(items));
		todoSpinner.setSelection(selectedTODO);
		items.clear();
		items.add("Empty");
		int selectedPriority = 0;
		List<String> prStates = controller.getPrioritiesNG();
		for (int i = 0; i < prStates.size(); i++) {
			items.add(prStates.get(i));
			if (prStates.get(i).equals(data.getString("priority"))) {
				selectedPriority = i+1;
			}
		}
		prioritySpinner.setAdapter(new StringListAdapter(items));
		prioritySpinner.setSelection(selectedPriority);
		tagsText.setText(data.getString("tags"));
	}
	
	public void saveData(Bundle data) {
		if (0 == todoSpinner.getSelectedItemPosition()) {
			data.putString("todo", null);
		} else {
			data.putString("todo", todoSpinner.getSelectedItem().toString());
		}
		if (0 == prioritySpinner.getSelectedItemPosition()) {
			data.putString("priority", null);
		} else {
			data.putString("priority", prioritySpinner.getSelectedItem().toString());
		}
		String tagsString = tagsText.getText().toString().trim();
		data.putString("tags", 
				"".equals(tagsString)? null
						: tagsString);
	}
	
}
