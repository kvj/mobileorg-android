package com.matburt.mobileorg.ng.synchronizers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRouteParams;
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
import android.net.Proxy;
import android.util.Log;

import com.matburt.mobileorg.ng.service.DataController;

public class WebDAVSynchronizer extends Synchronizer {

	public WebDAVSynchronizer(Context parentContext, DataController controller) {
		super(parentContext, controller);
	}

	@Override
	public FileInfo fetchOrgFile(String name) throws NotFoundException,
			ReportableError {
		DefaultHttpClient httpC = this.createConnection();
		return getUrlStream(pathFromSettings() + name, httpC);
	}

	private DefaultHttpClient createConnection() {
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
				appSettings.getString("webUser", ""), appSettings.getString(
						"webPass", ""));
		BasicCredentialsProvider cProvider = new BasicCredentialsProvider();
		cProvider.setCredentials(AuthScope.ANY, bCred);

		params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE,
				false);
		httpClient.setParams(params);

		String proxyHost = Proxy.getHost(rootContext);
		int proxyPort = Proxy.getPort(rootContext);
		if (proxyHost != null && proxyPort > 0) {
			params.setParameter(ConnRouteParams.DEFAULT_PROXY, new HttpHost(
					proxyHost, proxyPort));
		}
		DefaultHttpClient nHttpClient = new DefaultHttpClient(cm, params);
		nHttpClient.setCredentialsProvider(cProvider);
		return nHttpClient;
	}

	private HttpResponse makeRequest(HttpUriRequest request,
			DefaultHttpClient httpClient) throws ClientProtocolException,
			IOException, ReportableError {
		HttpResponse res = httpClient.execute(request);
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
		return res;
	}

	private FileInfo getUrlStream(String url, DefaultHttpClient httpClient)
			throws NotFoundException, ReportableError {
		try {
			HttpEntity entity = makeRequest(new HttpGet(url), httpClient)
					.getEntity();
			long size = entity.getContentLength();
			FileInfo info = new FileInfo(new BufferedReader(
					new InputStreamReader(entity.getContent(), "utf-8"),
					BUFFER_SIZE));
			info.size = size;
			return info;
		} catch (IOException e) {
			Log.e(LT, e.toString());
			Log.w(LT, "Failed to get URL");
			throw new ReportableError("Error downloading file", e);
		}
	}

	@Override
	public String getFileHash(String name) throws ReportableError {
		try {
			DefaultHttpClient httpClient = createConnection();
			HttpHead options = new HttpHead(pathFromSettings() + name);
			HttpResponse response = makeRequest(options, httpClient);
			if (response.containsHeader("ETag")) {
				return response.getFirstHeader("ETag").getValue();
			}
			if (response.containsHeader("Last-Modified")) {
				return response.getFirstHeader("Last-Modified").getValue();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean putFile(boolean append, String fileName, String data) {
		try {
			StringBuilder content = new StringBuilder();
			if (append) {
				try {
					content.append(fetchOrgFileString(fileName));
				} catch (Throwable e) {
				}
			}
			content.append(data);
			HttpClient httpClient = createConnection();
			HttpPut httpPut = new HttpPut(pathFromSettings() + fileName);
			httpPut.setEntity(new StringEntity(content.toString(), "utf-8"));
			HttpResponse response = httpClient.execute(httpPut);
			StatusLine statResp = response.getStatusLine();
			int statCode = statResp.getStatusCode();
			if (statCode >= 400) {
				Log.e(LT, "Error uploading: " + statResp);
				return false;
			}
			httpClient.getConnectionManager().shutdown();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String getIndexPath() {
		return appSettings.getString("webUrl", "");
	}
}
