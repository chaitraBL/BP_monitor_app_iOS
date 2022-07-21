package com.example.bpmonitorbleintegration.constants;

import android.util.Log;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;

public class StringUtils {
    private static final String TAG = "StringUtils";

    private static String byteToHex(byte b) {
        char char1 = Character.forDigit((b & 0xF0) >> 4, 16);
        char char2 = Character.forDigit((b & 0x0F), 16);

        return String.format("%1$s%2$s", char1, char2);
//                ("0x%1$s%2$s", char1, char2);
    }

    public static String byteArrayInHexFormat(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder(byteArray.length * 2);
//        stringBuilder.append("{ ");
        for (int i = 0; i < byteArray.length; i++) {
//            if (i > 0) {
//                stringBuilder.append(", ");
//            }
            String hexString = byteToHex(byteArray[i]);
            stringBuilder.append(hexString);
        }
//        stringBuilder.append(" }");

        return stringBuilder.toString();
    }

    public static byte[] bytesFromString(String string) {
        byte[] stringBytes = new byte[2];
        try {
            stringBytes = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to convert message string to byte array");
        }

        return stringBytes;
    }

    @Nullable
    public static String stringFromBytes(byte[] bytes) {
        String byteString = null;
        try {
            byteString = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Unable to convert message bytes to string");
        }
        return byteString;
    }

    public static String hexToString(String hex) {
        StringBuilder sb = new StringBuilder();
        char[] hexData = hex.toCharArray();
        for (int count = 0; count < hexData.length - 1; count += 2) {
            int firstDigit = Character.digit(hexData[count], 16);
            int lastDigit = Character.digit(hexData[count + 1], 16);
            int decimal = firstDigit * 16 + lastDigit;
            sb.append((char) decimal);
        }/* www  .j  av a  2 s .c  o m*/
        return sb.toString();
    }

    public static byte[] toBytes(String str) {
        StringBuffer convString;

        // Remove hex prefix of "0x" if exists
        if (str.length() > 1 && str.toLowerCase().startsWith("0x")) {
            convString = new StringBuffer(str.substring(2));
        }
        else {
            convString = new StringBuffer(str);
        }

        // For odd sized strings, pad on the left with a 0
        if (convString.length() % 2 == 1) {
            convString.insert(0, '0');
        }

        byte[] result = new byte[convString.length() / 2];

        for (int i = 0; i < convString.length(); i += 2) {
            result[i/2] = (byte) (Integer.parseInt(convString.substring(i, i + 2), 16) & 0xFF);
        }

        return result;
    }


}
