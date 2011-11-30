package com.matburt.mobileorg;

import com.matburt.mobileorg.service.DataService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class MobileOrgStartupIntentReceiver extends BroadcastReceiver {

    private SharedPreferences appSettings;

    private boolean shouldStartService(Context ctxt) {
        this.appSettings = PreferenceManager.getDefaultSharedPreferences(ctxt.getApplicationContext());
        return this.appSettings.getBoolean("doAutoSync", false);
    }

	@Override
	public void onReceive(Context context, Intent intent) {
        if (this.shouldStartService(context)) {
            Intent serviceIntent = new Intent(context, DataService.class);
            context.startService(serviceIntent);
        }
	}
}