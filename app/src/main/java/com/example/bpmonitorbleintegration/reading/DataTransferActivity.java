package com.example.bpmonitorbleintegration.reading;

import static com.example.bpmonitorbleintegration.R.layout.activity_data_transfer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
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

//For reading data through alert dialog
public class DataTransferActivity extends AppCompatActivity{

    ImageButton startBtn;
    private BLEService mBluetoothLeService;
    private String deviceAddress;
    public BluetoothGattCharacteristic mNotifyCharacteristic;
    private boolean mConnected;
    BluetoothDevice bluetoothDevice;
    IntentFilter intentFilter;
    AlertDialog.Builder builder, builder1;
    TextView statusText, systolicText, diastolicText, heartRateText,rangeText, tv, batteryLevel;
    private String TAG = "DataTransferActivity";
    SharedPreferences pref;

    Button readBtn;
//    RecyclerView recyclerView;
    Decoder decoder;
    public AlertDialog dialog, dialog1;
    RoomDB localDB;
    ProgressBar progress;
    String resultData;

    public int counter = 0;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private long mTimeLeftInMillis = 2000;
    private final long startTime = 50;
    View customView;

    private final Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case Constants.RAW_COMMANDID:
                    Log.i(TAG, "obj " + String.valueOf(message.obj));
                    Log.i(TAG, "arg1 & arg 2" + message.arg1 + " " + message.arg2);
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(activity_data_transfer);

        startBtn = findViewById(R.id.btn_read);
        statusText = findViewById(R.id.actual_status);
        systolicText = findViewById(R.id.systalic_val);
        diastolicText = findViewById(R.id.dystalic_val);
        heartRateText = findViewById(R.id.rate_val);
        rangeText = findViewById(R.id.range_val);
        deviceAddress = getIntent().getStringExtra("Device");
        readBtn = findViewById(R.id.final_val);
//        recyclerView = findViewById(R.id.recyclerview_tasks);
        batteryLevel = findViewById(R.id.battery_level);
        progress = findViewById(R.id.progress_start);

//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        intentFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(broadCastReceiver, intentFilter);

        //Initialising Decoder class.
        decoder = new Decoder();
        localDB = new RoomDB();

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.readings);

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        // Connect to the device through BLE.
        registerReceiver(broadCastReceiver, GattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mBluetoothLeService.close();
                mBluetoothLeService.disconnect();
                mBluetoothLeService.stopSelf();
                boolean result = mBluetoothLeService.connect(deviceAddress);
            }
        }

        //Send request to START the readings.
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mNotifyCharacteristic != null) {
//                    Log.i(TAG, "Start value " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                    Constants.startValue = decoder.computeCheckSum(Constants.startValue);
//                    Log.i(TAG, "Start value after checksum " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                    mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.startValue);
                }
//
                mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
                    @Override
                    public void onTick(long l) {
                        counter++;
                        mCountDownTimer = new CountDownTimer(startTime,10) {
                            @Override
                            public void onTick(long l) {
                                counter++;
                            }

                            @Override
                            public void onFinish() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (Constants.is_ackReceived == true) {
                                            mCountDownTimer.cancel();
//                                                Constants.is_ackReceived = false;

                                                //Alert controller to start readings.
                                                builder = new AlertDialog.Builder(DataTransferActivity.this);
                                                builder.setTitle(getApplication().getResources().getString(R.string.readings));
                                                LayoutInflater layoutInflater = getLayoutInflater();

                                                //this is custom dialog
                                                //custom_popup_dialog contains textview only
                                                View customView = layoutInflater.inflate(R.layout.custom_popup_dialog, null);
//                                    LinearLayout attLayout = customView.findViewById(R.id.att_layout);
                                                // reference the textview of custom_popup_dialog
                                                tv = customView.findViewById(R.id.textView_popup);
                                                tv.setTextSize(15);
//                                    tv.setText(resultData);

                                                //Send request to force STOP the readings.
                                                builder.setNegativeButton(getApplication().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        // Nothing done here.
                                                    }
                                                });

                                                //Send Ack - received readings.
                                                builder.setPositiveButton(getApplication().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        if (mNotifyCharacteristic != null) {
                                                            Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                            Log.i(TAG, "Stop value after checksum " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                                                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);

                                                            mCountDownTimer = new CountDownTimer(startTime, 10) {
                                                                @Override
                                                                public void onTick(long l) {
                                                                    counter++;
//                                                Log.i(TAG, "counter Started " + startTime);
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Log.i(TAG, "run: cuff replaced " + Constants.is_cuffReplaced);
                                                                                mCountDownTimer = new CountDownTimer(50, 10) {
                                                                                    @Override
                                                                                    public void onTick(long l) {
                                                                                        counter++;
//                                                Log.i(TAG, "counter Started " + startTime);

                                                                                    }

                                                                                    @Override
                                                                                    public void onFinish() {
//                                                Log.i(TAG, "Stopped");
                                                                                        runOnUiThread(new Runnable() {
                                                                                            @Override
                                                                                            public void run() {
                                                                                                if (Constants.is_ackReceived == true) {
                                                                                                    mCountDownTimer.cancel();
                                                                                                    dialog.dismiss();
                                                                                                    Constants.is_readingStarted = false;
                                                                                                    Constants.is_resultReceived = false;
//                                                                                Constants.is_ackReceived = false;
                                                                                                }
                                                                                                else if (Constants.is_cuffReplaced == true) {
                                                                                                    mCountDownTimer.cancel();
                                                                                                    dialog.dismiss();
                                                                                                Log.i(TAG, "run: cuff replaced before alert start " + Constants.is_cuffReplaced);
                                                                                                    alertDialogForReset();
                                                                                                }
                                                                                            }
                                                                                        });
                                                                                    }
                                                                                }.start();
                                                                        }
                                                                    });
                                                                }

                                                                @Override
                                                                public void onFinish() {
//                                                Log.i(TAG, "Stopped");
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
//                                                                            Log.i(TAG, "run: cuff replaced " + Constants.is_cuffReplaced);
                                                                            if (Constants.is_ackReceived == false){
                                                                                dialog.show();
                                                                                Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                            Log.i(TAG, "Stop value after checksum " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                                                                                mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                                                                start();
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }.start();
                                                        }
                                                    }
                                                });

                                                builder.setView(customView);
                                                dialog = builder.create();
                                                //Prevent dialog box from getting dismissed on back key pressed
                                                dialog.setCancelable(false);
                                                //Prevent dialog box from getting dismissed on outside touch
                                                dialog.setCanceledOnTouchOutside(false);
                                                //To hide Ok button until readings complete.
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
                                                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                                    @Override
                                                    public void onShow(DialogInterface dialogInterface) {
//                                                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                                    }
                                                });
                                            }
                                            dialog.show();

                                                //Send request to force STOP the readings WRT: timer.
                                                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        if (mNotifyCharacteristic != null) {
                                                            Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
//                            Log.i(TAG, "Force stop value after checksum " + Arrays.toString(Constants.startValue) + " " + Constants.startValue);
                                                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);

                                                            mCountDownTimer = new CountDownTimer(startTime, 10) {
                                                                @Override
                                                                public void onTick(long l) {
                                                                    counter++;
//                                            Log.i(TAG, "counter Started " + startTime);
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
//                                                                            Log.i(TAG, "run ontick: ack " + Constants.is_ackReceived);
//                                                                            Toast.makeText(getApplicationContext(), "ontick ack " + Constants.is_ackReceived, Toast.LENGTH_SHORT).show();
                                                                            mCountDownTimer = new CountDownTimer(30, 10) {
                                                                                @Override
                                                                                public void onTick(long l) {
                                                                                    counter++;
//                                            Log.i(TAG, "counter Started " + startTime);
                                                                                }

                                                                                @Override
                                                                                public void onFinish() {
//                                            Log.i(TAG, "Stopped");
                                                                                    runOnUiThread(new Runnable() {
                                                                                        @Override
                                                                                        public void run() {
//                                                                            Log.i(TAG, "run onfinish: ack " + Constants.is_ackReceived);
//                                                                            Toast.makeText(getApplicationContext(), "onfinish ack " + Constants.is_ackReceived, Toast.LENGTH_SHORT).show();
                                                                                            if (Constants.is_ackReceived == true) {
                                                                                                mCountDownTimer.cancel();
                                                                                                dialog.dismiss();
//                                                                                dialog.setCancelable(true);
                                                                                                Constants.is_readingStarted = false;
//                                                                            Constants.is_ackReceived = false;
                                                                                            }
                                                                                        }
                                                                                    });
                                                                                }
                                                                            }.start();
                                                                        }
                                                                    });
                                                                }

                                                                @Override
                                                                public void onFinish() {
//                                            Log.i(TAG, "Stopped");
                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
//                                                                            Log.i(TAG, "run onfinish: ack " + Constants.is_ackReceived);
//                                                                            Toast.makeText(getApplicationContext(), "onfinish ack " + Constants.is_ackReceived, Toast.LENGTH_SHORT).show();
                                                                            if (Constants.is_ackReceived == false){
//                                                Log.i(TAG, "Start again");
                                                                                dialog.show();
                                                                                Constants.cancelValue = decoder.computeCheckSum(Constants.cancelValue);
                                                                                mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.cancelValue);
                                                                                start();
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }.start();
                                                        }
                                                    }
                                                });
//                                                mCountDownTimer.cancel();
//                                                Constants.is_ackReceived = false;
                                        }
                                    }
                                });
                            }
                        }.start();
//                        Log.i(TAG, "counter Started " + mTimeLeftInMillis);
                    }

                    @Override
                    public void onFinish() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (Constants.is_ackReceived == false){
                                    mCountDownTimer.cancel();
                                    Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.please_start_again), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }.start();
            }
        });

        // To read final values and store it to local DB.
        readBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress.setVisibility(View.VISIBLE);
                systolicText.setText(String.valueOf(mBluetoothLeService.systalic));
                diastolicText.setText(String.valueOf(mBluetoothLeService.dystolic));
                heartRateText.setText(String.valueOf(mBluetoothLeService.rate));
                rangeText.setText(String.valueOf(mBluetoothLeService.range));

                if (systolicText.getText().toString().equals(" ")) {
                    Toast.makeText(getApplicationContext(),"Please enter systolic value",Toast.LENGTH_SHORT).show();
                }
                else if (diastolicText.getText().toString().equals(" ")) {
                    Toast.makeText(getApplicationContext(),"Please enter diastolic value",Toast.LENGTH_SHORT).show();
                }
                else if (heartRateText.getText().toString().equals(" ")) {
                    Toast.makeText(getApplicationContext(),"Please enter heart rate value",Toast.LENGTH_SHORT).show();
                }
                else if (rangeText.getText().toString().equals(" ")) {
                    Toast.makeText(getApplicationContext(),"Please enter MAP value",Toast.LENGTH_SHORT).show();
                }
                else {

                    localDB.saveTask(deviceAddress, mBluetoothLeService.systalic, mBluetoothLeService.dystolic, mBluetoothLeService.rate, mBluetoothLeService.range, "0",DataTransferActivity.this);

                }
                progress.setVisibility(View.GONE);
            }
        });
    }

    // To check cuff replacement is reset or not.
    private void alertDialogForReset() {
        builder1 = new AlertDialog.Builder(DataTransferActivity.this);
        builder1.setTitle(getApplication().getResources().getString(R.string.message));
        builder1.setMessage(getApplication().getResources().getString(R.string.cuff_replaced));
        builder1.setPositiveButton(getApplication().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, int which) {
                        Log.i(TAG, "onClick: cuff replaced after alert " + Constants.is_cuffReplaced);

                            Constants.resetValue = decoder.computeCheckSum(Constants.resetValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.resetValue);

                        setTimerForResetVal();
                    }
                });
        builder1.setNegativeButton(getApplication().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                            Constants.noResetValue = decoder.computeCheckSum(Constants.noResetValue);
//          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.noResetValue);

                            mCountDownTimer = new CountDownTimer(startTime, 10) {
                                @Override
                                public void onTick(long l) {
                                    counter++;
//                                                Log.i(TAG, "counter Started " + startTime);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (Constants.is_ackReceived == true) {
                                                mCountDownTimer.cancel();
                                                dialog1.dismiss();
                                                Constants.is_cuffReplaced = false;
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onFinish() {
//                                                Log.i(TAG, "Stopped");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (Constants.is_ackReceived == false){
//                                                Log.i(TAG, "Start again");
                                                dialog1.show();
                                                Constants.noResetValue = decoder.computeCheckSum(Constants.noResetValue);
////          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                                                mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.noResetValue);
                                                start();
                                            }
                                        }
                                    });
                                }
                            }.start();
                    }
                });
        dialog1 = builder1.create();
        //Prevent dialog box from getting dismissed on back key pressed
        dialog1.setCancelable(false);
        //Prevent dialog box from getting dismissed on outside touch
        dialog1.setCanceledOnTouchOutside(false);
        dialog1.show();
    }

    public void setTimerForResetVal() {
        mCountDownTimer = new CountDownTimer(startTime, 10) {
            @Override
            public void onTick(long l) {
                counter++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Constants.is_ackReceived == true) {
                            mCountDownTimer.cancel();
                            dialog1.dismiss();
                            Constants.is_cuffReplaced = false;
                        }
                    }
                });
            }

            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Constants.is_ackReceived == false){
                            dialog1.show();
                            Constants.resetValue = decoder.computeCheckSum(Constants.resetValue);
////          Log.i(TAG, "Reset value after checksum " + Arrays.toString(Constants.resetValue) + " " + Constants.resetValue);
                            mBluetoothLeService.writeCharacteristics(mNotifyCharacteristic, Constants.resetValue);
                            start();
                        }
                    }
                });
            }
        }.start();
    }

    //To delete the memory cache.
//    public void deleteCache(Context context) {
//        try {
//            File dir = context.getCacheDir();
//            if (dir.list() != null) {
//                deleteDir2(dir);
//            }
//        } catch (Exception e) { e.printStackTrace();}
//    }
//
//    public boolean deleteDir2(File dir) {
//        if (dir.isDirectory()) {
//            for (File child : dir.listFiles()) {
//                boolean success = deleteDir2(child);
//                if (!success) {
//                    return false;
//                }
//            }
//        }
//        return dir.delete();
//    }

    @Override
    protected void onResume() {
        super.onResume();
        // Connect to the device through BLE.
            registerReceiver(broadCastReceiver, GattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                mBluetoothLeService.close();
                mBluetoothLeService.disconnect();
                mBluetoothLeService.stopSelf();
                boolean result = mBluetoothLeService.connect(deviceAddress);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Disconnect through services.
        unregisterReceiver(broadCastReceiver);
        dialog.dismiss();
        dialog1.dismiss();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //To bind the service connection.
        Intent getServiceIntent = new Intent(DataTransferActivity.this, BLEService.class);
        bindService(getServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        progress.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind service connection.
        unbindService(mServiceConnection);
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        deleteCache(getApplicationContext());
//    }

    //Menu item.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.data_menu_file, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.disable_bluetooth) {
            dialog = new AlertDialog.Builder(DataTransferActivity.this)
                    .setTitle(getApplication().getResources().getString(R.string.message))
                    .setMessage(getApplication().getResources().getString(R.string.are_you_sure_disable_bluetooth))
                    .setPositiveButton(getApplication().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
                            bAdapter.disable();
                            mBluetoothLeService.disconnect();
                            mBluetoothLeService.close();
                            //Navigating to next activity on tap of ok button.
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Intent intent = new Intent(DataTransferActivity.this, HomePage.class);
                                startActivity(intent);
//                                finish();
                            }
                        }
                    })
                    .setNegativeButton(getApplication().getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialog.dismiss();
                        }
                    }).show();
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
        @Override
        public void onReceive(Context context, Intent intent) {
//            updateGUI(intent);
            final String action = intent.getAction();
            if (Constants.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState("Connected");
            }
//            else if (Constants.ACTION_GATT_CONNECTING.equals(action)) {
//
//            }
            else if (Constants.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState("Disconnected");
            }

            else if (Constants.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //Receive services and characteristics
                List<BluetoothGattService> gattService = mBluetoothLeService.getSupportedGattServices();
//                Log.i("TAG", "Size " + gattService.size());
                for (BluetoothGattService service : gattService)
                {
//                    Log.i(TAG, service.getUuid().toString());
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
//                                Log.i(TAG, BLEGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
                                mNotifyCharacteristic = gattCharacteristic;
                                mBluetoothLeService.setCharacteristicNotification(gattCharacteristic,true);
                                return;
                            }
                        }
                    }
                }
            }

            else if (Constants.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(Constants.EXTRA_DATA));
            }
        }
    };

//    https://designcode.io/uikit-ios15-part2-version-control

    private  void displayData(String data) {

//        Log.i(TAG, "received data before " + data);
        if (data != null) {
//           Log.i(TAG, "reading started and result received " + Constants.is_readingStarted + " " + Constants.is_resultReceived);
            mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
//                    mTimeLeftInMillis = millisUntilFinished;
                    counter = counter++;
//                    Log.i(TAG, "timer in readings " + mTimeLeftInMillis);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progress.setVisibility(View.GONE);
                            if (Constants.is_readingStarted == true)
                            {
                                mCountDownTimer.cancel();
                                if ((mBluetoothLeService.systalic < 30) || (mBluetoothLeService.systalic > 200)){
                                    Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.systolic_error), Toast.LENGTH_SHORT).show();
                                }
                                else if ((mBluetoothLeService.dystolic < 40) || (mBluetoothLeService.dystolic > 120)) {
                                    Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.diastolic_error), Toast.LENGTH_SHORT).show();
                                }
                                else {
                                    tv.setText(data);
                                }
//                                Constants.is_readingStarted = false;
                            }

                            // Method 1: To enable/disable Ok/cancel button on basis of readings.
                            if (Constants.is_resultReceived == true) {
                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                            }
                            else
                            {
                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                            }

//          Method 2:  ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(Constants.is_resultReceived == true);
                        }
                    });
                }

                @Override
                public void onFinish() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Constants.is_readingStarted == false){
                                mCountDownTimer.cancel();
                                Toast.makeText(DataTransferActivity.this,getApplication().getResources().getString(R.string.please_start_again),Toast.LENGTH_SHORT).show();
                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
//                                dialog.dismiss();
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

    private void startTimer() {
        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                counter = counter++;
//                Log.i(TAG, "timer in battery " + mTimeLeftInMillis);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Constants.is_batterValueReceived == true)
                        {
                            mCountDownTimer.cancel();
                            //Showing battery level using color code.
                            showBattery();
//                    Constants.is_batterValueReceived = false;
                        }
                    }
                });
            }

            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Constants.is_batterValueReceived == false){
                            mCountDownTimer.cancel();
                            Toast.makeText(DataTransferActivity.this,getApplication().getResources().getString(R.string.please_connect_again),Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }.start();
    }

    // Change color on basis of battery level.
    public void showBattery(){
//        Log.i(TAG, "Battery level " + mBluetoothLeService.batteryLevel);
//        progress.setVisibility(View.GONE);
//        if (mBluetoothLeService.batteryLevel == Constants.HIGH_BATTERY) {
//            batteryLevel.setBackgroundColor(Color.parseColor("#008000"));
//        }
//        else if (mBluetoothLeService.batteryLevel == Constants.MID_BATTERY){
//            batteryLevel.setBackgroundColor(Color.parseColor("#FFA500"));
//        }
//        else if (mBluetoothLeService.batteryLevel == Constants.LOW_BATTERY) {
//            batteryLevel.setBackgroundColor(Color.parseColor("#FF0000"));
//            Toast.makeText(getApplicationContext(), getApplication().getResources().getString(R.string.low_battery),Toast.LENGTH_SHORT).show();
//        }
//        Constants.is_batterValueReceived = false;
    }

    // Connect and disconnect to the services.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BLEService.LocalBinder) service).getService();

            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
//                finish();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mBluetoothLeService.close();
                mBluetoothLeService.disconnect();
                mBluetoothLeService.stopSelf();
                mBluetoothLeService.connect(deviceAddress);
                mBluetoothLeService.setHandler(myHandler);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    //Updating connection status through text field, if disconnected status navigating to mainActivity through alert dialog.
    private void updateConnectionState(final String status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText(status);

                if (status.equals("Disconnected")){
                    new AlertDialog.Builder(DataTransferActivity.this)
                            .setTitle(getApplication().getResources().getString(R.string.message))
                            .setMessage(getApplication().getResources().getString(R.string.connection_terminated))
                            .setPositiveButton(getApplication().getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    //Navigating to next activity on tap of ok button.
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        Intent intent = new Intent(DataTransferActivity.this, MainActivity.class);
                                        startActivity(intent);
                                    }
                                }
                            }).show();
                }
            }
        });
    }
}

