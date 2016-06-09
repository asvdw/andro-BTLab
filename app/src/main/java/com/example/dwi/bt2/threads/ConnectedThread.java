package com.example.dwi.bt2.threads;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by DWI on 08/06/16.
 */
public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
    private final Listener mListener;

    public ConnectedThread(BluetoothSocket socket, Listener listener) {
            mListener = listener;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream

                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    //mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                    //        .sendToTarget();
//                    int i;
//                    while((i = mmInStream.read())!=-1) {
//
//                    }

                    //buffer contient les 1024 chars reçu et bytes la size !!!


                    byte [] subArray = Arrays.copyOfRange(buffer, 0, bytes);

                    mListener.onReceived(subArray);
                } catch (IOException e) {
                    break;
                }
            }
            mListener.onStopped();
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }

    public interface Listener{
        void onReceived(byte[] message);
        void onStopped();
    }
    }