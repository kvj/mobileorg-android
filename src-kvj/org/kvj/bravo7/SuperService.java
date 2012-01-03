package org.kvj.bravo7;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.RemoteViews;

public abstract class SuperService<T, A extends ApplicationContext> extends
		Service {

	protected T controller = null;
	private Class<T> controllerClass = null;
	private final IBinder binder = new LocalBinder();
	private Notification notification = null;
	protected String title = "Application";
	private static final int SERVICE_NOTIFY = 100;
	private static final String TAG = "SuperService";
	private static final String LOCK_NAME = "SuperService";
	protected int notificationID = SERVICE_NOTIFY;
	private Class<? extends AlarmReceiver> alarmBroadcastReceiverClass = null;
	static WakeLock lockStatic = null;

	public SuperService() {
		super();
	}

	synchronized private static PowerManager.WakeLock getLock(Context context) {
		if (lockStatic == null) {
			PowerManager mgr = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);

			lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					LOCK_NAME);
			lockStatic.setReferenceCounted(true);
		}
		return (lockStatic);
	}

	public static synchronized void powerLock(Context context) {
		getLock(context).acquire();
	}

	public static synchronized void powerUnlock(Context context) {
		getLock(context).release();
	}

	public SuperService(Class<T> controllerClass, String title) {
		this();
		this.controllerClass = controllerClass;
		this.title = title;
	}

	public class LocalBinder extends Binder {

		public T getController() {
			return controller;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		ApplicationContext ctx = A.getInstance();
		controller = ctx.getBean(controllerClass);
		notification = new Notification();
	}

	public void raiseNotification(int icon, String text,
			Class<? extends Activity> received) {
		notification.icon = icon;
		notification.setLatestEventInfo(getApplicationContext(), title, text,
				PendingIntent.getActivity(getApplicationContext(), 0,
						new Intent(getApplicationContext(), received),
						PendingIntent.FLAG_CANCEL_CURRENT));
		startForeground(notificationID, notification);
	}

	public void raiseNotification(int icon, RemoteViews views,
			Class<? extends Activity> received) {
		notification.icon = icon;
		notification.contentView = views;
		notification.contentIntent = PendingIntent.getActivity(
				getApplicationContext(), 0, new Intent(getApplicationContext(),
						received), PendingIntent.FLAG_CANCEL_CURRENT);
		startForeground(notificationID, notification);
	}

	public void hideNotification() {
		((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
				.cancel(notificationID);
		stopForeground(true);
	}

	@Override
	public void onDestroy() {
		hideNotification();
		super.onDestroy();
	}

	protected PendingIntent runAtTime(PendingIntent toCancel, Long date,
			String message) {
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		if (null != toCancel) {
			alarmManager.cancel(toCancel);
		}
		if (null == date) {
			return null;
		}
		Intent intent = new Intent(this, alarmBroadcastReceiverClass);
		Log.i(TAG, "runAtTime - " + alarmBroadcastReceiverClass.getName());
		intent.putExtra("message", message);
		PendingIntent pintent = PendingIntent.getBroadcast(this, 0, intent, 0);
		alarmManager.set(AlarmManager.RTC_WAKEUP, date, pintent);
		return pintent;
	}

	public void setAlarmBroadcastReceiverClass(
			Class<? extends AlarmReceiver> alarmBroadcastReceiverClass) {
		this.alarmBroadcastReceiverClass = alarmBroadcastReceiverClass;
	}
}
