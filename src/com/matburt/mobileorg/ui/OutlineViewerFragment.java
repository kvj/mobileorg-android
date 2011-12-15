package com.matburt.mobileorg.ui;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class OutlineViewerFragment extends ListFragment {

	public interface DataListener {
		
		public void onOpen(OutlineViewerFragment fragment, int position);
		
	}

	private static final String TAG = null;
	
	OutlineViewerAdapter adapter = null;
	String name;
	DataController controller;
	Bundle data;
	DataListener dataListener = null;
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		adapter = new OutlineViewerAdapter(getActivity());
		setListAdapter(adapter);
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view,
					int pos, long id) {
				if (null != dataListener) {
					dataListener.onOpen(OutlineViewerFragment.this, pos);
					return true;
				}
				return true;
			}
		});
		getListView().setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				return keyListener(keyCode, event);
			}
		});
	}
	
	public boolean keyListener(int keyCode, KeyEvent event) {
//		Log.i(TAG, "Key listener: "+keyCode);
		int pos = getSelectedItemPosition();
//		if (KeyEvent.KEYCODE_SPACE == keyCode && -1 != pos) {
//			adapter.collapseExpand(pos, true);
//			return true;
//		}
		if (KeyEvent.KEYCODE_SPACE == keyCode && -1 != pos) {
			if (null != dataListener) {
				dataListener.onOpen(this, pos);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.viewer_menu, menu);
	}

	public void loadData(String name, DataController controller, Bundle data) {
		this.name = name;
		this.controller = controller;
		this.data = data;
		adapter.setController(data.getInt(name+"_id", -1), controller, data.getIntegerArrayList(name+"_sel"));
	}
	
	public void saveData() {
		Log.i(TAG, "Saving data: "+name);
		if (null != data) {
			data.putIntegerArrayList(name+"_sel", adapter.getSelection());
			Log.i(TAG, "Saved data: "+data.getIntegerArrayList(name+"_sel"));
		}
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		adapter.collapseExpand(position, true);
	}
	
	public void setDataListener(DataListener dataListener) {
		this.dataListener = dataListener;
	}

	public void reload() {
		adapter.reload(adapter.getSelection());
	}
	
}
