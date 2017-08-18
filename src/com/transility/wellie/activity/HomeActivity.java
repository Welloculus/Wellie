package com.transility.wellie.activity;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import com.transility.wellie.R;
import com.transility.wellie.controller.NetworkResultReceiver;
import com.transility.wellie.controller.NetworkService;
import com.transility.wellie.face.ui.IdentificationActivity;
import com.transility.wellie.speechtotext.SpeechToTextUtils;
import com.transility.wellie.speechtotext.SpeechToTextUtils.SpeechToTextCallback;
import com.transility.wellie.texttospeech.OnTextToSpeechCompletedListener;
import com.transility.wellie.texttospeech.TextToSpeechUtils;
import com.transility.wellie.uicomponents.CustomTextView;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * This is the controller activity of the Wellie POC application. It
 * communicates with SpeechToTextUtils and TextToSpeechActivity using various
 * VOICE_RECOGNITION and UTTERANCE ids respectively.
 * 
 * On completion of use case of determining the user spoken data, It shows the
 * result on the screen.
 * 
 * @author shridutt.kothari
 * 
 */
public class HomeActivity extends BaseActivity implements
		OnTextToSpeechCompletedListener, SpeechToTextCallback, NetworkResultReceiver.Receiver {

	private static final String TAG = HomeActivity.class.getName();
	/* Ids used for communication with speech to text activity */
	private static final int VOICE_RECOGNITION_ID = 10001;
	private static final int RESULT_QUESTION_1 = 10002;

	/* Ids used for communication with text to speech activity */
	private static final String UTTERANCE_ID_1 = "UTTERANCE_ID_1";
	private static final String UTTERANCE_ID_ERROR = "error";
	private static final String UTTERANCE_ID_CALLBACK = "give_callback";
    private static final String BASE_SERVER_URL = "http://192.168.146.54:8181/ApiGateway/wellie/";
    private static final String REST_ENDPOINT_QUERY = "query";
    private static final String MICROPFONE_ACTIVE = "file:///android_asset/microphone_active.gif";
    private static final String MICROPFONE_IDLE = "file:///android_asset/microphone_idle.gif";
    private static final String MICROPFONE_WAIT = "file:///android_asset/microphone_wait.gif";
    private static final int WELLIE_STATE_IDLE_1 = 1;
    private static final int WELLIE_STATE_IDLE_2 = 2;
    private static final int WELLIE_STATE_IDLE_3 = 3;
    private static final int WELLIE_STATE_IDLE_4 = 4;
    private static final int WELLIE_STATE_LISTENING = 5;
    private static final int WELLIE_STATE_SEARCHING = 6;
    private static final int WELLIE_STATE_RESULT_SUCCESS = 7;
    private static final int WELLIE_STATE_RESULT_ERROR = 8;
    private static final int WELLIE_STATE_RESULT_DIDNT_UNDERSTAND = 9;
    private static final long TEXT_DELAY = 50;


    private TextToSpeechUtils textToSpeechUtils;
	private CustomTextView customTextView;
    private CustomTextView textView;
	private ImageView wellieCharacterImageView;
    private NetworkResultReceiver networResultReceiver;
    private WebView webView;
    private static int currentState;
	private boolean deviceIMEI;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_new);
		customTextView = (CustomTextView) findViewById(R.id.customTextView);
		customTextView.setEnabled(false);
        textView = (CustomTextView) findViewById(R.id.textView);
        textView.setEnabled(false);
		wellieCharacterImageView = (ImageView) findViewById(R.id.imageview_wellie_character);
		wellieCharacterImageView.setEnabled(true);
        webView = (WebView) findViewById(R.id.webview_mic);
        webView.setVisibility(View.GONE);

        setState(WELLIE_STATE_IDLE_1);
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);

		wellieCharacterImageView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//identification();
                startListening();
			}
		});
		textToSpeechUtils = new TextToSpeechUtils(getApplicationContext());
		textToSpeechUtils.setOnTextToSpeechCompletedListener(this);
	}

	private void startListening(){
        SpeechToTextUtils speechToTextUtils = new SpeechToTextUtils(
                HomeActivity.this, HomeActivity.this);
        speechToTextUtils.startListening(VOICE_RECOGNITION_ID);
        setState(WELLIE_STATE_LISTENING);

    }
	public void identification() {
		Intent intent = new Intent(this, IdentificationActivity.class);
		startActivity(intent);
	}

	@Override
	protected void onPause() {
		/**
		 * if app is in gone in background super will send notification to ivi
		 * status bar and finish.
		 * 
		 */
		super.onPause();
	}

	/**
	 * Handle error flow according to the error.
	 * 
	 * @param errorCode
	 *            : error code returned from SpeechToText component
	 * @param requestCode
	 *            : request code returned from SpeechToText component used for
	 *            further flow
	 */
	private void handleError(int errorCode, int requestCode) {
        setState(WELLIE_STATE_RESULT_ERROR);
		Log.e(TAG, "Speech To Text Recognizer Error!");
		switch (errorCode) {
		case SpeechToTextUtils.ERROR_NETWORK_TIMEOUT:
			Log.e(TAG, "Network timeout!");
			showToast(getString(R.string.network_timout_toast));
			speakError(getString(R.string.network_timout), requestCode);
			break;
		case SpeechToTextUtils.ERROR_NETWORK:
			Log.e(TAG, "Error in network!");

			break;
		case SpeechToTextUtils.ERROR_AUDIO:
			Log.e(TAG, "Error in audio!");
			showToast(getString(R.string.audio_error_toast));
			speakError(getString(R.string.audio_error), requestCode);
			break;
		case SpeechToTextUtils.ERROR_SERVER:
			Log.e(TAG, "Server error!");
			showToast(getString(R.string.server_error_toast));

			break;
		case SpeechToTextUtils.ERROR_CLIENT:
			Log.e(TAG, "Client error!");
			showToast(getString(R.string.client_error_toast));
			break;
		case SpeechToTextUtils.ERROR_SPEECH_TIMEOUT:
			Log.e(TAG, "User din't speak or took too much time!");
			showToast(getString(R.string.speech_timout_toast));
			speakError(getString(R.string.speech_timout), requestCode);
			break;
		case SpeechToTextUtils.ERROR_NO_MATCH:
			Log.e(TAG, "No recognition result matched!");
			showToast(getString(R.string.no_match_toast));
			speakError(getString(R.string.no_match), requestCode);
			break;
		case SpeechToTextUtils.ERROR_RECOGNIZER_BUSY:
			Log.e(TAG, "Recognition busy!");
			showToast(getString(R.string.server_error_toast));
			// TODO
			break;
		case SpeechToTextUtils.ERROR_INSUFFICIENT_PERMISSIONS:
			Log.e(TAG,
					"Please provide permission <android.permission.RECORD_AUDIO>!");
			showToast(getString(R.string.no_audio_permission_toast));
			speakError(getString(R.string.no_audio_permission), requestCode);
			break;
		}
	}

	/**
	 * Process the speech to text results.
	 * 
	 * @param requestCode
	 *            : request code to determine the response validation.
	 * @param resultData
	 *            : response from speech to text recognizer to be processed
	 *            further.
	 */
	private void processVoiceResult(int requestCode,
			ArrayList<String> resultData) {

		String voiceResult = resultData.get(0);
		switch (requestCode) {
		case VOICE_RECOGNITION_ID:
			onVoiceToTextDataReceive(voiceResult);
			break;

		}
	}

	/**
	 * Handle the flow for get Contact No command
	 * 
	 * @param voiceResult
	 *            : Speech To Text result
	 */
	private void onVoiceToTextDataReceive(String voiceResult) {
		Log.v(TAG, "User Said " + voiceResult);
		Toast.makeText(getApplicationContext(), "User Said " + voiceResult,
                Toast.LENGTH_LONG);
		//customTextView.setTextWithDelay(voiceResult, TEXT_DELAY);
		askWallieQuestion(getApplicationContext(), REST_ENDPOINT_QUERY, voiceResult);


        //speak(voiceResult);
	}

	/**
	 * Speaks through Android Text To Speech without call back action.
	 * 
	 * @param data
	 *            : data to be spoken.
	 */
	private void speak(String data) {
		textToSpeechUtils.speak(data, TextToSpeechUtils.QUEUE_FLUSH, null);
	}

	/**
	 * Speaks through Android text to Speech and perform callback action
	 * specified in method onTextToSpeechDone() matched with this callBackId.
	 * 
	 * @param data
	 *            : data to be spoken.
	 * @param callBackId
	 *            : callback id to be returned in intent when speaking
	 *            completes.
	 */
	private void speak(String data, String callBackId) {
		HashMap<String, String> ttsParams = new HashMap<String, String>();
		ttsParams.put(TextToSpeechUtils.KEY_PARAM_UTTERANCE_ID, callBackId);
		textToSpeechUtils.speak(data, TextToSpeechUtils.QUEUE_FLUSH, ttsParams);
	}

	/**
	 * Speaks the error specified and sets the callbak id for the further flow,
	 * with the help of requestId provided.
	 * 
	 * @param error
	 *            : error to be spoken
	 * @param requestId
	 *            : request id returned with error used to set text to speech
	 *            callBackId
	 */
	private void speakError(String error, int requestId) {
		String textToSpeechCallBackId = null;
		switch (requestId) {
		case VOICE_RECOGNITION_ID:
			textToSpeechCallBackId = UTTERANCE_ID_1;
			break;

		}
		HashMap<String, String> textToSpeechParams = new HashMap<String, String>();
		textToSpeechParams.put(TextToSpeechUtils.KEY_PARAM_UTTERANCE_ID,
				textToSpeechCallBackId);
		textToSpeechUtils.speak(error, TextToSpeechUtils.QUEUE_FLUSH,
				textToSpeechParams);
	}

	@Override
	public void onTextToSpeechStart(String utteranceId) {
		// TODO: Nothing To do here right now... Reserved for future use.
	}

	@Override
	public void onTextToSpeechDone(String utteranceId) {
		if (utteranceId.equals(UTTERANCE_ID_ERROR)
				|| isApplicationBroughtToBackground()) {
			/* Send Callback and finish here */

		} else if (utteranceId.equals(UTTERANCE_ID_CALLBACK)) {
			/* Send Callback with data(contactNo , messageData) and finish here */

		} else if (!isApplicationBroughtToBackground()) {

			Intent speechRecognizerIntent = new Intent(
					SpeechToTextUtils.ACTION_RECOGNIZE_SPEECH);
			if (utteranceId.equals(UTTERANCE_ID_1)) {
				startActivityForResult(speechRecognizerIntent,
						VOICE_RECOGNITION_ID);
			}
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

	@Override
	public void onTextToSpeechError(String utteranceId) {
		/* Send Callback and finish here */

	}

	@Override
	protected void onDestroy() {
		if (null != textToSpeechUtils) {
			textToSpeechUtils.shutdown();
		}
		super.onDestroy();
	}

	@Override
	public void callback(int resultType, int requestCode, ArrayList<String> resultData) {
		if (null != resultData) {
			switch (resultType) {
			case SpeechToTextUtils.RESULT_TYPE_FOUND:
				// Got Result From Voice Recognizer
				processVoiceResult(requestCode, resultData);
                setState(WELLIE_STATE_IDLE_1);
                break;
			case SpeechToTextUtils.RESULT_TYPE_NOT_FOUND:
				// Result Not Found
				Log.d(TAG, "No matches returned from server!");
				speakError(getString(R.string.no_result_returned), requestCode);
                setState(WELLIE_STATE_IDLE_2);
                break;
			case SpeechToTextUtils.RESULT_TYPE_CANCEL:
				// User canceled
				Log.d(TAG, "User canceled!");
				/* Send Callback and finish here */
                setState(WELLIE_STATE_IDLE_1);
				break;
			case SpeechToTextUtils.RESULT_TYPE_ERROR:
				// Returned Error code
				handleError(resultType, requestCode);
                startListening();
                break;

            case SpeechToTextUtils.RESULT_TYPE_END_OF_SPEECH:
                // Returned intermidiate end of speech
                setState(WELLIE_STATE_SEARCHING);
                break;
            }
        } else {
			Log.d(TAG, "onActivityResult");
            setState(WELLIE_STATE_IDLE_1);
		}

	}

	@Override
	public void onReceiveResult(int resultCode, Bundle resultData) {
		String result = resultData
				.getString(NetworkService.NETWORK_SERVICE_RESPONSE);
		if (null != result) {
			result = result.trim();
			switch (resultCode) {

				case RESULT_QUESTION_1: {
					// Log.i(TAG + ".onReceiveResult();", "RESULT_STATION_SEARCH");
					processWallieAnswer(result);
					break;
				}
			}
		} else {// No result means some exception
			Throwable exception = (Throwable) resultData
					.getSerializable(NetworkService.NETWORK_SERVICE_EXCEPTION);
			if (null != exception) {
                setState(WELLIE_STATE_RESULT_ERROR);
				exception.printStackTrace();
				switch (resultCode) {
					case RESULT_QUESTION_1: {
						break;
					}
				}
			}
		}
	}

	private void askWallieQuestion(Context appContext, String restService, String query) {
		networResultReceiver = new NetworkResultReceiver(new Handler());
		networResultReceiver.setReceiver(this);
		setState(WELLIE_STATE_SEARCHING);
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("query", query);
			jsonObject.put("sessionId", getDeviceIMEI());
			String postData = jsonObject.toString();
			Log.d(TAG, "Server URL: " + BASE_SERVER_URL + restService);
			Log.d(TAG, "post data: " + postData);
			NetworkService.forkNetworService(BASE_SERVER_URL, restService, true, null, RESULT_QUESTION_1, NetworkService.POST_METHOD, postData, networResultReceiver, appContext, NetworkService.AUTH_TYPE_BASIC, "superuser", "superuser");
		} catch (JSONException e) {
			e.printStackTrace();
			setState(WELLIE_STATE_RESULT_ERROR);
		}

	}


	private void processWallieAnswer(String result) {
        if(null != result && !"".equalsIgnoreCase(result)) {
            try {
                JSONObject jsonObject = new JSONObject(result);
                boolean success = jsonObject.optBoolean("success");
                if(success) {
                    String responseContent = jsonObject.optString("responseContent");
                    if(null != responseContent && !"".equalsIgnoreCase(responseContent)) {
                        customTextView.setTextWithDelay(responseContent, TEXT_DELAY);
                        speak(responseContent);
                        setState(WELLIE_STATE_RESULT_SUCCESS);
                    } else {
                        speak("Sorry no answer found for your question");
                        setState(WELLIE_STATE_RESULT_DIDNT_UNDERSTAND);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                speak("Sorry error occurred");
                setState(WELLIE_STATE_RESULT_ERROR);
            }
        } else {
            setState(WELLIE_STATE_RESULT_ERROR);
        }
	}



	private void setState(int state) {
        currentState = state;
        switch (state) {
            case WELLIE_STATE_IDLE_1: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_1));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.may_i_help_you));
                break;
            }
            case WELLIE_STATE_IDLE_2: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_2));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.you_can_speak_to_me));
                break;
            }
            case WELLIE_STATE_IDLE_3: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_3));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.how_can_i_help_you));
                break;
            }
            case WELLIE_STATE_IDLE_4: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_4));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.talk_to_me));
                break;
            }
            case WELLIE_STATE_LISTENING: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.while_talk));
                webView.loadUrl(MICROPFONE_ACTIVE);
                customTextView.clear("");
                textView.clear(getString(R.string.listening_to_you));
                break;
            }
            case WELLIE_STATE_SEARCHING: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.while_wait));
                webView.loadUrl(MICROPFONE_WAIT);
                customTextView.clear("");
                textView.clear(getString(R.string.let_me_get_on_to_it));
                break;
            }
            case WELLIE_STATE_RESULT_SUCCESS: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.show_result));
                webView.loadUrl(MICROPFONE_IDLE);
                textView.clear(getString(R.string.got_it));
                break;
            }
            case WELLIE_STATE_RESULT_ERROR: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.while_error));
                webView.loadUrl(MICROPFONE_ACTIVE);
                customTextView.clear("");
                textView.clear(getString(R.string.didnt_understant));
                speak(getString(R.string.didnt_understant));
                break;
            }
            case WELLIE_STATE_RESULT_DIDNT_UNDERSTAND: {
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.no_result));
                webView.loadUrl(MICROPFONE_ACTIVE);
                customTextView.clear("");
                textView.clear(getString(R.string.talk_again));
                speak(getString(R.string.talk_again));
                break;
            }
        }
    }

    // Create the Handler object (on the main thread by default)
    private Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            Log.d("Handlers", "Called on main thread");
            // Repeat this the same runnable code block again another 2 seconds

            switch (currentState) {
                case WELLIE_STATE_IDLE_1: {
                    wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_1));
                    webView.loadUrl(MICROPFONE_IDLE);
                    customTextView.clear(getString(R.string.no_talk_yet));
                    textView.clear(getString(R.string.may_i_help_you));
                    currentState = WELLIE_STATE_IDLE_2;
                    break;
                }
                case WELLIE_STATE_IDLE_2: {
                    wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_2));
                    webView.loadUrl(MICROPFONE_IDLE);
                    customTextView.clear(getString(R.string.no_talk_yet));
                    textView.clear(getString(R.string.you_can_speak_to_me));
                    currentState = WELLIE_STATE_IDLE_3;
                    break;
                }
                case WELLIE_STATE_IDLE_3: {
                    wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_3));
                    webView.loadUrl(MICROPFONE_IDLE);
                    customTextView.clear(getString(R.string.no_talk_yet));
                    textView.clear(getString(R.string.how_can_i_help_you));
                    currentState = WELLIE_STATE_IDLE_4;
                    break;
                }
                case WELLIE_STATE_IDLE_4: {
                    wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_4));
                    webView.loadUrl(MICROPFONE_IDLE);
                    customTextView.clear(getString(R.string.no_talk_yet));
                    textView.clear(getString(R.string.talk_to_me));
                    currentState = WELLIE_STATE_IDLE_1;
                    break;
                }
            }
            handler.postDelayed(runnableCode, 5000);
        }
    };

	/**
	 * Returns the unique identifier for the device
	 *
	 * @return unique identifier for the device
	 */
	public String getDeviceIMEI() {
		String deviceUniqueIdentifier = null;
		TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		if (null != tm) {
			deviceUniqueIdentifier = tm.getDeviceId();
		}
		if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
			deviceUniqueIdentifier = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
		}
		return deviceUniqueIdentifier;
	}
}
