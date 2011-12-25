package com.matburt.mobileorg.settings.widget;

import java.util.ArrayList;
import java.util.List;

import org.kvj.bravo7.ControllerConnector.ControllerReceiver;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.app.DialogFragment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.NoteNG;

public class OutlineListPreference extends Preference implements
		ControllerReceiver<DataController> {

	private static final String TAG = "OutlineListPref";
	public static final int SELECT_OUTLINE = 103;
	LinearLayout listView = null;
	private DataController controller = null;
	List<String> data = new ArrayList<String>();
	int editedItem = -1;
	BaseAdapter adapter = new BaseAdapter() {

		@Override
		public View getView(final int pos, View view, ViewGroup root) {
			if (null == view) {
				LayoutInflater inflater = (LayoutInflater) root.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inflater.inflate(R.layout.outline_list_list_item, root,
						false);
			}
			TextView textView = (TextView) view
					.findViewById(R.id.outline_list_item_text);
			ImageButton removeButton = (ImageButton) view
					.findViewById(R.id.outline_list_item_remove_button);
			removeButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					data.remove(pos);
					saveData();
				}
			});
			ImageButton editButton = (ImageButton) view
					.findViewById(R.id.outline_list_item_edit_button);
			editButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					editItem(pos);
				}
			});
			NoteNG note = controller.findNoteByLink(data.get(pos));
			if (null == note) {
				textView.setText("Error!");
			} else {
				textView.setText(note.title);
			}
			return view;
		}

		@Override
		public long getItemId(int arg0) {
			return arg0;
		}

		@Override
		public Object getItem(int arg0) {
			return data.get(arg0);
		}

		@Override
		public int getCount() {
			return data.size();
		}
	};

	public OutlineListPreference(Context context, AttributeSet set) {
		super(context, set);
		setLayoutResource(R.layout.outline_list_view);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = super.onCreateView(parent);
		return view;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		listView = (LinearLayout) view.findViewById(R.id.outline_list_list);
		Button addButton = (Button) view
				.findViewById(R.id.outline_list_add_button);
		adapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				listView.removeAllViews();
				for (int i = 0; i < adapter.getCount(); i++) {
					LinearLayout.LayoutParams params = new LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.WRAP_CONTENT);
					View view = adapter.getView(i, null, listView);
					listView.addView(view, params);
				}
				listView.requestLayout();
				// Log.i(TAG, "Layout changed: "+adapter.getCount());
			}
		});
		addButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				data.add("e::");
				editItem(data.size() - 1);
			}
		});
		adapter.notifyDataSetChanged();
	}

	private void editItem(int pos) {
		editedItem = pos;
		Intent intent = new Intent(getContext(), SelectOutlineActivity.class);
		intent.putExtra("data", data.get(pos));
		((Activity) getContext())
				.startActivityForResult(intent, SELECT_OUTLINE);
	}

	public void itemModified(String link) {
		Log.i(TAG, "Item selected: " + link);
		data.set(editedItem, link);
		saveData();
	}

	@Override
	protected void onPrepareForRemoval() {
		super.onPrepareForRemoval();
	}

	private void saveData() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.size(); i++) {
			if (i > 0) {
				sb.append("\n");
			}
			sb.append(data.get(i));
		}
		getSharedPreferences().edit().putString(getKey(), sb.toString())
				.commit();
		reloadData();
	}

	private void reloadData() {
		data.clear();
		String[] items = getPersistedString("\n").split("\n");
		for (int i = 0; i < items.length; i++) {
			if (!"".equals(items[i])) {
				data.add(items[i]);
			}
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onController(DataController controller) {
		this.controller = controller;
		reloadData();
	}

	class SelectOutlineDialog extends DialogFragment {

		public SelectOutlineDialog() {
			setCancelable(true);
			setTitle("Select item");
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.select_outline_dialog,
					container, false);
			return v;
		}
	}

}
