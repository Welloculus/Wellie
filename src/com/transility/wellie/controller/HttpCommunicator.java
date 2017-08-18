package com.transility.wellie.controller;

/**
 * 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

/**
 * This class can be used to connect to restful web service.
 * 
 * @author shridutt.kothari
 */
public class HttpCommunicator {

	private String service = "";
	private String contentTypePost = "application/json";
	private String acceptType = "application/json";
	private String encodingType = "UTF-8";
	private HttpClient httpclient;
	private int responseCode = -1;
	private StringBuffer result;
	@SuppressWarnings("unused")
	private String TAG = "HttpCommunicator";
	private StringBuilder baseServerUrl;
	private static final int CONNECTION_TIMEOUT = 180;

	public HttpCommunicator(StringBuilder baseServerUrl, String service) {
		this.service = service;
		this.baseServerUrl = baseServerUrl;
		httpclient = new DefaultHttpClient();
		setTimout(CONNECTION_TIMEOUT);
	}

	public HttpCommunicator() {
		httpclient = new DefaultHttpClient();
		setTimout(CONNECTION_TIMEOUT);
	}

	private void setTimout(int timout) {
		HttpParams httpParams = httpclient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, timout);
		ConnManagerParams.setTimeout(httpParams, timout);
	}

	public String doPost(HashMap<String, String> queryParam,
			String postContent, boolean isHttp, String authType, String userName, String password) throws IOException {
		return doService(queryParam, NetworkService.POST_METHOD, postContent,
				isHttp, authType, userName, password);
	}

	public String doGet(HashMap<String, String> queryParam, boolean isHttp, String authType, String userName, String password)
			throws IOException {
		return doService(queryParam, NetworkService.GET_METHOD, null, isHttp, authType, userName, password);
	}

	private String doService(HashMap<String, String> queryParam, String method,
			String postContent, boolean isHTTP, String authType, String userName, String password) throws IOException {
		try {
			ArrayList<String> params = new ArrayList<String>();

			if (queryParam != null && queryParam.size() > 0) {
				Set<String> keys = queryParam.keySet();
				for (String key : keys) {
					String value = queryParam.get(key);
					params.add(key + "=" + value);
				}
			}
			StringBuilder queryBuilder = new StringBuilder("");
			Iterator<String> iterator = params.iterator();
			while (iterator.hasNext()) {
				queryBuilder.append(iterator.next());
				queryBuilder.append("&");
			}
			String query = "";
			if (queryBuilder.toString().endsWith("&")) {
				query += (queryBuilder.toString().substring(0, queryBuilder
						.toString().length() - 1));
			}

			if (null != service) {
				baseServerUrl.append(service);
			}
			if ("" != query) {
				baseServerUrl.append("?" + query);
			}
			//Log.i(TAG, baseServerUrl.toString());
			/*if (isHTTP) {
				return getUrlContents(baseServerUrl.toString());
			}*/
			HttpResponse response = null;
			if (method.equals(NetworkService.GET_METHOD)) {
				HttpGet httpget = new HttpGet(baseServerUrl.toString());
				httpget.setHeader("Content-Type", contentTypePost);
				httpget.setHeader("Accept", acceptType);
				//Log.i(TAG, "Executing GET request " + httpget.getRequestLine());
				response = httpclient.execute(httpget);
				responseCode = response.getStatusLine().getStatusCode();
			}

			if (method.equals(NetworkService.POST_METHOD)) {
				HttpPost httpPost = new HttpPost(baseServerUrl.toString());
				httpPost.setHeader("Content-Type", contentTypePost);
				httpPost.setHeader("Accept", acceptType);
				if (null != postContent) {
                    if(null != authType && NetworkService.AUTH_TYPE_BASIC.equalsIgnoreCase(authType)) {
                        httpPost.addHeader(BasicScheme.authenticate(new UsernamePasswordCredentials(userName, password), encodingType, false));
                    }
					httpPost.setEntity(new StringEntity(postContent));
				}
				//Log.i(TAG,"Executing POST request " + httpPost.getRequestLine());
				response = httpclient.execute(httpPost);
				responseCode = response.getStatusLine().getStatusCode();
			}

			if (responseCode != HttpStatus.SC_OK)
				return null;
			BufferedReader buffReader = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent()));
			String line = "";
			result = new StringBuffer();
			while (null != (line = buffReader.readLine())) {
				result.append(line);
			}
			return result.toString();
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
	}

	/**
	 * @return the responseCode
	 */
	public int getResponseCode() {
		return responseCode;
	}

	/**
	 * @param service
	 *            the service to set
	 */
	public void setService(String service) {
		this.service = service;
	}

	/**
	 * @param acceptType
	 *            the acceptType to set
	 */
	public void setAcceptType(String acceptType) {
		this.acceptType = acceptType;
	}

	public String getResult() {
		return (null != result) ? result.toString() : null;
	}

	private String getUrlContents(String theUrl) throws IOException {
		StringBuilder content = new StringBuilder();
		URL url = new URL(theUrl);
		URLConnection urlConnection = url.openConnection();
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(urlConnection.getInputStream()), 8);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			content.append(line + "\n");
		}
		bufferedReader.close();

		return content.toString();
	}

}
