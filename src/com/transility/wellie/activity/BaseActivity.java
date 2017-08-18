package com.transility.wellie.activity;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.transility.wellie.R;
/**
 * 
 * This is the parent activity class of activities of this application,
 * And manages the the application backgroundness scenarios.
 * It also sends the broadcast message to the IVIStatusBar and 
 * IVIBTSMSClient application.
 * 
 * @author shridutt.kothari
 *
 */
public class BaseActivity extends Activity {

	private static String TAG = BaseActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause(){
    	super.onPause();
    	if(isApplicationBroughtToBackground()){
    		//sendBroadcastToIviAndFinish(false);
    	}
    }
    @Override
    protected void onResume() {
    	Log.d(TAG, "onResume()");
    	super.onResume();
    }

	/**
	 * Creates a custom toast and shows given text on it. 
	 * @param textToShow : text to be shown through Toast.
	 */
	protected void showToast(String textToShow) {
		View toastView = getLayoutInflater().inflate(R.layout.toast_layout,	(ViewGroup) findViewById(R.id.toast_layout_root));
		TextView text = (TextView) toastView.findViewById(R.id.text);
		text.setText(textToShow);
		Toast toast1 = new Toast(getApplicationContext());
		toast1.setGravity(Gravity.CENTER_HORIZONTAL, 0, 100);
		toast1.setDuration(Toast.LENGTH_LONG);
		toast1.setView(toastView);
		toast1.show();
	}
	
	/**
	 * give the application current status that is it in foreground or not. 
	 *
	 */
	protected boolean isApplicationBroughtToBackground() {
		boolean appBgStatus = false;
		getApplicationContext();
		ActivityManager am = (ActivityManager) getApplicationContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> tasks = am.getRunningTasks(1);
		if (!tasks.isEmpty()) {
			ComponentName topActivity = tasks.get(0).topActivity;
			String topActivityPackage =topActivity.getPackageName();
			String myApppackage =getPackageName();
			if (!(topActivityPackage.equalsIgnoreCase(myApppackage))) {
				appBgStatus = true;
			}
		}
		return appBgStatus;
	}
	
}
