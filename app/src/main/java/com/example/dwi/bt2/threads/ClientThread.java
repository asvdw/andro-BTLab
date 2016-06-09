package com.example.dwi.bt2.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import com.example.dwi.bt2.AppConstant;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by DWI on 08/06/16.
 */
public class ClientThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;


    private final BluetoothAdapter mBluetoothAdapter;
    private final Listener mListener;

    public ClientThread(BluetoothAdapter adapter, BluetoothDevice device, Listener listener) {

        mBluetoothAdapter = adapter;
        mListener = listener;

        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmp = null;
        mmDevice = device;

        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
            tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(AppConstant.MY_UUID));
        } catch (IOException e) {
            mListener.onBTError(e);
        }
        mmSocket = tmp;
    }

    public void run() {
        // Cancel discovery because it will slow down the connection
        mBluetoothAdapter.cancelDiscovery();

        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
                mmSocket.close();
            } catch (IOException closeException) { }
            mListener.onBTError(connectException);
            return;
        }

        // Do work to manage the connection (in a separate thread)
        mListener.onBTConnect(mmSocket);
    }

    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }

    public interface Listener {
        void onBTError(Exception e);
        void onBTConnect(BluetoothSocket socket);
    }
}