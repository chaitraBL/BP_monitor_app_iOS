package com.example.bpmonitorbleintegration.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BpReadingsDao {

    @Query("SELECT * FROM BloodPressureDB")
    List<BloodPressureDB> getAll();

    @Insert
    void insert(BloodPressureDB chatDB);

    @Delete
    void delete(BloodPressureDB chatDB);

    @Update
    void update(BloodPressureDB chatDB);
}
