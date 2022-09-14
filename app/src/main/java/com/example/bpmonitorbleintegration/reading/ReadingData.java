package com.example.bpmonitorbleintegration.reading;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.util.Range;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bpmonitorbleintegration.R;
import com.example.bpmonitorbleintegration.bleconnect.BLEService;
import com.example.bpmonitorbleintegration.bleconnect.DecodeListener;
import com.example.bpmonitorbleintegration.bleconnect.Decoder;
import com.example.bpmonitorbleintegration.constants.BLEGattAttributes;
import com.example.bpmonitorbleintegration.constants.Constants;
import com.example.bpmonitorbleintegration.database.RoomDB;
import com.example.bpmonitorbleintegration.home.HomePage;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

//https://stackoverflow.com/questions/69529502/countdown-timer-which-runs-in-the-background-in-android - for background timer

public class ReadingData extends AppCompatActivity {

    private BLEService mBluetoothLeService;
    private String deviceAddress;
    public BluetoothGattCharacteristic mNotifyCharacteristic;
    BluetoothDevice bluetoothDevice;
    IntentFilter intentFilter;
    AlertDialog.Builder builder1;
    Decoder decoder;
    public AlertDialog dialog, dialog1, dialog2;
    RoomDB localDB;

    BluetoothAdapter bAdapter;
    public int counter = 0;
    private CountDownTimer mCountDownTimer;
    private long mTimeLeftInMillis = 2000 ;
    long ulong = mTimeLeftInMillis & 0xffffffffL;
    private final long startTime = 50;
    private boolean mTimerRunning = true;
    private final String TAG = ReadingData.class.getName();

    private TextView statusText, batteryText, systolicText, diastolicText, heartRateText, mapText, progressText, progressText1, issueStatus, statusText1, alertText;
    private Button startBtn;
    private Button stopBtn;
    Button saveReadingBtn;
    private ProgressBar progressBar, progress,progress1;
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
        progress1 = findViewById(R.id.progress_bar1);
        progressText = findViewById(R.id.progress_text);
        progress = findViewById(R.id.progress_start);
        progressBar = findViewById(R.id.progress_bar);
        startBtn = findViewById(R.id.start_reading);
        progressText1 = findViewById(R.id.progress_text1);
        stopBtn = findViewById(R.id.stop_reading);
        issueStatus = findViewById(R.id.issues);
        alertText = findViewById(R.id.error_val);
//        saveReadingBtn = (Button) findViewById(R.id.save_result1);
        statusText1 = findViewById(R.id.map1);
        bAdapter = BluetoothAdapter.getDefaultAdapter();

        deviceAddress = getIntent().getStringExtra("Device");
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(broadCastReceiver, intentFilter);

        //Initialising Decoder class.
        decoder = new Decoder();
        localDB = new RoomDB();

        progressText1.setText("");
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.readings);

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#151B54")));
//        pref = PreferenceManager.getDefaultSharedPreferences(this);

        // Connect to the device through BLE.
        registerReceiver(broadCastReceiver, GattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                mBluetoothLeService.close();
//                mBluetoothLeService.disconnect();
//                mBluetoothLeService.stopSelf();
                boolean result = mBluetoothLeService.connect(deviceAddress);
                if (!result) {
                    finish();
                }
            }
        }

//        Sending stop command to  stop the receiving readings
        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Constants.is_buttonStarted = false;
                Constants.is_cuffReplaced = false;
                Constants.is_irregularHB = false;
                Constants.is_ackReceived = false;
                Constants.is_readingStarted = false;
                Constants.is_errorReceived = false;
                Constants.is_batterValueReceived = false;
                Constants.is_batteryReceivedAtReading = false;

                if (mNotifyCharacteristic != null) {
                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                            Log.i(TAG, "Force stop value after checksum " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);

                    mCountDownTimer = new CountDownTimer(500, 500) { //500 500
                        @Override
                        public void onTick(long l) {
                            Log.d(TAG, "onTick: on tick");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    Log.i(TAG, "run: ack in stop " + Constants.is_ackReceived);
//                                    if ((Constants.is_ackReceived == true) || (Constants.is_cuffReplaced == true) || (Constants.is_irregularHB == true))
                                    if ((Constants.is_ackReceived == true)) {
                                        mTimerRunning = false;
                                        mCountDownTimer.cancel();
                                        Log.d(TAG, "run: ack received while stop");
                                        startBtnEnable();
//                                        Constants.is_buttonStarted = false;
                                        progressText.setText("---");
                                    }

                                }
                            });
                        }

                        @Override
                        public void onFinish() {
                            if (mTimerRunning == false) {
                                Log.d(TAG, "onFinish: mtimer running false");
                                mTimerRunning = false;
                                mCountDownTimer.cancel();
                                Log.d(TAG, "run: ack received while stop");
                                startBtnEnable();
//                                        Constants.is_buttonStarted = false;
                                progressText.setText("---");
                                Constants.is_ackReceived = false;
                            }
                            else if (mTimerRunning == true) {
                                if ((Constants.is_ackReceived == false)) {
                                    Log.i(TAG, "ack not received while stop");
                                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                    start();
                                }
                            }


                        }
                    }.start();
                }
            }
        });

//        Send start command to receive readings.
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Constants.is_buttonStarted = true;
                Constants.is_cuffReplaced = false;
                Constants.is_irregularHB = false;
                Constants.is_ackReceived = false;
                Constants.is_readingStarted = false;
                Constants.is_errorReceived = false;
                Constants.is_finalResult = false;
                Constants.is_batterValueReceived = false;
                Constants.cuffPop = false;
                Constants.heartbeatPop = false;
                Constants.batteryPop = false;
//                Constants.is_batteryReceivedAtReading = false;

                if (mNotifyCharacteristic != null) {
                    Constants.startValue = decoder.computeCheckSum(Constants.startValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.startValue);
                    Log.d(TAG, "onClick: button clicked " + Constants.is_buttonStarted);
//                    Constants.is_buttonStarted = true;

                    mCountDownTimer = new CountDownTimer(500, 500) {
                        @Override
                        public void onTick(long l) {
                            Log.d(TAG, "onTick: on tick");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {


                                }
                            });
                        }

                        @Override
                        public void onFinish() {

                            if (Constants.is_ackReceived == false) {
                                Log.i(TAG, "ack not received while stop");
                               toastMsgInReading(getString(R.string.please_start_again));
                            }
                        }
                    }.start();
                }
                systolicText.setText("");
                diastolicText.setText("");
                heartRateText.setText("");
                mapText.setText("");
                alertText.setText("");
                statusText1.setText("");
                issueStatus.setText("---");

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Connect to the device through BLE.
        registerReceiver(broadCastReceiver, GattUpdateIntentFilter());
    }

//    Performing background tasks - disconnect BLE services.
    @Override
    protected void onPause() {
        super.onPause();
        //Disconnect through services.
        unregisterReceiver(broadCastReceiver);
        Log.d(TAG, "onPause: minimised");
//        On app minimize or sleep mode need to disconnect from services & BLE.
        progressText.setText("---");
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);

        if(pm.isInteractive()){
            // not sleep

            if (isApplicationSentToBackground(this)) {

                    //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                        startActivity(intent1);

                        //Method 1
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);

                        //Method 2
//                moveTaskToBack(true);
            }

            }
            else {
                    //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                        startActivity(intent1);

                    }
            }
        }else{
            // sleep

                //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                    startActivity(intent1);

                }
        }
    }

//    Background services
    public boolean isApplicationSentToBackground(final Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
        startActivity(intent1);
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
                                    mBluetoothLeService.close();
                                    mBluetoothLeService.disconnect();
                                    mBluetoothLeService.stopSelf();
//                                    Intent intent = new Intent(ReadingData.this, HomePage.class);
                                    Intent intent = new Intent(ReadingData.this, MainActivity.class);
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
                if (data != null) {
//
                    Log.d(TAG, "displayData: start tapped " + Constants.is_buttonStarted);
//                    Log.d(TAG, "displayData: stop tapped " + Constants.is_stopButton);
                    if (Constants.is_buttonStarted == true) {

                        mCountDownTimer = new CountDownTimer(90000, 10000) {
                            @Override
                            public void onTick(long millisUntilFinished) {
//                                mTimeLeftInMillis = millisUntilFinished;
                                Log.d(TAG, "onTick: count in start");
                                runOnUiThread(new Runnable() {
                                    @SuppressLint("SetTextI18n")
                                    @Override
                                    public void run() {
                                        progress.setVisibility(View.GONE);
                                        Log.d(TAG, "displayData: ack in start " + Constants.is_ackReceived);
                                        if (Constants.is_ackReceived == true) {
                                            Log.d(TAG, "run: ack in start btn");
                                            mCountDownTimer.cancel();
                                            mTimerRunning = false;
                                            startBtn.setEnabled(false);
                                            startBtn.setVisibility(View.INVISIBLE);
                                            stopBtn.setVisibility(View.VISIBLE);
                                            stopBtn.setEnabled(true);
//                                            Constants.is_buttonStarted = false;

//                                 To display raw readings
                                            if (Constants.is_readingStarted == true) {
                                                Log.d(TAG, "run: inside row reading");
                                                progressText.setText(data);
                                                progressText1.setText("");//changes
//                                                issueStatus.setText("---");
//                                progressText.setText(mBluetoothLeService.rawReadings);
//                                                Constants.is_readingStarted = false;
                                            }

                                            //To display error msgs.
                                            if (Constants.is_errorReceived == true) {
                                                alertText.setText(data); //Changed
//                                                progressText.setText("---"); //Changed
                                                issueStatus.setText(mBluetoothLeService.errorMessage); //Changed
//                                                Constants.is_errorReceived = false;
                                                startBtnEnable(); //Changed
                                                Constants.is_errorReceived = false;
                                            }

                                            Log.d(TAG, "run: final result flag " + Constants.is_finalResult);

                                            // To display final results.
                                            if (Constants.is_finalResult == true) {
                                                if ((mBluetoothLeService.systalic < 60) || (mBluetoothLeService.systalic > 230)) {
                                                    progressText.setText("---");
                                                    progressText1.setText("");
                                                    toastMsgInReading(getApplication().getResources().getString(R.string.systolic_error));
                                                    startBtnEnable();
                                                    finalErrorMsg();
                                                } else if ((mBluetoothLeService.dystolic < 40) || (mBluetoothLeService.dystolic > 130)) {
                                                    progressText.setText("---");
                                                    progressText1.setText("");
                                                    toastMsgInReading(getApplication().getResources().getString(R.string.diastolic_error));
                                                    startBtnEnable();
                                                    finalErrorMsg();
                                                }
                                                else if ((mBluetoothLeService.rate < 60) || (mBluetoothLeService.rate >120)){
                                                    progressText.setText("---");
                                                    progressText1.setText("");
                                                    toastMsgInReading(getString(R.string.heart_rate_error));
                                                    startBtnEnable();
                                                    finalErrorMsg();
                                                }
                                                else {
//                                                    progressText.setText(data);
                                                    progressText.setText(mBluetoothLeService.finalResult);
                                                    progressText1.setText(mBluetoothLeService.rate + " bpm"); //Changes
                                                    // Changed
                                                    startBtnEnable();
                                                    saveData();
                                                    systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
                                                    diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
                                                    heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
                                                    String status = changeStatus(mBluetoothLeService.systalic, mBluetoothLeService.dystolic);
                                                    mapText.setText(status);
                                                    statusText1.setText(String.valueOf(mBluetoothLeService.range));
                                                    finalErrorMsg();
                                                }
                                            }
                                        }

                                        // To display battery status popup while receiving the data
                                        Log.d(TAG, "run: battry status while reading " + Constants.is_batteryReceivedAtReading);
                                        //  If battery low popup alert messages.

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

                                                                Log.d(TAG, "run: battery popup " + Constants.batteryPop);
                                                                if (Constants.batteryPop == false) {
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
                                                                                        startBtnEnable();
                                                                                        Constants.batteryPop = true;
//                                                                                        Constants.is_buttonStarted = false;
                                                                                        progressText.setText("---");
//                                                    dialog.dismiss();
                                                                                    }
                                                                                })
                                                                                .setCancelable(false)
                                                                                .show();
                                                                    }
                                                                }
                                                            }
                                                            else if (mBluetoothLeService.errorMessage.equals(getApplicationContext().getResources().getString(R.string.battery_low))){
                                                                batteryText.setBackgroundColor(Color.parseColor("#FF0000"));
                                                                progressText.setText("---");
                                                                if (Constants.batteryPop == false) {
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
                                                                                        startBtnEnable();
                                                                                        Constants.batteryPop = true;
//                                                                                        Constants.is_buttonStarted = false;
                                                                                        progressText.setText("---");
//                                                    dialog.dismiss();
                                                                                    }
                                                                                })
                                                                                .setCancelable(false)
                                                                                .show();
                                                                    }
                                                                }
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
                                    }
                                });
                            }

                            @Override
                            public void onFinish() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mTimerRunning == true) {
                                        Log.d(TAG, "run: inside finish");
                                            if (Constants.is_finalResult == false || Constants.is_errorReceived == false) {
                                                mCountDownTimer.cancel();
                                                if ((dialog == null) || !dialog.isShowing()) {
                                                    dialog = new AlertDialog.Builder(ReadingData.this)
                                                            .setTitle(getApplicationContext().getResources().getString(R.string.message))
                                                            .setMessage(getApplicationContext().getResources().getString(R.string.please_start_again))
                                                            .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                                @SuppressLint("MissingPermission")
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.cancelValue) + " " + Constants.cancelValue);
                                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                                                    startBtnEnable();
                                                                }
                                                            }).show();
                                                }
                                            }
                                        }
                                         if (mTimerRunning == false) {
                                            mCountDownTimer.cancel();
                                            Constants.is_ackReceived = false;
                                            Constants.is_buttonStarted = false;
                                        }
                                    }
                                });
                            }
                        }.start();
                    }
                        }
                else{
                    startTimer();
                }
    }

    private void finalErrorMsg() {
        String msg = "";
        switch (mBluetoothLeService.irregularHB){
            case 0:
                Log.d(TAG, "run: no irregular hb error");
                break;
            case 3:
                msg = getString(R.string.irregular_heartbeat);
                issueStatus.setText(msg);
                alertText.setText(msg);
                break;
            case 7:
                msg = getString(R.string.cuff_replacement);
                issueStatus.setText(msg);
                alertText.setText(msg);

                break;
            default:
                Log.d(TAG, "run: default");
                break;
        }

        switch (mBluetoothLeService.cuffReplace) {
            case 0:
                Log.d(TAG, "run: no cuff replace error");
                break;
            case 6:
                if (Constants.cuffPop == false) {
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

                                mCountDownTimer = new CountDownTimer(500, 100) {
                                    @Override
                                    public void onTick(long l) {
                                        // mTimerRunning = true;
                                        counter++;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (Constants.is_ackReceived == true) {
                                                    Constants.cuffPop = true;
                                                    mTimerRunning = false;
                                                    mCountDownTimer.cancel();
                                                    dialogInterface.dismiss();
                                                    dialog1.dismiss();
//                                                                    Constants.is_readingStarted = false;
//                                                                                    progressText.setText("---");
//                                                                                    progressText1.setText("");
                                                    startBtnEnable();
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
                                                if (mTimerRunning == true) {
                                                    if (Constants.is_ackReceived == false) {
                                                        dialog1.show();
                                                        Log.d(TAG, "run: ack not received while reset");
                                                        Constants.resetValue = decoder.computeCheckSum(Constants.resetValue);
////          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                                                        mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.resetValue);
                                                        start();
                                                    }
                                                } else if (mTimerRunning == false) {
                                                    mCountDownTimer.cancel();
//                                                                    Log.d(TAG, "run: timer off");
//                                                                                Constants.is_ackReceived = false;
                                                    progressText.setText("---");
                                                    startBtnEnable();
                                                    Constants.cuffPop = true;
//                                                                    Constants.is_stopButton = false;
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
                                mCountDownTimer = new CountDownTimer(500, 100) {
                                    @Override
                                    public void onTick(long l) {
                                        counter++;
//                                                Log.i(TAG, "counter Started " + startTime);
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                if (Constants.is_ackReceived == true) {
                                                    mTimerRunning = false;
                                                    Constants.cuffPop = true;
                                                    mCountDownTimer.cancel();
                                                    dialogInterface.dismiss();
                                                    dialog1.dismiss();
                                                    progressText.setText("---");
                                                    startBtnEnable();
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
                                                if (mTimerRunning == true) {
                                                    if (Constants.is_ackReceived == false) {
                                                        dialog1.show();
                                                        Log.d(TAG, "run: ack not received while non reset");
                                                        Constants.noResetValue = decoder.computeCheckSum(Constants.noResetValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                                                        mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.noResetValue);
                                                        start();
                                                    }
                                                } else if (mTimerRunning == false) {
                                                    mCountDownTimer.cancel();
//                                                                    Log.d(TAG, "run: timer off");
                                                    Constants.is_ackReceived = false;
                                                    progressText.setText("---");
                                                    startBtnEnable();
                                                    Constants.cuffPop = true;
//                                                                    Constants.is_stopButton = false;
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
                break;

            default:
                Log.d(TAG, "run: default case");
                break;
        }
    }

    // Status according to sys/dia ranges.
    private String changeStatus(int systolic, int diastolic) {
        String msg = null;
        if ((systolic < 80) || (diastolic < 60)) {
            msg = getString(R.string.low_bp);
        }
        else if ((systolic > 80 && systolic < 120) || (diastolic > 60 && diastolic < 80)) {
            msg = getString(R.string.normal_bp);
        }
        else if ((systolic > 120 && systolic < 139) || (diastolic > 80 && diastolic < 89)) {
            msg = getString(R.string.high_normal_bp);
        }
        else if ((systolic > 139 && systolic < 159) || (diastolic > 89 && diastolic < 99)) {
            msg = getString(R.string.hypertension_stage_1);
        }
        else if ((systolic > 159 && systolic < 179) || (diastolic > 99 && diastolic < 109)) {
            msg = getString(R.string.hypertension_stage_2);
        }
        else if ((systolic > 179 && systolic < 189) || (diastolic > 109 && diastolic < 119)) {
            msg = getString(R.string.hypertension_stage_3);
        }
        else {
            msg = getString(R.string.high_bp_crisis);
        }
        return msg;
    }

    private void saveData() {
        if (Constants.heartbeatPop == false) {
            if ((dialog == null) || !dialog.isShowing()) {
                dialog = new AlertDialog.Builder(ReadingData.this)
                        .setTitle(getApplicationContext().getResources().getString(R.string.message))
                        .setMessage("Do you want to save?")
                        .setPositiveButton(getApplicationContext().getResources().getString(R.string.yes), new DialogInterface.OnClickListener() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Constants.heartbeatPop = true;
//                                                    progress.setVisibility(View.VISIBLE);
                                if (systolicText.getText().toString().equals("")) {
                                    toastMsgInReading(getApplication().getResources().getString(R.string.check_value));
//                                                        progress.setVisibility(View.GONE);
                                } else if (diastolicText.getText().toString().equals("")) {
                                    toastMsgInReading(getApplication().getResources().getString(R.string.check_value));
//                                                        progress.setVisibility(View.GONE);
                                } else if (heartRateText.getText().toString().equals("")) {
                                    toastMsgInReading(getApplication().getResources().getString(R.string.check_value));
//                                                        progress.setVisibility(View.GONE);
                                } else if (mapText.getText().toString().equals("")) {
                                    toastMsgInReading(getApplication().getResources().getString(R.string.check_value));
//                                                        progress.setVisibility(View.GONE);
                                } else {
                                    if ((Integer.parseInt(systolicText.getText().toString()) < 30) || (Integer.parseInt(systolicText.getText().toString()) > 200)){
                                        progressText.setText("---");
                                        progressText1.setText("");
                                        toastMsgInReading(getApplicationContext().getResources().getString(R.string.systolic_range_fault));

//                                        progressBar.setVisibility(View.GONE);
                                    }
                                    else if ((Integer.parseInt(diastolicText.getText().toString()) < 40) || (Integer.parseInt(diastolicText.getText().toString()) > 120)) {
                                        progressText.setText("---");
                                        progressText1.setText("");
                                        toastMsgInReading(getApplicationContext().getResources().getString(R.string.diastolic_range_fault));
//                                        progressBar.setVisibility(View.GONE);
                                    }
                                    else if ((Integer.parseInt(heartRateText.getText().toString()) < 60) || (Integer.parseInt(heartRateText.getText().toString()) > 100)) {
                                        progressText.setText("---");
                                        progressText1.setText("");
                                        toastMsgInReading(getString(R.string.heart_rate_fault));
                                    }
                                    else {
                                        //                    Save to local DB
                                        localDB.saveTask(deviceAddress, Integer.parseInt(systolicText.getText().toString()), Integer.parseInt(diastolicText.getText().toString()), Integer.parseInt(heartRateText.getText().toString()), mBluetoothLeService.range, ReadingData.this);
//                    localDB.saveTask(deviceAddress, mBluetoothLeService.systalic, mBluetoothLeService.dystolic, mBluetoothLeService.rate, mBluetoothLeService.range, ReadingData.this); Integer.parseInt(mapText.getText().toString())
//
//                                        Constants.heartbeatPop = true;
                                        progressText.setText("---");
                                        progressText1.setText("");
                                        issueStatus.setText("---");
//                                                        progress.setVisibility(View.GONE);
                                    }

                                }
                            }
                        })
                        .setNegativeButton(getApplicationContext().getResources().getString(R.string.no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Constants.heartbeatPop = true;
                                progressText.setText("---");
                                progressText1.setText("");
                                issueStatus.setText("---");
                                dialog.dismiss();
                            }
                        }).show();
            }
        }
    }

    // To receive battery status
    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                millisUntilFinished--;
//                Log.d(TAG, "onTick: " + ulong);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (Constants.is_batterValueReceived == true)
                        {

                            mTimerRunning = false;
//                            mCountDownTimer.cancel();
                            cancel();
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
                        if (mTimerRunning == true) {
                            if (!Constants.is_batterValueReceived) {
                                mCountDownTimer.cancel();
                                toastMsgInReading(getApplicationContext().getResources().getString(R.string.please_connect_again));
                            }
                        }
                        else if (mTimerRunning == false){
                            mCountDownTimer.cancel();
                            Log.d(TAG, "run: timer off");
//                            Constants.is_batterValueReceived = false;
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
            Constants.is_batterValueReceived = false;
        }
        else if (mBluetoothLeService.batteryLevel == Constants.MID_BATTERY){
            batteryText.setBackgroundColor(Color.parseColor("#FFA500"));
//            Constants.ack = decoder.computeCheckSum(Constants.ack);
//            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
            Constants.is_batterValueReceived = false;
        }
        else if (mBluetoothLeService.batteryLevel == Constants.LOW_BATTERY) {
            batteryText.setBackgroundColor(Color.parseColor("#FF0000"));
            toastMsgInReading(getApplicationContext().getResources().getString(R.string.battery_low));
//            Constants.ack = decoder.computeCheckSum(Constants.ack);
//            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
            Constants.is_batterValueReceived = false;
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
                                Constants.is_batterValueReceived = false;
                                dialog.dismiss();
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
//                Log.e(TAG, "Unable to initialize Bluetooth");
                toastMsgInReading("Unable to initialize Bluetooth");
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

    private void toastMsgInReading(String msg) {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }

    private void startBtnEnable() {
        progressText.setText("---");
        startBtn.setEnabled(true);
        startBtn.setVisibility(View.VISIBLE);
        stopBtn.setVisibility(View.INVISIBLE);
        stopBtn.setEnabled(false);
    }

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
//                                            mBluetoothLeService.disconnect();
//                                            mBluetoothLeService.close();
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
