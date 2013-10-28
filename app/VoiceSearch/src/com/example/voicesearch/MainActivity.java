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
	 
	    /**
	     * Called with the activity is first created.
	     */
	    @Override
	    public void onCreate(Bundle savedInstanceState)
	    {
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
	        startVoiceRecognitionActivity();
	    }
	 
	    /**
	     * Fire an intent to start the voice recognition activity.
	     */
	    private void startVoiceRecognitionActivity()
	    {
	        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
	        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
	        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Voice recognition Demo...");
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
				String test = "";
				for(int i=0;i<matches.size();i++){
					test+=":";
					test += matches.get(i).toString();
					test+=":";
				}
//				tts.speak(test, TextToSpeech.QUEUE_FLUSH, null);
				//String text = matches.get(0);
					if(test!=null){
							if(!tts.isSpeaking()){
									tts.speak(test, TextToSpeech.QUEUE_FLUSH,null);
								}							
					}
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
