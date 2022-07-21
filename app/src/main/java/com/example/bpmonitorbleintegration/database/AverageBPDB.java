package com.example.bpmonitorbleintegration.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class AverageBPDB {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "systolic")
    private int systolic;

    @ColumnInfo(name = "dystolic")
    private int dystolic;

    @ColumnInfo(name = "date")
    private String date;

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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "AverageBPDB{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", systolic=" + systolic +
                ", dystolic=" + dystolic +
                ", date='" + date + '\'' +
                '}';
    }
}
