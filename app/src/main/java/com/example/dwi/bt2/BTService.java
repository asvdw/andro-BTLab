package com.example.dwi.bt2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.example.dwi.bt2.threads.ClientThread;
import com.example.dwi.bt2.threads.ConnectedThread;
import com.example.dwi.bt2.threads.ServerThread;

import java.util.Set;
import java.util.UUID;

/**
 * Created by DWI on 09/06/16.
 *
 * todo: centralize context ?
 */
public class BTService {

    private Listener mListener;
    private BluetoothAdapter mBluetoothAdapter;
    private ConnectedThread connectedThread;

    private boolean BTReady = false;
    private ServerThread serverThread;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                log("available: "+device.getName() + " - " + device.getAddress());
                mListener.onFoundDevice(device);

            }
            else if (BluetoothDevice.ACTION_UUID.equals(action)) {
                //we want it once (?)
                if (broadcastReceiverRegistred) context.unregisterReceiver(mReceiver);
                broadcastReceiverRegistred = false;

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                if(uuidExtra != null) {
                    log("found uuuids: "+uuidExtra.length);
                    for (Parcelable parcelable : uuidExtra) {
                        ParcelUuid parcelUuid = (ParcelUuid) parcelable;
                        UUID uuid = parcelUuid.getUuid();
                        log(device.getName()+" "+uuid.toString());
                    }
                }
            }
        }
    };
    private boolean broadcastReceiverRegistred = false;
    private ClientThread clientThread;

    public void setup(@NonNull  Listener listener, Activity activity) {
        log("Launch BT Service");
        mListener = listener;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.setName(AppConstant.DEVICENAME);
        if (mBluetoothAdapter == null) {
            log("No device !");
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            requestEnableBt(activity);
        } else {
            String adapterName = mBluetoothAdapter.getName();
            log("Bluetooth adapter found: "+adapterName);
        }
        BTReady = true; //todo: test before each method
    }
    private void log(String msg){
        if (mListener != null) mListener.log(msg);

    }
    public void makeDiscoverable(Context context) {
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        context.startActivity(discoverableIntent);
    }
    public void requestEnableBt(Activity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, AppConstant.REQUEST_ENABLE_BT);
    }

    public void lauchServer() {
        log("launch server");
        serverThread = new ServerThread(mBluetoothAdapter, new ServerThread.Listener() {
            @Override
            public void onBTError(Exception e) {
                log("BT Server error");
            }

            @Override
            public void onBTConnect(BluetoothSocket socket) {
                log("BT Connection established (as server");
                registerConnection(socket);
            }

            @Override
            public void onStop() {
                log("BT Server STOPPED");
            }
        });
        serverThread.start();
    }
    private void cancelServer(){
        if (serverThread != null) serverThread.cancel(); //todo: check if >null
    }

    private void registerConnection(BluetoothSocket socket) {
        connectedThread = new ConnectedThread(socket, new ConnectedThread.Listener() {
            @Override
            public void onReceived(byte[] message) {
                mListener.onReceived(message);
            }

            @Override
            public void onStopped() {
                log("Connection stopped");

            }
        });
        connectedThread.start();
    }
    public void cancelActiveConnection() {
        if (connectedThread != null) connectedThread.cancel();
    }
    public void findAvailableDevices(Context context) {
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        if (broadcastReceiverRegistred) context.unregisterReceiver(mReceiver);
        context.registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        broadcastReceiverRegistred = true;
        mBluetoothAdapter.startDiscovery();
    }

    public void findPairedDevices() {

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                log("Found paired device: "+device.getName());
                mListener.onFoundDevice(device);
            }
        }
    }


    public void connectToDevice(BluetoothDevice device) {

        log("connecting to device "+device.getName());



        clientThread = new ClientThread(mBluetoothAdapter, device, new ClientThread.Listener() {
            @Override
            public void onBTError(Exception e) {
                log("BT Connect error");
            }

            @Override
            public void onBTConnect(BluetoothSocket socket) {
                log("BT Connection established (as client)");
                registerConnection(socket);
            }
        });
        clientThread.start();
    }
    public void cancelClientThread() {
        if (clientThread != null) clientThread.cancel();
    }

    public void findUUIDs(BluetoothDevice device, Context context) {

        //todo: setup broadcast once
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_UUID);
        if (broadcastReceiverRegistred) context.unregisterReceiver(mReceiver);
        context.registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
        broadcastReceiverRegistred = true;

        device.fetchUuidsWithSdp(); //boolean
    }


    public void destroy(Context context){
        if (broadcastReceiverRegistred) context.unregisterReceiver(mReceiver);
        broadcastReceiverRegistred = false;
        cancelServer();
        cancelActiveConnection();
        cancelClientThread();

    }

    public void write(String message) {
        if (connectedThread != null) {
            connectedThread.write(message.getBytes());
        }
    }
    public void write(byte[] message) {
        if (connectedThread != null) {
            connectedThread.write(message);
        }
    }

    public interface Listener{
        void log(String msg);
        void onFoundDevice(BluetoothDevice device);
        void onReceived(byte[] message);

    }
}
