package com.matburt.mobileorg.service;

import org.kvj.bravo7.ApplicationContext;
import org.kvj.bravo7.SuperService;

import com.matburt.mobileorg.MobileOrgActivity;
import com.matburt.mobileorg.R;

public class DataService extends SuperService<DataController> {
	
	public DataService() {
		super(DataController.class);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		raiseNotification(R.drawable.logo_72, "MobileOrg ready", MobileOrgActivity.class);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		hideNotification();
	}

}
