package com.matburt.mobileorg.synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;

import com.matburt.mobileorg.service.DataController;

public class WebDAVSynchronizer extends Synchronizer {
	private boolean pushedStageFile = false;

	public WebDAVSynchronizer(Context parentContext, DataController controller) {
		this.rootContext = parentContext;
		this.r = this.rootContext.getResources();
		this.controller = controller;
		this.appSettings = PreferenceManager
				.getDefaultSharedPreferences(parentContext
						.getApplicationContext());
	}

	@Override
	public FileInfo fetchOrgFile(String orgUrl) throws NotFoundException,
			ReportableError {
		DefaultHttpClient httpC = this.createConnection(
				this.appSettings.getString("webUser", ""),
				this.appSettings.getString("webPass", ""));
		InputStream mainFile;
		try {
			mainFile = this.getUrlStream(orgUrl, httpC);
		} catch (IllegalArgumentException e) {
			throw new ReportableError("Invalid URL", e);
		}
		if (mainFile == null) {
			return null;
		}
		return new FileInfo(new BufferedReader(new InputStreamReader(mainFile)));
	}

	private String getRootUrl() throws NotFoundException, ReportableError {
		URL manageUrl = null;
		try {
			manageUrl = new URL(this.appSettings.getString("webUrl", ""));
		} catch (MalformedURLException e) {
			throw new ReportableError("Invalid URL", e);
		}

		String urlPath = manageUrl.getPath();
		String[] pathElements = urlPath.split("/");
		String directoryActual = "/";
		if (pathElements.length > 1) {
			for (int idx = 0; idx < pathElements.length - 1; idx++) {
				if (pathElements[idx].length() > 0) {
					directoryActual += pathElements[idx] + "/";
				}
			}
		}
		return manageUrl.getProtocol() + "://" + manageUrl.getAuthority()
				+ directoryActual;
	}

	private DefaultHttpClient createConnection(String user, String password) {
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpParams params = httpClient.getParams();
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory
				.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(
				params, schemeRegistry);

		UsernamePasswordCredentials bCred = new UsernamePasswordCredentials(
				user, password);
		BasicCredentialsProvider cProvider = new BasicCredentialsProvider();
		cProvider.setCredentials(AuthScope.ANY, bCred);

		params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE,
				false);
		httpClient.setParams(params);

		DefaultHttpClient nHttpClient = new DefaultHttpClient(cm, params);
		nHttpClient.setCredentialsProvider(cProvider);
		return nHttpClient;
	}

	private InputStream getUrlStream(String url, DefaultHttpClient httpClient)
			throws NotFoundException, ReportableError {
		try {
			HttpResponse res = httpClient.execute(new HttpGet(url));
			StatusLine status = res.getStatusLine();
			if (status.getStatusCode() == 401) {
				throw new ReportableError("Invalid username or password", null);
			}
			if (status.getStatusCode() == 404) {
				return null;
			}

			if (status.getStatusCode() < 200 || status.getStatusCode() > 299) {
				throw new ReportableError("Error: " + status.getReasonPhrase(),
						null);
			}
			return res.getEntity().getContent();
		} catch (IOException e) {
			Log.e(LT, e.toString());
			Log.w(LT, "Failed to get URL");
			throw new ReportableError("Error downloading file", e);
		}
	}

	private void putUrlFile(String url, DefaultHttpClient httpClient,
			String content) throws NotFoundException, ReportableError {
		try {
			HttpPut httpPut = new HttpPut(url);
			httpPut.setEntity(new StringEntity(content, "UTF-8"));
			HttpResponse response = httpClient.execute(httpPut);
			StatusLine statResp = response.getStatusLine();
			int statCode = statResp.getStatusCode();
			if (statCode >= 400) {
				this.pushedStageFile = false;
				throw new ReportableError("Server returned code: "
						+ Integer.toString(statCode), null);
			} else {
				this.pushedStageFile = true;
			}

			httpClient.getConnectionManager().shutdown();
		} catch (UnsupportedEncodingException e) {
			throw new ReportableError("Unsupported encoding", e);
		} catch (IOException e) {
			throw new ReportableError("IO error", e);
		}
	}

	private void appendUrlFile(String url, DefaultHttpClient httpClient,
			String content) throws NotFoundException, ReportableError {
		String originalContent = this.fetchOrgFileString(url);
		String newContent = originalContent + '\n' + content;
		this.putUrlFile(url, httpClient, newContent);
	}

	private String ReadInputStream(InputStream in) throws IOException {
		StringBuffer stream = new StringBuffer();
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			stream.append(new String(b, 0, n));
		}
		return stream.toString();
	}
}
