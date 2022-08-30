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
import android.app.ActivityManager;
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
import android.os.Handler;
import android.os.IBinder;
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
//                mBluetoothLeService.close();
//                mBluetoothLeService.disconnect();
//                mBluetoothLeService.stopSelf();
                boolean result = mBluetoothLeService.connect(deviceAddress);
                if (!result) {
                    finish();
                }
            }
        }

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Constants.is_stopButton = true;

                Constants.is_buttonStarted = false;
                Constants.is_cuffReplaced = false;
                Constants.is_irregularHB = false;
                Constants.is_ackReceived = false;
                Constants.is_readingStarted = false;
                Constants.is_errorReceived = false;
                Constants.is_batterValueReceived = false;

                if (mNotifyCharacteristic != null) {
                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                            Log.i(TAG, "Force stop value after checksum " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                    Constants.is_stopButton = true;
                }
                    if (Constants.is_finalResult == true) {
                        systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
                        diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
                        heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
                        String status = changeStatus(mBluetoothLeService.systalic,mBluetoothLeService.dystolic);
                        mapText.setText(status);
                        statusText1.setText(String.valueOf(mBluetoothLeService.range));
//                        mapText.setText(String.valueOf(mBluetoothLeService.range));
//                        localDB.saveTask(deviceAddress, mBluetoothLeService.systalic, mBluetoothLeService.dystolic, mBluetoothLeService.rate, mBluetoothLeService.range, ReadingData.this);
                        Constants.is_stopButton = true;
                        Constants.is_finalResult = false;
                    }
//                }
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Constants.is_buttonStarted = true;
                Constants.is_stopButton = false;
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

                if (mNotifyCharacteristic != null) {
                    Constants.startValue = decoder.computeCheckSum(Constants.startValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.startValue);
                    Log.d(TAG, "onClick: button clicked " + Constants.is_buttonStarted);
//                    Constants.is_buttonStarted = true;
                }
                systolicText.setText("");
                diastolicText.setText("");
                heartRateText.setText("");
                mapText.setText("");
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
        Log.d(TAG, "onPause: minimised");
//        On app minimize or sleep mode need to disconnect from services & BLE.
        progressText.setText("---");
//        Constants.is_buttonStarted = false;
//        Constants.is_stopButton = false;
        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);

        if(pm.isInteractive()){
            // not sleep

            if (isApplicationSentToBackground(this)) {
                if (Constants.is_finalResult == true) {
                    systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
                    diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
                    heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
                    String status = changeStatus(mBluetoothLeService.systalic,mBluetoothLeService.dystolic);
                    mapText.setText(status);
                    statusText1.setText(String.valueOf(mBluetoothLeService.range));
                    Constants.is_finalResult = false;

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
//                                            mBluetoothLeService.disconnect();
//                                            mBluetoothLeService.close();
//                                            mBluetoothLeService.stopSelf();
                                            Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                                            startActivity(intent1);

                                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                                            startMain.addCategory(Intent.CATEGORY_HOME);
                                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(startMain);
                                        }
                                    }
                                })
                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialog.dismiss();
//                                        mBluetoothLeService.disconnect();
//                                        mBluetoothLeService.close();
//                                        mBluetoothLeService.stopSelf();
                                        //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                            Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                                            startActivity(intent1);

                                            Intent startMain = new Intent(Intent.ACTION_MAIN);
                                            startMain.addCategory(Intent.CATEGORY_HOME);
                                            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(startMain);
                                        }
                                    }
                                }).show();
                    }
                }
                else {
                    //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        mBluetoothLeService.disconnect();
//                        mBluetoothLeService.close();
//                        mBluetoothLeService.stopSelf();
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

            }
            else {
//                if (Constants.is_finalResult == true) {
//                    systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
//                    diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
//                    heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
//                    String status = changeStatus(mBluetoothLeService.systalic,mBluetoothLeService.dystolic);
//                    mapText.setText(status);
//                    statusText1.setText(String.valueOf(mBluetoothLeService.range));
//                    Constants.is_finalResult = false;
//
//                    if ((dialog == null) || !dialog.isShowing()) {
//                        dialog = new AlertDialog.Builder(ReadingData.this)
//                                .setTitle(getApplicationContext().getResources().getString(R.string.message))
//                                .setMessage(R.string.save_final_reading)
//                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//                                    @SuppressLint("MissingPermission")
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        localDB.saveTask(deviceAddress, Integer.parseInt(systolicText.getText().toString()), Integer.parseInt(diastolicText.getText().toString()), Integer.parseInt(heartRateText.getText().toString()), mBluetoothLeService.range, ReadingData.this);
//                                        //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
//                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
////                                            mBluetoothLeService.disconnect();
////                                            mBluetoothLeService.close();
////                                            mBluetoothLeService.stopSelf();
//                                            Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
//                                            startActivity(intent1);
//
//                                        }
//                                    }
//                                })
//                                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialogInterface, int i) {
//                                        dialog.dismiss();
////                                        mBluetoothLeService.disconnect();
////                                        mBluetoothLeService.close();
////                                        mBluetoothLeService.stopSelf();
//                                        //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
//                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                                            Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
//                                            startActivity(intent1);
//
//                                        }
//                                    }
//                                }).show();
//                    }
//                }
//                else {
                    //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                        mBluetoothLeService.disconnect();
//                        mBluetoothLeService.close();
//                        mBluetoothLeService.stopSelf();
                        Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                        startActivity(intent1);

                    }
//                }
            }
        }else{
            // sleep
//            if (Constants.is_finalResult == true) {
//                systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
//                diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
//                heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
//                String status = changeStatus(mBluetoothLeService.systalic,mBluetoothLeService.dystolic);
//                mapText.setText(status);
//                statusText1.setText(String.valueOf(mBluetoothLeService.range));
//                Constants.is_finalResult = false;
//
//                if ((dialog == null) || !dialog.isShowing()) {
//                    dialog = new AlertDialog.Builder(ReadingData.this)
//                            .setTitle(getApplicationContext().getResources().getString(R.string.message))
//                            .setMessage(R.string.save_final_reading)
//                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
//                                @SuppressLint("MissingPermission")
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    localDB.saveTask(deviceAddress, Integer.parseInt(systolicText.getText().toString()), Integer.parseInt(diastolicText.getText().toString()), Integer.parseInt(heartRateText.getText().toString()), mBluetoothLeService.range, ReadingData.this);
//                                    //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
////                                        mBluetoothLeService.disconnect();
////                                        mBluetoothLeService.close();
////                                        mBluetoothLeService.stopSelf();
//                                        Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
//                                        startActivity(intent1);
//
//                                    }
//                                }
//                            })
//                            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialogInterface, int i) {
//                                    dialog.dismiss();
////                                    mBluetoothLeService.disconnect();
////                                    mBluetoothLeService.close();
////                                    mBluetoothLeService.stopSelf();
//                                    //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
//                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                                        Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
//                                        startActivity(intent1);
//
//                                    }
//                                }
//                            }).show();
//                }
//            }
//            else {
                //Navigating to next activity on tap of ok button - On app minimize need to disconnect from services & BLE.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                    mBluetoothLeService.disconnect();
//                    mBluetoothLeService.close();
//                    mBluetoothLeService.stopSelf();
                    Intent intent1 = new Intent(ReadingData.this, MainActivity.class);
                    startActivity(intent1);

                }
//            }
        }

    }

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
//        finish();
//        mBluetoothLeService.disconnect();
//        mBluetoothLeService.close();
//        mBluetoothLeService.stopSelf();
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
//        Toast.makeText(getApplicationContext(), "received data before" + data, Toast.LENGTH_SHORT).show();
//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
                if (data != null) {

                    Log.d(TAG, "displayData: start tapped " + Constants.is_buttonStarted);
                    Log.d(TAG, "displayData: stop tapped " + Constants.is_stopButton);
                    if (Constants.is_buttonStarted == true) {

                        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 500) {
                            @Override
                            public void onTick(long millisUntilFinished) {
                                mTimeLeftInMillis = millisUntilFinished;

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progress.setVisibility(View.GONE);
                                        Log.d(TAG, "displayData: ack in start " + Constants.is_ackReceived);
                                        if (Constants.is_ackReceived)
                                        {
                                            Log.d(TAG, "run: ack in start btn");
                                            mCountDownTimer.cancel();
                                            mTimerRunning = false;
                                            startBtn.setEnabled(false);
                                            startBtn.setVisibility(View.INVISIBLE);
                                            stopBtn.setVisibility(View.VISIBLE);
                                            stopBtn.setEnabled(true);

//                                 To display raw readings
                                            if (Constants.is_readingStarted == true) {
                                                progressText.setText(data);
//                                progressText.setText(mBluetoothLeService.rawReadings);
                                                Constants.is_readingStarted = false;
                                            }

                                            //To display error msgs.
                                            if (Constants.is_errorReceived == true) {
                                                progressText.setText(data);
                                                Constants.is_errorReceived = false;
                                                Constants.is_buttonStarted = false;
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
                                                    Constants.is_buttonStarted = false;
                                                }
                                            }
                                        }

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
                                                                                    startBtn.setEnabled(true);
                                                                                    startBtn.setVisibility(View.VISIBLE);
                                                                                    stopBtn.setVisibility(View.INVISIBLE);
                                                                                    stopBtn.setEnabled(false);
                                                                                    Constants.is_buttonStarted = false;
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
                                            Constants.is_ackReceived = false;
                                            Constants.is_buttonStarted = false;
                                        }

                                    }
                                });
                            }
                        }.start();
                    }
                    else if (Constants.is_buttonStarted == false) {
                        Log.d(TAG, "displayData: start button not selected");
                        if (Constants.is_ackReceived == true) {
                            startBtn.setEnabled(true);
                            startBtn.setVisibility(View.VISIBLE);
                            stopBtn.setVisibility(View.INVISIBLE);
                            stopBtn.setEnabled(false);
                            progressText.setText("---");
                        }
                        else if (Constants.is_ackReceived == false) {
                            Log.d(TAG, "run: ack not received");
                        }
                    }

                    if (Constants.is_stopButton == true) {
                        stopBtnTap();
//                Constants.is_stopButton = false;
                        // To display irregular heartbeat popup.
                        mCountDownTimer = new CountDownTimer(50, 10) {
                            @Override
                            public void onTick(long l) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "run: irregular hb " + Constants.is_irregularHB);
                                        // Irregular heart beat alert popup
                                        if (Constants.is_irregularHB == true) {
                                            mTimerRunning = false;
//                                    Constants.is_readingStarted = false;
                                            mCountDownTimer.cancel();
                                            progressText.setText("---");
                                            if (Constants.heartbeatPop == false) {
                                                if ((dialog == null) || !dialog.isShowing()) {
                                                    dialog = new AlertDialog.Builder(ReadingData.this)
                                                            .setTitle(getApplicationContext().getResources().getString(R.string.message))
//                                                           .setMessage(data)
                                                            .setMessage(mBluetoothLeService.errorMessage)
                                                            .setPositiveButton(getApplicationContext().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Constants.heartbeatPop = true;
                                                                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
                                                                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);

                                                                    // To check the ack after the reset command sent
                                                                    mCountDownTimer = new CountDownTimer(300, 10) {
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
                                                                                        startBtn.setEnabled(true);
                                                                                        startBtn.setVisibility(View.VISIBLE);
                                                                                        stopBtn.setVisibility(View.INVISIBLE);
                                                                                        stopBtn.setEnabled(false);
//                                                                            Constants.is_readingStarted = false;
                                                                                        progressText.setText("---");
                                                                                        Constants.is_buttonStarted = false;
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
                                                                                            Log.d(TAG, "run: ack not received while irregular hb");
                                                                                            Constants.noResetValue = decoder.computeCheckSum(Constants.cancelValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.cancelValue) + " " + Constants.cancelValue);
                                                                                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                                                                            start();
                                                                                        }
                                                                                    }
                                                                                    else
                                                                                    {
                                                                                        mCountDownTimer.cancel();
//                                                                    Log.d(TAG, "run: timer off");
                                                                                        Constants.is_ackReceived = false;
                                                                                        progressText.setText("---");
                                                                                        startBtn.setEnabled(true);
                                                                                        startBtn.setVisibility(View.VISIBLE);
                                                                                        stopBtn.setVisibility(View.INVISIBLE);
                                                                                        stopBtn.setEnabled(false);
                                                                                        Constants.is_buttonStarted = false;
//                                                                            dialog1.dismiss();
                                                                                    }
                                                                                }
                                                                            });
                                                                        }
                                                                    }.start();
//                                                    dialog.dismiss();
                                                                }
                                                            }).setCancelable(false)
                                                            .show();
                                                }
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
//                            Constants.is_irregularHB = false;
//                            Constants.is_stopButton = false;
                                }
                            }
                        }.start();

                        // To display cuff replacement message popup
                        mCountDownTimer = new CountDownTimer(100, 50) {
                            @Override
                            public void onTick(long l) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "run: cuff " + Constants.is_cuffReplaced);
                                        if (Constants.is_cuffReplaced == true) {
                                            mTimerRunning = false;
                                            mCountDownTimer.cancel();
//                                    Constants.is_readingStarted = false;
                                            progressText.setText("---");
                                            Constants.is_buttonStarted = false;

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
//                                                                    Constants.is_readingStarted = false;
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
//                                                                    Log.d(TAG, "run: timer off");
//                                                                                Constants.is_ackReceived = false;
                                                                                progressText.setText("---");
                                                                                startBtn.setEnabled(true);
                                                                                startBtn.setVisibility(View.VISIBLE);
                                                                                stopBtn.setVisibility(View.INVISIBLE);
                                                                                stopBtn.setEnabled(false);
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
//                                                                    Log.d(TAG, "run: timer off");
                                                                                Constants.is_ackReceived = false;
                                                                                progressText.setText("---");
                                                                                startBtn.setEnabled(true);
                                                                                startBtn.setVisibility(View.VISIBLE);
                                                                                stopBtn.setVisibility(View.INVISIBLE);
                                                                                stopBtn.setEnabled(false);
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
//                            Constants.is_cuffReplaced = false;
//                            Constants.is_stopButton = false;
                                }
                            }
                        }.start();
                    }
                    else if (Constants.is_stopButton == false) {
                        Log.d(TAG, "displayData: stop button not selected");
                    }
                }
                else {
                    startTimer();
                }
//            }
//        }, 500);
    }

    private void stopBtnTap() {
        // Checking whether received Ack or not
        mCountDownTimer = new CountDownTimer(300, 50) {
            @Override
            public void onTick(long l) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.i(TAG, "run: ack in stop " + Constants.is_ackReceived);
                        if (Constants.is_ackReceived == true) {
                            mTimerRunning = false;
                            mCountDownTimer.cancel();
                            Log.d(TAG, "run: ack received while stop");
                            startBtn.setEnabled(true);
                            startBtn.setVisibility(View.VISIBLE);
                            stopBtn.setVisibility(View.INVISIBLE);
                            stopBtn.setEnabled(false);
                            Constants.is_buttonStarted = false;
                            progressText.setText("---");
//                            Constants.is_stopButton = false;
                        }

                    }
                });
            }

            @Override
            public void onFinish() {

                if (Constants.is_ackReceived == false) {
                    Log.i(TAG, "ack not received while stop");
                    Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                    start();
                }
            }
        }.start();
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
            Constants.ack = decoder.computeCheckSum(Constants.ack);
            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
            Constants.is_batterValueReceived = false;
        }
        else if (mBluetoothLeService.batteryLevel == Constants.MID_BATTERY){
            batteryText.setBackgroundColor(Color.parseColor("#FFA500"));
            Constants.ack = decoder.computeCheckSum(Constants.ack);
            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
            Constants.is_batterValueReceived = false;
        }
        else if (mBluetoothLeService.batteryLevel == Constants.LOW_BATTERY) {
            batteryText.setBackgroundColor(Color.parseColor("#FF0000"));
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.battery_low),Toast.LENGTH_SHORT).show();
            Constants.ack = decoder.computeCheckSum(Constants.ack);
            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
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
                                Constants.ack = decoder.computeCheckSum(Constants.ack);
                                mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.ack);
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