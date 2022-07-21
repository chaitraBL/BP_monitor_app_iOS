package com.example.bpmonitorbleintegration.bleconnect;

public interface DecodeListener {
    void pressureValue(int value1, int value2);
//    void pulseValue(int value);
    void deviceId(int deviceId);
    void systolic(int value);
    void diastolic(int value);
    void heartRate(int value);
    void range(int value);
    void errorMsg(int err);

//    void errorMsg(int errNo);

    void ackMsg(int ackNo);
    void batteryMsg(int value);
}
