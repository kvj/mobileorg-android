package com.matburt.mobileorg.synchronizers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.DropboxInputStream;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.matburt.mobileorg.R;
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

	public void push() throws NotFoundException, ReportableError {
		String fileActual = this.getPath() + "mobileorg.org";
		String storageMode = this.appSettings.getString("storageMode", "");
		String fileContents = "";

		BufferedReader reader = this.getReadHandle("mobileorg.org");

		if (reader == null) {
			return;
		}

		String thisLine = "";
		try {
			while ((thisLine = reader.readLine()) != null) {
				fileContents += thisLine + "\n";
			}
		} catch (java.io.IOException e) {
			throw new ReportableError(r.getString(R.string.error_file_read,
					"mobileorg.org"), e);
		}
		this.appendDropboxFile("mobileorg.org", fileContents);
	}

	public boolean checkReady() {
		// check key and secret also
		// possibly attempt to login and return false if it fails
		if (this.appSettings.getString("dropboxPath", "").equals(""))
			return false;
		return true;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.hasToken = loggedIn;
	}

	public void pull() throws NotFoundException, ReportableError {
		String indexFilePath = this.appSettings.getString("dropboxPath", "");
		// if(!indexFilePath.startsWith("/")) {
		// indexFilePath = "/" + indexFilePath;
		// }
		String masterStr = this.fetchOrgFileString(indexFilePath);
		Log.i(LT, "Contents: " + masterStr);
		if (masterStr.equals("")) {
			throw new ReportableError(r.getString(
					R.string.error_file_not_found, indexFilePath), null);
		}
		HashMap<String, String> masterList = this
				.getOrgFilesFromMaster(masterStr);
		ArrayList<HashMap<String, Boolean>> todoLists = this
				.getTodos(masterStr);
		ArrayList<ArrayList<String>> priorityLists = this
				.getPriorities(masterStr);
		controller.setTodoList(todoLists);
		controller.setPriorityList(priorityLists);
		String pathActual = this.getPath();
		// Get checksums file
		masterStr = this.fetchOrgFileString(pathActual + "checksums.dat");
		Map<String, String> newChecksums = this.getChecksums(masterStr);
		Map<String, String> oldChecksums = controller.getChecksums();

		// Get other org files
		for (String key : masterList.keySet()) {
			if (oldChecksums.containsKey(key) && newChecksums.containsKey(key)
					&& oldChecksums.get(key).equals(newChecksums.get(key)))
				continue;
			Log.d(LT,
					"Fetching: " + key + ": " + pathActual
							+ masterList.get(key));
			this.fetchAndSaveOrgFile(pathActual + masterList.get(key),
					masterList.get(key));
			controller.addOrUpdateFile(masterList.get(key), key,
					newChecksums.get(key));
		}
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

	public FileInfo fetchOrgFile(String orgPath) throws NotFoundException,
			ReportableError {
		Log.i(LT, "Downloading " + orgPath);
		DropboxInputStream fd;
		try {
			fd = api.getFileStream(getPath() + orgPath, null);
		} catch (Exception e) {
			throw new ReportableError(r.getString(R.string.dropbox_fetch_error,
					orgPath, e.toString()), null);
		}
		if (fd == null) {
			throw new ReportableError(r.getString(R.string.dropbox_fetch_error,
					orgPath, "Error downloading file"), null);
		}
		BufferedReader reader = new BufferedReader(new InputStreamReader(fd),
				20000);
		FileInfo result = new FileInfo(reader);
		result.size = fd.getFileInfo().getContentLength();
		Log.i(LT, "Finished downloading: "
				+ fd.getFileInfo().getContentLength());
		return result;
	}

	private void appendDropboxFile(String file, String content)
			throws ReportableError {
		String pathActual = this.getPath();
		String originalContent = this.fetchOrgFileString(pathActual + file);
		String newContent = "";
		if (originalContent.indexOf("{\"error\":") == -1)
			newContent = originalContent + "\n" + content;
		else
			newContent = content;
		this.removeFile("mobileorg.org");
		BufferedWriter writer = this.getWriteHandle("mobileorg.org");

		// Rewriting the mobileorg file with the contents on Dropbox is
		// dangerous
		// but the api sucks and automatically uses the File object's name when
		// figuring
		// out what to call the remote file
		try {
			writer.write(newContent);
			writer.flush();
			writer.close();
		} catch (java.io.IOException e) {
			Log.e(LT, "IO Exception trying to write file mobileorg.org");
			return;
		}

		File uploadFile = this.getFile("mobileorg.org");
		try {
			this.api.putFile(pathActual, new FileInputStream(uploadFile),
					uploadFile.length(), null, null);
		} catch (Exception e) {
			Log.e(LT, "Error uploading file:", e);
			throw new ReportableError("There was an error uploading file", e);
		}
		this.removeFile("mobileorg.org");
	}

	public File getFile(String fileName) throws ReportableError {
		String storageMode = this.appSettings.getString("storageMode", "");
		if (storageMode.equals("internal") || storageMode == null) {
			FileInputStream fs;
			File morgFile = new File("/data/data/com.matburt.mobileorg/files",
					fileName);
			return morgFile;
		} else if (storageMode.equals("sdcard")) {
			File root = Environment.getExternalStorageDirectory();
			File morgDir = new File(root, "mobileorg");
			File morgFile = new File(morgDir, fileName);
			if (!morgFile.exists()) {
				Log.i(LT, "Did not find " + fileName + " file, not pushing.");
				return null;
			}
			return morgFile;
		} else {
			throw new ReportableError(r.getString(
					R.string.error_local_storage_method_unknown, storageMode),
					null);
		}
	}

	public void showToast(String msg) {
		Toast error = Toast.makeText(this.rootContext, msg, Toast.LENGTH_LONG);
		error.show();
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
