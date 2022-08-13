package com.example.bpmonitorbleintegration.constants;

public class Constants {
    public final static String ACTION_GATT_CONNECTED =
            "android-er.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "android-er.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "android-er.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "android-er.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "android-er.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "android-er.EXTRA_DATA";

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static byte[] deviceId = {0x00,0x00,0x00,0x01};
//    public static final byte[] startValue = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x10,0x0A,0x00,0x01,0x00,0x1C,0x7D};
//    public static final byte[] checkSumError = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x14,0x0A,0x00,0x0E ,0x00,0x1C,0x7D};
//    public static final byte[] ack = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x14,0x0A,0x00,0x0A,0x00,0x1C,0x7D};
    public static byte[] startValue = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x10,0x0A,0x00,0x01,0x00,0x1C,0x7D};
    public static byte[] checkSumError = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x14,0x0A,0x00,0x0E ,0x00,0x1C,0x7D};
    public static byte[] ack = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x14,0x0A,0x00,0x0A,0x00,0x1C,0x7D};
    public static byte[] noAck = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x14,0x0A,0x00,0x00,0x00,0x1C,0x7D};
//    public static byte cancelValue = 0x02;
//    public static byte[] cancelValue = {0x7B,0x02,0x7D};
    public static byte[] cancelValue = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x10,0x0A,0x00,0x02,0x00,0x1C,0x7D};
    public static byte[] resetValue = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x02,0x0A,0x00,0x01,0x00,0x1C,0x7D};
    public static byte[] noResetValue = {0x7B,deviceId[0],deviceId[1],deviceId[2],deviceId[3],0x02,0x0A,0x00,0x00,0x00,0x1C,0x7D};
    public static final int RAW_COMMANDID = 17;
    public static final int RESULT_COMMANDID = 18;
    public static final int ERROR_COMMANDID = 19;
    public static final int ACK_COMMANDID = 20;
    public static final int DEVICE_COMMANDID = 01;
    public static final int BATTERY_COMMANDID = 21;
    public static boolean is_resultReceived = false;
    public static final int HIGH_BATTERY = 51;
    public static final int MID_BATTERY = 34;
    public static final int LOW_BATTERY = 17;
    public static final int HIGH_EXCEEDED = 170; // 120
    public static boolean is_batterValueReceived = false;
    public static boolean is_batteryReceivedAtReading = false;
    public static boolean is_ackReceived = false;
    public static boolean is_readingStarted = false;
    public static boolean is_cuffReplaced = false;
    public static boolean is_finalResult = false;
    public static boolean is_buttonStarted = false;
    public static boolean is_errorReceived = false;
    public static boolean is_irregularHB = false;
}
