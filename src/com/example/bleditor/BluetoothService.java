package com.example.bleditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.LOLLIPOP_MR1)

public class BluetoothService extends Service {
	private IBinder mBinder = new LocalBinder();
	
	private static final long SCAN_PERIOD = 5000;
	public final String UPDATE = "NEW_DATA";
	public final String CONNECTED = "CONNECTED_TO_DEVICE";
	public final String DISCONNECTED = "DISCONNECTED_FROM_DEVICE";
	public final String SERVICES_AVAILABLE = "SERVICES";
	public final String CHARACTERISTIC_DATA = "CHARACTERISTICS";
	
	private BluetoothAdapter mAdapter;
	public BluetoothLeScanner mScanner;
	private BluetoothDevice mDevice;
	public BluetoothGatt mGatt;
	public ArrayList<BluetoothGattService> mServices = new ArrayList<BluetoothGattService>();
	public HashMap<UUID, ArrayList<BluetoothGattCharacteristic>> mCharacteristics = new HashMap<UUID, ArrayList<BluetoothGattCharacteristic>>();
	public HashMap<UUID, ArrayList<BluetoothGattDescriptor>> mDescriptors = new HashMap<UUID, ArrayList<BluetoothGattDescriptor>>();
	
	private Handler scanHandler = new Handler();
	public boolean Scanning = false;
	public LeScans foundDevices = new LeScans();
	private Intent mIntent = new Intent();
	
	@Override
	public void onCreate() {
		super.onCreate();
		//final BluetoothManager bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mScanner = mAdapter.getBluetoothLeScanner();
		if(mScanner == null)
			Log.i("BLE Service", "Scanner not Initialized");
		Log.i("BLE Service", "Started");
		
	}
	
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		
		 @Override
	        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
			 if(status == BluetoothGatt.GATT_SUCCESS){
				 switch(newState){
				 case BluetoothGatt.STATE_CONNECTED:
					 mDevice = mGatt.getDevice();
					 UpdateBroadcast(CONNECTED);
					 break;
				 case BluetoothGatt.STATE_DISCONNECTED:
					 UpdateBroadcast(DISCONNECTED);
					 break;
				 }
			 }
		 }
		 
		 @Override
		 public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
			 if(status == BluetoothGatt.GATT_SUCCESS){
				 
			 }
		 }
		 
		 @Override
		 public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
			 if(status == BluetoothGatt.GATT_SUCCESS){
				 Log.i("BLE Service", "Characteristic Read Success");
				 UpdateBroadcast(CHARACTERISTIC_DATA);
				 
			 }
			 else
				 Log.i("BLE Service", "Characteristic Read Failed: " + status);
		 }
		 
		 @Override
		 public void onServicesDiscovered(BluetoothGatt gatt, int status){
			 if(status == BluetoothGatt.GATT_SUCCESS){
				 UpdateDeviceData();
				 UpdateBroadcast(SERVICES_AVAILABLE);
			 }
		 }
		
	};
	
	private void UpdateDeviceData(){
		mServices.clear();
		mServices.addAll(mGatt.getServices());
		for(BluetoothGattService s: mServices){
			mCharacteristics.put(s.getUuid(), new ArrayList<BluetoothGattCharacteristic>());
			for(BluetoothGattCharacteristic c: s.getCharacteristics()){
				mCharacteristics.get(s.getUuid()).add(c);
				mDescriptors.put(c.getUuid(), new ArrayList<BluetoothGattDescriptor>());
				for(BluetoothGattDescriptor d: c.getDescriptors()){
					mDescriptors.get(c.getUuid()).add(d);
				}
			}
		}
	}
	
	public void getServices(){
		mGatt.discoverServices();
	}
	
	private void UpdateBroadcast(String action){
		mIntent.setAction(action);
		sendBroadcast(mIntent);
	}
	
	public void connectDevice(BluetoothDevice device){
		mGatt = device.connectGatt(this, false, mGattCallback);
	}
	
	public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanHandler.postDelayed(new Runnable() {
				@Override
                public void run() {
                    Scanning = false;
                    mScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            Scanning = true;
            mIntent.setAction(UPDATE);
            mScanner.startScan(mScanCallback);
        } else {
            Scanning = false;
            mScanner.stopScan(mScanCallback);
           
        }
    }
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	public class LocalBinder extends Binder {
        BluetoothService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothService.this;
        }
    }

	class LeScans {
		private ArrayList<String> deviceData;
		private ArrayList<BluetoothDevice> devices;
		
		public LeScans() {
			deviceData = new ArrayList<String>();
			devices = new ArrayList<BluetoothDevice>();
		}
		
		public void clear(){
			deviceData.clear();
			devices.clear();
		}
		
		public void add(ScanResult result){
			//String temp;
			//temp = "Mac: " + result.getDevice().getAddress() + "    RSSI: " + result.getRssi();
			if(result.getScanRecord().getDeviceName() != null)
				deviceData.add(result.getScanRecord().getDeviceName());
			else
				deviceData.add(result.getDevice().getAddress());
			devices.add(result.getDevice());
		}
		
		public ArrayList<String> getDevices(){
			ArrayList<String> temp = deviceData;
			return temp;
		}
		
		public void update(ScanResult result){
			 int i = devices.indexOf(result.getDevice());
			 
			 if (result.getScanRecord().getDeviceName() != null)
				 deviceData.set(i, result.getScanRecord().getDeviceName());
			 else
				 deviceData.set(i, result.getDevice().getAddress());
			 
		}
		
		public boolean contains(ScanResult result){
			if(devices.contains(result.getDevice()))
				return true;
			else
				return false;
		}
		
		public BluetoothDevice getDevice(int position){
			return devices.get(position);
		}
	}
	
	public ScanCallback mScanCallback = new ScanCallback() {
		
		@Override
		public void onScanResult (int callbackType, ScanResult result){
			if(foundDevices.contains(result)){
				foundDevices.update(result);
			}
			else{
				foundDevices.add(result);
			}
			sendBroadcast(mIntent);
		}
		
	};

}
