package com.example.flightsearch;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.lang.*;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech;   
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
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
public class MainActivity extends Activity implements OnClickListener, OnInitListener{
	
	private static final int VR_REQUEST = 1234;
    private int MY_DATA_CHECK_CODE = 0;
    private TextToSpeech repeatTTS; 
	private ListView wordList;
	private final String LOG_TAG = "MainActivity";
	
	boolean [] flagVar = new boolean[10];
	
	String destination;
	String destination1;
	String origin;
	String time;
	String response;
	int connCount=0;
	ArrayList <String> matches;
	int initDialogueFlag = 0;
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
        List<ResolveInfo> intActivities = packManager.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
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
        else {
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
               	
            }
        });
    }
    
    public void speakCall(String wordChosen){
    	
    	String text = wordChosen;
    	//text = matches.get(0).toString();s
    	if (text!=null) {
            if (!repeatTTS.isSpeaking()) {
                repeatTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }    	
    }
    
    
    
    int initialDialogueCount=0;
    
    public void initialDialogue(){
		ttsCheck();
    	String text = "Hello, Welcome to Flight Search Dialogue System! How may I help you today";
		repeatTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		if (text!=null) {
            if (!repeatTTS.isSpeaking()) {
                repeatTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                initDialogueFlag=1;
            }
        }
		ttsCheck();
    }    
	/**
     * Called when the user presses the speak button
     */
    
    int tempVar =0;
    
    public void onClick(View v) {
    		//initDialogueThread.start();
    		if(initDialogueFlag == 0)
    			initialDialogue();
    		if(initDialogueFlag == 0)
    			initialDialogue();
    		listenToSpeech();
    		tempVar++;
			new AsyncTaskActivity().execute("http://54.201.35.119:8000/rawParser/origin/raleigh");
			//Toast.makeText(MainActivity.this,response, Toast.LENGTH_SHORT).show();
    }
    
    /* HTTP Connection */
    
	private class AsyncTaskActivity extends AsyncTask<String,Void,String>{

		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			
			
			String url = params[0];
			
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url);
			try{
				
				HttpResponse execute = client.execute(httpGet);
				InputStream content = execute.getEntity().getContent();
				
				BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
				
				String temp;
				while((temp = buffer.readLine())!=null){
					response=temp;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			
			return response.toString();
		}
	}
    
    
    
    /**
     * Instruct the app to listen for user speech input
     */
    private void listenToSpeech() {
    	
    	ttsCheck();	
    	//start the speech recognition intent passing required data
    	Intent listenIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    	//indicate package
    	listenIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
    	//message to display while listening
    	listenIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a word!");
    	//set speech model
    	listenIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    	//specify number of results to retrieve
    	listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

    	//start listening
        startActivityForResult(listenIntent, VR_REQUEST);      
    }
    
    /**
     * onActivityResults handles:
     *  - retrieving results of speech recognition listening
     *  - retrieving result of TTS data check
     */
    
    String text;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	//check speech recognition result 
        if (requestCode == VR_REQUEST && resultCode == RESULT_OK) 
        {
        	//store the returned word list as an ArrayList
            matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //set the retrieved list to display in the ListView using an ArrayAdapter
            wordList.setAdapter(new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1, matches));
            
            text = matches.get(0).toString();
            ttsCheck();
            
            if(text.indexOf("yes")>0)
            	speakCall("you said "+text);
            else{
            	speakCall("you did not say yes");
            }
           
            ttsCheck();
            listenToSpeech();
            
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
    
    public void ttsCheck(){
    
    	boolean speakingEnd = repeatTTS.isSpeaking();
		do{
		   speakingEnd = repeatTTS.isSpeaking();
		} while (speakingEnd);
		
    }

}