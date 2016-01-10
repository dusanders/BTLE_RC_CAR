/*  Author: Dustin Anderson (Zena)
    Title:  MainActivity.java
    Desc:   This file will provide Android system with a 'main' class to run the app.
            This file handles the GUI and is considered the 'main' thread of the app by the
                Android OS.
            onPause(), onResume(), and onStop() are not implemented within this app. If the app
                is existed the Android OS will destroy all states and variables.
 */




package com.zena.btcomm.app;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.*;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


public class MainActivity extends Activity {
	private static Activity mActivity;
	private TextView mTextView1;
	private TextView mTextView2;
	private Button mScanBluetooth;
	private ListView mBluetoothListView;
	public static TextView connectionView;

	private static BluetoothListAdapter mBluetoothListAdapter;
	private static List<BluetoothDevice> mBluetoothDeviceList;

	private static BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private static ArrayList<BluetoothGattCharacteristic> mCharacteristics;
	private static UUID[] mUUID = {UUID.fromString("00001101-0000-1000-8000-00807FEEE4FB"), UUID.fromString("00001101-0000-1000-8000-00805F90001B"),
			                       UUID.fromString("00001101-0000-1000-8000-00805F9FF01B"), UUID.fromString("00001101-0000-1000-8000-008ABD90001B"),
			                       UUID.fromString("00001101-0000-1000-8000-00805CDE001B"), UUID.fromString("00001101-0000-1000-8000-000ABDAC001B")};
	private static BluetoothGattService mService;

	private static BluetoothGattServerController mGattServerController;

    //ensure the phone has at least API level 18, else our system calls won't compile.
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mActivity = this;   //save our activity to send to the other classes/objects as they require
                            // a reference to the 'main GUI thread' to send Toasts
        //setup the GUI components...
		mTextView1 = (TextView) findViewById(R.id.textView1);
		mTextView2 = (TextView) findViewById(R.id.textView2);
		mScanBluetooth = (Button) findViewById(R.id.scanBluetooth);
		mBluetoothListView = (ListView) findViewById(R.id.bluetoothListView);
		mBluetoothDeviceList = new ArrayList<BluetoothDevice>();
		mBluetoothListAdapter = new BluetoothListAdapter(mActivity.getApplicationContext(), R.layout.list_item, mBluetoothDeviceList);
		mBluetoothListView.setAdapter(mBluetoothListAdapter);
        //request the Bluetooth service from Android
		mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
        //setup or main BTLE service
		mService = new BluetoothGattService(mUUID[0], BluetoothGattService.SERVICE_TYPE_PRIMARY);
        //array for our characteristics
		mCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
        //add 'forward' characteristic
		BluetoothGattCharacteristic characteristic1 = new BluetoothGattCharacteristic(mUUID[1], BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //add 'backwards' characteristic
		BluetoothGattCharacteristic characteristic2 = new BluetoothGattCharacteristic(mUUID[2], BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //add 'left' characteristic
		BluetoothGattCharacteristic characteristic3 = new BluetoothGattCharacteristic(mUUID[3], BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //add 'right characteristic
		BluetoothGattCharacteristic characteristic4 = new BluetoothGattCharacteristic(mUUID[4], BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
        //add the ACK Characteristic
		BluetoothGattCharacteristic characteristic5 = new BluetoothGattCharacteristic(mUUID[5], BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE);
		mCharacteristics.add(characteristic1);
		mCharacteristics.add(characteristic2);
		mCharacteristics.add(characteristic3);
		mCharacteristics.add(characteristic4);
		mCharacteristics.add(characteristic5);
        //add these characteristics to the BTLE main service
		mService.addCharacteristic(mCharacteristics.get(0));
		mService.addCharacteristic(mCharacteristics.get(1));
		mService.addCharacteristic(mCharacteristics.get(2));
		mService.addCharacteristic(mCharacteristics.get(3));
		mService.addCharacteristic(mCharacteristics.get(4));
        //open our custom BluetoothGattServerController object.
		mGattServerController = new BluetoothGattServerController(mBluetoothManager, mActivity, mCharacteristics, mService);
        //setup a click listener for the 'scan' button
		mScanBluetooth.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
                //requested a scan of BTLE. we want 'this' to be our callback object so pass it our activity
				final BluetoothLeScanCallback mLeScanCallback = new BluetoothLeScanCallback(mActivity);
				final long SCAN_PERIOD = 1000;  //scan for 1 sec
                //we need a delayed call to '.stopLeScan()' so set this up with an Android Handler object
				Handler scannerHandler = new Handler();
				scannerHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mBluetoothAdapter.stopLeScan(mLeScanCallback);
					}
				}, SCAN_PERIOD);
                //start our scan.
				mBluetoothAdapter.startLeScan(mLeScanCallback);
			}
		});

	}
    //this is called from BluetoothLeScanCallback class. Called when a BTLE device is found during
    //  a scan, we need to update our ListView of devices.
	public static void addBluetoothDevice(BluetoothDevice inDevice) {
        //Device list is empty, add found device.
        if (mBluetoothDeviceList.isEmpty()) {
            //add the device to the list of devices used by the ListViewAdapter
            mBluetoothDeviceList.add(inDevice);
            //tell Android to update our ListView with new objects.
            //  --This will force the View to re-draw itself.
            mBluetoothListAdapter.notifyDataSetChanged();
        }
        if (!mBluetoothDeviceList.isEmpty()) {
            //Android will report the same device multiple times during a scan, ensure we are not adding
            //  multiple copies of the same device.
            if (!mBluetoothDeviceList.contains(inDevice)) {
                mBluetoothDeviceList.add(inDevice);
                mBluetoothListAdapter.notifyDataSetChanged();
            }
        }
	}

    //inflates our 'Options' menu, we only have a 'Quit' button.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    //Android calls this method when the app 'loses' focus, such as navigating away or
    //  obscuring the app view.
    @Override
    public void onPause() {
        super.onPause();
        //if we have a sensor, stop it
        if(mGattServerController != null) {
            mGattServerController.stopSensor();
        }
    }

    //Android calls this method when the app returns from a paused or stopped state.
    //  restart our sensor if we have one.
    @Override
    public void onResume() {
        super.onResume();
        if(mGattServerController != null) {
            mGattServerController.resumeSensor();
        }
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.

        //our 'Quit' button was clicked, go for close
		int id = item.getItemId();
		if (id == R.id.action_settings) {
            if(mGattServerController != null) {
                mGattServerController.close();
            }
			finish(); //Android will clean us up.
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    //called when we want to scan/re-scan from Android Bluetooth. This will be called
    // when we scan for devices, restarting the connection if we are already connected.
    //  Called from BluetoothListAdapter class
	public static void startGattServer(final BluetoothDevice inBluetoothDevice) {
        //if already connected, disconnect
		if(mGattServerController.isConnected()){
			mGattServerController.disconnect(inBluetoothDevice);
		}
        //connect to BTLE Android service.
		mGattServerController.connect(inBluetoothDevice, true);
	}
}

