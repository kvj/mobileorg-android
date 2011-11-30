package org.kvj.bravo7;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SuperService<T> extends Service{

	protected T controller = null;
	private Class<T> controllerClass = null;
	private final IBinder binder = new LocalBinder();
	private Notification notification = null;
	protected String title = "Application";
    private static final int SERVICE_NOTIFY = 100;
	private static final String TAG = "SuperService";
    protected int notificationID = SERVICE_NOTIFY;
	
    public SuperService(Class<T> controllerClass) {
    	this.controllerClass = controllerClass;
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
    	ApplicationContext ctx = ApplicationContext.getInstance(this);
    	controller = ctx.getBean(controllerClass);
    	if (null == controller) {
			try {
				controller = controllerClass.getConstructor(Context.class).newInstance(this);
				ctx.publishBean(controller);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "Error creating controller");
			}
		}
		notification = new Notification();
	}
	
	public void raiseNotification(int icon, String text, Class<? extends Activity> received) {
		notification.icon = icon;
		notification.setLatestEventInfo(getApplicationContext(), title, text, 
				PendingIntent.getActivity(getApplicationContext(), 0, 
						new Intent(getApplicationContext(), received), 
						PendingIntent.FLAG_CANCEL_CURRENT));
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
}
