package com.example.voicesearch;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.List;
import android.view.Menu;
import java.util.Locale;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener, OnInitListener{

	 	private static final int REQUEST_CODE = 1234;
	    private ListView wordsList;
	    private TextToSpeech tts;
	    ArrayList <String> matches;
	    boolean [] flagVar = new boolean[10];
	    /**
	     * Called with the activity is first created.
	     */
	    @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
	    	for(int i=0;i<10;i++)
	    		flagVar[i] = false;
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.activity_main);
	        
	        tts = new TextToSpeech(this,this);
	        findViewById(R.id.listenButton).setOnClickListener(this);
	 
	        Button speakButton = (Button) findViewById(R.id.speakButton);
	 
	        wordsList = (ListView) findViewById(R.id.list);
	 
	        // Disable button if no recognition service is present
	        PackageManager pm = getPackageManager();
	        List<ResolveInfo> activities = pm.queryIntentActivities(
	                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
	        if (activities.size() == 0)
	        {
	            speakButton.setEnabled(false);
	            speakButton.setText("Recognizer not present");
	        }
	    }
	 
	    /**
	     * Handle the action of the button being clicked
	     */
	    public void speakButtonClicked(View v)
	    {
	        //tts.stop();
	        //tts.shutdown();
			if(flagVar[0] == false){
				String test="";
				test+=":";
				test+="Welcome to Flight Search!!";
				test+=":";
				test+="Where do you want to fly to?";
				if(test!=null){
					tts.speak(test, TextToSpeech.QUEUE_FLUSH,null);				
				}
			}
			flagVar[0]=true;	      
			startVoiceRecognitionActivity();
	    }
	 
	    /**
	     * Fire an intent to start the voice recognition activity.
	     */
	    private void startVoiceRecognitionActivity()
	    {
	    	boolean speakingEnd = tts.isSpeaking();
	    	
			do{
			   speakingEnd = tts.isSpeaking();
			} while (speakingEnd);	
			
	    	Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
	        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
	        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Flight Search");
	        startActivityForResult(intent, REQUEST_CODE);
	    }
	 
	    /**
	     * Handle the results from the voice recognition activity.
	     */
	    @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data)
	    {
	        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
	        {
	            // Populate the wordsList with the String values the recognition engine thought it heard
	            matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
	            wordsList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,matches));
	        }
	        super.onActivityResult(requestCode, resultCode, data);
	    }

		@Override
		public void onInit(int status) {
			if(status == TextToSpeech.SUCCESS){
				tts.setLanguage(Locale.getDefault());
			}
			else{
				tts=null;
				Toast.makeText(this,"Failed to initialize TTS engine.",Toast.LENGTH_SHORT).show();
			}
			
		}

		@Override
		public void onClick(View v) {
			if(tts!=null){
				if(flagVar[0]==true && flagVar[1]==false){
					String test = "";
						test+=":";
						test+="You want to fly to ";
						test += matches.get(0).toString();
						test+=":";
						test+=":";
						test+=":";
						test+="Is that correct?";
						test+=":";
						test+=":";
						test+="Please say yes or no";
					if(test!=null){
						if(!tts.isSpeaking()){
							tts.speak(test, TextToSpeech.QUEUE_FLUSH,null);
						}							
					}
				}
				flagVar[1]=true;
				
				boolean speakingEnd = tts.isSpeaking();
				do{
				   speakingEnd = tts.isSpeaking();
				} while (speakingEnd);	
				
				startVoiceRecognitionActivity();
				
				try{
					Thread.sleep(6000);
				}catch(InterruptedException e){
					System.out.println("Error");
				}
				
				if(flagVar[0]==true && flagVar[1]==true && flagVar[2]==false && matches.get(0).toString()=="yes"){
					String test = "";
					//for(int i=0;i<matches.size();i++){
						test+=":";
						test+="Where do you want to fly from ";
						test+=":";
						test+=":";					//}
	//				tts.speak(test, TextToSpeech.QUEUE_FLUSH, null);
					//String text = matches.get(0);
					if(test!=null){
						if(!tts.isSpeaking()){
							tts.speak(test, TextToSpeech.QUEUE_FLUSH,null);
						}							
					}
				}
				flagVar[2]=true;
			}
		}
		
		protected void onDestroy(){
			if(tts!=null){
				tts.stop();
				tts.shutdown();
			}
			super.onDestroy();
		}
}
