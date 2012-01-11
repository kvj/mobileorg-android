package com.matburt.mobileorg.ng.synchronizers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.Resources.NotFoundException;

import com.matburt.mobileorg.ng.service.DataController;

public class SDCardSynchronizer extends Synchronizer {

	public SDCardSynchronizer(Context parentContext, DataController controller) {
		super(parentContext, controller);
	}

	@Override
	public FileInfo fetchOrgFile(String orgPath) throws NotFoundException,
			ReportableError {
		try {
			File file = new File(pathFromSettings() + orgPath);
			FileInfo info = new FileInfo(new BufferedReader(
					new InputStreamReader(new FileInputStream(file), "utf-8"),
					BUFFER_SIZE));
			info.size = file.length();
			return info;
		} catch (Exception e) {
			throw new NotFoundException("Not found: " + orgPath);
		}
	}

	@Override
	public String getFileHash(String name) throws ReportableError {
		try {
			File file = new File(pathFromSettings() + name);
			if (file.exists()) {
				return Long.toString(file.lastModified());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean putFile(boolean append, String fileName, String data) {
		try {
			File file = new File(pathFromSettings() + fileName);
			FileOutputStream stream = new FileOutputStream(file, append);
			stream.write(data.getBytes("utf-8"));
			stream.flush();
			stream.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String getIndexPath() {
		return appSettings.getString("indexFilePath", "");
	}
}