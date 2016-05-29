package com.example.nico.sendbluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nico.sendbluetooth.R;


public class MainActivity extends Activity {

    // UUID fuer Kommunikation mit Seriellen Modulen
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String LOG_TAG = "SendBluetooth";

    // Variablen
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter adapter = null;
    private BluetoothSocket socket = null;
    private OutputStream stream_out = null;
    private InputStream stream_in = null;
    private boolean is_connected = false;
    private String TouchListener = null;
    private int verbindung_counter = 0;
    private static final int REQUEST_CONNECT = 1;
    private static final int REQUEST_CONNECT_FAILED = 1;
    String mac_adresse ="blub";
    private SeekBar speedBar;
    private String speed = "0";
    private String empfangen;
    private String LastSend = null;
    private String regler_parameter = "0";

    //----------------------------------------------------------------------------------------------OnStart
    @Override
    protected void onStart() {
        super.onStart();
        Log.i("TEST", "OnStart");
        // If Bluetooth is not on, request that it be enabled.
        if (adapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            onDestroy();
        }
    }

    //----------------------------------------------------------------------------------------------OnCreate
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("TEST", "OnCreate Anfang");
        setContentView(R.layout.activity_main);
        //------------------------------------------------------------------------------------------Countdown Timer zum Senden und Empfangen

        CountDownTimer Timer = new CountDownTimer(1000, 10) {
            public void onTick(long millisUntilFinished) {


            }
            public void onFinish() {
                //handshake();
                if (is_connected) {
                    senden("h");
                    empfangen = empfangen();

                    if (empfangen.indexOf("Handshake") != -1) {
                        Log.i("Handshake", "Zurücksetzen " + verbindung_counter);
                        verbindung_counter = 0;
                    } else {
                        verbindung_counter++;
                        Log.i("Handshake", "Missing " + verbindung_counter);
                    }

                    if (verbindung_counter > 3) { //5 Sek Timeout
                        Log.w("Connection", "Handshake error");
                        senden("X");
                        //trennen(null);
                        verbindung_counter = 0;

                    }
                }
                this.start();
                //Log.i("Timer", "Finished");
            }
        };
        Timer.start();
//--------------------------------------------------------------------------------------------------TouchListener Backward
        ImageButton Backward = (ImageButton) findViewById(R.id.Backward);
        Backward.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    Log.i("Movement", "S.ACTION_DOWN");
                    TouchListener = "S";
                    senden("S");
                } else if (action == MotionEvent.ACTION_UP) {
                    Log.i("Movement", "S.ACTION_UP");
                    TouchListener = null;
                    senden("X");
                }
                return false;
            }

        });
//--------------------------------------------------------------------------------------------------TouchListener Forward
        ImageButton Forward = (ImageButton) findViewById(R.id.Forward);
        Forward.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    Log.i("Movement", "W.ACTION_DOWN");
                    TouchListener = "W";
                    senden("W");
                } else if (action == MotionEvent.ACTION_UP) {
                    Log.i("Movement", "W.ACTION_UP");
                    TouchListener = null;
                    senden("X");
                }
                return false;
            }

        });
//--------------------------------------------------------------------------------------------------TouchListener Left
        ImageButton Left = (ImageButton) findViewById(R.id.Left);
        Left.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    Log.i("Movement", "A.ACTION_DOWN");
                    TouchListener = "A";
                    senden("A");
                } else if (action == MotionEvent.ACTION_UP) {
                    Log.i("Movement", "A.ACTION_UP");
                    TouchListener = null;
                    senden("X");
                }
                return false;
            }

        });
//--------------------------------------------------------------------------------------------------TouchListener Right
        ImageButton Right = (ImageButton) findViewById(R.id.Right);
        Right.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View view, MotionEvent motionevent) {
                int action = motionevent.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    Log.i("Movement", "D.ACTION_DOWN");
                    TouchListener = "D";
                    senden("D");
                } else if (action == MotionEvent.ACTION_UP) {
                    Log.i("Movement", "D.ACTION_UP");
                    TouchListener = null;
                    senden("X");
                }
                return false;
            }

        });
        //------------------------------------------------------------------------------------------SpeedBarListener
        speedBar = (SeekBar) findViewById(R.id.SpeedBar1);
        final TextView textView = (TextView) findViewById(R.id.textView);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                //Toast.makeText(getApplicationContext(), "Changing seekbar's progress", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                textView.setText("Covered: " + progress + "/" + seekBar.getMax());
                if (progress < 100 )
                {
                    speed = "G0" + progress + "Z0" + progress + "E";
                }
                else if (progress < 10 )
                {
                    speed = "G00" + progress + "Z00" + progress + "E";
                }
                else
                {
                    speed = "G" + progress + "Z" + progress + "E";
                }
                Log.i(LOG_TAG, "Speed: " + speed);
                senden(speed);
            }

        });

//--------------------------------------------------------------------------------------------------Verbindung zu Bluetooth Adapter herstellen
        Log.d(LOG_TAG, "Bluetest: OnCreate");

        // Verbindung mit Bluetooth-Adapter herstellen
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

            return;
        } else
            Log.d(LOG_TAG, "onCreate: Bluetooth-Adapter ist bereit");
    }
//--------------------------------------------------------------------------------------------------Ende OnCreate
    //----------------------------------------------------------------------------------------------Handshake
    private void handshake() {
        if (is_connected) {
            senden("h");


            if (empfangen.indexOf("Handshake") != -1) {
                Log.i("Handshake", "Zurücksetzen " + verbindung_counter);
                verbindung_counter = 0;
            } else {
                verbindung_counter++;
                Log.i("Handshake", "Missing " + verbindung_counter);
            }

            if (verbindung_counter > 3) { //5 Sek Timeout
                Log.w("Connection", "Handshake error");
                senden("X");
                trennen(null);
                verbindung_counter = 0;

            }
        }
    }
    //---------------------------------------------------------Reglerparameter senden
    public void send_parameter(View v){
        EditText text_P = (EditText) findViewById(R.id.editText_P);
        EditText text_I = (EditText) findViewById(R.id.editText_I);
        EditText text_D = (EditText) findViewById(R.id.editText_D);
        regler_parameter = "R" + text_P.getText() + "Z" + text_I.getText() + "Y" + text_D.getText();

        Log.i(LOG_TAG, "Regler: " + regler_parameter);
        senden(regler_parameter);
    }
    //----------------------------------------------------------Verbinden Button
    public void verbinden(View v) {
        // Start DeviceListActivity
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT);
    }
    //----------------------------------------------------------------------------------------------onActivityResult
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(LOG_TAG, "RequestCode: " + requestCode + "REQUEST_CONNECT" + REQUEST_CONNECT);
        if (requestCode == 1 && data !=  null) {

                Log.i(LOG_TAG, "resultCode: " + resultCode + "DeviceListActivity.RESULT_OK" + Activity.RESULT_OK);
                // DeviceListActivity gibt MAC-Adresse des zu verbindenden Geräts zurück
                if (resultCode == RESULT_OK) {
                        Log.i("sendBluetooth", data.getStringExtra("device_address"));
                        mac_adresse = data.getStringExtra("device_address");
                        connect();
                    //}
                }
        }
    }
    //--------------------------------------------------------------------------------------------------Connect
    public void connect() {

        Log.d(LOG_TAG, "Verbinde mit " + mac_adresse);
        BluetoothDevice remote_device = adapter.getRemoteDevice(mac_adresse);

        // Socket erstellen
        try {
            socket = remote_device
                    .createInsecureRfcommSocketToServiceRecord(uuid);
            Log.d(LOG_TAG, "Socket erstellt");
        } catch (Exception e) {
            Log.e(LOG_TAG, "Socket Erstellung fehlgeschlagen: " + e.toString());
        }

        adapter.cancelDiscovery();

        // Socket verbinden
        try {
            socket.connect();
            Log.d(LOG_TAG, "Socket verbunden");
            is_connected = true;
        } catch (IOException e) {
            is_connected = false;
            Log.e(LOG_TAG, "Socket kann nicht verbinden: " + e.toString());
        }

        // Socket beenden, falls nicht verbunden werden konnte
        if (!is_connected) {
            try {
                socket.close();
            } catch (Exception e) {
                Log.e(LOG_TAG,
                        "Socket kann nicht beendet werden: " + e.toString());
            }
        }

        // Outputstream erstellen:
        try {
            stream_out = socket.getOutputStream();
            Log.d(LOG_TAG, "OutputStream erstellt");
        } catch (IOException e) {
            Log.e(LOG_TAG, "OutputStream Fehler: " + e.toString());
            is_connected = false;
        }

        // Inputstream erstellen
        try {
            stream_in = socket.getInputStream();
            Log.d(LOG_TAG, "InputStream erstellt");
        } catch (IOException e) {
            Log.e(LOG_TAG, "InputStream Fehler: " + e.toString());
            is_connected = false;
        }

        if (is_connected) {
            Toast.makeText(this, "Verbunden mit " + mac_adresse,
                    Toast.LENGTH_LONG).show();
            ((Button) findViewById(R.id.bt_verbinden)).setBackgroundColor(Color.GREEN);
        }
        else {
            Toast.makeText(this, "Verbindungsfehler mit " + mac_adresse,
                    Toast.LENGTH_LONG).show();
            ((Button) findViewById(R.id.bt_verbinden))
                    .setBackgroundColor(Color.LTGRAY);
        }
    }
    //----------------------------------------------------------------------------------------------Text Senden
    public void senden(String message) {
        byte[] msgBuffer = message.getBytes();
        LastSend = message;
        if (is_connected) {
            if ( message != "X") {
                Log.d(LOG_TAG, "Sende Nachricht: " + message);
            }
            try {
                stream_out.write(msgBuffer);



            } catch (IOException e) {
                Log.e(LOG_TAG,
                        "Bluetest: Exception beim Senden: " + e.toString());
            }
        }
    }
    //--------------------------------------------------------------------------------------------------Empfangen
    public String empfangen() {
        byte[] buffer = new byte[1024]; // Puffer
        int laenge; // Anzahl empf. Bytes
        String msg = "";
        try {
            if (stream_in.available() > 0) {
                laenge = stream_in.read(buffer);
                Log.d(LOG_TAG,
                        "Anzahl empfangender Bytes: " + String.valueOf(laenge));

                // Message zusammensetzen:
                for (int i = 0; i < laenge; i++)
                {
                    msg += (char) buffer[i];

                }
                Log.d(LOG_TAG, "Message: " + msg);
            }

        } catch (Exception e) {
            Log.e(LOG_TAG, "Fehler beim Empfangen: " + e.toString());
        }
        return msg;
    }
    //----------------------------------------------------------------------------------------------Verbindung trennen
    public void trennen(View v) {
        senden("X");
        if (is_connected && stream_out != null) {
            is_connected = false;
            ((Button) findViewById(R.id.bt_verbinden))
                    .setBackgroundColor(Color.RED);
            Log.d(LOG_TAG, "Trennen: Beende Verbindung");
            try {
                stream_out.flush();
                socket.close();
                ((Button) findViewById(R.id.bt_verbinden))
                        .setBackgroundColor(Color.LTGRAY);

            } catch (IOException e) {
                Log.e(LOG_TAG,
                        "Fehler beim beenden des Streams und schliessen des Sockets: "
                                + e.toString());
            }
        } else
            Log.d(LOG_TAG, "Trennen: Keine Verbindung zum beenden");
    }
    //----------------------------------------------------------------------------------------------Verbindung beim Beenden der App trennen
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy. Trenne Verbindung, falls vorhanden");
        trennen(null);
    }


}