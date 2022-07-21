package com.example.bpmonitorbleintegration.constants;

import java.util.HashMap;
@SuppressWarnings("unchecked")
public class BLEGattAttributes {

    public static final String CLIENT_SERVICE_CONFIG = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "0000ffe1-0000-1000-8000-00805f9b34fb";

    private static final HashMap<String, String> attributes = new HashMap<String,String>();

    static {
        attributes.put(CLIENT_CHARACTERISTIC_CONFIG, "Character Level");
        attributes.put(CLIENT_SERVICE_CONFIG, "Service");
    }

    public static String lookup(String uuid) {
        String name;
        name = attributes.get(uuid);
        return name;
    }
}
