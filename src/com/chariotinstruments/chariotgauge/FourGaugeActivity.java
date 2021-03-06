package com.chariotinstruments.chariotgauge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.view.ViewManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class FourGaugeActivity extends Activity implements Runnable{
	GaugeBuilder analogGauge1;
	GaugeBuilder analogGauge2;
	GaugeBuilder analogGauge3;
	GaugeBuilder analogGauge4;
	MultiGauges  multiGauge1;
	MultiGauges  multiGauge2;
	MultiGauges  multiGauge3;
	MultiGauges  multiGauge4;
    ImageButton  btnOne;
    ImageButton	 btnTwo;
    Typeface	 typeFaceDigital;
    TextView 	 txtViewDigital;
    TextView 	 txtViewDigital2;
    TextView 	 txtViewDigital3;
    TextView 	 txtViewDigital4;
    int		     digitalToken;
    String	 	 currentMsg;
    Thread		 thread;
	
    float 	     flt;
    int			 minValue; //gauge min.
    int			 maxValue; //gauge max.
    double		 sensorMinValue; //the lowest value that has been sent from the arduino.
    double		 sensorMaxValue; //the highest value that has been sent from the arduino.
    boolean		 paused;
    Context		 context;
    float		 boostSValue;
    float		 wbSValue;
    float		 tempSValue;
    float		 oilSValue;
    
    //Prefs vars
    View		 root;
    boolean		 showAnalog; //Display the analog gauge or not.
    boolean		 showDigital; //Display the digital gauge or not.
    boolean		 showNightMode; //Change background to black.
    String		 gaugeOnePref;
    String		 gaugeTwoPref;
    int			 currentTokenOne = 1;
    int			 currentTokenTwo = 2;
    
    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ         = 2;
    public static final int MESSAGE_WRITE        = 3;
    public static final int MESSAGE_DEVICE_NAME  = 4;
    public static final int MESSAGE_TOAST        = 5;
    
    //Test
    
    // Key names received from the BluetoothChatService Handler
    public static final String TOAST        = "toast";
    private static final int BOOST_TOKEN 	= 1;
    private static final int WIDEBAND_TOKEN = 2;
    private static final int TEMP_TOKEN 	= 3;
    private static final int OIL_TOKEN 		= 4;
    
    BluetoothSerialService mSerialService;
     
    @Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.gauge_layout_4);
	    getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	    prefsInit(); //Load up the preferences.
	    context = this;
	    
	    //Instantiate the GaugeBuilder.
	    analogGauge1	= (GaugeBuilder) findViewById(R.id.analogGauge);
	    analogGauge2	= (GaugeBuilder) findViewById(R.id.analogGauge2);
	    analogGauge3	= (GaugeBuilder) findViewById(R.id.analogGauge3);
	    analogGauge4	= (GaugeBuilder) findViewById(R.id.analogGauge4);
	    multiGauge1 	= new MultiGauges(context);
	    multiGauge2 	= new MultiGauges(context);
	    multiGauge3 	= new MultiGauges(context);
	    multiGauge4 	= new MultiGauges(context);
	    txtViewDigital 	= (TextView) findViewById(R.id.txtViewDigital);
	    txtViewDigital2 = (TextView) findViewById(R.id.txtViewDigital2);
	    txtViewDigital3 = (TextView) findViewById(R.id.txtViewDigital3);
	    txtViewDigital4 = (TextView) findViewById(R.id.txtViewDigital4);
        btnOne			= (ImageButton) findViewById(R.id.btnOne);
        btnTwo			= (ImageButton) findViewById(R.id.btnTwo);  
        typeFaceDigital	= Typeface.createFromAsset(getAssets(), "fonts/LetsGoDigital.ttf");
        digitalToken	= 1;
        
        //Set the font of the digital.
        txtViewDigital.setTypeface(typeFaceDigital);
        txtViewDigital2.setTypeface(typeFaceDigital);
        txtViewDigital3.setTypeface(typeFaceDigital);
        txtViewDigital4.setTypeface(typeFaceDigital);
        txtViewDigital.setText("0.00");
        txtViewDigital2.setText("0.00");
        txtViewDigital3.setText("0.00");
        txtViewDigital4.setText("0.00");
   	    
        //Setup gauge 1
        multiGauge1.setAnalogGauge(analogGauge1);
        multiGauge1.buildGauge(BOOST_TOKEN);
        txtViewDigital.setText(Double.toString(multiGauge1.getSensorMaxValue()));
		
        //Setup gauge 2
        multiGauge2.setAnalogGauge(analogGauge2);
        multiGauge2.buildGauge(WIDEBAND_TOKEN);
        txtViewDigital2.setText(Double.toString(multiGauge2.getSensorMaxValue()));
		
        //Setup gauge 3
        multiGauge3.setAnalogGauge(analogGauge3);
        multiGauge3.buildGauge(TEMP_TOKEN);
        txtViewDigital3.setText(Double.toString(multiGauge3.getSensorMaxValue()));
		
        //Setup gauge 4
        multiGauge4.setAnalogGauge(analogGauge4);
        multiGauge4.buildGauge(OIL_TOKEN);
        txtViewDigital4.setText(Double.toString(multiGauge4.getSensorMaxValue()));

	  
	    //Get the mSerialService object from the UI activity.
	    Object obj = PassObject.getObject();
	    //Assign it to global mSerialService variable in this activity.
	    mSerialService = (BluetoothSerialService) obj;
	    //Update the BluetoothSerialService instance's handler to this activities.
	    mSerialService.setHandler(mHandler);
	   
	    if(!showAnalog){
	    	((ViewManager)analogGauge1.getParent()).removeView(analogGauge1); //Remove analog gauge
	    }
	    if(showNightMode){
	    	root = btnOne.getRootView(); //Get root layer view.
	        root.setBackgroundColor(getResources().getColor(R.color.black)); //Set background color to black.
	    }
	    
	    //Toast.makeText(getApplicationContext(), "Digital set to boost, tap a gauge to change", Toast.LENGTH_LONG).show();

	}
    
    //Handles the data being sent back from the BluetoothSerialService class.
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	case MESSAGE_READ:
            		
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
					//update/process the inbound data.
                    currentMsg = readMessage;
                    if(!paused){
                    	//Fire off worker thread to calculate/set gauge values
                    	thread = new Thread(FourGaugeActivity.this);
                        thread.start();
                    	updateGauges();     
                    }
            		break;
            	case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    
    //update gauge values in worker thread.
    public void run(){
    	parseInput(currentMsg);
    	multiGauge1.handleSensor(boostSValue);
		multiGauge2.handleSensor(wbSValue);
		multiGauge3.handleSensor(tempSValue);
		multiGauge4.handleSensor(oilSValue);
    }
    
    public void updateGauges(){
    	if(!paused){
    		analogGauge1.setValue(multiGauge1.getCurrentGaugeValue());
			analogGauge2.setValue(multiGauge2.getCurrentGaugeValue());
			analogGauge3.setValue(multiGauge3.getCurrentGaugeValue());
			analogGauge4.setValue(multiGauge4.getCurrentGaugeValue());
			
			txtViewDigital.setText(Float.toString(multiGauge1.getCurrentGaugeValue()));
			txtViewDigital2.setText(Float.toString(multiGauge2.getCurrentGaugeValue()));
			txtViewDigital3.setText(Float.toString(multiGauge3.getCurrentGaugeValue()));
			txtViewDigital4.setText(Float.toString(multiGauge4.getCurrentGaugeValue()));
    	}
    }
    
    private void parseInput(String sValue){
    	String[] tokens=sValue.split(","); //split the input into an array.
    	
    	try {
    		//Get current tokens for this gauge activity, cast as float.
    		boostSValue = new Float(tokens[BOOST_TOKEN].toString());
    		wbSValue 	= new Float(tokens[WIDEBAND_TOKEN].toString());
    		tempSValue 	= new Float(tokens[TEMP_TOKEN].toString());
    		oilSValue 	= new Float(tokens[OIL_TOKEN].toString());
		} catch (NumberFormatException e) {
			boostSValue = 0;
			wbSValue 	= 0;
			tempSValue 	= 0;
			oilSValue	= 0;
		} catch (ArrayIndexOutOfBoundsException e){
			boostSValue = 0;
			wbSValue 	= 0;
			tempSValue 	= 0;
			oilSValue	= 0;
		}
    }
    
    //Activity transfer
    public void goHome(View v){
    	PassObject.setObject(mSerialService);
    	onBackPressed();
		finish();
	}
    
    //Button one handling.
    public void buttonOneClick(View v){   
    	//Reset the max value.
    	multiGauge1.setSensorMaxValue(multiGauge1.getMinValue());
    	multiGauge2.setSensorMaxValue(multiGauge2.getMinValue());
    	multiGauge3.setSensorMaxValue(multiGauge3.getMinValue());
    	multiGauge4.setSensorMaxValue(multiGauge4.getMinValue());
    	Toast.makeText(getApplicationContext(), "Max value reset", Toast.LENGTH_SHORT).show();
	}
    
    //Button two handling.
    public void buttonTwoClick(View v){
    	if(!paused){
    		paused = true;
    		
    		//set the gauge/digital to the max value captured so far.
			txtViewDigital.setText(Double.toString(multiGauge1.getSensorMaxValue()));
			txtViewDigital2.setText(Double.toString(multiGauge2.getSensorMaxValue()));
			txtViewDigital3.setText(Double.toString(multiGauge3.getSensorMaxValue()));
			txtViewDigital4.setText(Double.toString(multiGauge4.getSensorMaxValue()));

    		analogGauge1.setValue((float)multiGauge1.getSensorMaxValue());
    		analogGauge2.setValue((float)multiGauge2.getSensorMaxValue());
    		analogGauge3.setValue((float)multiGauge3.getSensorMaxValue());
    		analogGauge4.setValue((float)multiGauge4.getSensorMaxValue());
        	btnTwo.setBackgroundResource(R.drawable.btn_bg_pressed);
    	}else{
    		paused = false;
    		btnTwo.setBackgroundResource(Color.TRANSPARENT);
    	}
	}
    
    //Analog gauge one click
    public void gaugeOneClick(View v){
    	//((ViewManager)txtViewDigital.getParent()).removeView(txtViewDigital);
    }
    
    public void gaugeTwoClick(View v){
    	//((ViewManager)txtViewDigital2.getParent()).removeView(txtViewDigital2);
    }
    
    public void gaugeThreeClick(View v){
    	//((ViewManager)txtViewDigital3.getParent()).removeView(txtViewDigital3);
    }
    
    public void gaugeFourClick(View v){
    	//((ViewManager)txtViewDigital4.getParent()).removeView(txtViewDigital4);
    }
    
    protected void onPause(){
    	super.onPause();
    }
    
    protected void onResume(){
    	super.onResume();
    	analogGauge1.invalidate();
    	analogGauge2.invalidate();
    	analogGauge3.invalidate();
    	analogGauge4.invalidate();
    }
       
    public void prefsInit(){
    	SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(this);
    	showAnalog = sp.getBoolean("showAnalog", true);
    	showDigital = sp.getBoolean("showDigital", true);
    	showNightMode = sp.getBoolean("showNightMode", false);
    	gaugeOnePref = sp.getString("multiGaugeOne", "boost");
    	gaugeTwoPref = sp.getString("multiGaugeTwo", "wb");
    	
    	if(gaugeOnePref.equals("Boost")){currentTokenOne = BOOST_TOKEN;}else 
    	if(gaugeOnePref.equals("Wideband O2")){currentTokenOne = WIDEBAND_TOKEN;}else 
    	if(gaugeOnePref.equals("Temperature")){currentTokenOne = TEMP_TOKEN;}else 
    	if(gaugeOnePref.equals("Oil Pressure")){currentTokenOne = OIL_TOKEN;}
    	if(gaugeTwoPref.equals("Boost")){currentTokenTwo = BOOST_TOKEN;}else
    	if(gaugeTwoPref.equals("Wideband O2")){currentTokenTwo = WIDEBAND_TOKEN;}else
    	if(gaugeTwoPref.equals("Temperature")){currentTokenTwo = TEMP_TOKEN;}else
    	if(gaugeTwoPref.equals("Oil Pressure")){currentTokenTwo = OIL_TOKEN;}
    }
}
