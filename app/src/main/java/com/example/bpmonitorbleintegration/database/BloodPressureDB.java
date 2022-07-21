package com.example.bpmonitorbleintegration.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class BloodPressureDB {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "systolic")
    private int systolic;

    @ColumnInfo(name = "dystolic")
    private int dystolic;

    @ColumnInfo(name = "heartRate")
    private int heartRate;

    @ColumnInfo(name = "range")
    private int range;

    @ColumnInfo(name = "date")
    private String date;

    @ColumnInfo(name = "time")
    private String time;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSystolic() {
        return systolic;
    }

    public void setSystolic(int systolic) {
        this.systolic = systolic;
    }

    public int getDystolic() {
        return dystolic;
    }

    public void setDystolic(int dystolic) {
        this.dystolic = dystolic;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(int heartRate) {
        this.heartRate = heartRate;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "BloodPressureDB{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", systolic=" + systolic +
                ", dystolic=" + dystolic +
                ", heartRate=" + heartRate +
                ", range=" + range +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                '}';
    }

}
