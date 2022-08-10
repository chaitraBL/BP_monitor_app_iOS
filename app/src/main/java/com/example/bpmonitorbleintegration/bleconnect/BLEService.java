package com.example.bpmonitorbleintegration.bleconnect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

import com.example.bpmonitorbleintegration.R;
import com.example.bpmonitorbleintegration.constants.BLEGattAttributes;
import com.example.bpmonitorbleintegration.constants.Constants;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BLEService extends Service implements DecodeListener {

    private final static String TAG = BLEService.class.getSimpleName();
    private static final boolean TODO = false;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private static int mConnectionState = Constants.STATE_DISCONNECTED;

    private final IBinder mBinder = new LocalBinder();
    private String bluetoothAddress;
    public final static UUID UUID_CHAR_LEVEL = UUID.fromString(BLEGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
    public final static UUID UUI_SERVICE_LEVEL = UUID.fromString(BLEGattAttributes.CLIENT_SERVICE_CONFIG);
    private Decoder decoder;
    public int systalic;
    public int dystolic;
    public int rate;
    public int range;
    public long pressure;
    public int batteryLevel;
    CoordinatorLayout coordinatorLayout;
    public String errorMessage;
    public String rawReadings;
    public String finalResult;

    //    Decoder mDecoder;
    Handler mHandler;

    // To connect to bluetooth device and gatt services.
    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean connect(String address) {
        if (mBluetoothAdapter == null || address == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.bluetooth_adapter_not_initialised), Toast.LENGTH_SHORT).show();
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                }
            };
            return false;
        }
        bluetoothGattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTING);
        if (mBluetoothAdapter != null && address.equals(bluetoothAddress) && mBluetoothGatt != null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.existing_connection), Toast.LENGTH_SHORT).show();
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                }
            };


            if (mBluetoothGatt.connect()) {
                mConnectionState = Constants.STATE_CONNECTING;
                mHandler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message message) {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.connecting), Toast.LENGTH_SHORT).show();
                        // This is where you do your work in the UI thread.
                        // Your worker tells you in the message what to do.
                    }
                };
                return true;
            } else {
                mHandler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message message) {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.cannot_connect), Toast.LENGTH_SHORT).show();
                        // This is where you do your work in the UI thread.
                        // Your worker tells you in the message what to do.
                    }
                };
                return false;
            }
        }
        final BluetoothDevice bluetoothDevice = Objects.requireNonNull(mBluetoothAdapter).getRemoteDevice(address);
        if (bluetoothDevice == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.device_not_found), Toast.LENGTH_SHORT).show();
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                }
            };
            return false;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            mBluetoothGatt = bluetoothDevice.connectGatt(this, false, bluetoothGattCallback, BluetoothDevice.TRANSPORT_LE, BluetoothDevice.PHY_LE_1M);
        } else {
            mBluetoothGatt = bluetoothDevice.connectGatt(this, false, bluetoothGattCallback);
        }
        bluetoothGattCallback.onConnectionStateChange(mBluetoothGatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED);

        bluetoothAddress = address;
        mConnectionState = Constants.STATE_CONNECTING;
        return true;
    }

    // BluetoothGatt callback to connect, discover services, read and write the characteristics and data etc...
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
           // Toast.makeText(getApplicationContext(),"status onConnection " + status, Toast.LENGTH_SHORT).show();
            String intentAction;
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        mConnectionState = Constants.STATE_CONNECTED;
                        intentAction = Constants.ACTION_GATT_CONNECTED;
                        broadcastUpdate(intentAction);

                       // Toast.makeText(BLEService.this, "Connected", Toast.LENGTH_SHORT).show();
                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        gatt.requestMtu(247);
                      //  gatt.discoverServices();
                        onMtuChanged(gatt,247,status);

                      //  Toast.makeText(BLEService.this, "Connected ", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    intentAction = Constants.ACTION_GATT_DISCONNECTED;
                    broadcastUpdate(intentAction);
                    mConnectionState = Constants.STATE_DISCONNECTED;
                    disconnect();
//                    new Handler(Looper.getMainLooper()).postDelayed({
//                            initialize()
//                    }, 500);
                    break;

                default:
//                    Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
          //  Toast.makeText(BLEService.this, "mtu status " + status, Toast.LENGTH_SHORT).show();
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    Thread.sleep(600);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                gatt.discoverServices();
                onServicesDiscovered(gatt,status);
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(Constants.ACTION_GATT_SERVICES_DISCOVERED);
               List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();

                for (BluetoothGattService gattService : gattServices) {
                   String serviceUUID = gattService.getUuid().toString();
                    if(serviceUUID.equalsIgnoreCase(BLEGattAttributes.CLIENT_SERVICE_CONFIG))
                    {
                        List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
                        {
                            if(gattCharacteristic.getUuid().toString().equalsIgnoreCase(BLEGattAttributes.CLIENT_CHARACTERISTIC_CONFIG))
                            {
                                onCharacteristicRead(gatt,gattCharacteristic,status);
                                return;
                            }
                        }
                    }
                }
                new ConnectionThread().start();

                try {
                    // To refresh BluetoothGatt gatt service
                    final Method refresh = gatt.getClass().getMethod("refresh");
                    if (refresh != null) {
                        refresh.invoke(gatt);
                    }
                } catch (Exception e) {
                    // Log it
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
          //  Toast.makeText(BLEService.this, "read status " + status, Toast.LENGTH_SHORT).show();

            if (status == BluetoothGatt.GATT_SUCCESS) {
              //  Toast.makeText(BLEService.this, "onCharacteristicRead" + Constants.ACTION_DATA_AVAILABLE, Toast.LENGTH_SHORT).show();
                broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, characteristic);
                readCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
          //  Toast.makeText(BLEService.this, "onCharacteristicWrite", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
//            byte[] messageBytes = characteristic.getValue();
////            Log.i(TAG, "onCharacteristicChange " + messageBytes);
//
//            String messageString = null;
//            try {
//                messageString = new String(messageBytes, "UTF-8");
//            } catch (UnsupportedEncodingException e) {
//                Log.e(TAG, "Unable to convert message bytes to string");
//            }
          //  Toast.makeText(BLEService.this, "onCharacteristicChange" + Constants.ACTION_DATA_AVAILABLE, Toast.LENGTH_SHORT).show();
            broadcastUpdate(Constants.ACTION_DATA_AVAILABLE, characteristic);

            onCharacteristicRead(gatt,characteristic,0);
        }
    };

    // Constructor
    public BLEService() {

    }

    public void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    // To retrieve the packets from device.
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        if (UUID_CHAR_LEVEL.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            String msg;


//            To convert data to hex value.
//            if (data != null && data.length > 0) {
//                final StringBuilder stringBuilder = new StringBuilder(data.length);
//                for (byte byteChar : data) {
//                    Log.i(TAG, "hex value " + String.format("%02X ", byteChar));
////                    stringBuilder.append(String.format("%02X ", byteChar));
//                }
////                intent.putExtra(Constants.EXTRA_DATA, new String(data) + "\n" +stringBuilder.toString());
//            }

//            Log.i(TAG, "length before switch " + data[6]);
            int length = data[6];
//            Log.d(TAG, "broadcastUpdate: length " + length);

            int[] value = new int[30];

            Arrays.fill(value, (byte) 0);
            for (int i = 0; i <= length; ++i) {
           //    Log.i("Decoder", "values " + i + " " + data[i])
                value[i] = (int) (data[i] & 0xff);
//               Log.i("Decoder", "new values " + value[i]);
            }
//            Log.i("Decoder", "Command id " + (value[5]));

            // Check for checksum
            boolean checkSumVal = decoder.checkSumValidation(value, characteristic);

           // Log.i("Decoder", "checksum " + checkSumVal);
            decoder.add1(value, action);
            if (checkSumVal) {
               // Log.d(TAG, "broadcastUpdate: Checksum verified ");
                //Command Id wise receiving data.
                switch (value[5]) {
                    case Constants.DEVICE_COMMANDID:
                        Constants.deviceId = new byte[]{(byte) value[1], (byte) value[2], (byte) value[3], (byte) value[4]};
//                        Log.i(TAG, "broadcastUpdate: device byte " + (byte) value[1] + (byte) value[2] + (byte) value[3] + (byte) value[4]);
                        Constants.startValue = decoder.replaceArrayVal(Constants.startValue, Constants.deviceId);
                        Constants.ack = decoder.replaceArrayVal(Constants.ack, Constants.deviceId);
                        Constants.noAck = decoder.replaceArrayVal(Constants.noAck, Constants.deviceId);
                        Constants.resetValue = decoder.replaceArrayVal(Constants.resetValue, Constants.deviceId);
                        Constants.noResetValue = decoder.replaceArrayVal(Constants.noResetValue, Constants.deviceId);
                        Constants.checkSumError = decoder.replaceArrayVal(Constants.checkSumError, Constants.deviceId);
//                        Log.i(TAG, "new start value " + Constants.startValue);
                        break;

                    case Constants.RAW_COMMANDID:
                        Constants.is_readingStarted = true;
                        int cuffValue = value[8] * 256 + value[9];
                        int pulseValue = value[10] * 256 + value[11];
                        rawReadings = cuffValue + " / " + pulseValue;
                        intent.putExtra(Constants.EXTRA_DATA, cuffValue + " / " + pulseValue);
                        break;

                    case Constants.RESULT_COMMANDID:
                        Constants.is_finalResult = true;
                        int systolic = value[8] * 256 + value[9];
                        int dystolic = value[10] * 256 + value[11];
                        int heartRateValue = value[12];

                        finalResult = systolic + " / " + dystolic + " / " + heartRateValue;
                       // Log.d(TAG, "broadcastUpdate: raw readings " + systolic + " / " + dystolic + " / " + heartRateValue);
                        intent.putExtra(Constants.EXTRA_DATA, systolic + " / " + dystolic + " / " + heartRateValue);
//                        int rangeValue = value[13];
                        Constants.ack = decoder.computeCheckSum(Constants.ack);
                        writeCharacteristics(characteristic, Constants.ack);
                        break;

                    case Constants.ERROR_COMMANDID:
                        int error = value[8];
//                        Log.d(TAG, "broadcastUpdate: error msg " + error);
                        switch (error) {
                            case 1:
                                Constants.is_errorReceived = true;
                                msg = getString(R.string.cuff_fitment);
                                errorMessage = msg + "\n" + getString(R.string.try_again);
                                intent.putExtra(Constants.EXTRA_DATA, msg + "\n" + getString(R.string.try_again));
                                Constants.ack = decoder.computeCheckSum(Constants.ack);
                                writeCharacteristics(characteristic, Constants.ack);
                                break;

                            case 2:
                                Constants.is_errorReceived = true;
                                msg = getString(R.string.hand_movement);
                                errorMessage = msg + "\n" + getString(R.string.try_again);
                                intent.putExtra(Constants.EXTRA_DATA, msg + "\n" + getString(R.string.try_again));
                                Constants.ack = decoder.computeCheckSum(Constants.ack);
                                writeCharacteristics(characteristic, Constants.ack);
                                break;

                            case 3:
                                Constants.is_irregularHB = true;
                                msg = getString(R.string.irregular_heartbeat);
                                errorMessage = msg + "\n" + getString(R.string.try_again);
                                intent.putExtra(Constants.EXTRA_DATA, msg + "\n" + getString(R.string.try_again));
                                break;

                            case 4:
                                Constants.is_errorReceived = true;
                                msg = getString(R.string.cuff_over_pressured);
                                intent.putExtra(Constants.EXTRA_DATA, msg + "\n" + getString(R.string.try_again));
                                errorMessage = msg + "\n" + getString(R.string.try_again);
                                Constants.ack = decoder.computeCheckSum(Constants.ack);
                                writeCharacteristics(characteristic, Constants.ack);
                                break;

                            case 5:
                                Constants.is_batteryReceivedAtReading = true;
                                msg = getString(R.string.low_battery);
                                errorMessage = msg + "\n" + getString(R.string.try_again);
                                intent.putExtra(Constants.EXTRA_DATA, msg + "\n" + getString(R.string.try_again));
                                break;

                            case 6:
                                Constants.is_cuffReplaced = true;
                                msg = getString(R.string.cuff_replacement);
                                Log.d(TAG, "broadcastUpdate: cuff replaced ");
//                                errorMessage = msg ;
                                intent.putExtra(Constants.EXTRA_DATA, msg);
                                break;

                            case 7:
                                Constants.is_irregularHB = true;
                                msg = getString(R.string.Heartbeat_vary);
                                errorMessage = msg;
                                intent.putExtra(Constants.EXTRA_DATA, msg);
                                break;

                            case 8:
                                Constants.is_batteryReceivedAtReading = true;
                                msg = getString(R.string.battery_limit_exceeds);
                                errorMessage = msg;
                                intent.putExtra(Constants.EXTRA_DATA,msg);
                                break;

                            case 17:
                                Constants.is_batteryReceivedAtReading = true;
                                break;

                            default:
                                msg = " ";
                                intent.putExtra(Constants.EXTRA_DATA, msg);
                                break;
                        }
                        break;

                    case Constants.ACK_COMMANDID:
                        Constants.is_ackReceived = true;
                        int ack = value[8];
                     //   Log.i(TAG, "ack in bleservice " + ack)
                        break;

                    case Constants.BATTERY_COMMANDID:
                        Constants.is_batterValueReceived = true;
                        int batteryLevel = value[8];
                        break;
                }
            } else {
                Constants.checkSumError = decoder.computeCheckSum(Constants.checkSumError);
                writeCharacteristics(characteristic, Constants.checkSumError);
            }
        }
        sendBroadcast(intent);
    }

    // To read the data.
    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.bluetooth_adapter_not_initialised), Toast.LENGTH_SHORT).show();
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                }
            };

            return;
        }

        mBluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
    }

    //To write data to the device.
    @SuppressLint("MissingPermission")
    public void writeCharacteristics(BluetoothGattCharacteristic characteristics, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.bluetooth_adapter_not_initialised), Toast.LENGTH_SHORT).show();
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                }
            };

            return;
        }

        characteristics.setValue(value);

        mBluetoothGatt.writeCharacteristic(characteristics);
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.bluetooth_adapter_not_initialised), Toast.LENGTH_SHORT).show();
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                }
            };
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristickIndication(BluetoothGattCharacteristic characteristic, Boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            mHandler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message message) {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.bluetooth_adapter_not_initialised), Toast.LENGTH_SHORT).show();
                    // This is where you do your work in the UI thread.
                    // Your worker tells you in the message what to do.
                }
            };
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

//        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(charUuid);
//        if (enabled) {
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//        }
//        else {
//            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
//        }
//        mBluetoothGatt.writeDescriptor(descriptor);
    }

    // To get list of gatt services from bluetooth device.
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    // To get list of gatt characteristics from bluetooth device.
    public List<BluetoothGattCharacteristic> getSupportedGattCharacteristics(BluetoothGattService service) {
        if (mBluetoothGatt == null) {
            return null;
        }
        return service.getCharacteristics();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }

        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.d(TAG, "Bluetooth adapter not initialize");
            return;
        }

        mBluetoothGatt.disconnect();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
        clearServicesCache();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    //remove device authorization/ bond/ pairing
//    public static void removeBond(BluetoothDevice device){
//        try {
//            if (device == null){
//                throw new Exception();
//            }
//            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
//            method.invoke(device, (Object[]) null);
//            Log.d(LOG_TAG, "removeBond() called");
//            Thread.sleep(600);
//            Log.d(LOG_TAG, "removeBond() - finished method");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // To clear gatt services cache.
    private boolean clearServicesCache()
    {
        boolean result = false;
        try {
            Method refreshMethod = mBluetoothGatt.getClass().getMethod("refresh");
            if(refreshMethod != null) {
                result = (boolean) refreshMethod.invoke(mBluetoothGatt);
            }
        } catch (Exception e) {
            Log.e(TAG, "ERROR: Could not invoke refresh method");
        }
        return result;
    }
    public static BluetoothAdapter getBluetoothAdapter(Context context) {
        BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(BLUETOOTH_SERVICE);
        return mBluetoothManager.getAdapter();
    }

    public boolean initialize() {
        mBluetoothAdapter = getBluetoothAdapter(this);
        return true;
    }

    @Override
    public void pressureValue(int value1, int value2) {
        pressure = value1;

        if (mHandler != null) {
//            Log.i(TAG, " pressure value " + value1 + " " + value2);
            mHandler.obtainMessage(Constants.RAW_COMMANDID,value1,value2);
        }
    }

    @Override
    public void deviceId(int deviceId) {
//        Log.i(TAG, "device Id" + deviceId);
        if (mHandler != null) {
            mHandler.obtainMessage(deviceId).sendToTarget();
        }
    }

    @Override
    public void systolic(int value) {
//        Log.i(TAG, "Systa " + value);
        systalic = value;
    }

    @Override
    public void diastolic(int value) {
//        Log.i(TAG, "Diasta " + value);
        dystolic = value;
    }

    @Override
    public void heartRate(int value) {
//        Log.i(TAG, "heart " + value);
        rate = value;
    }

    @Override
    public void range(int value) {
//        Log.i(TAG, "range " + value);
        range = value;
    }

    @Override
    public void errorMsg(int err) {
        if (mHandler != null) {
            mHandler.obtainMessage(err).sendToTarget();
        }
    }

    @Override
    public void ackMsg(int ackNo) {
//        Log.i(TAG, "Ack " + ackNo);
        if (mHandler != null) {
            mHandler.obtainMessage(ackNo).sendToTarget();
        }
    }

    @Override
    public void batteryMsg(int value) {
        batteryLevel = value;
     //   Toast.makeText(getApplicationContext(), "battery4",Toast.LENGTH_SHORT).show();
//        Log.i(TAG, "Battery level " + batteryLevel);
    }

    public class LocalBinder extends Binder {
        public BLEService getService() {
            return BLEService.this;
        }
    }

    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            super.run();
            decoder = new Decoder(BLEService.this);
            decoder.start();
        }
    }
}
