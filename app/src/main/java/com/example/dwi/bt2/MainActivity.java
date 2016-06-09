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
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dwi.bt2.threads.ClientThread;
import com.example.dwi.bt2.threads.ConnectedThread;
import com.example.dwi.bt2.threads.ServerThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {



    //todo: pair devices

    List<byte[]> mockedMessages;
    private static final String TAG = AppConstant.TAG + "Main";
    private EditText commandText;
    private TextView logText;


    private BTService btService = new BTService();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        commandText = (EditText)findViewById(R.id.commandText);
        logText = (TextView)findViewById(R.id.logText);
        setupMock();
        setupCommand();
        setupBluetooth();
    }

    private void setupMock() {
        mockedMessages = new ArrayList<>();
         mockedMessages.add(new byte[] {115, 97, 108, 117, 116});
         mockedMessages.add(new byte[] {115, 97, 108, '\r', '\n'});
        mockedMessages.add("Hello \n".getBytes());


    }

    private void setupCommand() {
        commandText.setFocusableInTouchMode(true);
        commandText.requestFocus();
        commandText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    lauchCommand(commandText.getText().toString());
                    commandText.setText("");
                    handled = true;
                }
                return handled;
            }
        });

    }
    private void findAvailableDevices() {
        int hasPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                showMessageOKCancel("You need to allow access to Location",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                                        AppConstant.REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        });
                return;
            }
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.ACCESS_COARSE_LOCATION},
                    AppConstant.REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        btService.findAvailableDevices(this);
    }

    private void lauchCommand(String command) {
        if (command.equalsIgnoreCase("cls")){
            logText.setText("");
        }
        else if (command.equalsIgnoreCase("reset")){
            btService.destroy(this);
        }
        else if (command.startsWith("p")){
            btService.findPairedDevices();
        }
        else if (command.equalsIgnoreCase("disc")){
            findAvailableDevices();
        }
        else if (command.startsWith("s")){
            btService.lauchServer();
        }
        else if (command.equalsIgnoreCase("expose")){
            btService.makeDiscoverable(this);
        }
        else if (command.startsWith("w ")){
            btService.write(command.substring(2));

        }
        else if (command.startsWith("W ")){
            btService.write(command.substring(2) + "\r\n");

        }        else if (command.startsWith("m ")){

            int index = Integer.parseInt(command.substring(1));
            btService.write(mockedMessages.get(index));

        }
        else {
            log("What ??");
        }

    }





    private void setupBluetooth() {
        btService.setup(new BTService.Listener() {
            @Override
            public void log(String msg) {
                MainActivity.this.log(msg);
            }

            @Override
            public void onFoundDevice(BluetoothDevice device) {

                if (device.getName() != null && device.getName().toUpperCase().contains(AppConstant.DEVICENAME)){
                    //askForConnection(device);
                    btService.findUUIDs(device, MainActivity.this);
                }
            }

            @Override
            public void onReceived(byte[] message) {
                MainActivity.this.log(message.length + " >> " + new String(message));

                Log.w(TAG, Arrays.toString(message));
            }

        }, this);

    }



    private void askForConnection(final BluetoothDevice device) {
        showMessageOKCancel("Connect to " + device.getName() + " ?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                btService.connectToDevice(device);
            }
        });
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
    public void log(final String str){
        Log.i(TAG, str);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logText.setText(String.format("%s\n%s", str, logText.getText()));
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case AppConstant.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    setupBluetooth();
                } else {
                    Toast.makeText(MainActivity.this, "YOU MUST enable bluetooth to use bluetooth :-/", Toast.LENGTH_SHORT)
                            .show();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btService.destroy(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case AppConstant.REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    btService.findAvailableDevices(this);
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "YOU MUST ALLOW Location permission to use bluetooth", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
