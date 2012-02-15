package net.tallpixel.mobile;

import java.text.NumberFormat;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;

import com.flurry.android.FlurryAgent;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Xml;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

public class TheBACAppActivity extends Activity {

	// Splash screen dialog
	protected Dialog splash_dialog;
	final int SPLASH_TIME = 1500;
	
	SharedPreferences settings;
	
	SeekBar massSlider;
	SeekBar standarddrinksSlider;
	SeekBar timeSlider;
	
	String gender;
	TextView massValue;
	TextView standarddrinksValue;
	TextView timeValue;

	final float KG_TO_LBS = 2.20462262f;
	final float LBS_TO_KG = 0.45359237f;
	float MASS_CONVERSION;
	float INV_MASS_CONVERSION;
	String MASS_UNIT;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    // Initialise Flurry analytics
	    FlurryAgent.onStartSession(this, this.getString(R.string.flurry_key));
	    FlurryAgent.setUseHttps(true);

	    // Store start time to determine how long to show the splash screen for
	    final long start_time = new Date().getTime();
	    
	    // Get hold of the preferences
	    settings = PreferenceManager.getDefaultSharedPreferences(this);
	    
	    MyStateSaver data = (MyStateSaver) getLastNonConfigurationInstance();
	    if(data != null) {
	        // Show splash screen if still loading
	        if(data.showSplashScreen) {
	            showSplashScreen();
	        }
	        setContentView(R.layout.main);        
	        
	        // Rebuild the UI with the stored state information here (of which we have none)...
	        
	    } else {
	        showSplashScreen();
	        setContentView(R.layout.main);
	        
	        /* If we had any heavy setup code to do we would run it here *in a background thread*
	         * whilst the splash screen is displayed. As we don't, we're just using the UI thread
	         * to set up some UI stuff
	         */

	        // Recalculate the units
			recalculateUnits();
	        
	        // Set up the gender spinner
	        Spinner genderSpinner = (Spinner) this.findViewById(R.id.gender_spinner);
	        //ArrayAdapter genderAdapter = ArrayAdapter.createFromResource(this, R.array.genders, android.R.layout.simple_spinner_item);
	        //genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        //genderSpinner.setAdapter(genderAdapter);
	        genderSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
					gender = (String) parent.getItemAtPosition(pos);
					recalculateBAC();
				}
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					
				}
	        	
	        });
	        genderSpinner.setSelection(0);
	        

	        // Set up the mass slider
	        massValue = (TextView) (this.findViewById(R.id.mass_value));
	        massSlider = (SeekBar) (this.findViewById(R.id.mass_slider));
	        massSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					float f = 0.5f;
					float mass = (progress*2 + 40) * MASS_CONVERSION;
					
					// Round to nearest half
					mass = (float) (Math.round(mass / f) * f);
					
					NumberFormat nf = NumberFormat.getInstance();
					nf.setMinimumFractionDigits(1);
					nf.setMaximumFractionDigits(1);
					
					massValue.setText(nf.format(mass) + MASS_UNIT);
					recalculateBAC();
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
	        });
	        // Triger a mass slider change event
	        massSlider.incrementProgressBy(1);
	        massSlider.incrementProgressBy(-1);
	        
	        // Set up the standard drinks slider
	        standarddrinksValue = (TextView) (this.findViewById(R.id.standarddrinks_value));
	        standarddrinksSlider = (SeekBar) (this.findViewById(R.id.standarddrinks_slider));
	        standarddrinksSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					standarddrinksValue.setText(String.valueOf(progress*0.5f+0.5f));
					recalculateBAC();
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
	        });
	        // Triger a mass slider change event
	        standarddrinksSlider.incrementProgressBy(1);
	        standarddrinksSlider.incrementProgressBy(-1);
	        
	        // Set up the time slider
	        timeValue = (TextView) (this.findViewById(R.id.time_value));
	        timeSlider = (SeekBar) (this.findViewById(R.id.time_slider));
	        timeSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					timeValue.setText(String.valueOf(progress*0.5f+0.5f) + "hrs");
					recalculateBAC();
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
				}
	        });
	        // Triger a mass slider change event
	        timeSlider.incrementProgressBy(1);
	        timeSlider.incrementProgressBy(-1);
	        
	        recalculateBAC();
	        
	        // Ok, we're good to go...
	        long ready_time = new Date().getTime();
	        float delta = (float) (ready_time - start_time);

	        /* Show the splash screen
	         */
	        if(delta >= SPLASH_TIME) {
	        	removeSplashScreen();
	        } else {
	        	final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
				    @Override
				    public void run() {
				        removeSplashScreen();
				    }
				}, (int)(SPLASH_TIME - delta));
	        }
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_menu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.menu_preferences:
	    	Intent myIntent = new Intent(TheBACAppActivity.this, PreferencesActivity.class);
	    	TheBACAppActivity.this.startActivity(myIntent);
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	public void onStop() {
	   super.onStop();
	   
	   // End the flurry analytics session
	   FlurryAgent.onEndSession(this);
	}
	
	public void recalculateBAC() {
		
		/* Use the Widmark formula to calculate BAC
		 * BAC = A / (r * W) * 100 - Vt
		 * Where
		 *  BAC = Blood Alcohol Content (%)
		 *  A   = Alcohol consumed (grams). A standard drink has 10g of alcohol
		 *  r   = Widmark Factor, % of body mass that is water ~(0.7 for males, 0.6 for female)
		 *  W   = Body mass (grams)
		 *  V   = Rate at which body eliminates alcohol (%/hr), ~0.015
		 *  t   = Time in hours since started drinking
		 */
		
		if(gender == null) return;
		
		if(standarddrinksValue == null) return;
		String sA = (String) standarddrinksValue.getText();
		if(sA.equals("") || sA == null) return;

		if(massValue == null) return;
		String sW = (String) massValue.getText();
		if(sW.equals("") || sW == null) return;

		if(timeValue == null) return;
		String st = (String) timeValue.getText();
		if(st.equals("") || st == null) return;
		
		try {
			float A = Float.parseFloat(sA) * 10;
			float r = (gender.toLowerCase().equals("male")) ? 0.7f : 0.6f;
			float W = Float.parseFloat(sW.split(MASS_UNIT)[0]) * INV_MASS_CONVERSION * 1000;
			float V = 0.015f;
			float t = Float.parseFloat(st.split("hrs")[0]);
			
			float bac = A / (r * W) * 100 - V*t;
			if(bac <= 0) bac = 0.000f;
			
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMinimumFractionDigits(3);
			nf.setMaximumFractionDigits(3);
			
			TextView output = (TextView) this.findViewById(R.id.bac);
			output.setText(nf.format(bac));
			
			// Set the color of the output text
			float limit = Float.valueOf(settings.getString("pref_bac_limit", "0.05"));
			if(bac >= limit) {
				output.setTextAppearance(getApplicationContext(), R.style.Widget_TextView_BAC_OVER);
			} else {
				output.setTextAppearance(getApplicationContext(), R.style.Widget_TextView_BAC);
			}
			
		} catch (NumberFormatException e) {
			return;
		}
	}

	/**
	 * Recalculates units on all values
	 */
	public void recalculateUnits() {
		final int SI = 1,
				  IMPERIAL = 2;
		
		switch(Integer.valueOf(settings.getString("pref_units", "1"))) {
		case IMPERIAL:
			MASS_CONVERSION = KG_TO_LBS;
			INV_MASS_CONVERSION = LBS_TO_KG;
			MASS_UNIT = "lbs";
			break;
		default:
			MASS_CONVERSION = 1.0f;
			INV_MASS_CONVERSION = 1.0f;
			MASS_UNIT = "kg";
		}

        // Trigger some slider change events
		if(massSlider != null) {
	        massSlider.incrementProgressBy(1);
	        massSlider.incrementProgressBy(-1);
		}

		if(massSlider != null) {
	        standarddrinksSlider.incrementProgressBy(1);
	        standarddrinksSlider.incrementProgressBy(-1);
		}
		
		if(massSlider != null) {
	        timeSlider.incrementProgressBy(1);
	        timeSlider.incrementProgressBy(-1);
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Recalculate units on return from preferences
		recalculateUnits();
		
		// Recalculate the BAC when returning from the preferences activity
		recalculateBAC();
	}
	
	
	@Override
	public Object onRetainNonConfigurationInstance() {
	    MyStateSaver data = new MyStateSaver();
	    
	    // Save any important data here...
	    
	    if(splash_dialog != null) {
	        data.showSplashScreen = true;
	        removeSplashScreen();
	    }
	    return data;
	}
	 
	/**
	 * Removes the Dialog that displays the splash screen
	 */
	protected void removeSplashScreen() {
	    if(splash_dialog != null) {
	    	splash_dialog.dismiss();
	    	splash_dialog = null;
	    	
	        // Show the EULA
	        new SimpleEULA(this).show();
	    }
	}
	 
	/**
	 * Shows the splash screen over the full Activity
	 */
	protected void showSplashScreen() {
		splash_dialog = new Dialog(this, R.style.splash_screen);
		splash_dialog.setContentView(R.layout.splash_screen);
		splash_dialog.setCancelable(false);
		splash_dialog.show();
		
	    // Set fall-back Runnable to remove splash screen after 3 seconds
	    final Handler handler = new Handler();
	    handler.postDelayed(new Runnable() {
		    @Override
		    public void run() {
		        removeSplashScreen();
		    }
	    }, 3000);
	}
	 
	/**
	 * Simple class for storing important data across config changes
	 */
	private class MyStateSaver {
	    public boolean showSplashScreen = false;
	    
	    // Add ant other important, stateful fields here
	}
}

