package org.kvj.bravo7;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

	private static final String TAG = "AlarmReceiver";
	protected Class<? extends Service> serviceClass = null;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG,
				"Alarm!: " + serviceClass.getName() + ", "
						+ intent.getComponent());
		try {
			Intent serviceIntent = new Intent(context, serviceClass);
			serviceIntent.putExtras(intent);
			SuperService.powerLock(context);
			context.startService(serviceIntent);
			// Log.i(TAG, "Service intent sent: " + cn);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
