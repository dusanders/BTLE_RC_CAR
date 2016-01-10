/*  Author: Dustin Anderson (Zena)
    Title:  BluetoothGattServerController.java
    Desc:   This file is intended to handle the BluetoothGattServerCallback provided by Android OS.
            This file will handle the BTLE protocol of LE Services and Characteristics.
            This file implements the BluetoothGattServerCallback class of Android OS, this is
                implemented with a variable 'gattServer'. The Android OS will send events to the
                BluetoothGattServerCallback class when BTLE events are received by the OS. We
                override this class with the variable 'gattServer' allowing us to implement our own
                actions when these events are received.
 */

package com.zena.btcomm.app;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.widget.Toast;
import java.util.ArrayList;

/**
 * Created by Zena on 10/11/2014.
 */
public class BluetoothGattServerController {
    //class variables
	private BluetoothManager manager;
	private BluetoothGattServer gattServer;
	private BluetoothDevice bluetoothDevice;
	private SensorManager sensorManager;
	public  SensorController sensorController;
	private Sensor sensor;
	private Activity activity;
	private ArrayList<BluetoothGattCharacteristic> characteristics;
	private BluetoothGattService gattService;
	private BluetoothGattServerController gattServerController;
	public boolean clearToSend;


	public BluetoothGattServerController(BluetoothManager inManager, final Activity inActivity,
                                         ArrayList<BluetoothGattCharacteristic> inCharacteristics,
                                         BluetoothGattService inService)
    {
        //get our passed BluetoothManager object
		manager = inManager;
		activity = inActivity;
		characteristics = inCharacteristics;
		gattService = inService;
		gattServerController = this;
        //clearToSend is the ACK state from the rc car.
		clearToSend = false;
        //create the gattServer object which implements BluetoothGattServerCallback class.
        //  This allows us to receive BTLE events from Android OS.
		gattServer = manager.openGattServer(activity, new BluetoothGattServerCallback() {
			@Override
			public void onConnectionStateChange(final BluetoothDevice device, int status, final int newState) {
				super.onConnectionStateChange(device, status, newState);
                //our state changed to 'Connected'
				if(newState == 2) {
                    //notify user via a Toast, must be run on main GUI thread.
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							MainActivity.connectionView.setText("Connected");
						}
					});
                    //if this is the first time connnecting, we need to open our SensorController class
                    //  to handle the Accelerometer.
					if(sensorController == null) {
                        //we request the sensor here because the SensorController implements the SensorEventListener
                        //  class of Android. This listener is responsible for receiving the events posted by the OS
                        //  when the sensor changes. The SensorManager tells Android we want to use a Sensor within
                        //  the device.
						sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
						sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                        //create our custom listener to receive Sensor events from Android.
						sensorController = new SensorController(activity, gattServerController, characteristics, device);
                        //register the listener with a 250000 sampling rate. Changes to the sampling rate affect the
                        //  rate at which the OS reads the sensor, too low and we receive excessive amounts of updates.
						sensorManager.registerListener(sensorController, sensor, 25000);
                        //Toast user that we activated the Sensor.
						activity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Toast toast = Toast.makeText(activity," Sensor Activated!", Toast.LENGTH_SHORT);
								toast.show();
							}
						});
					}
                    //we are clear to send to the car.
					clearToSend = true;
				}
                //connection state is no longer 'Connected'
				else if(newState == 0) {
					activity.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							MainActivity.connectionView.setText("Disconnected");
						}
					});
				}
                //Toast the returned state.
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast toast = Toast.makeText(activity, device.getName() + "State Change: " + newState, Toast.LENGTH_SHORT);
						toast.show();
					}
				});
			}

            //  NOT USED    //
			@Override
			public void onServiceAdded(final int status, final BluetoothGattService service) {
				super.onServiceAdded(status, service);
			}
            //  NOT USED    //
			@Override
			public void onCharacteristicReadRequest(final BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
				super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
			}
            //  This method is called when the car wants to write to a characteristic, we use this as the ACK signal from the car.
            //  Written value is arbitrary here as we only want to know that the car received our value and has executed its proper PWM.
			@Override
			public void onCharacteristicWriteRequest(BluetoothDevice device, final int requestId, final BluetoothGattCharacteristic characteristic,
                                                     final boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
				super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                //we got an ACK
                clearToSend = true;
			}
            //  NOT USED    //
			@Override
			public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
				super.onDescriptorReadRequest(device, requestId, offset, descriptor);
			}
            //  NOT USED    //
			@Override
			public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
				super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
			}
            //  NOT USED    //
			@Override
			public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
				super.onExecuteWrite(device, requestId, execute);
			}
            //  NOT USED   //
			@Override
			protected Object clone() throws CloneNotSupportedException {
				return super.clone();
			}
            //  NOT USED    //
			@Override
			public boolean equals(Object o) {
				return super.equals(o);
			}
            //  NOT USED    //
			@Override
			protected void finalize() throws Throwable {
				super.finalize();
			}
            //  NOT USED    //
			@Override
			public int hashCode() {
				return super.hashCode();
			}
            //  NOT USED    //
			@Override
			public String toString() {
				return super.toString();
			}
		});
	}
    //  This method is called by the SensorController class when the value needs to be sent to the car.
    //  The sensor will set the new Characteristic value and call this with a passed Characteristic
    //      which we need to broadcast has changed.
	public boolean notifyCharacteristic(BluetoothGattCharacteristic inCharacteristic) {
        //this call will tell Android to broadcast that the characteristic is changed.
		gattServer.notifyCharacteristicChanged(bluetoothDevice, inCharacteristic, true);
        //sent a value to car - we need to reset the ACK state.
        clearToSend = false;
        return true;
	}

    //general method to see if we have a connection
	public boolean isConnected() {
		if(bluetoothDevice != null){
			return true;
		}
		return false;
	}
    //general method to close our connection
	public void disconnect(BluetoothDevice inDevice) {
		gattServer.cancelConnection(inDevice);
	}

    //general method to connect to device
	public void connect(BluetoothDevice inDevice, boolean autoConnect) {
		gattServer.addService(gattService);
		if(gattServer.connect(inDevice, autoConnect)) {
			bluetoothDevice = inDevice;
		}
	}
    //general method to close, closes the Server and stop sensor listener
    //  Android closes the SensorManager with an 'unregisterListener()' call.
	public void close() {
		gattServer.close();
		if(sensor != null)
			sensorManager.unregisterListener(sensorController);
	}
    //called from MainActivity during 'onPause()'
    public void stopSensor() {
        //send '0' command to car
        if(sensorController != null) {
            sensorController.stopSensor();
        }
        if(sensor != null)
        {   //stop the sensor
            sensorManager.unregisterListener(sensorController);
        }
    }
    //called from MAinActivity during 'onResume()'
    public void resumeSensor() {
        if(sensorManager != null) {
            //restart the sensor
            sensorManager.registerListener(sensorController, sensor, 25000);
        }
    }
}
