package org.kvj.bravo7;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class ApplicationContext {
	
	public class LogEntry {
		String entry;
		Date date = new Date();
		
		public LogEntry(String entry) {
			this.entry = entry;
		}
		
		public Date getDate() {
			return date;
		}
		
		public String getText() {
			return entry;
		}
	}
	
	public static final String PREF_NAME = "prefs";
	private static final String TAG = "ApplicationContext";
	
	private SharedPreferences preferences = null;
	private Context context = null;
	private Map<String, Object> registry = new HashMap<String, Object>();
	private static final int MAX_LOG = 100;
	private Queue<LogEntry> log = new LinkedList<ApplicationContext.LogEntry>();
	
	private ApplicationContext(Context ctx) {
		Log.i(TAG, "Creating new instance...: "+ctx.getClass().getName());
		context = ctx;
		preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
	}
	
	private static ApplicationContext instance = null;
	
	public static ApplicationContext getInstance() {
		return instance;
	}
	
	public static ApplicationContext getInstance(Context ctx) {
		if (instance == null) {
			instance = new ApplicationContext(ctx);
		}
		return instance;
	}
	
	public SharedPreferences getPreferences() {
		return preferences;
	}
	
	public String getStringPreference(String name, String defaultValue) {
		return preferences.getString(name, defaultValue);
	}
	
	public void setStringPreference(String name, String value) {
		preferences.edit().putString(name, value).commit();
	}
	
	public List<String> getStringArrayPreference(String name) {
		List<String> result = new ArrayList<String>();
		String ids = getStringPreference(name, "");
		String[] arr = ids.split(" ");
		for (String id : arr) {
			if (id != null && !"".equals(id)) {
				result.add(id);
			}
		}
		return result;
	}
	
	public List<String> setStringArrayPreference(String name, String id, boolean add) {
		List<String> result = getStringArrayPreference(name);
		if (id == null || "".equals(id)) {
			return result;
		}
		for (int i = 0; i < result.size(); i++) {
			if (id.equals(result.get(i))) {
				if (add) {
					return result;//Already here
				} else {
					result.remove(i);
					i--;
				}
			}
		}
		if (add) {
			result.add(id);
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < result.size(); i++) {
			if (i>0) {
				builder.append(" ");
			}
			builder.append(result.get(i));
		}
		setStringPreference(name, builder.toString());
		return result;
	}
	
	public int getIntPreference(String name, int defaultID) {
		try {
			return Integer.parseInt(preferences.getString(name, context.getString(defaultID)));
		} catch (Exception e) {
			try {
				return Integer.parseInt(context.getString(defaultID));
			} catch (Exception e2) {
			}
		}
		return -1;
	}
	
	public void setIntPreference(String name, int value) {
		preferences.edit().putString(name, Integer.toString(value)).commit();
	}

	
	
	public Context getContext() {
		return context;
	}
	
	public void setWidgetConfig(int id, JSONObject config) {
		JSONObject obj = new JSONObject();
		try {
			obj.putOpt("id", id);
			obj.putOpt("config", config);
			setStringPreference("widget_"+id, obj.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject getWidgetConfig(int id) {
		try {
			JSONObject obj = new JSONObject(getStringPreference("widget_"+id, "{}"));
			return obj.optJSONObject("config");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Map<Integer, JSONObject> getWidgetConfigs(String provider) {
		Map<Integer, JSONObject> result = new HashMap<Integer, JSONObject>();
		Set<String> keys = preferences.getAll().keySet();
		AppWidgetManager manager = AppWidgetManager.getInstance(getContext());
		for (String key : keys) {
			if (key.startsWith("widget_")) {
				try {
					JSONObject object = new JSONObject(getStringPreference(key, "{}"));
					int widgetID = object.optInt("id", -1);
					JSONObject config = object.optJSONObject("config");
					AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetID);
					if (info == null || config == null) {
						Log.w(TAG, "updateWidgets no info or config for "+widgetID);
						continue;
					}
					if (info.provider.getClassName().equals(provider)) {
						result.put(widgetID, config);
					}
				} catch (Exception e) {
					Log.e(TAG, "updateWidgets error:", e);
				}
			}
		}
		return result;
	}
	
	public void updateWidgets(int id) {
		Set<String> keys = preferences.getAll().keySet();
		List<String> toRemove = new ArrayList<String>();
		AppWidgetManager manager = AppWidgetManager.getInstance(getContext());
		for (String key : keys) {
			if (key.startsWith("widget_")) {
				try {
//					Log.i(TAG, "updateWidgets: key: "+key);
					JSONObject object = new JSONObject(getStringPreference(key, "{}"));
					int widgetID = object.optInt("id", -1);
					AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetID);
					if (info == null) {
						Log.w(TAG, "updateWidgets no info for "+widgetID);
						toRemove.add(key);
						continue;
					}
					if (id != -1 && id != widgetID) {
						Log.w(TAG, "updateWidgets not a requested ID");
						continue;
					}
					AppWidgetProvider provider = (AppWidgetProvider) getClass().getClassLoader().loadClass(info.provider.getClassName()).newInstance();
//					Log.i(TAG, "updateWidgets calling update...");
					provider.onUpdate(getContext(), manager, new int[] {widgetID});
				} catch (Exception e) {
					Log.e(TAG, "updateWidgets error:", e);
					toRemove.add(key);
				}
			}
		}
		for (String key : toRemove) {
			preferences.edit().remove(key).commit();
		}
	}
	
	public void publishBean(Object object) {
		registry.put(object.getClass().getName(), object);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> cl) {
		return (T)registry.get(cl.getName());
	}
	
	public void publishBean(String name, Object object) {
		registry.put(name, object);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, Class<T> cl) {
		return (T)registry.get(name);
	}
	
	public void log(String entry) {
		synchronized (log) {
			while (log.size()>MAX_LOG) {
				log.poll();
			}
			log.add(new LogEntry(entry));
		}
	}
	
	public Queue<LogEntry> getLog() {
		return log;
	}
}
