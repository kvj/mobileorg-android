package com.matburt.mobileorg.Synchronizers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.net.Proxy;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Account;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.matburt.mobileorg.R;


public class DropboxAuthActivity extends Activity {

    private static final String LT = "MobileOrg";
    AndroidAuthSession session = null;
    private DropboxAPI<AndroidAuthSession> api = null;

    private TextView dbInfo;
    private Button dbLogin;
    private Button dbLogout;
    private Resources r;
    private AppKeyPair dbConfig;
    
    @Override
    protected void onResume() {
    	super.onResume();
		if (session.authenticationSuccessful()) {
			Log.i(LT, "Auth OK - finishing");
			session.finishAuthentication();
			AccessTokenPair tokens = session.getAccessTokenPair();
			storeKeys(tokens);
			refreshAccountState();
		}
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.androidauth);
        session = createSession(this);
        api = new DropboxAPI<AndroidAuthSession>(session);
        
        dbInfo = (TextView)findViewById(R.id.dbox_current_token);
        dbLogin = (Button)findViewById(R.id.dbox_login);
        dbLogout = (Button)findViewById(R.id.dbox_logout);
        dbLogin.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				try {
					Log.i(LT, "Starting auth...");
					session.startAuthentication(DropboxAuthActivity.this);
				} catch (IllegalStateException e) {
					Log.e(LT, "Error in start auth", e);
				}
			}
		});
        dbLogout.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				session.unlink();
				clearKeys();
				refreshAccountState();
			}
		});
        r = this.getResources();
    }
    
    @Override
    protected void onStart() {
    	super.onStart();
    	refreshAccountState();
    }

    /**
     * This lets us use the Dropbox API from the LoginAsyncTask
     */
    public DropboxAPI getAPI() {
    	return api;
    }

    /**
     * Convenience function to change UI state based on being logged in
     */
    public void refreshAccountState() {
		dbLogin.setEnabled(false);
		dbLogout.setEnabled(false);
    	new AsyncTask<Void, Void, Account>() {

			@Override
			protected Account doInBackground(Void... params) {
				try {
					return api.accountInfo();
				} catch (Exception e) {
					Log.e(LT, "Error getting account info:", e);
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Account account) {
				if (null != account) {
					dbLogout.setEnabled(true);
				} else {
					dbLogin.setEnabled(true);
				}
				displayAccountInfo(account);
			}
    	}.execute((Void)null);
    }

    public void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        error.show();
    }

    /**
     * Displays some useful info about the account, to demonstrate
     * that we've successfully logged in
     * @param account
     */
    public void displayAccountInfo(DropboxAPI.Account account) {
    	if (account != null) {
    		String info = "Name: " + account.displayName + "\n" +
    			"User ID: " + account.uid + "\n" +
    			"Quota: " + account.quota;
    		this.dbInfo.setText(info);
    	} else {
    		this.dbInfo.setText("Not authenticated");
    	}
    }
    
    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     */
    public void storeKeys(AccessTokenPair keyPair) {
        // Save the access key for later
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Editor edit = prefs.edit();
        edit.putString("dbPrivKey", keyPair.key);
        edit.putString("dbPrivSecret", keyPair.secret);
        edit.commit();
    }
    
    public void clearKeys() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        Editor edit = prefs.edit();
        edit.remove("dbPrivKey");
        edit.remove("dbPrivSecret");
        edit.commit();
    }
    
    static class ProxyAndroidAuthSession extends AndroidAuthSession {

    	Context context = null;
    	
		public ProxyAndroidAuthSession(Context context, AppKeyPair arg0, AccessType arg1) {
			super(arg0, arg1);
			this.context = context;
		}
		public ProxyAndroidAuthSession(Context context, AppKeyPair appKeyPair,
				AccessType dropbox, AccessTokenPair accessTokenPair) {
			super(appKeyPair, dropbox, accessTokenPair);
			this.context = context;
		}
		@Override
		public synchronized ProxyInfo getProxyInfo() {
	        String proxyHost = Proxy.getHost(context);
	        int proxyPort = Proxy.getPort(context);
	        if (proxyHost != null && proxyPort>0) {
	        	return new ProxyInfo(proxyHost, proxyPort);
	        }
	        return super.getProxyInfo();
		}
    	
    }
    
    public static AndroidAuthSession createSession(Context context) {
    	Resources r = context.getResources();
    	AppKeyPair appKeyPair = new AppKeyPair(r.getString(R.string.dropbox_consumer_key, "invalid"), 
    			r.getString(R.string.dropbox_consumer_secret, "invalid"));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String key = prefs.getString("dbPrivKey", null);
        String secret = prefs.getString("dbPrivSecret", null);
        if (key != null && secret != null) {
        	return new ProxyAndroidAuthSession(context, appKeyPair, AccessType.DROPBOX, new AccessTokenPair(key, secret));
        } else {
        	return new ProxyAndroidAuthSession(context, appKeyPair, AccessType.DROPBOX);
        }
    	
    }
    
}
