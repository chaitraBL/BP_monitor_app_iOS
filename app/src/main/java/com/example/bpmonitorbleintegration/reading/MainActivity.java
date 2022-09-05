package com.example.bpmonitorbleintegration.reading;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
//import org.apache.commons.codec.binary.Hex;
import com.example.bpmonitorbleintegration.home.HomePage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bpmonitorbleintegration.R;
import com.example.bpmonitorbleintegration.home.HomePage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    List<BluetoothDevice> listBluetoothDevice;
    List<String> mBlE;
    BluetoothAdapter bluetoothAdapter;
    ArrayList<String> deviceList;
    ArrayAdapter<String> deviceAdapter;
    ListAdapter adapterLeScanResult;
    private BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 100000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private static final int PERMISSIONS_REQUEST_LOCATION = 101;
    ListView listView;
    Button manualMeasurement;
    String deviceAddress;
    ProgressBar progressBar;
    private static final String TAG = "BluetoothLEService";
    BluetoothDevice bluetoothDevice;
    BluetoothManager bluetoothManager;
    Context context;
    HashMap<String, String> filterDevices;

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        //To change status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(MainActivity.this, R.color.blue_200));
        View decorView = getWindow().getDecorView(); //set status background black
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); //set status text  light

        // Check if BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    getApplicationContext().getResources().getString(R.string.bluetooth_le_not_supported),
                    Toast.LENGTH_LONG).show();
//            finish();
        }

        //Check for bluetooth enable and disable.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        context = this;
        progressBar = findViewById(R.id.progress_scan);
        listView = findViewById(R.id.lelist);
        listBluetoothDevice = new ArrayList<BluetoothDevice>();
        deviceList = new ArrayList<String>();
        deviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceList);
        listView.setAdapter(deviceAdapter);
        manualMeasurement = findViewById(R.id.measurement);

        listView.setOnItemClickListener(scanResultOnItemClickListener);

        filterDevices = new HashMap<>();
        checkPermissions(MainActivity.this, this);
        mHandler = new Handler();

        ActionBar actioBar = getSupportActionBar();
        Objects.requireNonNull(actioBar).setTitle(R.string.scan_device);
//        actioBar.setHomeAsUpIndicator(R.drawable.ic_baseline_keyboard_arrow_left_24);
        actioBar.setDisplayHomeAsUpEnabled(true);
        actioBar.setDisplayShowHomeEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#151B54")));

        bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Navigates to manualReading activity.
        manualMeasurement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ManualReadings.class);
                startActivity(intent);
            }
        });
    }

    // List of bluetooth scan devices.
    AdapterView.OnItemClickListener scanResultOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//            bluetoothDevice = (BluetoothDevice) adapterView.getItemAtPosition(i);

            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);

            String msg = info;
            deviceAddress = address;

//            new AlertDialog.Builder(MainActivity.this)
////                    .setTitle(bluetoothDevice.getName())
//                    .setMessage(msg)
//                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {

            //Navigating to next activity on tap of bluetooth device address.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent intent = new Intent(MainActivity.this, ReadingData.class);
                intent.putExtra("Device", deviceAddress);
                intent.putExtra("DeviceName", info);
                startActivity(intent);
            }
//                        }
//                    })
//                    .show();
        }
    };

    //Describes bluetooth device type.
    @SuppressLint("MissingPermission")
    private String getBTDevieType(BluetoothDevice d) {
        String type = "";

        switch (d.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "unknown...";
        }
        return type;
    }

    //Menu item.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu_file, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bluetooth_searching:
                progressBar.setVisibility(View.VISIBLE);
                scanLeDevice(true);
                return true;

            case android.R.id.home:
                //To refresh activity
                Intent i = new Intent(this, HomePage.class);
//                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
//                this.finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressLint({"ObsoleteSdkInt", "MissingPermission"})
    @Override
    protected void onResume() {
        super.onResume();
        //Check for bluetooth enable and disable.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Stop scanning bluetooth devices.
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mGatt == null) {
            return;
        }
        mGatt.close();
        mGatt = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.bluetooth_not_enable), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //Scan for bluetooth devices.
    @SuppressLint({"ObsoleteSdkInt", "MissingPermission"})
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            progressBar.setVisibility(View.GONE);
            mHandler.postDelayed(new Runnable() {
                @SuppressLint({"ObsoleteSdkInt", "MissingPermission"})
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
//                adapterBluetoothDevice.clear();
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            bluetoothDevice = result.getDevice();
            addBluetoothDevice(bluetoothDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
//                Log.i("TAG", "scan data" + sr.getDevice());
                addBluetoothDevice(sr.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.no_device_found), Toast.LENGTH_SHORT).show();
        }

        //Adding the scanned devices to listview.
        @SuppressLint("MissingPermission")
        private void addBluetoothDevice(BluetoothDevice device) {
            if (!listBluetoothDevice.contains(device)) {
//                listBluetoothDevic1.add(device.getName() + "\n" + device.getAddress());
                listBluetoothDevice.add(device);
//                filterDevices.put(device, btMac);
                deviceList.add(device.getName() + "\n" + device.getAddress());
//                deviceList.add(device.getName());
                deviceAdapter.notifyDataSetChanged();
                listView.invalidateViews();
            }
        }
    };

    private final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            Log.i("onLeScan", device.toString());
                        }
                    });
                }
            };

    //Check permissions for location.
    public void checkPermissions(Activity activity, Context context) {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_LOCATION);
        }
    }
}