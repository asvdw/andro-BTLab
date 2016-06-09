package com.example.dwi.bt2.threads;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.example.dwi.bt2.AppConstant;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by DWI on 08/06/16.
 */
public class ServerThread extends Thread {

    private final BluetoothServerSocket mmServerSocket;
    private Listener mListener;

    public ServerThread(BluetoothAdapter mBluetoothAdapter, Listener listener) {
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        mListener = listener;
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(AppConstant.NAME, UUID.fromString(AppConstant.MY_UUID));
            Log.e("DW%", "Listening with UUID " + UUID.fromString(AppConstant.MY_UUID).toString());
        } catch (IOException e) {
            mListener.onBTError(e);
        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket = null;
        // Keep listening until exception occurs or a socket is returned
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }
            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                mListener.onBTConnect(socket);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        mListener.onStop();
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }

    public interface Listener {
        void onBTError(Exception e);
        void onBTConnect(BluetoothSocket socket);

        void onStop();
    }
}