/*  Author: Dustin Anderson (Zena)
    Title:  BluetoothLeScanCallback.java
    Desc:   This file is intended to handle the callback events fired by Android OS when it
                detects a BTLE device during a scan. This file is used to add the detected
                device to the main GUI ListView object.
 */

package com.zena.btcomm.app;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

/**
 * Created by Zena on 10/11/2014.
 */

//Callback class to add a device to the ListView on the GUI
public class BluetoothLeScanCallback implements BluetoothAdapter.LeScanCallback {
	private Activity lActivity;
	public BluetoothLeScanCallback (Activity activity) {
		lActivity = activity;
	}
    //Android detected a BTLE device. Calls this method.
	@Override
	public void onLeScan(final BluetoothDevice bluetoothDevice, int rssi, byte[] bytes) {
        //notify our GUI thread that we found a device.
        //  --only the main thread can run its methods/change GUI elements, so we need
        //      to wrap this call within a runnable and pass it to the Android 'runOnUiThread'
        //      method which will pass the runnable to the main thread where it will be executed.
		lActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity.addBluetoothDevice(bluetoothDevice);
			}
		});
	}
}
