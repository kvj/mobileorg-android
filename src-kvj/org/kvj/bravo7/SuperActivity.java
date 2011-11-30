package org.kvj.bravo7;

import org.kvj.bravo7.ControllerConnector;
import org.kvj.bravo7.ControllerConnector.ControllerReceiver;

import android.app.Activity;
import android.widget.Toast;

public class SuperActivity<T, S extends SuperService<T>> extends Activity implements ControllerReceiver<T>{

	Class<S> serviceClass = null;
	
	public SuperActivity(Class<S> serviceClass) {
		this.serviceClass = serviceClass;
	}
	private static final String TAG = "SuperActivity";
	protected T controller = null;
	ControllerConnector<T, S> connector = new ControllerConnector<T, S>(this, this);
	
	public void onController(T controller) {
		this.controller = controller;
	}

	@Override
	protected void onStart() {
		super.onStart();
		connector.connectController(serviceClass);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		connector.disconnectController();
	}
	
	public void notifyUser(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}
	
}
