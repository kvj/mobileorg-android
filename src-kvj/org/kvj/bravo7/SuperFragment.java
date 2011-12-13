package org.kvj.bravo7;

import org.kvj.bravo7.ControllerConnector.ControllerReceiver;

import android.support.v4.app.Fragment;
import android.widget.Toast;

public class SuperFragment<A extends ApplicationContext, T, S extends SuperService<T, A>> extends Fragment implements ControllerReceiver<T>{

	Class<S> serviceClass = null;
	
	public SuperFragment(Class<S> serviceClass) {
		this.serviceClass = serviceClass;
	}
	private static final String TAG = "SuperFragment";
	protected T controller = null;
	ControllerConnector<A, T, S> connector = null;
	
	public void onController(T controller) {
		this.controller = controller;
	}

	@Override
	public void onStart() {
		super.onStart();
		connector = new ControllerConnector<A, T, S>(getActivity(), this);
		connector.connectController(serviceClass);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		connector.disconnectController();
	}
	
	public void notifyUser(String message) {
		Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
	}
	
}
