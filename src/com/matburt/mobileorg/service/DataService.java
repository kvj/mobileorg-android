package com.matburt.mobileorg.service;

import org.kvj.bravo7.SuperService;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.ui.OutlineViewer;

public class DataService extends SuperService<DataController, App> {
	
	public DataService() {
		super(DataController.class);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		raiseNotification(R.drawable.logo_72, "MobileOrg ready", OutlineViewer.class);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		hideNotification();
	}

}
