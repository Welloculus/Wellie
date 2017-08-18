package com.transility.wellie.controller;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * This class will be used to register the receiver for network result
 * 
 * @author shridutt.kothari
 * 
 */
public class NetworkResultReceiver extends ResultReceiver {

	private Receiver mReceiver;

	public NetworkResultReceiver(Handler handler) {
		super(handler);
	}

	/**
	 * Callback Interface for getting callback from NetworkService
	 * 
	 * @author shridutt.kothari
	 * 
	 */
	public interface Receiver {
		/**
		 * Callback method for getting call on receiving response from
		 * SecureAndroid server.
		 * 
		 * @param resultCode
		 * @param resultData
		 */
		public void onReceiveResult(int resultCode, Bundle resultData);
	}

	/**
	 * 
	 * @param receiver
	 */

	public void setReceiver(Receiver receiver) {
		mReceiver = receiver;
	}

	@Override
	protected void onReceiveResult(int resultCode, Bundle resultData) {
		if (null != mReceiver) {
			mReceiver.onReceiveResult(resultCode, resultData);
		}
	}

}