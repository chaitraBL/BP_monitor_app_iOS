package com.example.bpmonitorbleintegration.reading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.bpmonitorbleintegration.R;
import com.example.bpmonitorbleintegration.bleconnect.Decoder;
import com.example.bpmonitorbleintegration.database.RoomDB;
import com.example.bpmonitorbleintegration.home.HomePage;

import java.util.Objects;

public class ManualReadings extends AppCompatActivity {
    EditText systolic, diastolic, heartRate;
    Button save;
    Decoder decoder;
    RoomDB database;
    int map = 0;
//    RecyclerView manualList;
    String TAG = ManualReadings.class.getName();
    ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_manual_readings);

        //To change status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(ManualReadings.this, R.color.blue_200));
        View decorView = getWindow().getDecorView(); //set status background black
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); //set status text  light

        systolic = findViewById(R.id.manual_systa);
        diastolic = findViewById(R.id.manual_diasta);
        heartRate = findViewById(R.id.manual_heartRate);
        save = findViewById(R.id.save_manual);
//        manualList = findViewById(R.id.recyclerview_list);
        progressBar = findViewById(R.id.progress_manual);
//        manualList.setLayoutManager(new LinearLayoutManager(this));
        decoder = new Decoder();
        database = new RoomDB();

        ActionBar actioBar = getSupportActionBar();
        Objects.requireNonNull(actioBar).setTitle(R.string.manual_reading);
//        actioBar.setHomeAsUpIndicator(R.drawable.ic_baseline_keyboard_arrow_left_24);
        actioBar.setDisplayHomeAsUpEnabled(true);
        actioBar.setDisplayShowHomeEnabled(true);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                //Method 1: Validating the edit text fields.
//                if(TextUtils.isEmpty(strUserName)) {
//                    etUserName.setError("Your message");
//                    return;
//                }

                //Method 2: Validating the edit text fields.
                if (systolic.getText().toString().equals(""))
                {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.enter_systolic_value),Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
                else if (diastolic.getText().toString().equals(""))
                {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.enter_diastolic_value),Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
                else if (heartRate.getText().toString().equals(""))
                {
                    Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.enter_heart_rate),Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
                else {
                    //Calculates mean arterial pressure(MAP) value.
                    map = decoder.calculateMAP(Integer.parseInt(systolic.getText().toString()),Integer.parseInt(diastolic.getText().toString()));

                    if ((Integer.parseInt(systolic.getText().toString()) < 30) || (Integer.parseInt(systolic.getText().toString()) > 200)){
                        Toast.makeText(getApplicationContext(),  getApplicationContext().getResources().getString(R.string.systolic_range_fault), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                    else if ((Integer.parseInt(diastolic.getText().toString()) < 40) || (Integer.parseInt(diastolic.getText().toString()) > 120)) {
                        Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.diastolic_range_fault), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                    else {
                        //Saves to local database.
                        database.saveTask("No device",Integer.parseInt(systolic.getText().toString()),Integer.parseInt(diastolic.getText().toString()),Integer.parseInt(heartRate.getText().toString()),map,ManualReadings.this);
                        //After saving data make textfield empty.
                        systolic.setText("");
                        diastolic.setText("");
                        heartRate.setText("");
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //To refresh activity
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
//        startActivity(getIntent());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}