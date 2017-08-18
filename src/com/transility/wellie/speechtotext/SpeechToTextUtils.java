package com.transility.wellie.speechtotext;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.widget.Toast;

/**
 * This Class Recognizes Speech and returns it in text format. It uses Android's
 * text to speech Engine internally and in case of error it returns the error
 * codes. It should be initialized before use so that it will give callback
 * after TTS initialized.
 * 
 * @author shridutt.kothari
 * 
 */
public class SpeechToTextUtils {
	/* Log tag. */
	private static final String TAG = SpeechToTextUtils.class.getName();

	/* Action to be used for using this activity. */
	public static final String ACTION_RECOGNIZE_SPEECH = "action_recognize_speech";

	/*
	 * Result codes to get the result data from the intent this activity
	 * returns, these are bounded with result types this activity returns.
	 */
	public static final String ERROR_CODE = "ERROR_CODE";
	public static final String RESULTS = "RESULT";

	/*
	 * Key used to specify Maximum no of results to be returned, can be
	 * specified in Intent which starts this activity, default value is 1
	 */
	public static final String EXTRA_MAX_RESULTS = RecognizerIntent.EXTRA_MAX_RESULTS;
	/*
	 * Key used to specify Speech timout interval, can be specified in Intent
	 * which starts this activity, default value is 10000 milli sec
	 */
	public static final String SPEECH_TIMOUT_INTERVAL = "SPEECH_TIMOUT_INTERVAL";

	/*
	 * Result types which will be returened in the response to the caller of
	 * SpeeshToTextActivity
	 */
	public static final int RESULT_TYPE_ERROR = 0;
	public static final int RESULT_TYPE_FOUND = 1;
	public static final int RESULT_TYPE_NOT_FOUND = 2;
	public static final int RESULT_TYPE_CANCEL = 3;
	public static final int RESULT_TYPE_END_OF_SPEECH = 4;

	/* Maximum no of spech to text results to returned by SpeeshToTextActivity */
	private int MAX_SPEECH_TO_TEXT_RESULTS;
	/*
	 * speech timout after which voice recognition, this class will be stopped forcefully
	 */
	private int speechTimout = 10000;
	boolean isPaused = false;
	private Activity mContext;
	
	/* Android Speech Recognizer used for speech recognition */
	private SpeechRecognizer speechRecognizer;
	/* Android speech Recognizer used to get call back from Speech Recognizer */
	private VoiceRecognitionListener voiceRecognitionListener;

	private SpeechToTextCallback mSpeechToTextCallback;

	private int voiceRecognitionId;

	/* Speech States of the voice recognizer */
	private static enum SpeechState {
		speechNotStarted, speechStarted, speechEnded
	}

	/* Current speech state, default value is SpeechState.speechNotStartedYet */
	private static SpeechState currentSpeechState = SpeechState.speechNotStarted;
	
	
	/* Network operation timed out. */
	public static final int ERROR_NETWORK_TIMEOUT = SpeechRecognizer.ERROR_NETWORK_TIMEOUT;
	/* Other network related errors. */
	public static final int ERROR_NETWORK = SpeechRecognizer.ERROR_NETWORK;
	/* Audio recording error. */
	public static final int ERROR_AUDIO = SpeechRecognizer.ERROR_AUDIO;
	/* Server sends error status. */
	public static final int ERROR_SERVER = SpeechRecognizer.ERROR_SERVER;
	/* Other client side errors. */
	public static final int ERROR_CLIENT = SpeechRecognizer.ERROR_CLIENT;
	/* No speech input. */
	public static final int ERROR_SPEECH_TIMEOUT = SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
	/* No recognition result matched. */
	public static final int ERROR_NO_MATCH = SpeechRecognizer.ERROR_NO_MATCH;
	/* RecognitionService busy. */
	public static final int ERROR_RECOGNIZER_BUSY = SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
	/* Insufficient permissions. */
	public static final int ERROR_INSUFFICIENT_PERMISSIONS = SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
	/* Handler to handle the speech timout scenario */
	private static Handler speechTimoutHandler = new Handler();

	
	public SpeechToTextUtils(Activity activityContext, SpeechToTextCallback speechToTextCallback) {

		Log.v(TAG, "onCreate");
		this.mContext = activityContext;
		this.mSpeechToTextCallback = speechToTextCallback;
		initSpeechRecognizer();
		setMaxSpeechToTextResultsNo(5);
		setSpeechTimout(10000);
		//startListening();

	}

	/**
	 * lazy initialize the speech recognizer
	 */
	private void initSpeechRecognizer() {

		if (null == speechRecognizer) {
			speechRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
			if (null == voiceRecognitionListener) {
				voiceRecognitionListener = new VoiceRecognitionListener();
			}
			speechRecognizer.setRecognitionListener(voiceRecognitionListener);
		}
	}

	/**
	 * set the maximum no of speech recognition results to be returned.
	 */
	private void setMaxSpeechToTextResultsNo(int maxSpeechToTextResults) {

		this.MAX_SPEECH_TO_TEXT_RESULTS = maxSpeechToTextResults;
	}

	/**
	 * set the timeout in which user must speak otherwise recognizer can be
	 * closed.
	 */
	private void setSpeechTimout(int speechTimout) {

		this.speechTimout = speechTimout;
	}

	/**
	 * Start to listen Android Speech Recognizer i.e. bound our listener with
	 * SpeechRecognizer.
	 */
	public void startListening(int voiceRecognitionId) {
		this.voiceRecognitionId = voiceRecognitionId;
		Toast.makeText(mContext.getApplicationContext(), "Started Listening",
				Toast.LENGTH_SHORT);
		Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,	mContext.getPackageName());
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_SPEECH_TO_TEXT_RESULTS);

		// Start Listening
		speechRecognizer.startListening(intent);
		Log.i("startListening", "started Listening");

		// Start Timeout Handler.
		handleSpeechTimout();
	}

	/**
	 * Start a post delayed handler with specific timeout to make timeout in
	 * case user don't speak. To change the timeout change the speechTimout.
	 */
	private void handleSpeechTimout() {
		Log.d(TAG, "handleSpeechTimout()");
		speechTimoutHandler.postDelayed(new Runnable() {
			public void run() {
				// Task to be done after specified delay of milliseconds
				if (currentSpeechState == SpeechState.speechNotStarted) {
					speechRecognizer.cancel();
					Log.d(TAG, "Speech Timout!");
					mSpeechToTextCallback.callback(RESULT_TYPE_ERROR, ERROR_SPEECH_TIMEOUT, null);
				}
			}
		}, speechTimout);
	}

	/**
	 * This Class listens for voice recognize and provides the callback through
	 * Result codes and Intent.
	 * 
	 * @author shridutt.kothari
	 * 
	 */
	private class VoiceRecognitionListener implements RecognitionListener {

		/**
		 * Called when the end pointer is ready for the user to start speaking.
		 * 
		 * @param params
		 *            parameters set by the recognition service. Reserved for
		 *            future use.
		 */
		public void onReadyForSpeech(Bundle params) {
			Log.d(TAG, "ReadyForSpeech");
		}

		/**
		 * The user has started to speak.
		 */
		public void onBeginningOfSpeech() {
			Log.d(TAG, "BeginningOfSpeech");
			currentSpeechState = SpeechState.speechStarted;
		}

		/**
		 * The sound level in the audio stream has changed. There is no
		 * guarantee that this method will be called.
		 * 
		 * @param rmsdB
		 *            the new RMS dB value
		 */
		public void onRmsChanged(float rmsdB) {
			// Log.d(TAG +"onRmsChanged",""+rmsdB);
		}

		/**
		 * More sound has been received. The purpose of this function is to
		 * allow giving feedback to the user regarding the captured audio. There
		 * is no guarantee that this method will be called.
		 * 
		 * @param buffer
		 *            a buffer containing a sequence of big-endian 16-bit
		 *            integers representing a single channel audio stream. The
		 *            sample rate is implementation dependent.
		 */
		public void onBufferReceived(byte[] buffer) {
			Log.d(TAG, "BufferReceived");
		}

		/**
		 * Called after the user stops speaking.
		 */
		public void onEndOfSpeech() {
			Log.d(TAG, "EndofSpeech");
			currentSpeechState = SpeechState.speechEnded;
			SpeechToTextUtils.this.mSpeechToTextCallback.callback(RESULT_TYPE_END_OF_SPEECH, -1, null);
		}

		/**
		 * Called when a network or recognition error occurred in the voice
		 * recognizer. Errors returned are: 1. ERROR_NETWORK_TIMEOUT 2.
		 * ERROR_NETWORK 3. ERROR_AUDIO 4. ERROR_SERVER 5. ERROR_CLIENT 6.
		 * ERROR_SPEECH_TIMEOUT 7. ERROR_NO_MATCH 8. ERROR_RECOGNIZER_BUSY 9.
		 * ERROR_INSUFFICIENT_PERMISSIONS
		 */
		public void onError(int error) {

			Log.d(TAG, "Error " + error);
			SpeechToTextUtils.this.mSpeechToTextCallback.callback(RESULT_TYPE_ERROR, error, null);
			
		}

		/**
		 * Called when recognition results are ready.
		 * 
		 * @param results
		 *            the recognition results. To retrieve the results in
		 *            {@code ArrayList<String>} format use
		 *            {@link Bundle#getStringArrayList(String)} with
		 *            {@link SpeechRecognizer#RESULTS_RECOGNITION} as a
		 *            parameter. A float array of confidence values might also
		 *            be given in {@link SpeechRecognizer#CONFIDENCE_SCORES}.
		 */
		public void onResults(Bundle results) {

			Log.d(TAG, "onResults " + results);
			ArrayList<String> resultData = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
			if (null != resultData) {
				SpeechToTextUtils.this.mSpeechToTextCallback.callback(RESULT_TYPE_FOUND, SpeechToTextUtils.this.voiceRecognitionId, resultData);
			} else {
				Log.d(TAG, "No match found!");
				SpeechToTextUtils.this.mSpeechToTextCallback.callback(RESULT_TYPE_NOT_FOUND, -1, resultData);
			}
		}

		/**
		 * Called when partial recognition results are available. The callback
		 * might be called at any time between {@link #onBeginningOfSpeech()}
		 * and {@link #onResults(Bundle)} when partial results are ready. This
		 * method may be called zero, one or multiple times for each call to
		 * {@link SpeechRecognizer#startListening(Intent)}, depending on the
		 * speech recognition service implementation. To request partial
		 * results, use {@link RecognizerIntent#EXTRA_PARTIAL_RESULTS}
		 * 
		 * @param partialResults
		 *            the returned results. To retrieve the results in
		 *            ArrayList&lt;String&gt; format use
		 *            {@link Bundle#getStringArrayList(String)} with
		 *            {@link SpeechRecognizer#RESULTS_RECOGNITION} as a
		 *            parameter
		 */
		public void onPartialResults(Bundle partialResults) {
			Log.d(TAG, "PartialResults");
		}

		/**
		 * Reserved for adding future events.
		 * 
		 * @param eventType
		 *            the type of the occurred event
		 * @param params
		 *            a Bundle containing the passed parameters
		 */
		public void onEvent(int eventType, Bundle params) {
			Log.d(TAG, "onEvent " + eventType);
		}
	}

	public interface SpeechToTextCallback {
		
		/**
		 * Set the result that this class will return to its caller, it does not
		 * close the speech recognizer, we need to call close method seperatly.
		 * 
		 * @param resultType
		 *            : resultType to be send to the caller
		 * @param resultCode
		 *            : resultCode to be send to the caller
		 * 
		 * @param resultData
		 *            : list of result data to be send to the caller
		 */
		public void callback(int resultType, int resultCode, ArrayList<String> resultData);


		
	}

	/**
	 * Destroy the speechRecognizer.
	 */
	public void close() {

		if (null != speechRecognizer && SpeechRecognizer.isRecognitionAvailable(mContext)) {
			speechRecognizer.destroy();
			Log.i("close", "speechRecognizer destroyed!");
		}
	}
	
}