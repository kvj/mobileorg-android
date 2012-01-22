package com.matburt.mobileorg.ng.service;

import java.util.Calendar;
import java.util.List;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.SuperService;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.matburt.mobileorg.ng.App;
import com.matburt.mobileorg.ng.R;
import com.matburt.mobileorg.ng.service.DataController.ControllerListener;
import com.matburt.mobileorg.ng.service.DataController.NewNoteData;
import com.matburt.mobileorg.ng.service.OrgNGParser.ParseProgressListener;
import com.matburt.mobileorg.ng.ui.FOutlineViewer;

public class DataService extends SuperService<DataController, App> implements
		ControllerListener {

	private static final String TAG = "DataService";
	private static final String INTENT_SYNC = "com.matburt.mobileorg.ng.SYNC";
	private static final String INTENT_CREATE = "com.matburt.mobileorg.ng.CREATE";
	private static final String BROADCAST_CREATED = "com.matburt.mobileorg.ng.CREATED";
	PendingIntent syncIntent = null;

	public DataService() {
		super(DataController.class, "MobileOrg");
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
		// Log.i(TAG, "Sync finished: " + success);
		reschedule(controller.hasChanges(), success);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand: " + flags);
		String message = null;
		try {
			message = intent.getStringExtra("message");
		} catch (Exception e) {
		}
		Log.i(TAG, "Handle intent: " + message);
		if (null != intent && INTENT_CREATE.equals(intent.getAction())) {
			createData(intent);
		}
		if ("sync".equals(message)
				|| (null != intent && INTENT_SYNC.equals(intent.getAction()))) {
			try {
				powerLock(this);
				new AsyncTask<Void, Void, String>() {

					@Override
					protected String doInBackground(Void... params) {
						return controller.refresh(new ParseProgressListener() {

							@Override
							public void progress(int total, int totalPos,
									int current, int currentPos, String message) {
							}
						});
					}

					protected void onPostExecute(String error) {
						if (null != error) {
							SuperActivity.notifyUser(DataService.this, error);
						}
						SuperService.powerUnlock(DataService.this);
					};

				}.execute();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return Service.START_STICKY;
	}

	private void createData(Intent intent) {
		NoteNG note = new NoteNG();
		note.title = intent.getStringExtra("text");
		note.raw = note.title;
		note.priority = intent.getStringExtra("priority");
		note.tags = intent.getStringExtra("tags");
		if (!TextUtils.isEmpty(note.tags)) {
			if (!note.tags.startsWith(":")) {
				note.tags = ":" + note.tags;
			}
			if (!note.tags.endsWith(":")) {
				note.tags += ":";
			}
		}
		note.todo = intent.getStringExtra("todo");
		note.type = NoteNG.TYPE_OUTLINE;
		note.fileID = -1;
		NewNoteData data = new NewNoteData();
		List<String> params = intent
				.getStringArrayListExtra("properties_names");
		List<String> param_values = intent
				.getStringArrayListExtra("properties_values");
		if (null != params && null != param_values
				&& params.size() == param_values.size()) {
			for (int i = 0; i < params.size(); i++) {
				data.properties.put(params.get(i), param_values.get(i));
			}
		}
		List<String> childrenTypes = intent
				.getStringArrayListExtra("children_types");
		List<String> childrenValues = intent
				.getStringArrayListExtra("children_values");
		Log.i(TAG, "Create children: " + childrenTypes + ", " + childrenValues);
		if (null != childrenTypes && null != childrenValues
				&& childrenTypes.size() == childrenValues.size()) {
			for (int i = 0; i < childrenTypes.size(); i++) {
				NoteNG n = new NoteNG();
				n.type = childrenTypes.get(i);
				n.title = childrenValues.get(i);
				n.raw = childrenValues.get(i);
				data.children.add(n);
			}
		}
		data.attachment = intent.getStringExtra("attachment");

		if (null == controller.createNewNote(note, data)) {
			return;
		}
		controller.addChange(note.id, "data", null, null);
		Intent bcastIntent = new Intent(BROADCAST_CREATED);
		bcastIntent.putExtra("callback", intent.getIntExtra("callback", -1));
		sendBroadcast(bcastIntent);
		controller.notifyChangesHaveBeenMade();
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

	@Override
	public void progress(int total, int totalPos, int current, int currentPos,
			String message) {
		RemoteViews views = new RemoteViews(getPackageName(),
				R.layout.progress_dialog_status);
		views.setProgressBar(R.id.progress_bar1, total, totalPos, total == -1);
		views.setProgressBar(R.id.progress_bar2, current, currentPos,
				current == -1);
		views.setTextViewText(R.id.progress_text, message);
		raiseNotification(R.drawable.logo_status_reload, views,
				FOutlineViewer.class);
	}

}
