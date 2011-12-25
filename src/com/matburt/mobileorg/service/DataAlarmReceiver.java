package com.matburt.mobileorg.service;

import org.kvj.bravo7.AlarmReceiver;

public class DataAlarmReceiver extends AlarmReceiver {

	public DataAlarmReceiver() {
		serviceClass = DataService.class;
	}

}
