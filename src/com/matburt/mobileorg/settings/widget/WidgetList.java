package com.matburt.mobileorg.settings.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.matburt.mobileorg.App;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class WidgetList extends ListFragment {

	public interface ClickListener {
		public void click(WidgetInfo info);
	}
	
	public class WidgetInfo {
		public int id;
		String type;
		
		@Override
		public String toString() {
			return type+id;
		}
	}
	
	ClickListener clickListener = null;
	List<WidgetInfo> list = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		App.getInstance().setWidgetConfig(0, "outline");
		App.getInstance().setWidgetConfig(1, "outline");
		reloadData();
	}
	
	private void reloadData() {
		Map<Integer, String> data = App.getInstance().getWidgetConfigs(null);
		list = new ArrayList<WidgetList.WidgetInfo>();
		for (Integer id : data.keySet()) {
			WidgetInfo info = new WidgetInfo();
			info.id = id;
			info.type = data.get(id);
			list.add(info);
		}
		setListAdapter(new ArrayAdapter<WidgetInfo>(getActivity(), android.R.layout.simple_selectable_list_item, list));
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		reloadData();
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		if (null != clickListener) {
			clickListener.click(list.get(position));
		}
		
	}
	
	public void setClickListener(ClickListener clickListener) {
		this.clickListener = clickListener;
	}
	
}
