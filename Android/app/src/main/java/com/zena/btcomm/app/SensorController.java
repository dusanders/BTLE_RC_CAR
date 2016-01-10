/*  Author: Dustin Anderson (Zena)
    Title:  SensorController.java
    Desc:   This file is intended to handle the accelerometer sensor provided by the Android OS.
                This file will handle all sensor events provided by Android.
 */


package com.zena.btcomm.app;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Zena on 10/11/2014.
 */
public class SensorController implements SensorEventListener {


    //class variables
	private BluetoothGattServerController gattServerController;
	private Activity activity;
	private ArrayList<BluetoothGattCharacteristic> characteristics;
	private BluetoothDevice device;
    private boolean stopX = false;
    private boolean stopY = false;
    private int lastSent;

	public SensorController(Activity inActivity,
                            BluetoothGattServerController inGattServerController,
                            ArrayList<BluetoothGattCharacteristic> inCharacteristic,
                            BluetoothDevice inDevice) {

		activity = inActivity;
		gattServerController = inGattServerController;
		characteristics = inCharacteristic;
		device = inDevice;
	}
	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
        //get the values from the sensor
		float[] values = sensorEvent.values;
        //parse the proper coordinates from the returned array.
		Integer x = (int)values[0]; // x plane always 0 index
		Integer y = (int)values[1]; // y plane always 1 index
		int z = (int)values[2]; // z plane always 2 index


		if(x == 0 && !stopX)  {
            //set the values and stopX, as we got a 0 value from sensor,
            //stopX will be sent to true so don't continually send '0' to car. only need to send
            //once.
			if(gattServerController.clearToSend) {
				characteristics.get(0).setValue(new byte[]{x.byteValue()});
				gattServerController.notifyCharacteristic(characteristics.get(0));
                lastSent = 0;
                stopX = true;
			}
		}
		else if (x > 2) {
            //create Command object to hold update value
            characteristics.get(1).setValue(new byte[] {x.byteValue()});
            //wait for ACK signal, or timeout
            //we got an acceptable command to move, so switch our stopX to allow
            // sending a new '0' value when we are requested to stop.
			 if(gattServerController.clearToSend) {
                 //update characteristic value & notify of change
				 gattServerController.notifyCharacteristic(characteristics.get(1));
                 lastSent = 1;
                 stopX = false; //reset or stop, we are moving now so the next '0' will need to
                                    //be sent.
			 }
		}
        else if(x < -2) {
            //phone axis returns negative, we need absolute value for car.
            x = Math.abs(x);
            characteristics.get(0).setValue(new byte[] {x.byteValue()});
            //wait for ACK or time out
            if(gattServerController.clearToSend) {
                //update characteristic value and notify of change.
                gattServerController.notifyCharacteristic(characteristics.get(0));
                lastSent = 0;
                stopX = false;
            }
        }

        //got a '0' y value, if we are already stopped, skip.
		if(y == 0 && !stopY) {
            //wait for ACK or timeout
			if(gattServerController.clearToSend) {
                //update the characteristic and notify of change
				characteristics.get(2).setValue(new byte[]{y.byteValue()});
				gattServerController.notifyCharacteristic(characteristics.get(2));
                lastSent = 2;
                //no need to send this continually, will skip next '0' value
                stopY = true;
			}
		}
        //got acceptable move signal
		else if (y > 2) {
            characteristics.get(3).setValue(new byte[] {y.byteValue()});
			 if(gattServerController.clearToSend) {
				 gattServerController.notifyCharacteristic(characteristics.get(3));
                 lastSent = 3;
                 stopY = false; // reset our stopY as we are moving, next '0' will need to be sent.
			 }
		}
        else if(y < -2 ) {
            y = Math.abs(y);
            characteristics.get(2).setValue(new byte[] {y.byteValue()});
            if(gattServerController.clearToSend) {
                gattServerController.notifyCharacteristic(characteristics.get(2));
                lastSent = 2;
                stopY = false;
            }
        }
        //update the GUI with new accel. data
		TextView textView = (TextView)activity.findViewById(R.id.textView1);
		textView.setText("");
		textView.append("x: "+ (int)values[0] + " " + "y: " + (int)values[1] + " " + "z: " + z);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {
        //NOT USED///
	}

    //method to stop the car when the app loses focus, or the user exits the app.
    //     --'stops' the last characteristic sent to the car.
    public void stopSensor() {
        byte[] stopByte = {0};
        characteristics.get(lastSent).setValue(stopByte);
        gattServerController.notifyCharacteristic(characteristics.get(lastSent));
    }
}
