package com.example.bleditor;

import com.example.bleditor.BluetoothService.LocalBinder;

import android.annotation.TargetApi;
import android.app.Activity;
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
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends Activity {
	
	private Button btnScan;
	private Button btnStop;
	private ListView listDevices;
	
	public static BluetoothService mBLEService = new BluetoothService();
	private ArrayAdapter<String> adapter;
	private IntentFilter mFilter = new IntentFilter();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Intent intent = new Intent(this, BluetoothService.class);
		startService(intent);
		getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		
		SetUpUI();
		SetUpReciever();
		registerReceiver(mReciever, mFilter);
	}
	
	private void SetUpReciever(){
		mFilter.addAction(mBLEService.UPDATE);
		mFilter.addAction(mBLEService.CONNECTED);
		mFilter.addAction(mBLEService.DISCONNECTED);
		
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBLEService = null;
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mBLEService = binder.getService();
		}
	};
	
	private void SetUpUI(){
		
		
		btnScan = (Button)(findViewById(R.id.buttonScan));
		btnScan.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mBLEService.foundDevices.clear();
				mBLEService.scanLeDevice(true);
			};
		});
		
		btnStop = (Button)(findViewById(R.id.buttonStop));
		btnStop.setOnClickListener(new android.view.View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mBLEService.scanLeDevice(false);
				
			}
		});
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		listDevices = (ListView)(findViewById(R.id.listDevices));
		listDevices.setAdapter(adapter);
		listDevices.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mBLEService.connectDevice(mBLEService.foundDevices.getDevice(position));
				
			}
		});
	}
	
	private void updateList(){
		adapter.clear();
		adapter.addAll(mBLEService.foundDevices.getDevices());
	}
	
	private final BroadcastReceiver mReciever = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(context == null || intent == null || intent.getAction() == null)
				return;
			
			String action = intent.getAction();
			
			if(action == mBLEService.UPDATE){updateList();}
			
			else if(action == mBLEService.CONNECTED){mBLEService.scanLeDevice(false); StartTagActivity();}
		
			else if(action == mBLEService.DISCONNECTED){/*TODO Disconnected Call*/}
			
		} 
		
	};
	
	private void StartTagActivity(){
		Intent updateIntent = new Intent(MainActivity.this, TagEdit.class);
		startActivity(updateIntent);
	}
}