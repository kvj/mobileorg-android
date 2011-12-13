package com.matburt.mobileorg.ui;

import org.kvj.bravo7.SuperActivity;

import android.os.Bundle;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataService;

public class DataEditor extends SuperActivity<App, DataController, DataService>{

	public DataEditor() {
		super(DataService.class);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onController(DataController controller) {
		super.onController(controller);
	}
}
