package com.matburt.mobileorg.settings.widget;

import org.kvj.bravo7.ControllerConnector.ControllerReceiver;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.ui.DataEditOptionsPanel;

public class CaptureTemplatePreference extends Preference implements
		ControllerReceiver<DataController> {

	private static final String TAG = "CaptureTemplate";
	private Spinner todoSpinner;
	private Spinner prioritySpinner;
	private EditText tagsText;
	private EditText titleText;
	private DataController controller;

	public CaptureTemplatePreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setLayoutResource(R.layout.capture_template);
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		todoSpinner = (Spinner) view.findViewById(R.id.data_panel_todo);
		prioritySpinner = (Spinner) view.findViewById(R.id.data_panel_priority);
		tagsText = (EditText) view.findViewById(R.id.data_panel_tags);
		titleText = (EditText) view.findViewById(R.id.data_panel_title);
		DataEditOptionsPanel.loadPanel(getContext(), controller, todoSpinner,
				getSharedPreferences().getString("template_todo", null),
				prioritySpinner,
				getSharedPreferences().getString("template_priority", null),
				tagsText,
				getSharedPreferences().getString("template_tags", null));
		titleText.setText(getSharedPreferences().getString("template", null));
	}

	@Override
	public void onController(DataController controller) {
		this.controller = controller;
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		saveData();
		return super.onSaveInstanceState();
	}

	public void saveData() {
		Editor editor = getSharedPreferences().edit();
		editor.putString("template_todo",
				DataEditOptionsPanel.getSpinnerValue(todoSpinner));
		editor.putString("template_priority",
				DataEditOptionsPanel.getSpinnerValue(prioritySpinner));
		editor.putString("template_tags", tagsText.getText().toString());
		editor.putString("template", titleText.getText().toString());
		editor.commit();
	}

}
