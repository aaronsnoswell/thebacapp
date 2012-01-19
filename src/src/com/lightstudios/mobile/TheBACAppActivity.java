package com.lightstudios.mobile;

import java.text.NumberFormat;
import java.util.Date;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
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
	
	String gender;
	TextView massValue;
	TextView standarddrinksValue;
	TextView timeValue;
	
	float limit = 0.05f;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    // Store start time to determine how long to show the splash screen for
	    final long start_time = new Date().getTime();
	    
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
	        SeekBar massSlider = (SeekBar) (this.findViewById(R.id.mass_slider));
	        massSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					massValue.setText(String.valueOf(progress*2+40)+"kg");
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
	        SeekBar standarddrinksSlider = (SeekBar) (this.findViewById(R.id.standarddrinks_slider));
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
	        SeekBar timeSlider = (SeekBar) (this.findViewById(R.id.time_slider));
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

	        /* XXX ajs 27/12/11 The client likes to see the splash screen for a bit,
	         * even if we finish loading early... This makes me cry a bit inside :(
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
			float W = Float.parseFloat(sW.split("kg")[0]) * 1000;
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
			//if(bac >= limit) output.setTextColor(colors)
			
		} catch (NumberFormatException e) {
			return;
		}
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

