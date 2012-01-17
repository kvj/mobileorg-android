package com.matburt.mobileorg.ng.ui;

import org.kvj.bravo7.SuperActivity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.DataController;
import com.matburt.mobileorg.ng.service.DataService;
import com.matburt.mobileorg.ng.ui.adapter.OutlineViewerAdapter;

public class SearchActivity extends
		SuperActivity<App, DataController, DataService> {

	private static final String TAG = "SearchActivity";
	private ListView listView;
	private OutlineViewerAdapter adapter = null;

	public SearchActivity() {
		super(DataService.class);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Log.i(TAG, "New intent: " + intent.getAction());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String id = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY);
			if (null != id) {
				Intent viewIntent = new Intent(this, FOutlineViewer.class);
				if (id.startsWith("id:")) {
					viewIntent.putExtra("noteLink", id);
				} else {
					viewIntent.putExtra("noteID",
							Integer.parseInt(id.substring(4)));
				}
				finish();
				startActivity(viewIntent);
				return;
			}
		}
		setContentView(R.layout.search_outline);
		listView = (ListView) findViewById(R.id.search_outline_list);
		adapter = new OutlineViewerAdapter(this, null);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				Intent intent = new Intent(SearchActivity.this,
						FOutlineViewer.class);
				intent.putExtra("noteID", adapter.getItem(pos).id);
				startActivity(intent);
			}
		});
	}

	@Override
	public void onController(DataController controller) {
		super.onController(controller);
		Intent intent = getIntent();
		Log.i(TAG,
				"New activity intent: " + intent.getAction() + ", "
						+ intent.getStringExtra(SearchManager.EXTRA_DATA_KEY)
						+ ", " + intent.getStringExtra(SearchManager.QUERY)
						+ ", " + intent.getExtras());
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			adapter.search(controller, query);
		}
	}
}
