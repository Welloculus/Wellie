package com.transility.wellie.texttospeech;

/**
 * Interface for getting callback from TextToSpeechUtils class.
 * 
 * @author shridutt.kothari
 * 
 */
public interface OnTextToSpeechCompletedListener {

	/**
	 * This method will be called when TextToSpeechUtils starts to perform Text To
	 * Speech operation.
	 * 
	 * @param utteranceId
	 *            : id associated with this Text To Speech operation.
	 */
	public void onTextToSpeechStart(String utteranceId);

	/**
	 * This method will be called when TextToSpeechUtils completes the Text To
	 * Speech operation.
	 * 
	 * @param utteranceId
	 *            : id associated with this Text To Speech operation.
	 */
	public void onTextToSpeechDone(String utteranceId);

	/**
	 * This method will be called when any error occures while performing Text To
	 * Speech operation in TextToSpeechUtils.
	 * 
	 * @param utteranceId
	 *            : id associated with this Text To Speech operation.
	 */
	public void onTextToSpeechError(String utteranceId);
}
