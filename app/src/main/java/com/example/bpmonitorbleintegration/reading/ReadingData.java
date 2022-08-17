package com.example.bpmonitorbleintegration.reading;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.util.Range;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bpmonitorbleintegration.R;
import com.example.bpmonitorbleintegration.bleconnect.BLEService;
import com.example.bpmonitorbleintegration.bleconnect.Decoder;
import com.example.bpmonitorbleintegration.constants.BLEGattAttributes;
import com.example.bpmonitorbleintegration.constants.Constants;
import com.example.bpmonitorbleintegration.database.RoomDB;
import com.example.bpmonitorbleintegration.home.HomePage;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

//https://stackoverflow.com/questions/69529502/countdown-timer-which-runs-in-the-background-in-android - for background timer

public class ReadingData extends AppCompatActivity {

    private BLEService mBluetoothLeService;
    private String deviceAddress;
    public BluetoothGattCharacteristic mNotifyCharacteristic;
    BluetoothDevice bluetoothDevice;
    IntentFilter intentFilter;
    AlertDialog.Builder builder1;
    Decoder decoder;
    public AlertDialog dialog, dialog1;
    RoomDB localDB;

    BluetoothAdapter bAdapter;
    public int counter = 0;
    private CountDownTimer mCountDownTimer;
    private long mTimeLeftInMillis = 2000;
    private final long startTime = 50;
    private boolean mTimerRunning;
    private final String TAG = ReadingData.class.getName();

    private TextView statusText, batteryText, systolicText, diastolicText, heartRateText, mapText, progressText, statusText1;
    private Button startBtn;
    private Button stopBtn;
    Button saveReadingBtn;
    private ProgressBar progressBar, progress;
    int i = 0;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_reading_data1);

        //To change status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(ReadingData.this, R.color.blue_200));
        View decorView = getWindow().getDecorView(); //set status background black
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); //set status text  light

        statusText = findViewById(R.id.actual_status);
        batteryText = findViewById(R.id.battery_level);
        systolicText = findViewById(R.id.systalic_val);
        diastolicText = findViewById(R.id.dystalic_val);
        heartRateText = findViewById(R.id.rate_val);
        mapText = findViewById(R.id.range_val);
        progressText = findViewById(R.id.progress_text);
        progress = findViewById(R.id.progress_start);
        progressBar = findViewById(R.id.progress_bar);
        startBtn = findViewById(R.id.start_reading);
        stopBtn = findViewById(R.id.stop_reading);
        saveReadingBtn = (Button) findViewById(R.id.save_result1);
        statusText1 = findViewById(R.id.map1);
        bAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceAddress = getIntent().getStringExtra("Device");
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(broadCastReceiver, intentFilter);

        //Initialising Decoder class.
        decoder = new Decoder();
        localDB = new RoomDB();

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.readings);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#151B54")));
//        pref = PreferenceManager.getDefaultSharedPreferences(this);

        // Connect to the device through BLE.
        registerReceiver(broadCastReceiver, GattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mBluetoothLeService.close();
                mBluetoothLeService.disconnect();
                mBluetoothLeService.stopSelf();
                boolean result = mBluetoothLeService.connect(deviceAddress);
                if (!result) {
                    finish();
                }
            }
        }

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
//                    Constants.is_ackReceived = false;
                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                            Log.i(TAG, "Force stop value after checksum " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);

                    // Checking whether received Ack or not
                    mCountDownTimer = new CountDownTimer(startTime, 10) {
                        @Override
                        public void onTick(long l) {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (Constants.is_ackReceived == true) {
                                        mTimerRunning = false;
                                        mCountDownTimer.cancel();
                                        progressText.setText("---");

                                        startBtn.setEnabled(true);
                                        startBtn.setVisibility(View.VISIBLE);
                                        stopBtn.setVisibility(View.INVISIBLE);
                                        stopBtn.setEnabled(false);
                                    }
//                                    mCountDownTimer = new CountDownTimer(30, 10) {
//                                        @Override
//                                        public void onTick(long l) {
//                                     //       mTimerRunning = true;
//                                            counter++;
////                                            Log.i(TAG, "counter Started " + startTime);
//                                        }
//
//                                        @Override
//                                        public void onFinish() {
////                                            Log.i(TAG, "Stopped");
//                                            runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
////                                                    Log.i(TAG, "run: cuff replaced before condition " + Constants.is_cuffReplaced);
//                                                    if (Constants.is_ackReceived) {
//                                                        mTimerRunning = false;
//                                                        mCountDownTimer.cancel();
//                                                        progressText.setText("---");
//
//                                                        startBtn.setEnabled(true);
//                                                        startBtn.setVisibility(View.VISIBLE);
//                                                        stopBtn.setVisibility(View.INVISIBLE);
//                                                        stopBtn.setEnabled(false);
//                                                    }
//
//                                                }
//                                            });
//                                        }
//                                    }.start();
                                }
                            });
                        }

                        @Override
                        public void onFinish() {
//                                            Log.i(TAG, "Stopped");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (mTimerRunning) {
                                        if (!Constants.is_ackReceived) {
//                                                Log.i(TAG, "Start again");
                                            Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
                                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                            start();
                                        }
                                    }
                                   else {
                                       Constants.is_ackReceived = false;
                                    }
                                }
                            });
                        }
                    }.start();
                    if (Constants.is_finalResult == true) {
                        systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
                        diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
                        heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
                        String status = changeStatus(mBluetoothLeService.systalic,mBluetoothLeService.dystolic);
                        mapText.setText(status);
                        statusText1.setText(String.valueOf(mBluetoothLeService.range));
//                        mapText.setText(String.valueOf(mBluetoothLeService.range));
//                        localDB.saveTask(deviceAddress, mBluetoothLeService.systalic, mBluetoothLeService.dystolic, mBluetoothLeService.rate, mBluetoothLeService.range, ReadingData.this);
                        Constants.is_finalResult = false;
                    }
                }
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Constants.is_buttonStarted = true;
                Constants.is_cuffReplaced = false;
                Constants.is_ackReceived = false;
                Constants.is_readingStarted = false;
             //
                if (mNotifyCharacteristic != null) {
                    Constants.startValue = decoder.computeCheckSum(Constants.startValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.startValue);
                }

                systolicText.setText("");
                diastolicText.setText("");
                heartRateText.setText("");
                mapText.setText("");
//                startBtn.setText("Cancel");
            }
        });

        saveReadingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress.setVisibility(View.VISIBLE);
                if (systolicText.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.check_value), Toast.LENGTH_SHORT).show();
                    progress.setVisibility(View.GONE);
                } else if (diastolicText.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),getApplication().getResources().getString(R.string.check_value), Toast.LENGTH_SHORT).show();
                    progress.setVisibility(View.GONE);
                } else if (heartRateText.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),getApplication().getResources().getString(R.string.check_value), Toast.LENGTH_SHORT).show();
                    progress.setVisibility(View.GONE);
                } else if (mapText.getText().toString().equals("")) {
                    Toast.makeText(getApplicationContext(),getApplication().getResources().getString(R.string.check_value), Toast.LENGTH_SHORT).show();
                    progress.setVisibility(View.GONE);
                } else {
                    localDB.saveTask(deviceAddress, Integer.parseInt(systolicText.getText().toString()), Integer.parseInt(diastolicText.getText().toString()), Integer.parseInt(heartRateText.getText().toString()), mBluetoothLeService.range, ReadingData.this);
//                    localDB.saveTask(deviceAddress, mBluetoothLeService.systalic, mBluetoothLeService.dystolic, mBluetoothLeService.rate, mBluetoothLeService.range, ReadingData.this); Integer.parseInt(mapText.getText().toString())
                    systolicText.setText("");
                    diastolicText.setText("");
                    heartRateText.setText("");
                    mapText.setText("");
                    progress.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Connect to the device through BLE.
        registerReceiver(broadCastReceiver, GattUpdateIntentFilter());

//        if (mBluetoothLeService != null) {
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                mBluetoothLeService.close();
//                mBluetoothLeService.disconnect();
//                mBluetoothLeService.stopSelf();
//                boolean result = mBluetoothLeService.connect(deviceAddress);
//                if (!result) {
//                    finish();
//                }
//            }
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Disconnect through services.
        unregisterReceiver(broadCastReceiver);
//        On app minimize or sleep mode need to disconnect from services & BLE.
        progressText.setText("---");
        if (Constants.is_finalResult == true) {
            systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
            diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
            heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
            String status = changeStatus(mBluetoothLeService.systalic,mBluetoothLeService.dystolic);
            mapText.setText(status);
            statusText1.setText(String.valueOf(mBluetoothLeService.range));
            Constants.is_finalResult = false;
//            startBtn.setEnabled(true);
//            startBtn.setVisibility(View.VISIBLE);
//            stopBtn.setVisibility(View.INVISIBLE);
//            stopBtn.setEnabled(false);

            if ((dialog == null) || !dialog.isShowing()) {
                dialog = new AlertDialog.Builder(ReadingData.this)
                        .setTitle(getApplicationContext().getResources().getString(R.string.message))
                        .setMessage(R.string.save_final_reading)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                localDB.saveTask(deviceAddress, Integer.parseInt(systolicText.getText().toString()), Integer.parseInt(diastolicText.getText().toString()), Integer.parseInt(heartRateText.getText().toString()), mBluetoothLeService.range, ReadingData.this);
                                //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                                    startActivity(intent1);
                                }
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialog.dismiss();
                                //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                                    startActivity(intent1);
                                }
                            }
                        }).show();
            }
        }
        else {
            //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                startActivity(intent1);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //To bind the service connection.
        Intent getServiceIntent = new Intent(ReadingData.this, BLEService.class);
        bindService(getServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind service connection.
        unbindService(mServiceConnection);
    }

    //Menu item.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.data_menu_file, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
// To disconnect from bluetooth
        if (item.getItemId() == R.id.disable_bluetooth) {
            if ((dialog == null) || !dialog.isShowing()) {
                dialog = new AlertDialog.Builder(ReadingData.this)
                        .setTitle(getApplicationContext().getResources().getString(R.string.message))
                        .setMessage(getApplicationContext().getResources().getString(R.string.are_you_sure_disable_bluetooth))
                        .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                bAdapter = BluetoothAdapter.getDefaultAdapter();

                                bAdapter.disable();
                                mBluetoothLeService.disconnect();
                                mBluetoothLeService.close();
                                //Navigating to next activity on tap of ok button.
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    Intent intent = new Intent(ReadingData.this, HomePage.class);
                                    startActivity(intent);
//                                finish();
                                }
                            }
                        })
                        .setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialog.dismiss();
                            }
                        }).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    // To add actions to intent filter.
    private static IntentFilter GattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_GATT_CONNECTED);
        intentFilter.addAction(Constants.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(Constants.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    //Broadcast receiver to receive services and data.
    private final BroadcastReceiver broadCastReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            boolean mConnected;

            // To update connection status
            if (Constants.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(getApplicationContext().getResources().getString(R.string.connected));
            }

            else if (Constants.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(getApplicationContext().getResources().getString(R.string.disconnected));
            }

            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        bAdapter.disable();
                        mBluetoothLeService.disconnect();
                        mBluetoothLeService.close();
                        //Navigating to next activity on tap of ok button.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            Intent intent1 = new Intent(ReadingData.this, HomePage.class);
                            startActivity(intent1);
                        }
                        break;
                }
            }

            // To receive services and characteristics
            else if (Constants.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //Receive services and characteristics
                List<BluetoothGattService> gattService = mBluetoothLeService.getSupportedGattServices();
//                Log.i("TAG", "Size " + gattService.size());
                for (BluetoothGattService service : gattService)
                {
//                    Log.i(TAG, BLEGattAttributes.CLIENT_SERVICE_CONFIG);
                    if(service.getUuid().toString().equalsIgnoreCase(BLEGattAttributes.CLIENT_SERVICE_CONFIG))
                    {
                        List<BluetoothGattCharacteristic> gattCharacteristics =
                                service.getCharacteristics();
//                        Log.i(TAG, "Count is:" + gattCharacteristics.size());
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
                        {
                            if(gattCharacteristic.getUuid().toString().equalsIgnoreCase(BLEGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
                            {
//                                Log.i(TAG, gattCharacteristic.getUuid().toString());
                                mNotifyCharacteristic = gattCharacteristic;
                                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                                return;
                            }
                        }
                    }
                }
            }

            //To receive data.
            else if (Constants.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(Constants.EXTRA_DATA));
            }
        }
    };

    // To Display data on text field and popups
    private  void displayData(String data) {
//        Toast.makeText(getApplicationContext(), "received data before" + data, Toast.LENGTH_SHORT).show();
        if (data != null) {

            if (Constants.is_ackReceived == true)
            {
                startBtn.setEnabled(false);
                startBtn.setVisibility(View.INVISIBLE);
                stopBtn.setVisibility(View.VISIBLE);
                stopBtn.setEnabled(true);
                mTimerRunning = false;

                // To display raw readings
                if (Constants.is_readingStarted == true) {
                    progressText.setText(data);
//                progressText.setText(mBluetoothLeService.rawReadings);
                }
            }

            mCountDownTimer = new CountDownTimer(500, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                    mTimeLeftInMillis = millisUntilFinished;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            progress.setVisibility(View.GONE);
//
//                            if (Constants.is_ackReceived)
//                            {
////                                startBtn.setEnabled(false);
//                                startBtn.setVisibility(View.INVISIBLE);
//                                stopBtn.setVisibility(View.VISIBLE);
//                                stopBtn.setEnabled(true);

                                mCountDownTimer.cancel();
                                mTimerRunning = false;


                                // To display raw readings
//                                if (Constants.is_readingStarted) {
//                                    progressText.setText(data);
//                                progressText.setText(mBluetoothLeService.rawReadings);
//                                    Constants.is_readingStarted = false;
//                                }

                                //To display error msgs.
                                if (Constants.is_errorReceived == true) {
                                    progressText.setText(data);
                                    Constants.is_errorReceived = false;
                                }

                                // To display final results.
                                if (Constants.is_finalResult == true) {
                                    if ((mBluetoothLeService.systalic < 30) || (mBluetoothLeService.systalic > 200)){
                                        Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.systolic_error), Toast.LENGTH_SHORT).show();
                                    }
                                    else if ((mBluetoothLeService.dystolic < 40) || (mBluetoothLeService.dystolic > 120)) {
                                        Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.diastolic_error), Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        progressText.setText(data);
                                    }
                                }
//                            }

                            // To display battery status popup while receiving the data
                            mCountDownTimer = new CountDownTimer(50, 10) {
                                @Override
                                public void onTick(long l) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            //  If battery low popup alert messages.
                                            if (Constants.is_batteryReceivedAtReading == true) {
                                                mTimerRunning = false;
                                                mCountDownTimer.cancel();
                                                progressText.setText("---");
                                                //                                                            .setMessage(data)
                                                if (mBluetoothLeService.errorMessage.equals(getString(R.string.battery_limit_exceeds))) {
                                                    batteryText.setBackgroundColor(Color.parseColor("#A41E22"));
                                                    progressText.setText("---");
                                                }
                                                else {
                                                    batteryText.setBackgroundColor(Color.parseColor("#FF0000"));
                                                    progressText.setText("---");
                                                }
                                                if ((dialog == null) || !dialog.isShowing()) {
                                                    dialog = new AlertDialog.Builder(ReadingData.this)
                                                            .setTitle(getApplicationContext().getResources().getString(R.string.message))
                                                            .setMessage(data)
//                                            .setMessage(mBluetoothLeService.errorMessage)
                                                            .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
                                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                                                    startBtn.setEnabled(true);
                                                                    startBtn.setVisibility(View.VISIBLE);
                                                                    stopBtn.setVisibility(View.INVISIBLE);
                                                                    stopBtn.setEnabled(false);
                                                                    progressText.setText("---");
//                                                    dialog.dismiss();
                                                                }
                                                            })
                                                            .setCancelable(false)
                                                            .show();
                                                }
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onFinish() {
                                    if (mTimerRunning) {
                                        if (!Constants.is_batteryReceivedAtReading) {
                                            mCountDownTimer.cancel();
                                            Log.d(TAG, "onFinish: Not able to find");
                                        }
                                    }
                                    else {
                                        mCountDownTimer.cancel();
                                        Constants.is_batteryReceivedAtReading = false;
                                    }
                                }
                            }.start();

                            // To display irregular heartbeat popup.
                            mCountDownTimer = new CountDownTimer(50, 10) {
                                @Override
                                public void onTick(long l) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Irregular heart beat alert popup
                                            if (Constants.is_irregularHB == true) {
                                                mTimerRunning = false;
                                                mCountDownTimer.cancel();
                                                progressText.setText("---");
                                                if ((dialog == null) || !dialog.isShowing()) {
                                                    dialog = new AlertDialog.Builder(ReadingData.this)
                                                            .setTitle(getApplicationContext().getResources().getString(R.string.message))
//                                                           .setMessage(data)
                                                            .setMessage(mBluetoothLeService.errorMessage)
                                                            .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
                                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                                                    startBtn.setEnabled(true);
                                                                    startBtn.setVisibility(View.VISIBLE);
                                                                    stopBtn.setVisibility(View.INVISIBLE);
                                                                    stopBtn.setEnabled(false);
                                                                    progressText.setText("---");
//                                                    dialog.dismiss();
                                                                }
                                                            }).setCancelable(false)
                                                            .show();
                                                }
                                            }
                                        }
                                    });
                                }
                                @Override
                                public void onFinish() {
                                    if (mTimerRunning) {
                                        if (!Constants.is_irregularHB) {
                                            mCountDownTimer.cancel();
                                            Log.d(TAG, "onFinish: not found");
                                        }
                                    }
                                    else {
                                        mCountDownTimer.cancel();
                                        Constants.is_irregularHB = false;
                                    }
                                }
                            }.start();

//                            if ((Constants.is_resultReceived) || (Constants.is_readingStarted)) {
                            // To display cuff replacement message popup
                            mCountDownTimer = new CountDownTimer(100, 50) {
                                @Override
                                public void onTick(long l) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (Constants.is_cuffReplaced == true) {
                                                mTimerRunning = false;
                                                mCountDownTimer.cancel();
                                                progressText.setText("---");

                                                if ((dialog1 == null) || !dialog1.isShowing()) {
                                                    builder1 = new AlertDialog.Builder(ReadingData.this);
                                                    builder1.setTitle(getApplicationContext().getResources().getString(R.string.message));
                                                    builder1.setMessage(getApplicationContext().getResources().getString(R.string.cuff_replaced));
                                                    // On click ok button reset command will be sent
                                                    builder1.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                        @Override

                                                        public void onClick(DialogInterface dialogInterface, int which) {
                                                            Constants.resetValue = decoder.computeCheckSum(Constants.resetValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                                                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.resetValue);

                                                            mCountDownTimer = new CountDownTimer(100, 10) {
                                                                @Override
                                                                public void onTick(long l) {
                                                                    // mTimerRunning = true;
                                                                    counter++;
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            if (Constants.is_ackReceived == true) {
                                                                                mTimerRunning = false;
                                                                                mCountDownTimer.cancel();
                                                                                dialogInterface.dismiss();
                                                                                dialog1.dismiss();
                                                                                progressText.setText("---");
                                                                                startBtn.setEnabled(true);
                                                                                startBtn.setVisibility(View.VISIBLE);
                                                                                stopBtn.setVisibility(View.INVISIBLE);
                                                                                stopBtn.setEnabled(false);
                                                                            }
                                                                        }
                                                                    });
                                                                }

                                                                @Override
                                                                public void onFinish() {
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            // If ack not received
                                                                            if (mTimerRunning) {
                                                                                if (Constants.is_ackReceived == false) {
                                                                                    dialog1.show();
                                                                                    Log.d(TAG, "run: ack not received while reset");
                                                                                    Constants.resetValue = decoder.computeCheckSum(Constants.resetValue);
////          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                                                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.resetValue);
                                                                                    start();
                                                                                }
                                                                            }
                                                                            else
                                                                            {
                                                                                mCountDownTimer.cancel();
                                                                                Log.d(TAG, "run: timer off");
                                                                                Constants.is_ackReceived = false;
                                                                                progressText.setText("---");
                                                                                startBtn.setEnabled(true);
                                                                                startBtn.setVisibility(View.VISIBLE);
                                                                                stopBtn.setVisibility(View.INVISIBLE);
                                                                                stopBtn.setEnabled(false);
                                                                                dialog1.dismiss();
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }.start();
                                                        }
                                                    });
                                                    // On click cancel button no-reset command will be sent
                                                    builder1.setNegativeButton(getApplicationContext().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {

                                                            Constants.noResetValue = decoder.computeCheckSum(Constants.noResetValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                                                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.noResetValue);
//                Log.i(TAG, "run: ack in ok before timer " + Constants.is_ackReceived);

                                                            // To check the ack after the reset command sent
                                                            mCountDownTimer = new CountDownTimer(100, 10) {
                                                                @Override
                                                                public void onTick(long l) {
                                                                    counter++;
//                                                Log.i(TAG, "counter Started " + startTime);
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {

                                                                            if (Constants.is_ackReceived == true) {
                                                                                mTimerRunning = false;
                                                                                mCountDownTimer.cancel();
                                                                                dialogInterface.dismiss();
                                                                                dialog1.dismiss();
                                                                                progressText.setText("---");
                                                                                startBtn.setEnabled(true);
                                                                                startBtn.setVisibility(View.VISIBLE);
                                                                                stopBtn.setVisibility(View.INVISIBLE);
                                                                                stopBtn.setEnabled(false);
//                                    Log.i(TAG, "run: ack in cancel condition " + Constants.is_ackReceived);
                                                                            }
                                                                        }
                                                                    });
                                                                }

                                                                @Override
                                                                public void onFinish() {
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            // If ack not received
                                                                            if (mTimerRunning) {
                                                                                if (Constants.is_ackReceived == false) {
                                                                                    dialog1.show();
                                                                                    Log.d(TAG, "run: ack not received while non reset");
                                                                                    Constants.noResetValue = decoder.computeCheckSum(Constants.noResetValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                                                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.noResetValue);
                                                                                    start();
                                                                                }
                                                                            }
                                                                            else
                                                                            {
                                                                                mCountDownTimer.cancel();
                                                                                Log.d(TAG, "run: timer off");
                                                                                Constants.is_ackReceived = false;
                                                                                progressText.setText("---");
                                                                                startBtn.setEnabled(true);
                                                                                startBtn.setVisibility(View.VISIBLE);
                                                                                stopBtn.setVisibility(View.INVISIBLE);
                                                                                stopBtn.setEnabled(false);
                                                                                dialog1.dismiss();
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }.start();
                                                        }
                                                    });
                                                    //To create alert dialog
                                                    dialog1 = builder1.create();
                                                    //Prevent dialog box from getting dismissed on back key pressed
                                                    dialog1.setCancelable(false);
                                                    //Prevent dialog box from getting dismissed on outside touch
                                                    dialog1.setCanceledOnTouchOutside(false);
                                                    //To show alert dialog
                                                    dialog1.show();
                                                }
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onFinish() {

                                    if (mTimerRunning) {
                                        if (!Constants.is_cuffReplaced) {
                                            mCountDownTimer.cancel();
                                            Log.d(TAG, "onFinish: not able to find");
                                        }
                                    }
                                    else {
                                        mCountDownTimer.cancel();
                                        Constants.is_cuffReplaced = false;
                                    }
                                }
                            }.start();
//                            }
                        }
                    });
                }

                @Override
                public void onFinish() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (mTimerRunning) {
                                if (!Constants.is_ackReceived){
                                    mCountDownTimer.cancel();
                                    Toast.makeText(ReadingData.this,getApplicationContext().getResources().getString(R.string.please_start_again),Toast.LENGTH_SHORT).show();
                                    startBtn.setEnabled(true);
                                    startBtn.setVisibility(View.VISIBLE);
                                    stopBtn.setVisibility(View.INVISIBLE);
                                    stopBtn.setEnabled(false);
                                }
                            }
                            else {
                                mCountDownTimer.cancel();
//                                Constants.is_ackReceived = false;
                            }

                        }
                    });
                }
            }.start();
        }
        else {
            startTimer();
        }
    }

    // Status according to sys/dia ranges.
    private String changeStatus(int systolic, int diastolic) {
        String msg = null;
        if ((systolic < 80) || (diastolic < 60)) {
            msg = getString(R.string.low_bp);
        }
        else if ((systolic <= 120) && (diastolic <= 80)) {
            msg = getString(R.string.normal_bp);
        }
        else if ((systolic <= 139) || (diastolic <= 89)) {
            msg = getString(R.string.high_normal_bp);
        }
        else if ((systolic <= 159) || (diastolic <= 99)) {
            msg = getString(R.string.hypertension_stage_1);
        }
        else if ((systolic <= 179) || (diastolic <= 109)) {
            msg = getString(R.string.hypertension_stage_2);
        }
        else {
            msg = getString(R.string.high_bp_crisis);
        }
        return msg;
    }

    // To receive battery status
    private void startTimer() {
        mCountDownTimer = new CountDownTimer(100, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (Constants.is_batterValueReceived == true)
                        {
                            mTimerRunning = false;
                            mCountDownTimer.cancel();
                            // Showing battery level using color code.
                            showBattery();
                        }
                    }
                });
            }
            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTimerRunning) {
                            if (!Constants.is_batterValueReceived) {
                                mCountDownTimer.cancel();
                                Toast.makeText(ReadingData.this, getApplicationContext().getResources().getString(R.string.please_connect_again), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            mCountDownTimer.cancel();
                            Log.d(TAG, "run: timer off");
                            Constants.is_batterValueReceived = false;
                        }
                    }
                });
            }
        }.start();
    }

    // Change color on basis of battery level/ battery level exceeds alert popup and navigates to home screen.
    public void showBattery(){
        progress.setVisibility(View.GONE);
        if (mBluetoothLeService.batteryLevel == Constants.HIGH_BATTERY) {
            batteryText.setBackgroundColor(Color.parseColor("#008000"));
//            Constants.ack = decoder.computeCheckSum(Constants.ack);
//            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
        }
        else if (mBluetoothLeService.batteryLevel == Constants.MID_BATTERY){
            batteryText.setBackgroundColor(Color.parseColor("#FFA500"));
//            Constants.ack = decoder.computeCheckSum(Constants.ack);
//            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
        }
        else if (mBluetoothLeService.batteryLevel == Constants.LOW_BATTERY) {
            batteryText.setBackgroundColor(Color.parseColor("#FF0000"));
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.battery_low),Toast.LENGTH_SHORT).show();
//            Constants.ack = decoder.computeCheckSum(Constants.ack);
//            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
        }
        else if (mBluetoothLeService.batteryLevel == Constants.HIGH_EXCEEDED) {
            batteryText.setBackgroundColor(Color.parseColor("#A41E22"));
            if ((dialog == null) || !dialog.isShowing()) {
                builder1 = new AlertDialog.Builder(ReadingData.this);
                builder1.setTitle(getApplicationContext().getResources().getString(R.string.message));
                builder1.setMessage(getApplicationContext().getResources().getString(R.string.battery_limit_exceeds));
                builder1.setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                                Constants.ack = decoder.computeCheckSum(Constants.ack);
//                                mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
//                                dialog.dismiss();
                            }
                        });
                dialog = builder1.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        }
    }

    // Connect and disconnect to the services.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BLEService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBluetoothLeService.close();
                mBluetoothLeService.disconnect();
                mBluetoothLeService.stopSelf();
                boolean result = mBluetoothLeService.connect(deviceAddress);
                if (!result) {
                    finish();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent = new Intent(ReadingData.this, MainActivity.class);
                startActivity(intent);
            }
        }
    };

    //Updating connection status through text field. if disconnected, status navigating to mainActivity through alert dialog.
    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText(status);
               // Log.d(TAG, "run: ");
                if (status.equals("Disconnected")){
                    if ((dialog == null) || !dialog.isShowing()) {
                        new AlertDialog.Builder(ReadingData.this)
                                .setTitle(getApplicationContext().getResources().getString(R.string.message))
                                .setMessage(getApplicationContext().getResources().getString(R.string.connection_terminated))
                                .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        //Navigating to next activity on tap of ok button.
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            Intent intent = new Intent(ReadingData.this, MainActivity.class);
                                            startActivity(intent);
                                        }
                                    }
                                }).show();
                    }
                }
            }
        });
    }
}

//Latest
//    private  void displayData1(String data) {
//        if (data != null) {
//
//            // To display raw result after receiving ack.
//            //Declare the timer
//            Timer t = new Timer();
////Set the schedule function and rate
//            t.scheduleAtFixedRate(new TimerTask() {
//
//                                      @Override
//                                      public void run() {
//                                          //Called each time when 1000 milliseconds (1 second) (the period parameter)
//
//                                          runOnUiThread(new Runnable() {
//                                              @Override
//                                              public void run() {
//                                                  // To display raw value.
//                                                  mTimeLeftInMillis -= 1;
//
//                                                  if (mTimeLeftInMillis > 0) {
//                                                      if (Constants.is_ackReceived) {
//                                                          t.cancel();
//
//                                                          if (Constants.is_readingStarted) {
//
//                                                              startBtn.setEnabled(false);
//                                                              startBtn.setVisibility(View.INVISIBLE);
//                                                              stopBtn.setVisibility(View.VISIBLE);
//                                                              stopBtn.setEnabled(true);
////                             progressText.setText(mBluetoothLeService.rawReadings);
//                                                              progressText.setText(data);
//                                                              Constants.is_readingStarted = false;
//                                                          }
//
//                                                      }
//                                                  }
//                                                  else  if (mTimeLeftInMillis == 0) {
//                                                      if (!Constants.is_ackReceived){
//                                                          t.cancel();
//                                                          Toast.makeText(ReadingData.this,getApplicationContext().getResources().getString(R.string.please_start_again),Toast.LENGTH_SHORT).show();
//                                                          startBtn.setEnabled(true);
//                                                          startBtn.setVisibility(View.VISIBLE);
//                                                          stopBtn.setVisibility(View.INVISIBLE);
//                                                          stopBtn.setEnabled(false);
//                                                      }
//                                                      if (!Constants.is_readingStarted) {
//                                                          t.cancel();
//                                                          Toast.makeText(ReadingData.this,getApplicationContext().getResources().getString(R.string.please_start_again),Toast.LENGTH_SHORT).show();
//                                                          startBtn.setEnabled(true);
//                                                          startBtn.setVisibility(View.VISIBLE);
//                                                          stopBtn.setVisibility(View.INVISIBLE);
//                                                          stopBtn.setEnabled(false);
//                                                      }
//                                                  }
//
//                                              }
//                                          });
//
//                                      }
//                                  },
////Set how long before to start calling the TimerTask (in milliseconds)
//                    0,
////Set the amount of time between each execution (in milliseconds)
//                    1000);
//
//            // To display final result
//            mCountDownTimer = new CountDownTimer(50, 10) {
//                @Override
//                public void onTick(long l) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (Constants.is_finalResult) {
//                                mTimerRunning = false;
//                                mCountDownTimer.cancel();
//                                if ((mBluetoothLeService.systalic < 30) || (mBluetoothLeService.systalic > 200)){
//                                    Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.systolic_error), Toast.LENGTH_SHORT).show();
//                                }
//                                else if ((mBluetoothLeService.dystolic < 40) || (mBluetoothLeService.dystolic > 120)) {
//                                    Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.diastolic_error), Toast.LENGTH_SHORT).show();
//                                }
//                                else {
////                                        progressText.setText(data);
//                                    progressText.setText(mBluetoothLeService.finalResult);
//
//                                    String status = changeStatus(mBluetoothLeService.systalic, mBluetoothLeService.dystolic);
//                                    systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
//                                    diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
//                                    heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
//                                    mapText.setText(status);
////                                    mapText.setText(String.valueOf(mBluetoothLeService.range));
//                                }
//                            }
//                        }
//                    });
//                }
//
//                @Override
//                public void onFinish() {
//                    if (mTimerRunning) {
//                        if (!Constants.is_finalResult) {
//                            mCountDownTimer.cancel();
//                            Toast.makeText(ReadingData.this,getApplicationContext().getResources().getString(R.string.please_start_again),Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                    else {
//                        mCountDownTimer.cancel();
////
//                        Constants.is_finalResult = false;
//                    }
//                }
//            }.start();
//
//            // To display Error messages.
//            mCountDownTimer = new CountDownTimer(50, 10) {
//                @Override
//                public void onTick(long l) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (Constants.is_errorReceived) {
//                                mTimerRunning = false;
//                                mCountDownTimer.cancel();
//
//                                progressText.setText(data);
////                                    progressText.setText(mBluetoothLeService.finalResult);
//
//                            }
//                        }
//                    });
//                }
//
//                @Override
//                public void onFinish() {
//                    if (mTimerRunning) {
//                        if (!Constants.is_errorReceived) {
//                            mCountDownTimer.cancel();
//                            Toast.makeText(ReadingData.this,getApplicationContext().getResources().getString(R.string.please_start_again),Toast.LENGTH_SHORT).show();
//                        }
//                    }
//                    else {
//                        mCountDownTimer.cancel();
//                        Constants.is_errorReceived = false;
//                    }
//                }
//            }.start();
//
//            // To display battery status popup while receiving the data
//            mCountDownTimer = new CountDownTimer(50, 10) {
//                @Override
//                public void onTick(long l) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            //  If battery low popup alert messages.
//                            if (Constants.is_batteryReceivedAtReading) {
//                                mTimerRunning = false;
//                                mCountDownTimer.cancel();
//                                progressText.setText("---");
//                                //                                                            .setMessage(data)
//                                if (mBluetoothLeService.errorMessage.equals(getString(R.string.battery_limit_exceeds))) {
//                                    batteryText.setBackgroundColor(Color.parseColor("#A41E22"));
//                                    progressText.setText("---");
//                                }
//                                else {
//                                    batteryText.setBackgroundColor(Color.parseColor("#FF0000"));
//                                    progressText.setText("---");
//                                }
//                                if ((dialog == null) || !dialog.isShowing()) {
//                                    dialog = new AlertDialog.Builder(ReadingData.this)
//                                            .setTitle(getApplicationContext().getResources().getString(R.string.message))
//                                            .setMessage(data)
////                                            .setMessage(mBluetoothLeService.errorMessage)
//                                            .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
//                                                    startBtn.setEnabled(true);
//                                                    startBtn.setVisibility(View.VISIBLE);
//                                                    stopBtn.setVisibility(View.INVISIBLE);
//                                                    stopBtn.setEnabled(false);
//                                                    progressText.setText("---");
////                                                    dialog.dismiss();
//                                                }
//                                            })
//                                            .setCancelable(false)
//                                            .show();
//                                }
//                            }
//                        }
//                    });
//                }
//
//                @Override
//                public void onFinish() {
//                    if (mTimerRunning) {
//                        if (!Constants.is_batteryReceivedAtReading) {
//                            mCountDownTimer.cancel();
//                            Log.d(TAG, "onFinish: Not able to find");
//                        }
//                    }
//                    else {
//                        mCountDownTimer.cancel();
//                        Constants.is_batteryReceivedAtReading = false;
//                    }
//                }
//            }.start();
//
//            // To display irregular heartbeat popup.
//            mCountDownTimer = new CountDownTimer(50, 10) {
//                @Override
//                public void onTick(long l) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            // Irregular heart beat alert popup
//                            if (Constants.is_irregularHB) {
//                                mTimerRunning = false;
//                                mCountDownTimer.cancel();
//                                if ((dialog == null) || !dialog.isShowing()) {
//                                    dialog = new AlertDialog.Builder(ReadingData.this)
//                                            .setTitle(getApplicationContext().getResources().getString(R.string.message))
////                                                           .setMessage(data)
//                                            .setMessage(mBluetoothLeService.errorMessage)
//                                            .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
//                                                @Override
//                                                public void onClick(DialogInterface dialog, int which) {
//                                                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
//                                                    startBtn.setEnabled(true);
//                                                    startBtn.setVisibility(View.VISIBLE);
//                                                    stopBtn.setVisibility(View.INVISIBLE);
//                                                    stopBtn.setEnabled(false);
//                                                    progressText.setText("---");
////                                                    dialog.dismiss();
//                                                }
//                                            }).setCancelable(false)
//                                            .show();
//                                }
//                            }
//                        }
//                    });
//                }
//                @Override
//                public void onFinish() {
//                    if (mTimerRunning) {
//                        if (!Constants.is_irregularHB) {
//                            mCountDownTimer.cancel();
//                            Log.d(TAG, "onFinish: not found");
//                        }
//                    }
//                    else {
//                        mCountDownTimer.cancel();
//                        Constants.is_irregularHB = false;
//                    }
//                }
//            }.start();
//
//            // To display cuff replacement message popup
//            mCountDownTimer = new CountDownTimer(50, 10) {
//                @Override
//                public void onTick(long l) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (Constants.is_cuffReplaced) {
//                                mTimerRunning = false;
//                                mCountDownTimer.cancel();
//                                progressText.setText("---");
//                                alertDialogForReset();
//                            }
//                        }
//                    });
//                }
//                @Override
//                public void onFinish() {
//
//                    if (mTimerRunning) {
//                        if (!Constants.is_cuffReplaced) {
//                            mCountDownTimer.cancel();
//                            Log.d(TAG, "onFinish: not able to find");
//                        }
//                    }
//                    else {
//                        mCountDownTimer.cancel();
//                        Constants.is_cuffReplaced = false;
//                    }
//                }
//            }.start();
//        }
//        else {
//            // Battery status with timer
//            startTimer();
//        }
//    }