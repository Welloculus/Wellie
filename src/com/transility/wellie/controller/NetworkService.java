package com.transility.wellie.controller;

import java.io.Serializable;
import java.util.HashMap;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

/**
 * This class is responsible for network operation and notifying to various
 * receivers.</Br> call forkNetworService method to invoke the service with
 * given parameters.
 * 
 * @author shridutt.kothari
 * 
 */
public class NetworkService extends IntentService {
	static String TAG = "NetworkService";

	public static final String RECEIVER = "receiver";
	public static final String NETWORK_SERVICE_RESPONSE = "response";
	public static final String NETWORK_SERVICE_EXCEPTION = "exception";
	public static final String BASE_SERVER_URL = "BASE_SERVER_URL";
	public static final String REST_SERVICE_NAME = "REST_SERVICE_NAME";
	public static final String RESULT_CODE = "result_code";
	public static final String QUERY_PARAM_STRING = "QUERY_PARAM_STRING";
	public static final String USE_GLOBAL_SERVER = "USE_GLOBAL_SERVER";
	public static final String NO_RESTFUL = "NO_RESTFUL";
	public static final String METHOD_TYPE = "METHOD_TYPE";
	public static final String GET_METHOD = "GET";
	public static final String POST_METHOD = "POST";

	public static final String USERNAME = "USERNAME";
	public static final String PASSWORD = "PASSWORD";
    public static final String AUTH_TYPE = "AUTH_TYPE";
    public static final String AUTH_TYPE_BASIC = "AUTH_TYPE_BASIC";


	public static final String POST_DATA = "POST_DATA";

	public NetworkService() {
		super(TAG);
	}

	public NetworkService(String name) {
		super(name);

	}

	/**
	 * Forks the Network Service with given parameters.
	 *  @param baseServerUrl
	 *            : ServerUrl.
	 * @param restService
	 *            : Rest service name.
     * @param notResful
 *            : Not a restful call
     * @param queryParam
*            : Query parameters for the given rest service.
     * @param methodType
*            : HTTP method type (ex. GET, POST)
     * @param postData
*            : payload data of request if request is of type POST
     * @param networResultReceiver
*            : Result receiver to get asynchronous callback
     * @param appContext
     *
     * @param authType*
     * @param userName
     * @param password
     *
     */
	public static void forkNetworService(String baseServerUrl, String restService, boolean notResful, HashMap<String, String> queryParam, int resultCode, String methodType, String postData, NetworkResultReceiver networResultReceiver, Context appContext,
                                         String authType, String userName, String password) {
		Intent networkServiceIntent = new Intent(appContext, NetworkService.class);
		networkServiceIntent.putExtra(NetworkService.RECEIVER,networResultReceiver);
		networkServiceIntent.putExtra(NetworkService.BASE_SERVER_URL, baseServerUrl);
		networkServiceIntent.putExtra(NetworkService.REST_SERVICE_NAME,restService);
		networkServiceIntent.putExtra(NO_RESTFUL, notResful);
		networkServiceIntent.putExtra(NetworkService.METHOD_TYPE, methodType);

        networkServiceIntent.putExtra(AUTH_TYPE, authType);
        networkServiceIntent.putExtra(USERNAME, userName);
        networkServiceIntent.putExtra(PASSWORD, password);

		if (null != postData) {
			networkServiceIntent.putExtra(NetworkService.POST_DATA, postData);
		}
		networkServiceIntent.putExtra(NetworkService.RESULT_CODE, resultCode);
		if (null != queryParam) {
			networkServiceIntent.putExtra(NetworkService.QUERY_PARAM_STRING,
					queryParam);
		}
		appContext.startService(networkServiceIntent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		StringBuilder baseServerUrl = new StringBuilder(
				intent.getStringExtra(BASE_SERVER_URL));
		String restServiceName = intent.getStringExtra(REST_SERVICE_NAME);
		String methodType = intent.getStringExtra(METHOD_TYPE);
		boolean notRestful = intent.getBooleanExtra(NO_RESTFUL, true);
		String userName = intent.getStringExtra(USERNAME);
		String password = intent.getStringExtra(PASSWORD);
        String authType = intent.getStringExtra(AUTH_TYPE);


		@SuppressWarnings("unchecked")
		HashMap<String, String> queryParamString = (HashMap<String, String>) intent
				.getSerializableExtra(QUERY_PARAM_STRING);
		int resultCode = intent.getIntExtra(RESULT_CODE, 0);
		ResultReceiver resultReceiver = intent.getParcelableExtra(RECEIVER);
		Bundle resultData = new Bundle();
		String result = null;
		try {
			HttpCommunicator httpUtil = new HttpCommunicator(baseServerUrl,
					restServiceName);
			if (methodType.equalsIgnoreCase(POST_METHOD)) {
				String postContent = intent.getStringExtra(POST_DATA);
				result = httpUtil.doPost(queryParamString, postContent, notRestful, authType, userName, password);
			} else if (methodType.equalsIgnoreCase(GET_METHOD)) {
				result = httpUtil.doGet(queryParamString, notRestful, authType, userName, password);
			}
			resultData.putString(NETWORK_SERVICE_RESPONSE, result);
			resultReceiver.send(resultCode, resultData);
		} catch (Exception exception) {
			exception.printStackTrace();
			resultData.putString(NETWORK_SERVICE_RESPONSE, result);
			resultData.putSerializable(NETWORK_SERVICE_EXCEPTION,
					(Serializable) exception);
			resultReceiver.send(resultCode, resultData);
		}
	}
}
