package com.transility.wellie.texttospeech;

import java.util.HashMap;

import android.content.Context;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

/**
 * This is the text to speech controller of IVIVoiceMessenger application.
 * It performs the text to speech operations using android's default 
 * Text To Speech Engine.
 * 
 * It also provides the callback for text to speech status using
 * registered OnTextToSpeechCompletedListener.
 * 
 * @author shridutt.kothari
 *
 */
public class TextToSpeechUtils extends UtteranceProgressListener implements OnInitListener{

	/* Log Tag */
	private static final String TAG =TextToSpeechUtils.class.getName();
	/* Queue mode */
	public static final int QUEUE_FLUSH = TextToSpeech.QUEUE_FLUSH;
	/* CallBack Id For Text To Speech. */
	public static final String KEY_PARAM_UTTERANCE_ID = TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID;
	/* Action for success to be sent with Intent*/
	public static final String ACTION_TEXT_TO_SPEECH_INIT_SUCCESS = "TextToSpeech_Success";
	/* Action for failure to be sent with Intent*/
	public static final String ACTION_TEXT_TO_SPEECH_INIT_FAILURE = "TextToSpeech_Failure";
	/* Text to speech current status*/
	public static int textToSpeechInitStatus = TextToSpeech.SUCCESS;
	/* Speeking Speed */
	private static final float SPEECH_RATE = (float) 0.9;
	/* Android TTS */
	private TextToSpeech textToSpeech;
	/* Listener to send callback after complition of speaking */
	private OnTextToSpeechCompletedListener onTextToSpeechCompletedListener;
	
	
	/* Constructor*/
	public TextToSpeechUtils(Context context) {
		textToSpeech = new TextToSpeech(context, this);
		textToSpeech.setSpeechRate(SPEECH_RATE);
		textToSpeech.setOnUtteranceProgressListener(this);
	}

	@Override
	public void onInit(int status) {
		Log.d(TAG,"On init text to speech!");
		switch (status) {
			case TextToSpeech.SUCCESS: {
				Log.i(TAG,"success in text to speech!");
				textToSpeechInitStatus = TextToSpeech.SUCCESS;
				Intent textToSpeechSuccessIntent  = new Intent(ACTION_TEXT_TO_SPEECH_INIT_SUCCESS);
				textToSpeechSuccessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//appContext.startActivity(textToSpeechSuccessIntent);
				break;
			}
			case TextToSpeech.ERROR: {
				// failed to init
				Log.e(TAG,"failed to initialize text to speech!");
				textToSpeechInitStatus = TextToSpeech.ERROR;
				Intent textToSpeechSuccessIntent  = new Intent(ACTION_TEXT_TO_SPEECH_INIT_FAILURE);
				textToSpeechSuccessIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				//appContext.startActivity(textToSpeechSuccessIntent);
				break;
			}
		}
	}
	
	/**
	 * Shutdown the text to speech component.
	 */
	public void shutdown() {
		if (null != textToSpeech) {
			Log.i(TAG,"shuting down text to speech!");
			textToSpeech.shutdown();
		}
	}

	/**
	 * Returns the status of text to speech readyness.
	 * @return the textToSpeechStatus : status of text to speech component
	 */
	public static int getTextToSpeechInitStatus() {
		return textToSpeechInitStatus;
	}
	
	/**
	 * Speaks the given text and consumes the parameters in a hashmap.
	 * @param text : Text to be spoken
	 * @param queueMode : quemode to set for text to speech component
	 * @param params : parameters like callbake id etc.
	 */
	public void speak(String text, int queueMode, HashMap<String, String> params) {
		 textToSpeech.speak(text, queueMode, params);
	}
	
	/**
	 * Set the listener which will get the callback for text to speech complition.
	 * @param onTextToSpeechCompletedListener : Listener to be called on completion of text to speech. 
	 */
	public void setOnTextToSpeechCompletedListener(OnTextToSpeechCompletedListener onTextToSpeechCompletedListener){
		this.onTextToSpeechCompletedListener = onTextToSpeechCompletedListener;
	}

	@Override
	public void onStart(String utteranceId) {
		Log.i(TAG,"starting text to speech!");
		if(null != onTextToSpeechCompletedListener){
			onTextToSpeechCompletedListener.onTextToSpeechStart(utteranceId);
		}
	}
	
	@Override
	public void onDone(String utteranceId) {
		Log.i(TAG,"text to speech completed!");
		if(null != onTextToSpeechCompletedListener){
			onTextToSpeechCompletedListener.onTextToSpeechDone(utteranceId);
		}
	}

	@Override
	public void onError(String utteranceId) {
		Log.e(TAG,"error in text to speech!");
		if(null != onTextToSpeechCompletedListener){
			onTextToSpeechCompletedListener.onTextToSpeechError(utteranceId);
		}
	}
}
