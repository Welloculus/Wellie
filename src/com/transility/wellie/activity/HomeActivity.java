package com.transility.wellie.activity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.IdentifyResult;
import com.microsoft.projectoxford.face.contract.TrainingStatus;
import com.transility.wellie.R;
import com.transility.wellie.controller.NetworkResultReceiver;
import com.transility.wellie.controller.NetworkService;
import com.transility.wellie.face.helper.SampleApp;
import com.transility.wellie.face.helper.StorageHelper;
import com.transility.wellie.face.persongroupmanagement.PersonGroupListActivity;
import com.transility.wellie.face.ui.IdentificationActivity;
import com.transility.wellie.facetracker.FaceDetectionCallback;
import com.transility.wellie.facetracker.FaceGraphic;
import com.transility.wellie.facetracker.ui.camera.CameraSourcePreview;
import com.transility.wellie.facetracker.ui.camera.GraphicOverlay;
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
		OnTextToSpeechCompletedListener, SpeechToTextCallback, NetworkResultReceiver.Receiver, FaceDetectionCallback {

	private static final String TAG = HomeActivity.class.getName();
	/* Ids used for communication with speech to text activity */
	private static final int VOICE_RECOGNITION_ID = 10001;
	private static final int RESULT_QUESTION_1 = 10002;

	/* Ids used for communication with text to speech activity */
	private static final String UTTERANCE_ID_1 = "UTTERANCE_ID_1";
	private static final String UTTERANCE_ID_ERROR = "error";
	private static final String UTTERANCE_ID_CALLBACK = "give_callback";
    /**
     * ndimp.impetus.co.in:6045
     * 192.168.146.54:80
     */
    //private static final String BASE_SERVER_URL = "http://192.168.146.54:80/ApiGateway/wellie/";
    private static final String BASE_SERVER_URL = "http://ndimp.impetus.co.in:6045/ApiGateway/wellie/";
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
	private static final int RC_HANDLE_PERM = 2;
	//private static final int RC_HANDLE_CAMERA_PERM = 2;
	private static final int RC_HANDLE_WRITE_EXTERNAL_STORAGE_PERM = 3;

	private TextToSpeechUtils textToSpeechUtils;
	private CustomTextView customTextView;
    private CustomTextView textView;
	private ImageView wellieCharacterImageView;
    private NetworkResultReceiver networResultReceiver;
    private WebView webView;
    private static int currentState;
	//private boolean deviceIMEI;

	private CameraSource mCameraSource = null;
	private CameraSourcePreview mPreview;
	private GraphicOverlay mGraphicOverlay;
	private static final int RC_HANDLE_GMS = 9001;
	// permission request codes need to be < 256

    public static boolean isNotWorking = true;
    public static String faceName = "";
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
				startListening();
			}
		});

		wellieCharacterImageView.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				identification();
				return false;
			}
		});


		textToSpeechUtils = new TextToSpeechUtils(getApplicationContext());
		textToSpeechUtils.setOnTextToSpeechCompletedListener(this);

		mPreview = (CameraSourcePreview) findViewById(R.id.preview);
		mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

		// Check for the camera permission before accessing the camera.  If the
		// permission is not granted yet, request permission.
		/*int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
		int rc1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
*/
		String permissions [] = {android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
		boolean has = hasPermissions(this, permissions);
		if (has) {
			createCameraSource();
		} else {
			ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_PERM);
		}
	}

	public static boolean hasPermissions(Context context, String... permissions) {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_home, menu);
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_manage_person_groups:
				managePersonGroups();
				return true;
		}
        return false;
	}

	private void startListening(){
        speak("Hi "+faceName +" How may i help you ");
        SpeechToTextUtils speechToTextUtils = new SpeechToTextUtils(
                HomeActivity.this, HomeActivity.this);
        speechToTextUtils.startListening(VOICE_RECOGNITION_ID);
        setState(WELLIE_STATE_LISTENING);

    }
	public void identification() {
		Intent intent = new Intent(this, IdentificationActivity.class);
		startActivity(intent);
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
		/**
		 * Releases the resources associated with the camera source, the associated detector, and the
		 * rest of the processing pipeline.
		 */
		if (mCameraSource != null) {
			mCameraSource.release();
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
                isNotWorking = true;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_1));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.may_i_help_you));
                break;
            }
            case WELLIE_STATE_IDLE_2: {
                isNotWorking = true;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_2));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.you_can_speak_to_me));
                break;
            }
            case WELLIE_STATE_IDLE_3: {
                isNotWorking = true;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_3));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.how_can_i_help_you));
                break;
            }
            case WELLIE_STATE_IDLE_4: {
                isNotWorking = true;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.idle_4));
                webView.loadUrl(MICROPFONE_IDLE);
                customTextView.clear(getString(R.string.no_talk_yet));
                textView.clear(getString(R.string.talk_to_me));
                break;
            }
            case WELLIE_STATE_LISTENING: {
                isNotWorking = false;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.while_talk));
                webView.loadUrl(MICROPFONE_ACTIVE);
                customTextView.clear("");
                textView.clear(getString(R.string.listening_to_you));
                break;
            }
            case WELLIE_STATE_SEARCHING: {
                isNotWorking = false;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.while_wait));
                webView.loadUrl(MICROPFONE_WAIT);
                customTextView.clear("");
                textView.clear(getString(R.string.let_me_get_on_to_it));
                break;
            }
            case WELLIE_STATE_RESULT_SUCCESS: {
                isNotWorking = true;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.show_result));
                webView.loadUrl(MICROPFONE_IDLE);
                //textView.clear(getString(R.string.got_it));
                break;
            }
            case WELLIE_STATE_RESULT_ERROR: {
                isNotWorking = false;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.while_error));
                webView.loadUrl(MICROPFONE_ACTIVE);
                customTextView.clear("");
                textView.clear(getString(R.string.didnt_understant));
                speak(getString(R.string.didnt_understant));
                break;
            }
            case WELLIE_STATE_RESULT_DIDNT_UNDERSTAND: {
                //isNotWorking = false;
                wellieCharacterImageView.setBackground(getDrawable(R.drawable.no_result));
                webView.loadUrl(MICROPFONE_ACTIVE);
                customTextView.clear("");
                textView.clear(getString(R.string.talk_again));
                speak(getString(R.string.talk_again));
                break;
            }
        }
    }

    // Create the Handler object (on the camera_preview_layout thread by default)
    private Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the camera_preview_layout thread
            Log.d("Handlers", "Called on camera_preview_layout thread");
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


	/**
	 * Handles the requesting of the camera permission.  This includes
	 * showing a "Snackbar" message of why the permission is needed then
	 * sending the request.
	 */
	/*private void requestCameraPermission() {
		Log.w(TAG, "Camera permission is not granted. Requesting permission");

		final String[] permissions = new String[]{Manifest.permission.CAMERA};

		if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
				Manifest.permission.CAMERA)) {
			ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
			return;
		}

		final Activity thisActivity = this;

		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ActivityCompat.requestPermissions(thisActivity, permissions,
						RC_HANDLE_CAMERA_PERM);
			}
		};

		Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
				Snackbar.LENGTH_INDEFINITE)
				.setAction(android.R.string.ok, listener)
				.show();
	}*/


	/**
	 * Handles the requesting of the file system permission.  This includes
	 * showing a "Snackbar" message of why the permission is needed then
	 * sending the request.
	 */
	private void requestFileSystemPermission() {
		Log.w(TAG, "File system permission is not granted. Requesting permission");

		final String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

		if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_WRITE_EXTERNAL_STORAGE_PERM);
			return;
		}

		final Activity thisActivity = this;

		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				ActivityCompat.requestPermissions(thisActivity, permissions,
						RC_HANDLE_WRITE_EXTERNAL_STORAGE_PERM);
			}
		};

		Snackbar.make(mGraphicOverlay, R.string.permission_file_system_rationale,
				Snackbar.LENGTH_INDEFINITE)
				.setAction(android.R.string.ok, listener)
				.show();
	}


	/**
	 * Creates and starts the camera.  Note that this uses a higher resolution in comparison
	 * to other detection examples to enable the barcode detector to detect small barcodes
	 * at long distances.
	 */
	private void createCameraSource() {

		Context context = getApplicationContext();
		FaceDetector detector = new FaceDetector.Builder(context)
				.setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
				.build();

		detector.setProcessor(
				new MultiProcessor.Builder<>(new HomeActivity.GraphicFaceTrackerFactory())
						.build());

		if (!detector.isOperational()) {
			// Note: The first time that an app using face API is installed on a device, GMS will
			// download a native library to the device in order to do detection.  Usually this
			// completes before the app is run for the first time.  But if that download has not yet
			// completed, then the above call will not detect any faces.
			//
			// isOperational() can be used to check if the required native library is currently
			// available.  The detector will automatically become operational once the library
			// download completes on device.
			Log.w(TAG, "Face detector dependencies are not yet available.");
		}

		mCameraSource = new CameraSource.Builder(context, detector)
				.setRequestedPreviewSize(640, 480)
				.setFacing(CameraSource.CAMERA_FACING_FRONT)
				.setRequestedFps(30.0f)
				.build();
	}
    private Set<String> personGroupIds;
	/**
	 * Restarts the camera.
	 */
	@Override
	protected void onResume() {
		super.onResume();

		startCameraSource();
        personGroupIds = StorageHelper.getAllPersonGroupIds(HomeActivity.this);

	}

	/**
	 * Stops the camera.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mPreview.stop();
	}

	/**
	 * Callback for the result from requesting permissions. This method
	 * is invoked for every call on {@link #requestPermissions(String[], int)}.
	 * <p>
	 * <strong>Note:</strong> It is possible that the permissions request interaction
	 * with the user is interrupted. In this case you will receive empty permissions
	 * and results arrays which should be treated as a cancellation.
	 * </p>
	 *
	 * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
	 * @param permissions  The requested permissions. Never null.
	 * @param grantResults The grant results for the corresponding permissions
	 *                     which is either {@link PackageManager#PERMISSION_GRANTED}
	 *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
	 * @see #requestPermissions(String[], int)
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode != RC_HANDLE_PERM && requestCode != RC_HANDLE_WRITE_EXTERNAL_STORAGE_PERM) {
			Log.d(TAG, "Got unexpected permission result: " + requestCode);
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
			return;
		}

		if (grantResults.length >= 0) {

			if(permissions.length == 1) {
				if (permissions[0].equals(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
						Log.d(TAG, "permission granted - initialize the camera source");
						// we have permission, so start face detection again
						isNotWorking = true;
						return;
					} else {
						Log.d(TAG, "File write permission granted - initialize the App again");
						// we have permission, so create the camerasource
						DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								finish();
							}
						};

						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						builder.setTitle(getString(R.string.app_name))
								.setMessage(R.string.no_permission_file_system_rationale)
								.setPositiveButton(android.R.string.ok, listener)
								.show();
						return;
					}
				} else {
					// Nothing for now
				}
			}
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.d(TAG, "permission granted - initialize the camera source");
				// we have permission, so create the camerasource
				createCameraSource();
				return;
			}
		}
		Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
				" Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name))
				.setMessage(R.string.no_camera_permission)
				.setPositiveButton(android.R.string.ok, listener)
				.show();
	}

	//==============================================================================================
	// Camera Source Preview
	//==============================================================================================

	/**
	 * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
	 * (e.g., because onResume was called before the camera source was created), this will be called
	 * again when the camera source is created.
	 */
	private void startCameraSource() {

		// check that the device has play services available.
		int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
				getApplicationContext());
		if (code != ConnectionResult.SUCCESS) {
			Dialog dlg =
					GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
			dlg.show();
		}

		if (mCameraSource != null) {
			try {
				mPreview.start(mCameraSource, mGraphicOverlay);
			} catch (IOException e) {
				Log.e(TAG, "Unable to start camera source.", e);
				mCameraSource.release();
				mCameraSource = null;
			}
		}
	}

    @Override
    public void onFaceDetected() {

        //textView.setTextWithDelay("Face Detected ", 100);
        //Toast.makeText(this, "Face Detected " + (++count), Toast.LENGTH_LONG).show();
        if(isNotWorking) {
            faceName = "";
            mCameraSource.takePicture(new CameraSource.ShutterCallback() {
                @Override
                public void onShutter() {

                }
            }, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] bytes) {
                    isNotWorking = false;
                    Log.d(TAG, "onPictureTaken");
                    File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);


					int rc1 = ActivityCompat.checkSelfPermission(HomeActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
					if (rc1 == PackageManager.PERMISSION_GRANTED) {
						savePictureTo(picturesDir, bytes);
					} else {
						requestFileSystemPermission();
					}


                }
            });
        }
    }

	// Background task of face detection.
	private class DetectionTask extends AsyncTask<InputStream, String, com.microsoft.projectoxford.face.contract.Face[]> {
		@Override
		protected com.microsoft.projectoxford.face.contract.Face[] doInBackground(InputStream... params) {
			// Get an instance of face service client to detect faces in image.
			FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
			try {
				publishProgress("Detecting...");

				// Start detection.
				return faceServiceClient.detect(
						params[0],  /* Input stream of image to detect */
						true,       /* Whether to return face ID */
						false,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently we support:
                           age,gender,headPose,smile,facialHair */
						null);
			} catch (Exception e) {
				publishProgress(e.getMessage());
                isNotWorking = true;
				return null;
			}
		}

		@Override
		protected void onPreExecute() {
			//setUiBeforeBackgroundTask();
		}

		@Override
		protected void onProgressUpdate(String... values) {
			// Show the status of background detection task on screen.
			//setUiDuringBackgroundTask(values[0]);
		}

		@Override
		protected void onPostExecute(com.microsoft.projectoxford.face.contract.Face[] result) {
			//progressDialog.dismiss();

			//setAllButtonsEnabledStatus(true);

			if (result != null) {
				// Set the adapter of the ListView which contains the details of detected faces.


				if (result.length == 0) {
					detected = false;
					//setInfo("No faces detected!");
				} else {
					detected = true;
					//setInfo("Click on the \"Identify\" button to identify the faces in image.");
				}
			} else {
				detected = false;
			}

			if(detected) {
				com.microsoft.projectoxford.face.contract.Face face = result[0];
			}
            identify(result);

		}
	}

    private void savePictureTo(File directory, byte[] data) {

		String fileName = new SimpleDateFormat("'Wellie_'yyyyMMdd_HHmmss'.jpg'")
                .format(System.currentTimeMillis());
        File file = new File(directory, fileName);
        try{
			if(!directory.exists()) {
				directory.mkdirs();
			}
            OutputStream os = new FileOutputStream(file);
            os.write(data);
            os.close();

            MediaScannerConnection.scanFile(this, new String[ ] { file.toString() },
                    null, new MediaScannerConnection.OnScanCompletedListener()
                    {
                        @Override
                        public void onScanCompleted(String path, Uri imageUri)
                        {
                            Log.d(TAG, "savePictureTo: scanned path=" + path + ", uri=" + imageUri);
							try {
								Bitmap bitmap = MediaStore.Images.Media.getBitmap(HomeActivity.this.getContentResolver(), imageUri);
								detect(bitmap);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
                    });
        }
        catch(IOException e){
            isNotWorking = true;
			speak("Error while detecting face as unable to use file system");
            Log.e(TAG, "savePictureTo: error writing " + file, e);
        }
    }

	// Start detecting in image.
	private void detect(Bitmap rotatedBitmap) {
        rotatedBitmap = rotateBitmap(rotatedBitmap, 270);
        //ImageView wellie_character = (ImageView) findViewById(R.id.imageview_wellie_character);
        //wellie_character.setImageBitmap(rotatedBitmap);
		// Put the image into an input stream for detection.

		ByteArrayOutputStream output = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 10, output);
        byte [] data = output.toByteArray();
		ByteArrayInputStream inputStream = new ByteArrayInputStream(data);

		//setAllButtonsEnabledStatus(false);

		// Start a background task to detect faces in the image.
		new HomeActivity.DetectionTask().execute(inputStream);
	}

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

	boolean detected;

	// Called when the "Detect" button is clicked.
	public void identify(com.microsoft.projectoxford.face.contract.Face [] faces) {
        if(null!= personGroupIds) {
            for (String personGroupId : personGroupIds) {
                mPersonGroupId = personGroupId;
                break;
            }
        }
		// Start detection task only if the image to detect is selected.
		if (detected && mPersonGroupId != null) {
			// Start a background task to identify faces in the image.
			List<UUID> faceIds = new ArrayList<>();
			for (com.microsoft.projectoxford.face.contract.Face face : faces) {
				faceIds.add(face.faceId);
			}
			new HomeActivity.IdentificationTask(mPersonGroupId).execute(faceIds.toArray(new UUID[faceIds.size()]));
		} else {
            isNotWorking = true;
			// Not detected or person group exists.
			speak(getString(R.string.no_face_detected_or_person_group_does_not_exists));
            Toast.makeText(HomeActivity.this, "No face detected or person group does not exist", Toast.LENGTH_LONG).show();
			//setInfo("Please select an image and create a person group first.");
		}
	}

	// Background task of face identification.
	private class IdentificationTask extends AsyncTask<UUID, String, IdentifyResult[]> {

		private boolean mSucceed = true;
		String mPersonGroupId;

		IdentificationTask(String personGroupId) {
			this.mPersonGroupId = personGroupId;
		}

		@Override
		protected IdentifyResult[] doInBackground(UUID... params) {
			String logString = "Request: Identifying faces ";
			for (UUID faceId : params) {
				logString += faceId.toString() + ", ";
			}
			logString += " in group " + mPersonGroupId;
			//addLog(logString);

			// Get an instance of face service client to detect faces in image.
			FaceServiceClient faceServiceClient = SampleApp.getFaceServiceClient();
			try {
				publishProgress("Getting person group status...");

				TrainingStatus trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(
						this.mPersonGroupId);     /* personGroupId */

				if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
					publishProgress("Person group training status is " + trainingStatus.status);
					mSucceed = false;
					return null;
				}

				publishProgress("Identifying...");

				// Start identification.
				return faceServiceClient.identity(
						this.mPersonGroupId,   /* personGroupId */
						params,                  /* faceIds */
						1);  /* maxNumOfCandidatesReturned */
			} catch (Exception e) {
                isNotWorking = true;
                mSucceed = false;
				publishProgress(e.getMessage());
				return null;
			}
		}

		@Override
		protected void onPreExecute() {

		}

		@Override
		protected void onProgressUpdate(String... values) {
			// Show the status of background detection task on screen.a
		}

		@Override
		protected void onPostExecute(IdentifyResult[] result) {
			// Show the result on screen when detection is done.
			onFaceIndentification(result, mSucceed);
		}
	}

	String mPersonGroupId;

	// Show the result on screen when detection is done.
	private void onFaceIndentification(IdentifyResult[] result, boolean succeed) {

		if (succeed) {
			// Set the information about the detection result.
			if (result != null) {
				StringBuilder logString = new StringBuilder("Response: Success. ");
				for (IdentifyResult identifyResult : result) {
                    if(identifyResult.candidates.size() > 0) {
                        String personName = StorageHelper.getPersonName(identifyResult.candidates.get(0).personId.toString(), mPersonGroupId, HomeActivity.this);
                        logString.append("Face is identified as " + personName);
                        faceName = personName;
                        startListening();
                    } else {
                        startListening();
                        logString.append("Face is identified as Unknown Person");
                    }

				}
				Log.e(TAG, logString.toString());
				//Toast.makeText(HomeActivity.this, logString.toString(), Toast.LENGTH_LONG).show();;

            }
		}
        //isNotWorking = true;

	}

	//==============================================================================================
	// Graphic Face Tracker
	//==============================================================================================

	/**
	 * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
	 * uses this factory to create face trackers as needed -- one for each individual.
	 */
	private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
		@Override
		public Tracker<Face> create(Face face) {
			return new HomeActivity.GraphicFaceTracker(mGraphicOverlay, HomeActivity.this);
		}
	}

	/**
	 * Face tracker for each detected individual. This maintains a face graphic within the app's
	 * associated face overlay.
	 */
	private class GraphicFaceTracker extends Tracker<Face> {
		private GraphicOverlay mOverlay;
		private FaceGraphic mFaceGraphic;
        private FaceDetectionCallback faceDetectionCallback;

		GraphicFaceTracker(GraphicOverlay overlay, FaceDetectionCallback faceDetectionCallback) {
			mOverlay = overlay;
			mFaceGraphic = new FaceGraphic(overlay);
            this.faceDetectionCallback = faceDetectionCallback;
		}

		/**
		 * Start tracking the detected face instance within the face overlay.
		 */
		@Override
		public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
            if(null != faceDetectionCallback) {
                faceDetectionCallback.onFaceDetected();
            }
		}

		/**
		 * Update the position/characteristics of the face within the overlay.
		 */
		@Override
		public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
			mOverlay.add(mFaceGraphic);
			mFaceGraphic.updateFace(face);
		}

		/**
		 * Hide the graphic when the corresponding face was not detected.  This can happen for
		 * intermediate frames temporarily (e.g., if the face was momentarily blocked from
		 * view).
		 */
		@Override
		public void onMissing(FaceDetector.Detections<Face> detectionResults) {
			mOverlay.remove(mFaceGraphic);
		}

		/**
		 * Called when the face is assumed to be gone for good. Remove the graphic annotation from
		 * the overlay.
		 */
		@Override
		public void onDone() {
			mOverlay.remove(mFaceGraphic);
		}
	}


	public void managePersonGroups() {
		Intent intent = new Intent(this, PersonGroupListActivity.class);
		startActivity(intent);
	}

    public void managePersonGroups(View view) {
        Intent intent = new Intent(this, PersonGroupListActivity.class);
        startActivity(intent);
    }

}
