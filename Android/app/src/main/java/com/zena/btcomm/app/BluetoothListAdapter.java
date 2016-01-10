/*  Author: Dustin Anderson (Zena)
    Title:  BluetoothListAdapter.java
    Desc:   This file is intended to handle the ListView actions called by the Android OS for
                updating a ListView object. The OS uses and ArrayAdapter object to parse elements
                for a ListView object used with a GUI. We use this custom class for inserting
                elements into our ListView used on the GUI.
 */

package com.zena.btcomm.app;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

/**
 * Created by Zena on 10/11/2014.
 */
public class BluetoothListAdapter extends ArrayAdapter<BluetoothDevice> {
    //class variables.
	private List<BluetoothDevice> aDeviceList;
	private Context aContext;

	public BluetoothListAdapter(Context context, int resource, List<BluetoothDevice> objects) {
		super(context, resource, objects);
		aDeviceList = objects;
		aContext = context;
	}

    //Android calls this method when an element needs to be inserted into a ListView object.
    //  This allows us to use a customized View as our ListView row to be inserted.
    //  The view used here can be changed by the 'list_item.xml' file within 'layout' project folder.
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
        //inflate our layout used for the row
		LayoutInflater inflater = (LayoutInflater)aContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //get the GUI elements from the layout
		View listItem = inflater.inflate(R.layout.list_item, parent, false);
		TextView bluetoothDeviceName = (TextView)listItem.findViewById(R.id.bluetoothDeviceName);
		Button connectButton = (Button)listItem.findViewById(R.id.connectButton);
		MainActivity.connectionView = (TextView)listItem.findViewById(R.id.connectionStateView);
        //ensure we actually have a device
		if(!aDeviceList.isEmpty()) {
            //set the name of the device at this position
			bluetoothDeviceName.setText(aDeviceList.get(position).getName() + "\n" + aDeviceList.get(position).getAddress());
            //set the 'Connect' button with a click listener
			connectButton.setOnClickListener(new View.OnClickListener() {
                //once a click happens, we start the gatt server on the selected device
				@Override
				public void onClick(View view) {
					MainActivity.startGattServer(aDeviceList.get(position));
				}
			});
		}
        //return the item - if a proper device was not found, an empty view will be returned.
		return listItem;
	}
    //method to retrieve a clicked list item.
	@Override
	public BluetoothDevice getItem(int position){
		return aDeviceList.get(position);
	}
}
