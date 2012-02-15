package net.tallpixel.mobile;

import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.TextView;

public class SimpleEULA {
	
	private String EULA_PREFIX = "eula_";
	private Activity mActivity;
	
	public Boolean visible = false;
	
	public SimpleEULA(Activity context) {
		mActivity = context;
	}
	
	private PackageInfo getPackageInfo() {
        PackageInfo pi = null;
        try {
             pi = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pi; 
    }

     public void show() {
        PackageInfo versionInfo = getPackageInfo();

        // the eulaKey changes every time you increment the version number in the AndroidManifest.xml
		final String eulaKey = EULA_PREFIX + versionInfo.versionCode;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean hasBeenShown = prefs.getBoolean(eulaKey, false);
        if(hasBeenShown == false) {
        	
        	// Show the Eula
            String title = mActivity.getString(R.string.app_name) + " v" + versionInfo.versionName;
            
            //Includes the updates as well so users know what changed. 
            String message = mActivity.getString(R.string.updates) + "<br /><br />" + mActivity.getString(R.string.eula);
            
            // Wrap it up in a webview
            message = "<html><body style='background:#000;font-size:14pt;color:#eee;padding:0px;'>" + message.toString() + "</body></html>";
            WebView webview = new WebView(mActivity);
            webview.setVerticalScrollBarEnabled(true);
            webview.setBackgroundColor(android.R.color.black);
            webview.setPadding(5, 5, 5, 5);
            webview.loadData(message, "text/html", "utf-8");
            webview.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					// Don't do anything and don't bubble
					return true;
				}
            });
            
            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(title)
                    .setView(webview)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {
                    	
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Mark this version as read.
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(eulaKey, true);
                            editor.commit();
                            
                            // Log that the user accepted the EULA
                            FlurryAgent.logEvent("EULA_ACCEPTED");
                            
                            dialogInterface.dismiss();
                            visible = false;
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {
                    	
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
                            // Log that the user declined the EULA
                            FlurryAgent.logEvent("EULA_DECLINED");
							
                            visible = false;
                            
							// Close the activity as they have declined the EULA
							mActivity.finish(); 
						}
                    	
                    });
            final AlertDialog d = builder.create();
            
            d.show();
            visible = true;
        }
    }
	
}
