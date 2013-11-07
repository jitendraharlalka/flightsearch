package com.example.flightsearch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale; 

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech;   
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

/**
 * SpeechRepeatActivity
 * - processes speech input 
 * - presents user with list of suggested words
 * - when user selects a word from the list, the app speaks the word back using the TTS engine 
 */
public class MainActivity extends Activity implements OnClickListener, OnInitListener {
	
	//variable for checking Voice Recognition support on user device
	private static final int VR_REQUEST = 999;
	
	//variable for checking TTS engine data on user device
    private int MY_DATA_CHECK_CODE = 0;
    
    //Text To Speech instance
    private TextToSpeech repeatTTS; 
	
    //ListView for displaying suggested words
	private ListView wordList;
	
	//Log tag for output information
	private final String LOG_TAG = "MainActivity";
	
	boolean [] flagVar = new boolean[10];
	
	String destination;
	String origin;
	String time;
    /** Create the Activity, prepare to process speech and repeat */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	for(int i=0;i<10;i++)
    		flagVar[i]=false;
    	
    	//call superclass
        super.onCreate(savedInstanceState);
        //set content view
        setContentView(R.layout.activity_main);

        //gain reference to speak button
        Button speechBtn = (Button) findViewById(R.id.speechButton);
        //gain reference to word list
        wordList = (ListView) findViewById(R.id.wordList);
        
        //find out whether speech recognition is supported
        PackageManager packManager = getPackageManager();
        List<ResolveInfo> intActivities = packManager.queryIntentActivities
        		(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (intActivities.size() != 0) {
        	//speech recognition is supported - detect user button clicks
            speechBtn.setOnClickListener(this);
            //prepare the TTS to repeat chosen words
            Intent checkTTSIntent = new Intent();  
            //check TTS data  
            checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);  
            //start the checking Intent - will retrieve result in onActivityResult
            startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE); 
        }
        else 
        {
        	//speech recognition not supported, disable button and output message
            speechBtn.setEnabled(false);
            Toast.makeText(this, "Oops - Speech recognition not supported!", Toast.LENGTH_LONG).show();
        }
        
        //detect user clicks of suggested words
        wordList.setOnItemClickListener(new OnItemClickListener() {
        	
        	//click listener for items within list
        	public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
            {
            	
    	    	boolean speakingEnd = repeatTTS.isSpeaking();
    			do{
    			   speakingEnd = repeatTTS.isSpeaking();
    			} while (speakingEnd);	
        		
        		//cast the view
            	TextView wordView = (TextView)view;
            	//retrieve the chosen word
            	String wordChosen = (String) wordView.getText();
            	//output for debugging
            	Log.v(LOG_TAG, "chosen: "+wordChosen);
               	
            	if(flagVar[7] == false && flagVar[6] == true && flagVar[5]==true && flagVar[0]==true && flagVar[1]==true && (flagVar[2]==true  || (flagVar[3]==true && flagVar[4]==true)) && (wordChosen.indexOf("yes")>=0 || wordChosen.indexOf("Yes")>=0)){
            		
            		String output = "Ok, showing all flights from "+origin + " to " + destination +" in " + time +":";
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "showing all flights from "+origin + " to " + destination +" in " + time, Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[7] = true;
            		
            	}
            	
            	
            	if(flagVar[6] == false && flagVar[5]==true && flagVar[0]==true && flagVar[1]==true && (flagVar[2]==true || (flagVar[3]==true && flagVar[4]==true))){
            		
            		String output = "You want to fly in the "+wordChosen+": Is that correct, again please say yes or no? ";
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "You want to fly in "+wordChosen, Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[6] = true;
            		time = wordChosen;
            		
            	}
            	
            	if(flagVar[5]==false && flagVar[0]==true && flagVar[1]==true && ((flagVar[3]==true && flagVar[4]==true  && (wordChosen.indexOf("yes")>=0 || wordChosen.indexOf("Yes")>=0)) || flagVar[2]==true)){
            		
            		String output = "Ok, at what time do you want to fly from: "+origin+" :to "+destination;
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "You want to fly from "+origin+" to "+destination, Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[5] = true;
            		
            	}
            	if(flagVar[4]==false && flagVar[0]==true && flagVar[1]==true && flagVar[3]==true){
            		
            		String output = "Ok, you want to fly from "+wordChosen+":Is that correct? Please say yes or no?";
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "Fly From", Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[4] = true;
            		origin = wordChosen;
            	}
            	if(flagVar[3]==false && flagVar[2]==false && flagVar[0]==true && flagVar[1]==true && (wordChosen.indexOf("no")>=0 || wordChosen.indexOf("No")>=0)){
            		
            		String output = "Ok, where do you want to fly from";
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "Ask Destination", Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[3] = true;
            	}
            	
            	if((flagVar[2]==false && flagVar[3]==false) && flagVar[0]==true && flagVar[1]==true && (wordChosen.indexOf("yes")>=0 || wordChosen.indexOf("Yes")>=0)){
            		
            		String output = "Ok, at what time do you want to fly from"+origin+"to"+destination;
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "You want to fly from "+origin+" to "+destination, Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[2] = true;
            		origin = "Raleigh";
            	}
            	
            	if(flagVar[1]==false && flagVar[0]==true){
            		String tempStr="Raleigh";
            		String output = "Ok, and do you want to fly from "+tempStr+"?Please say yes or no";
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "You want to fly from  "+tempStr, Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[1] = true;
            		origin = "Raleigh";
            	}
            	
            	if(flagVar[0]==false){
            		//speak the word using the TTS
            		String output = ":"+":"+":You want to fly to "+wordChosen+":Is that correct? Please say yes or no:";
            		destination = wordChosen;
            		repeatTTS.speak(output, TextToSpeech.QUEUE_FLUSH, null);
            		//output Toast message
            		Toast.makeText(MainActivity.this, "You want to fly to  "+wordChosen, Toast.LENGTH_SHORT).show();//**alter for your Activity name***
            		flagVar[0] = true;
            	}
            	
 
            }
            	
        });
        
        
        /* Location Manager */
        
     // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
              // Called when a new location is found by the network location provider.
              makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
          
        };
        
          String locationProvider = LocationManager.NETWORK_PROVIDER;
          
          locationManager.requestLocationUpdates(locationProvider, 10000, 500, locationListener);
          
          Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
          
          //Toast.makeText(MainActivity.this, lastKnownLocation.toString(), Toast.LENGTH_LONG).show();
        
    
  		
    
    }
    
    protected void makeUseOfNewLocation(Location location) {
		// TODO Auto-generated method stub
		
	}

	/**
     * Called when the user presses the speak button
     */
    public void onClick(View v) {
    	if (v.getId() == R.id.speechButton) {
    		repeatTTS.speak("", TextToSpeech.QUEUE_FLUSH, null);
    		repeatTTS.speak("Welcome to Flight Search:", TextToSpeech.QUEUE_FLUSH, null);
    		repeatTTS.speak(":", TextToSpeech.QUEUE_FLUSH, null);
    		repeatTTS.speak(":", TextToSpeech.QUEUE_FLUSH, null);
    		repeatTTS.speak("Where do you want to fly to?", TextToSpeech.QUEUE_FLUSH, null);
    		repeatTTS.speak(":", TextToSpeech.QUEUE_FLUSH, null);
    		repeatTTS.speak(":", TextToSpeech.QUEUE_FLUSH, null);
    		//listen for results
	    	boolean speakingEnd = repeatTTS.isSpeaking();
			do{
			   speakingEnd = repeatTTS.isSpeaking();
			} while (speakingEnd);	
    		listenToSpeech();
        }
    }
    
    /**
     * Instruct the app to listen for user speech input
     */
    private void listenToSpeech() {
    	
    	//start the speech recognition intent passing required data
    	Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    	//indicate package
    	listenIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
    	//message to display while listening
    	listenIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a word!");
    	//set speech model
    	listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    	//specify number of results to retrieve
    	listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);

    	//start listening
        startActivityForResult(listenIntent, VR_REQUEST);
    }
    
    /**
     * onActivityResults handles:
     *  - retrieving results of speech recognition listening
     *  - retrieving result of TTS data check
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	//check speech recognition result 
        if (requestCode == VR_REQUEST && resultCode == RESULT_OK) 
        {
        	//store the returned word list as an ArrayList
            ArrayList<String> suggestedWords = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //set the retrieved list to display in the ListView using an ArrayAdapter
            wordList.setAdapter(new ArrayAdapter<String> (this, R.layout.words, suggestedWords));
        }
        
        //returned from TTS data check
        if (requestCode == MY_DATA_CHECK_CODE) 
        {  
	        //we have the data - create a TTS instance
	        if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)  
	        	repeatTTS = new TextToSpeech(this, this);  
	        //data not installed, prompt the user to install it  
	        else 
	        {  
	        	//intent will take user to TTS download page in Google Play
	        	Intent installTTSIntent = new Intent();  
	        	installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);  
	        	startActivity(installTTSIntent);  
	        }  
        }

        //call superclass method
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    /**
     * onInit fires when TTS initializes
     */
    public void onInit(int initStatus) { 
    	//if successful, set locale
    	 if (initStatus == TextToSpeech.SUCCESS)   
    	  repeatTTS.setLanguage(Locale.US);//***choose your own locale here***
    	   
    }
}