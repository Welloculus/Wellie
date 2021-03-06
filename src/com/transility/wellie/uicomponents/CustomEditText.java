package com.transility.wellie.uicomponents;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.EditText;
/**
 * 
 * @author shridutt.kothari
 *
 */
public class CustomEditText extends EditText {

    private CharSequence data;
    private CharSequence dataToAppend;
    private int index;
    private long delay = 500; //Default 500ms delay
    private Handler uiHandler = new Handler();

    public CustomEditText(Context context) {
        super(context);
    }

    public CustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

   
    private Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            setText(data.subSequence(0, index++));
            if(index <= data.length()) {
                uiHandler.postDelayed(characterAdder, delay);
            }
        }
    };

    
    public void setTextWithDelay(CharSequence text, long millis) {
        data = text;
        index = 0;
        delay = millis;
        
        setText("");
        uiHandler.removeCallbacks(characterAdder);
        uiHandler.postDelayed(characterAdder, delay);
    }

    private Runnable characterAppender = new Runnable() {
        @Override
        public void run() {
        	if(index < dataToAppend.length()){
        		append(dataToAppend.subSequence(index, ++index));
        	}
            if(index <= dataToAppend.length()) {
                uiHandler.postDelayed(characterAppender, delay);
            }
        }
    };
    public void appendTextWithDelay(CharSequence text, long millis) {
        dataToAppend = text;
        index = 0;
        delay = millis;
        
        setText(getText().toString());
        uiHandler.removeCallbacks(characterAppender);
        uiHandler.postDelayed(characterAppender, delay);
    }
}
