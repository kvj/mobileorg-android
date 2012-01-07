package com.matburt.mobileorg.synchronizers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.matburt.mobileorg.service.DataController;

public class DropboxSynchronizer extends Synchronizer {
	private boolean hasToken = false;

	private AndroidAuthSession session = null;
	private DropboxAPI<AndroidAuthSession> api = null;

	public DropboxSynchronizer(Context parentContext, DataController controller) {
		session = DropboxAuthActivity.createSession(parentContext);
		api = new DropboxAPI<AndroidAuthSession>(session);
		this.rootContext = parentContext;
		this.controller = controller;
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(parentContext
						.getApplicationContext());
		r = parentContext.getResources();
	}

	private String getPath() {
		String _indexPath = appSettings.getString("dropboxPath", "");
		if (_indexPath.indexOf("/") != -1) {
			_indexPath = _indexPath.substring(0,
					_indexPath.lastIndexOf("/") + 1);
		}
		if (_indexPath.startsWith("/")) {
			_indexPath = _indexPath.substring(1);
		}
		return "/" + _indexPath;
	}

	@Override
	public FileInfo fetchOrgFile(String orgPath) throws NotFoundException,
			ReportableError {
		Log.i(LT, "Downloading " + orgPath);
		DropboxInputStream fd;
		try {
			fd = api.getFileStream(getPath() + orgPath, null);
		} catch (Exception e) {
			throw new ReportableError("Error downloading file", null);
		}
		if (fd == null) {
			throw new ReportableError("Error downloading file", null);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(fd),
				20000);
		FileInfo result = new FileInfo(reader);
		result.size = fd.getFileInfo().getContentLength();
		Log.i(LT, "Finished downloading: "
				+ fd.getFileInfo().getContentLength());
		return result;
	}

	/**
	 * Shows keeping the access keys returned from Trusted Authenticator in a
	 * local store, rather than storing user name & password, and
	 * re-authenticating each time (which is not to be done, ever).
	 * 
	 * @return Array of [access_key, access_secret], or null if none stored
	 */
	public String[] getKeys() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this.rootContext
						.getApplicationContext());
		String key = prefs.getString("dbPrivKey", null);
		String secret = prefs.getString("dbPrivSecret", null);
		if (key != null && secret != null) {
			String[] ret = new String[2];
			ret[0] = key;
			ret[1] = secret;
			return ret;
		} else {
			return null;
		}
	}

	@Override
	public String getFileHash(String name) throws ReportableError {
		try {
			Entry entry = api.metadata(getPath() + name, 1, null, false, null);
			return entry.rev;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ReportableError("Error getting last rev", e);
		}
	}

	@Override
	public boolean putFile(boolean append, String fileName, String data) {
		ByteArrayOutputStream fileData = new ByteArrayOutputStream();
		if (append) {
			// Read file first
			try {
				BufferedReader br = fetchOrgFile(fileName).reader;
				String line = null;
				while (null != (line = br.readLine())) {
					fileData.write(line.getBytes("utf-8"));
					fileData.write('\n');
				}
				br.close();
			} catch (Throwable e) {
			}
		}
		try {
			fileData.write(data.getBytes("utf-8"));
			fileData.close();
			long size = fileData.size();
			ByteArrayInputStream stream = new ByteArrayInputStream(
					fileData.toByteArray());
			api.putFileOverwrite(getPath() + fileName, stream, size, null);
			stream.close();
			return true;
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return false;
	}
}
