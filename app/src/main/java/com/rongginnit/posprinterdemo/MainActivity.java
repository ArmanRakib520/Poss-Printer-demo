package com.rongginnit.posprinterdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    TextView myLabel, myTextbox, thankyou, txtphone, txtemail, txtticket, txttotalAmount, dateTimeDisplay;
    EditText etAmount, etTicketNumber;
    Button openButton,sendButton,closeButton;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;

    private Calendar calendar;
    private SimpleDateFormat dateFormat;
    private String date;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();
        isInternetOn();

    }

    private void initView() {
        setContentView(R.layout.activity_main);
        openButton = findViewById(R.id.open);
        sendButton = findViewById(R.id.send);
        closeButton =  findViewById(R.id.close);
        myLabel = findViewById(R.id.label);
        myTextbox =  findViewById(R.id.entry);
        dateTimeDisplay = findViewById(R.id.text_date_display);
        etAmount = findViewById(R.id.amount);
        thankyou = findViewById(R.id.thank);
        txtphone = findViewById(R.id.txt_phone);
        txtemail = findViewById(R.id.txt_email);
        txtticket = findViewById(R.id.txt_ticket);
        etTicketNumber = findViewById(R.id.amount_of_ticket);
        txttotalAmount = findViewById(R.id.total_amount);

        calendar = Calendar.getInstance();

        dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy, h:mm:ss a");
        date = dateFormat.format(calendar.getTime());
        dateTimeDisplay.setText(date);

        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    findBluetooth();
                    openBluetooth();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int a = Integer.parseInt(etAmount.getText().toString().trim());
                int b = Integer.parseInt(etTicketNumber.getText().toString().trim());
                int total = a * b;
                txttotalAmount.setText(String.valueOf(total));
                try {
                    sendData();
                    cleardata();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    closeBluetooth();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void findBluetooth() {

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Log.i("device",mBluetoothAdapter.getName());;

            if(mBluetoothAdapter == null) {
                myLabel.setText(R.string.bluetooth_adapter_not_available);
            }

            if(!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetooth, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if(pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    Log.i("name", device.getName());
                    if (device.getName().equals("RPP02N")) {
                        mmDevice = device;
                        myLabel.setText(R.string.bluetooth_found);
                        break;
                    }
                }
            }else {
                myLabel.setText("Not Found");
            }



        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void openBluetooth() throws IOException {
        try {

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();

            beginListenForData();

            myLabel.setText( R.string.bluetooth_open);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeBluetooth() throws IOException {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            myLabel.setText(R.string.bluetooth_close);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendData() throws IOException {

        try {
            String msg = "     " + myTextbox.getText().toString().toUpperCase()
                    + "\n" + "" + txtphone.getText().toString()
                    + "\n" + "    " + txtemail.getText().toString()
                    + "\n" + "          " + txtticket.getText().toString()
                    + "\n" + dateTimeDisplay.getText().toString()
                    + "\n" + "------------------------------"
                    + "\n" + "Total Ticket:  " + etTicketNumber.getText().toString()
                    + "\n" + "Ticket Price:  " + etAmount.getText().toString() + " /- "
                    + "\n" + "---------------------"
                    + "\n" + "Total Amount: " + txttotalAmount.getText().toString() + " /- "
                    + "\n" + "------------------------------"
                    + "\n" + "          " + thankyou.getText().toString().toUpperCase()
                    + "\n" + "\n" + "\n" + "\n";

            msg += "\n";
            mmOutputStream.write(msg.getBytes());
            myLabel.setText(R.string.sent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cleardata() {
        etAmount.setText("");
        etTicketNumber.setText("");
        txttotalAmount.setText("0");
    }

    private void beginListenForData() {
        try {
            final Handler handler = new Handler();
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(new Runnable() {
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                        try {

                            int bytesAvailable = mmInputStream.available();

                            if (bytesAvailable > 0) {

                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);

                                for (int i = 0; i < bytesAvailable; i++) {

                                    byte b = packetBytes[i];
                                    if (b == delimiter) {

                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedBytes, 0,
                                                encodedBytes.length
                                        );

                                        final String data = new String(encodedBytes, "US-ASCII");
                                        readBufferPosition = 0;

                                        handler.post(new Runnable() {
                                            public void run() {
                                                myLabel.setText(data);
                                            }
                                        });

                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }

                        } catch (IOException ex) {
                            stopWorker = true;
                        }
                    }
                }
            });
            workerThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isInternetOn() {

        ConnectivityManager connect = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // Check for network connections
        if (connect.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||
                connect.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connect.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connect.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {

            Toast.makeText(this, "Network Connected", Toast.LENGTH_LONG).show();

            return true;

        } else if (
                connect.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connect.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED) {

            Toast.makeText(this, "Connect to Network ", Toast.LENGTH_LONG).show();
            return false;
        }
        return false;
    }
}
