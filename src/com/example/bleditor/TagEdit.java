package com.example.bleditor;

import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import com.example.bleditor.BluetoothService.LocalBinder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)

public class TagEdit extends Activity {

	private BluetoothService mBLEService;
	private BluetoothGattService selectedService;
	private BluetoothGattCharacteristic selectedCharacteristic;
	private BluetoothGattDescriptor selectedDescriptor;
	private IntentFilter mFilter = new IntentFilter();
	
	private TextView TagName;
	private Spinner spinnerService;
	private Spinner spinnerCharacteristic;
	private Spinner spinnerDescriptor;
	private EditText valueStr;
	private EditText valueHex;
	private Button btnRead;
	private Button btnWrite;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_tag);
		
		Intent intent = new Intent(this, BluetoothService.class);
		getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
	}
	
	private final BroadcastReceiver mReciever = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(context == null || intent == null || intent.getAction() == null)
				return;
			
			String action = intent.getAction();
			
			if (action == mBLEService.SERVICES_AVAILABLE){UpdateServiceSpinner(mBLEService.mServices);}
			else if(action == mBLEService.CHARACTERISTIC_DATA){ReadCharacteristic();}
			else if(action == mBLEService.DESCRIPTOR_DATA){ReadDescriptor();}
			else if(action == mBLEService.CHARACTERISTIC_WRITE){Toast toast = Toast.makeText(getApplicationContext(), "Write Success!", Toast.LENGTH_SHORT); toast.show();};
		} 
		
	};
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
	
	private void ReadDescriptor(){
		String temp = bytesToHex(selectedDescriptor.getValue());
		if(temp == null)
			Log.i("Tag Activity", "Data Retrival Failed");
		else{
			Toast toast = Toast.makeText(getApplicationContext(), temp, Toast.LENGTH_SHORT);
			toast.show();
			Log.i("Tag Activity", temp);
		}
	}
	
	private void ReadCharacteristic(){
		String bytetemp = bytesToHex(selectedCharacteristic.getValue());
		String strtemp = new String(selectedCharacteristic.getValue());
		if(bytetemp == null)
			Log.i("Tag Activity", "Data Retrival Failed");
		else{
			valueHex.setText(bytetemp);
			valueStr.setText(strtemp);
		}
	}
	
	private void UpdateServiceSpinner(ArrayList<BluetoothGattService> Services) {
		
		final String[] serviceName = new String[Services.size() +1];
		serviceName[0] = "Select Service";
		
		int i = 1;
		for(BluetoothGattService m: Services)
		{
			serviceName[i] = m.getUuid().toString();
			i++;
		}
	
		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, serviceName);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(R.layout.spinner_view);
		// Apply the adapter to the spinner
		spinnerService.setAdapter(adapter);
		spinnerService.setSelection(0);
	}
		
	private void UpdateCharacteristicSpinner(ArrayList<BluetoothGattCharacteristic> Characteristic) {
	
		final String[] characteristicName = new String[Characteristic.size() + 1];
		characteristicName[0] = "Select Characteristic";
		Log.i("Tag Activity", "Characteristics: " + Characteristic.size());
		
		int i = 1;
		for(BluetoothGattCharacteristic m: Characteristic)
		{
			characteristicName[i] = m.getUuid().toString();	
			i++;
		}
		
		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, characteristicName);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(R.layout.spinner_view);
		// Apply the adapter to the spinner
		spinnerCharacteristic.setAdapter(adapter);
		spinnerCharacteristic.setSelection(0);
	}
	
	private void UpdateDesciptorSpinner(ArrayList<BluetoothGattDescriptor>Descriptors){
		
		final String[] descriptorName = new String[Descriptors.size() + 1];
		descriptorName[0] = "Select Descriptor";
		int i = 1;
		
		for(BluetoothGattDescriptor m : Descriptors)
		{
			descriptorName[i] = m.getUuid().toString();
			i++;
		}
		
		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, descriptorName);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(R.layout.spinner_view);
		// Apply the adapter to the spinner
		spinnerDescriptor.setAdapter(adapter);
		spinnerDescriptor.setSelection(0);

	}
	
	private void SetUpReciever(){
		mFilter.addAction(mBLEService.SERVICES_AVAILABLE);
		mFilter.addAction(mBLEService.CHARACTERISTIC_DATA);
		mFilter.addAction(mBLEService.DESCRIPTOR_DATA);
		mFilter.addAction(mBLEService.CHARACTERISTIC_WRITE);
	}
	
	private void SetUpUI(){
		TagName = (TextView)(findViewById(R.id.textTagName));
		TagName.setText(mBLEService.mGatt.getDevice().getName());
		spinnerService = (Spinner)(findViewById(R.id.spinnerServices));
		spinnerService.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if(position != 0){
					selectedService = mBLEService.mServices.get(position - 1);
					UpdateCharacteristicSpinner(mBLEService.mCharacteristics.get(mBLEService.mServices.get(position - 1).getUuid()));
				}
				else{
					UpdateCharacteristicSpinner(new ArrayList<BluetoothGattCharacteristic>());
				}
				
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
		
		spinnerCharacteristic = (Spinner)(findViewById(R.id.spinnerCharacteristics));
		spinnerCharacteristic.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if(position != 0){
					selectedCharacteristic = mBLEService.mCharacteristics.get(selectedService.getUuid()).get(position-1);
					UpdateDesciptorSpinner(mBLEService.mDescriptors.get(selectedCharacteristic.getUuid()));
				}
				else{
					UpdateDesciptorSpinner(new ArrayList<BluetoothGattDescriptor>());
				}
				
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
		
		spinnerDescriptor = (Spinner)(findViewById(R.id.spinnerDescriptors));
		spinnerDescriptor.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if(position != 0)
					selectedDescriptor = selectedCharacteristic.getDescriptors().get(position - 1);
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
		});
		
		valueStr = (EditText)(findViewById(R.id.textStrValue));
		valueHex = (EditText)(findViewById(R.id.textByteValue));
		
		btnRead = (Button)(findViewById(R.id.buttonRead));
		btnRead.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mBLEService.mGatt.readCharacteristic(selectedCharacteristic);
				
			}
		});
		
		btnWrite = (Button)(findViewById(R.id.buttonWrite));
		btnWrite.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selectedCharacteristic.setValue(hexStringToByteArray(valueHex.getText().toString()));
				mBLEService.mGatt.writeCharacteristic(selectedCharacteristic);
				
			}
		});
		
		
		
	}
	
	
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBLEService = null;
			Log.i("Tag Activity", "Binding Failed");
			
		}
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mBLEService = binder.getService();
			Log.i("Tag Activity", "Binding Success");
			SetUpReciever();
			registerReceiver(mReciever, mFilter);
			SetUpUI();
			mBLEService.getServices();
	
		}
	};
	
	private void InitSpinners(){
		UpdateServiceSpinner(new ArrayList<BluetoothGattService>());
		UpdateCharacteristicSpinner(new ArrayList<BluetoothGattCharacteristic>());
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		mBLEService.mGatt.disconnect();
		//unregisterReceiver(mReciever);
		UpdateServiceSpinner(new ArrayList<BluetoothGattService>());
		InitSpinners();
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		//registerReceiver(mReciever, mFilter);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		//registerReceiver(mReciever, mFilter);
	}
	@Override
	protected void onStop() {
		super.onStop();
		mBLEService.mGatt.disconnect();
		//unregisterReceiver(mReciever);
		finish();
	}

}
