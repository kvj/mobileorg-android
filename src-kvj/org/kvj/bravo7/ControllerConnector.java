package org.kvj.bravo7;

import org.kvj.bravo7.SuperService.LocalBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class ControllerConnector<A extends ApplicationContext, T extends Object, S extends SuperService<T, A>> implements ServiceConnection {
	
	private static final String TAG = "ControllerConnector";
	private Context activity = null;
	private ControllerReceiver<T> receiver = null;
	@SuppressWarnings("rawtypes")
	private LocalBinder localBinder = null;
	
	public ControllerConnector(Context activity, ControllerReceiver<T> receiver) {
		this.activity = activity;
		this.receiver = receiver;
	}
	
	public interface ControllerReceiver<T> {
		public void onController(T controller);
	}
	
    public void onServiceDisconnected(ComponentName arg0) {
//	    Log.i(TAG, "["+activity.getClass().getSimpleName()+"]service disconnected");
    	localBinder = null;
    	receiver.onController(null);
    }

	@SuppressWarnings("rawtypes")
	public void onServiceConnected(ComponentName name, IBinder service) {
//	    Log.i(TAG, "["+activity.getClass().getSimpleName()+"]service connected: "+service);
        localBinder = (LocalBinder) service;
        receiver.onController(getController());
	}
	
	public void connectController(Class<S> cl) {
	    Intent intent = new Intent(activity, cl);
        activity.bindService(intent, this, Context.BIND_AUTO_CREATE);
	}
	
	public void disconnectController() {
		if (localBinder != null) {
			activity.unbindService(this);
		}
	}
	
	@SuppressWarnings("unchecked")
	public T getController() {
		if (localBinder == null) {
			return null;
		}
		return (T) localBinder.getController();
	}
}
