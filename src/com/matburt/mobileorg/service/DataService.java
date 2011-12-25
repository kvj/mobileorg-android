package com.matburt.mobileorg.service;

import java.util.Calendar;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.SuperService;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

import com.matburt.mobileorg.App;
import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController.ControllerListener;
import com.matburt.mobileorg.ui.FOutlineViewer;

public class DataService extends SuperService<DataController, App> implements
		ControllerListener {

	private static final String TAG = "DataService";
	PendingIntent syncIntent = null;

	public DataService() {
		super(DataController.class);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		setAlarmBroadcastReceiverClass(DataAlarmReceiver.class);
		controller.setListener(this);
		App.getInstance()
				.getPreferences()
				.registerOnSharedPreferenceChangeListener(
						new OnSharedPreferenceChangeListener() {

							@Override
							public void onSharedPreferenceChanged(
									SharedPreferences sharedPreferences,
									String key) {
								if ("doAutoSync".equals(key)) {
									reschedule(controller.hasChanges(), true);
								}
							}
						});
		reschedule(controller.hasChanges(), true);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		hideNotification();
	}

	@Override
	public void dataModified() {
		reschedule(true, true);
	}

	@Override
	public void syncStarted() {
		raiseNotification(R.drawable.logo_status_reload, "Sync is in progress",
				FOutlineViewer.class);
	}

	@Override
	public void syncFinished(boolean success) {
		reschedule(controller.hasChanges(), success);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand: " + flags);
		String message = intent.getStringExtra("message");
		Log.i(TAG, "Handle intent: " + message);
		if (null == message) {
			return Service.START_STICKY;
		}
		if ("sync".equals(message)) {
			try {
				new Thread() {
					public void run() {
						String error = controller.refresh();
						if (null != error) {
							SuperActivity.notifyUser(DataService.this, error);
						}
						SuperService.powerUnlock(DataService.this);
					};
				}.start();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
		return Service.START_STICKY;
	}

	private void reschedule(boolean changed, boolean success) {
		java.text.DateFormat timeFormat = android.text.format.DateFormat
				.getTimeFormat(this);
		int icon = changed ? R.drawable.logo_status_changes
				: R.drawable.logo_status;
		String text = "Error!";
		syncIntent = runAtTime(syncIntent, null, null);
		SharedPreferences prefs = App.getInstance().getPreferences();
		Calendar c = Calendar.getInstance();
		if (changed) {
			int autoSend = App.getInstance().getIntPreference(
					getString(R.string.autoSend), R.string.autoSendDefault);
			if (autoSend > 0) {
				c.add(Calendar.MILLISECOND, autoSend);
				// c.add(Calendar.MILLISECOND, 10000);
				syncIntent = runAtTime(syncIntent, c.getTimeInMillis(), "sync");
				text = "Autosend at " + timeFormat.format(c.getTime());
			} else {
				text = "Autosend is disabled";
			}
		} else {
			boolean autoSync = prefs.getBoolean(getString(R.string.doAutoSync),
					false);
			text = success ? "" : "Sync failed. ";
			if (autoSync) {
				c.add(Calendar.MILLISECOND,
						App.getInstance().getIntPreference("autoSyncInterval",
								R.string.autoSyncIntervalDefault));
				// c.add(Calendar.MILLISECOND, 10000);
				syncIntent = runAtTime(syncIntent, c.getTimeInMillis(), "sync");
				text += "Autosync at " + timeFormat.format(c.getTime());
			} else {
				text += "Autosync disabled";
			}
		}
		raiseNotification(icon, text, FOutlineViewer.class);
	}

}
