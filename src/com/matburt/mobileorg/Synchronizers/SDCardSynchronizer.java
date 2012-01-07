package com.matburt.mobileorg.synchronizers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matburt.mobileorg.service.DataController;

public class SDCardSynchronizer extends Synchronizer {

	public SDCardSynchronizer(Context parentContext, DataController controller) {
		this.rootContext = parentContext;
		this.r = this.rootContext.getResources();
		this.controller = controller;
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(parentContext
						.getApplicationContext());
	}

	private void putFile(String path, String content) throws NotFoundException,
			ReportableError {
		Log.d(LT, "Writing to mobileorg.org file at: " + path);
		BufferedWriter fWriter;
		try {
			File fMobileOrgFile = new File(path);
			FileWriter orgFWriter = new FileWriter(fMobileOrgFile, true);
			fWriter = new BufferedWriter(orgFWriter);
			fWriter.write(content);
			fWriter.flush();
			fWriter.close();
		} catch (java.io.IOException e) {
			throw new ReportableError("Error writing file", e);

		}
	}

	private String readFile(String filePath) throws ReportableError,
			java.io.FileNotFoundException {
		FileInputStream readerIS;
		BufferedReader fReader;
		File inpfile = new File(filePath);
		try {
			readerIS = new FileInputStream(inpfile);
			fReader = new BufferedReader(new InputStreamReader(readerIS));
		} catch (java.io.FileNotFoundException e) {
			Log.d(LT, "Could not locate file " + filePath);
			throw e;
		}
		String fileBuffer = "";
		String fileLine = "";
		try {
			while ((fileLine = fReader.readLine()) != null) {
				fileBuffer += fileLine + "\n";
			}
		} catch (java.io.IOException e) {
			throw new ReportableError("Error reading file", e);
		}
		return fileBuffer;
	}
}