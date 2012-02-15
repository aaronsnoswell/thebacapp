package net.tallpixel.mobile;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

public class PreferencesActivity extends PreferenceActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		final Activity me = this;
		
		// Force the keyboard to hide itself after preferences are edited
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		// Set numeric keyboard for the bac limit preference
		EditTextPreference bac_limit = (EditTextPreference) findPreference("pref_bac_limit");
		EditText bac_limit_edit_text = (EditText) bac_limit.getEditText();
		bac_limit_edit_text.setKeyListener(DigitsKeyListener.getInstance(false, true));
		
		bac_limit.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				// Select the text when the dialog opens
				((EditTextPreference) findPreference("pref_bac_limit")).getEditText().selectAll();
				return true;
			}
		});
		
		// Set up the clear data option
		Preference clear_data_pref = (Preference) findPreference("pref_clear_data");
		clear_data_pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				//Ask the user if they want to quit
		        new AlertDialog.Builder(me)
			        .setTitle(R.string.preferences_clear_data_title)
			        .setMessage(R.string.preferences_clear_data_dialog_message)
			        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Clear the application's data
							//clearApplicationData();
							SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(me);
							Editor editor = settings.edit();
							editor.clear();
							editor.commit();
							
							// Show a confirmation toast
							Toast.makeText(
								getBaseContext(),
                                getBaseContext().getString(R.string.preferences_clear_data_dialog_confirm),
                                Toast.LENGTH_SHORT
                            ).show();
							
							// Exit the preferences activity
							finish();
							
						}
			        })
			        .setNegativeButton(R.string.no, null)
			        .show();
				
				return true;
			}

		});
	}
	
	public void clearApplicationData() {
		File cache = getCacheDir();
		File appDir = new File(cache.getParent());
		if (appDir.exists()) {
			String[] children = appDir.list();
			for (String s : children) {
				if (!s.equals("lib")) {
					deleteDir(new File(appDir, s));
				}
			}
		}
	}
	
	public static boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}

		return dir.delete();
	}
}