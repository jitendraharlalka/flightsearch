package com.example.hackdukevoicetest;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.lang.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.*;

/**
 * SpeechRepeatActivity
 * - processes speech input 
 * - presents user with list of suggested words
 * - when user selects a word from the list, the app speaks the word back using the TTS engine 
 */
public class MainActivity extends Activity implements OnClickListener, OnInitListener,RecognitionListener{
	
	private static final int VR_REQUEST = 1234;
    private int MY_DATA_CHECK_CODE = 0;
    private TextToSpeech repeatTTS; 
	private ListView wordList;

	int flagVar[] = new int[4];
	String eventRetStr="",dateRetStr="",timeRetStr="";
	
	SQLiteDatabase dbObj = null;
    
    String TableName = "events";
    
    String text="";
	
	int connCount=0;
	ArrayList <String> matches;
	int initDialogueFlag = 0;
	String response;
	String dbResponse="";
	
	public long dateTimeVal;
	
	public HashMap<String,String[]> globalFrame;
	public HashMap<String,String[]> conflictFrame;
	
	/* speech recognizer variables */
	
	private SpeechRecognizer speechRecognizer;
	
	
    /** Create the Activity, prepare to process speech and repeat */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
    	super.onCreate(savedInstanceState);
    	for(int i=0;i<4;i++)
    		flagVar[i] = 0;
    	
    	
    	
    	// initializing the global data frame
		
    	globalFrame = new HashMap<String,String[]>();
    	
    	globalFrame.put("DESTINATION", new String[] {"-1"});
    	globalFrame.put("ORIGIN", new String[] {"-1"});
    	globalFrame.put("DATE", new String[] {"-1"});
    	globalFrame.put("TIME", new String[] {"-1"});
    	
    	Iterator globalHashIterator = globalFrame.keySet().iterator();
		while(globalHashIterator.hasNext()){
			String[] values = globalFrame.get(globalHashIterator.next());
			for(int i=0;i<values.length;i++){
				//Toast.makeText(MainActivity.this,values[i], Toast.LENGTH_SHORT).show();
			}
		}
		
    	
		// initializing conflict frame
		
		conflictFrame = new HashMap<String,String[]>();
    	
    	conflictFrame.put("DESTINATION", new String[] {"-1"});
    	conflictFrame.put("ORIGIN", new String[] {"-1"});
    	conflictFrame.put("DATE", new String[] {"-1"});
    	conflictFrame.put("TIME", new String[] {"-1"});
    	
    	/*
    	 * Iterator conflictHashIterator = conflictFrame.keySet().iterator();
		while(conflictHashIterator.hasNext()){
			String[] values = conflictFrame.get(conflictHashIterator.next());
			for(int i=0;i<values.length;i++){
				Toast.makeText(MainActivity.this,values[i], Toast.LENGTH_SHORT).show();
			}
		}
		*/
		
    	//call superclass
        super.onCreate(savedInstanceState);
        //set content view
        setContentView(R.layout.activity_main);

        //gain reference to speak button
        Button speechBtn = (Button) findViewById(R.id.speechButton);
        //gain reference to word list
        wordList = (ListView) findViewById(R.id.wordList);
        
        // speech recognizer 
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        speechRecognizer.setRecognitionListener(this);
        
        //database entries

        // database creation
        
       
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
           	
            	//dialogueControl(wordChosen);
               	
            }
        });
    }
    
    
    public void speakCall(String wordChosen){
    	
    	String text = wordChosen;
    	//text = matches.get(0).toString();
    	if (text!=null) {
            if (!repeatTTS.isSpeaking()) {
                repeatTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }    	
    }
    
    int initialDialogueCount=0;
    
    public void initialDialogue(){
		ttsCheck();
    	String text = "Hello, How may I help you today";
		repeatTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
		if (text!=null) {
            if (!repeatTTS.isSpeaking()) {
                repeatTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
		ttsCheck();
		initDialogueFlag = 1;
    }

    /* Background thread for initial dialogue */
    
    Thread initDialogueThread = new Thread(){
    	@Override
    	public void run(){
    		try{
    			initialDialogue();
    			sleep(1000);
    		}catch(InterruptedException e){
    			e.printStackTrace();
    		}
    	}
    };
    
    
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
			new AsyncTaskActivity().execute("http://www.anantagarwal.in/testscript.php");
			//Toast.makeText(MainActivity.this,response, Toast.LENGTH_SHORT).show();
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
    	listenIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);

    	//start listening
        startActivityForResult(listenIntent, VR_REQUEST);      
    }
    
    
    
    
    /**
     * onActivityResults handles:
     *  - retrieving results of speech recognition listening
     *  - retrieving result of TTS data check
     */
    
    String t=""; public int count=0;
    ArrayList<String> displayList = new ArrayList<String>();
    ArrayList<String> finalList = new ArrayList<String>();
    @Override
   
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	//check speech recognition result 
        if (requestCode == VR_REQUEST && resultCode == RESULT_OK) 
        {
        	//store the returned word list as an ArrayList
            matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            
            /* temp code for TTS */
            text = matches.get(0).toString();
            
            ttsCheck();
            	/*send utterance to server*/
            	speakCall(text);
            	try{
            		String modText = text+" .";
            		String url = modText.replaceAll(" ", "%20");
            		//new AsyncTaskActivity().execute("http://www.anantagarwal.in/testscript.php");
            		new AsyncTaskActivity().execute("http://54.201.35.119:8000/rawParser/tag/"+url);
            		ttsCheck();
            		Thread.sleep(3000);
            		String outText = response.toString().replaceAll("'", "\"");
            		Toast.makeText(MainActivity.this,"first query result"+response, Toast.LENGTH_SHORT).show();
            		wordList.setAdapter(new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1, displayList));
            		HashMap<String,String[]> hashMapObj = jsonParser(outText);
            		
            		//Thread.sleep(3000);
            		// test to check whether hash map is being built or not.
            		/*	Iterator hashIterator = globalFrame.entrySet().iterator();
            			HashMap.Entry entry = (HashMap.Entry)hashIterator.next();
            			String[] values = (String[])entry.getValue();
            			String key = (String)entry.getKey();
            			if(values[0].toString()=="-1" && key!="TIME" && key=="DATE" && flagVar[2]==0){
            				
            				Toast.makeText(MainActivity.this,"global frame iterator", Toast.LENGTH_SHORT).show();
            				speakCall("Ok, got it!!"+"On what date do you want to fly on!!");
            				ttsCheck();
            				listenToSpeech();
            				entry = (HashMap.Entry)hashIterator.next();
            			}
            			else if(values[0].toString()=="-1" && key!="TIME" && key=="ORIGIN" && flagVar[0]==0){
            				
            				Toast.makeText(MainActivity.this,"global frame iterator", Toast.LENGTH_SHORT).show();
            				speakCall("Ok, got it!!"+"Do you want to fly from Raleigh or some other city!!");
            				ttsCheck();
            				listenToSpeech();
            				//hashIterator = globalFrame.entrySet().iterator();
            				entry = (HashMap.Entry)hashIterator.next();
            			}
            			else if(values[0].toString()=="-1" && key!="TIME" && key=="DESTINATION" && flagVar[1]==0){
            				
            				Toast.makeText(MainActivity.this,"global frame iterator", Toast.LENGTH_SHORT).show();
            				speakCall("Ok, got it!!"+"Where do you want to fly to?");
            				ttsCheck();
            				listenToSpeech();
            				//hashIterator = globalFrame.entrySet().iterator();
            				entry = (HashMap.Entry)hashIterator.next();
            			}
            		*/
            		
            		//speakCall(response.toString());
                	//ttsCheck();
            	}catch(Exception e){
            		Toast.makeText(MainActivity.this,e.toString(), Toast.LENGTH_SHORT).show();
            	}
            	
            
            if(count>=3){
            	

            	
            	new HttpAsyncTask().execute("http://54.201.35.119:8000/rawParser/search/");
            	
            	try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	
            	Log.v("custom", tempStr);
            	
            	dbParser(tempStr);
            	
            	//Toast.makeText(getBaseContext(), dbResponse.toString(), Toast.LENGTH_LONG).show();
            	wordList.setAdapter(new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1, finalList));
            	
            	//HashMap<String,String[]> hashMapObj = jsonParser(tempStr);
            	
            	/*speakCall("Thank You for your inputs. We are quering your results...");
            	ttsCheck();
            	t = mainObj.toString();
            	new AsyncTaskActivity().execute("http://54.201.35.119:8000/rawParser/search/"+t);
            	try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	Toast.makeText(MainActivity.this,"db query" + response, Toast.LENGTH_SHORT).show();*/
            }
            
            speakCall("");
            ttsCheck();
            
            try {
				Thread.sleep(700);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            //listenToSpeech();
            	
            
            //set the retrieved list to display in the ListView using an ArrayAdapter
            //wordList.setAdapter(new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1, matches));

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
    public void dbParser(String jsonString){
    	
    	try {
    		JSONArray flightData = new JSONArray(jsonString);
    		Log.v("custom","created JSONArray");
    		for(int i=0; i<flightData.length();i++){
				JSONObject flightObj = flightData.getJSONObject(i);
				JSONObject flightFields = flightObj.getJSONObject("fields");
				Iterator keys = flightFields.keys();
				FlightDetails flightDetailObj =  new FlightDetails();
				
				while(keys.hasNext()){
					
					Method method = null;
					String currentKey = (String)keys.next();
					String fieldVal = flightFields.get(currentKey).toString();
					Log.v("custoom",currentKey);
					Log.v("custom",fieldVal);
					String methodName = "set"+currentKey;
					
					try{
						method = flightDetailObj.getClass().getMethod(methodName, String.class);
					}catch(Exception e){
						Log.v("cusotm",methodName);
						Log.v("cusotm","method not defined");
					}
					try {
						method.invoke(flightDetailObj,fieldVal);
						Log.v("cusotm","method invokded");
					} catch (IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				finalList.add(flightDetailObj.toString());
				Log.v("custom",flightDetailObj.toString());
    		}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	
    }

    JSONObject mainObj; 
    public HashMap<String, String[]>jsonParser(String jsonString){
    	t = jsonString;
    	HashMap<String,String[]> hashMapObj = new HashMap<String,String[]>();
    	
    	try {
			mainObj = new JSONObject(jsonString);
			Iterator keys = mainObj.keys();
			while(keys.hasNext()){
				ArrayList<String> values=new ArrayList<String>();
				String currentKey = (String)keys.next();
				JSONArray jsonArray = mainObj.getJSONArray(currentKey);
				if(jsonArray !=null){
					int len=jsonArray.length();
					for(int i=0;i<len;i++){
						values.add(jsonArray.get(i).toString());
						//speakCall(jsonArray.get(i).toString());
					}
				}
				String[] vals=values.toArray(new String[values.size()]);
				//speakCall(vals.toString());
				Arrays.sort(vals);
//				for(int i=0;i<vals.length;i++)
//					Toast.makeText(MainActivity.this,currentKey+" "+" "+ vals[i]+" sort", Toast.LENGTH_SHORT).show();
//				
				hashMapObj.put(currentKey, vals);
				String[] mapValue = globalFrame.get(currentKey);
				//Arrays.sort(mapValue);
				
				if(mapValue[0].toString() == "-1"){
					//Toast.makeText(MainActivity.this,"adding value "+count, Toast.LENGTH_SHORT).show();
					if(currentKey.toString() == "DESTINATION" || currentKey.toString() == "ORIGIN"){
						globalFrame.put(currentKey, vals);
						String disp = currentKey+" : ";
						String disp1="";
						for(int i=0;i<vals.length;i++)
							disp1 = disp1 + vals[i] + ",";
						
						if(currentKey=="DESTINATION"){
							flagVar[0]=1;
						}
						if(currentKey=="ORIGIN"){
							flagVar[1]=1;
						}
						
//						if(disp1.endsWith(","))
//						{
//						  disp1 = disp1.substring(0,disp1.length() - 1);
//						}

						disp.concat(disp1);
						
						//Toast.makeText(MainActivity.this,disp, Toast.LENGTH_SHORT).show();
						displayList.add(disp);
					}else{
						if(currentKey=="TIME"){
							flagVar[2]=1;
						}else if(currentKey=="DATE"){
							flagVar[3]=1;
						}
						globalFrame.put(currentKey, vals);
						displayList.add(currentKey+" : "+vals[0]);
						//Toast.makeText(MainActivity.this,currentKey +":"+ vals[0], Toast.LENGTH_SHORT).show();
					}
					
					count++;
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	wordList.setAdapter(new ArrayAdapter<String> (this, android.R.layout.simple_list_item_1, displayList));
    	return hashMapObj;
    }
    
    /* POST */
    static StringEntity tempEnt;
    static String tempStr;
    static String jsonStr;
    public static String POST(String url,JSONObject obj){
    	InputStream inputStream = null;
    	String result = "";
    	try{
    		
    		HttpClient httpClient = new DefaultHttpClient();
    		HttpPost httpPost = new HttpPost(url);
    		
    		String json = "";
    		
    		json = obj.toString();
    		jsonStr = json;
    		StringEntity se = new StringEntity(json);
    		httpPost.setEntity(se);
    		tempEnt=se;
    		//Toast.makeText(MainActivity.this,"Yo "+se, Toast.LENGTH_SHORT).show();
    		
    		httpPost.setHeader("Accept", "application/json");
    		httpPost.setHeader("Content-type","application/json");
    		
    		HttpResponse httpResponse = httpClient.execute(httpPost);
    		
    		inputStream = httpResponse.getEntity().getContent();
    		
    		if(inputStream!=null){
    			result = convertInputStreamToString(inputStream);
    			tempStr = result;
    		}
    		else
    			result = "Did not work!";
    	}catch(Exception e){
    		
    	}
    	
    	return result;
    }
    
    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
 
        inputStream.close();
        return result;
 
    } 
    
    /* json http connection */
    
    
    private class HttpAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
        	//Toast.makeText(getBaseContext(), tempStr, Toast.LENGTH_LONG).show();
            
        	dbResponse = POST(urls[0],mainObj);
        	Log.v("custom", jsonStr);
            
        	return dbResponse;
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
        	
            Toast.makeText(getBaseContext(), "Data Sent!", Toast.LENGTH_LONG).show();
            
       }
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
    



	@Override
	public void onBeginningOfSpeech() {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onBufferReceived(byte[] buffer) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onEndOfSpeech() {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onError(int error) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onEvent(int eventType, Bundle params) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onPartialResults(Bundle partialResults) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onReadyForSpeech(Bundle params) {
		// TODO Auto-generated method stub
		
	}




	@Override
	public void onResults(Bundle results) {
		// TODO Auto-generated method stub
		ArrayList strList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
		
	}




	@Override
	public void onRmsChanged(float rmsdB) {
		// TODO Auto-generated method stub
		
	}
    
	
	public class FlightDetails {
		public String getcost() {
			return cost;
		}
		public void setcost(String cost) {
			this.cost = cost;
		}
		public String getorigin() {
			return origin;
		}
		public void setorigin(String origin) {
			this.origin = origin;
		}
		public String getdestination() {
			return destination;
		}
		public void setdestination(String destination) {
			this.destination = destination;
		}
		public String getstarttime() {
			return startTime;
		}
		public void setstarttime(String startTime) {
			this.startTime = startTime;
		}
		public String getendtime() {
			return endTime;
		}
		public void setendtime(String endTime) {
			this.endTime = endTime;
		}
		public String getdepartdate() {
			return departDate;
		}
		public void setdepartdate(String departDate) {
			this.departDate = departDate;
		}
		public String getairline() {
			return airline;
		}
		public void setairline(String airline) {
			this.airline = airline;
		}
		public String getstops() {
			return stops;
		}
		public void setstops(String stops) {
			this.stops = stops;
		}
		private String cost;
		private String origin;
		private String destination;
		private String startTime;
		private String endTime;
		private String departDate;
		private String airline;
		private String stops;
		
		public String toString(){
			
			String retDetail = "Cost : "+cost+"$\nOrigin : "+origin+"\nDestination : "+destination+"\nDeparture Date : "+ departDate+"\nDeparture Time : "+startTime+"\nArrival Time : "+endTime+"\nAirline : "+airline+"\nStops : "+stops;
			
			return retDetail;
		}
	}
	
}













