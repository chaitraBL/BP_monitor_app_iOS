package com.example.bpmonitorbleintegration.charts;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.example.bpmonitorbleintegration.R;
import com.example.bpmonitorbleintegration.database.BloodPressureDB;
import com.example.bpmonitorbleintegration.database.DatabaseClient;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Statistics2 extends AppCompatActivity {

    String xVal;
    public String TAG = Statistics2.class.getName();
    CandleStickChart candleStick;
    ArrayList<CandleEntry> yVal = new ArrayList<>();
    List<String> timeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_statistics2);
        ActionBar actioBar = getSupportActionBar();
        actioBar.setTitle("Analytics");
        actioBar.setHomeAsUpIndicator(R.drawable.ic_baseline_keyboard_arrow_left_24);
        actioBar.setDisplayHomeAsUpEnabled(true);
        xVal = getIntent().getStringExtra("xAxisVal");
        Log.i(TAG, "onCreate: xVal " + xVal);

        candleStick = findViewById(R.id.candleStickTime);

        getManualTasks();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //To retrieve data from Room DB.
    private void getManualTasks() {
        class GetTasks extends AsyncTask<Void, Void, List<BloodPressureDB>> {

            @Override
            protected List<BloodPressureDB> doInBackground(Void... voids) {
                List<BloodPressureDB> taskList = DatabaseClient
                        .getInstance(getApplicationContext())
                        .getAppDatabase()
                        .bpReadingsDao()
                        .getAll();
                return taskList;
            }

            @Override
            protected void onPostExecute(List<BloodPressureDB> tasks) {
                super.onPostExecute(tasks);
                plotCandleStickTimeWise(tasks);


            }
        }
        GetTasks gt = new GetTasks();
        gt.execute();
    }

    // Candle stick chart time based.
    public void plotCandleStickTimeWise(List<BloodPressureDB> tasks) {
        yVal.clear();
        timeList.clear();
        candleStick.clear();

        if (tasks != null && tasks.size() > 0) {
            int count = 0;
            for (BloodPressureDB list : tasks) {
                if (xVal.equals(list.getDate())) {
                    yVal.add(new CandleEntry(count, list.getSystolic(),list.getDystolic(),list.getSystolic(),list.getDystolic()));
                    timeList.add(list.getTime());
                    count++;
                }
            }

            Collections.sort(yVal,new EntryXComparator());

            CandleDataSet cds = new CandleDataSet(yVal, xVal);
            cds.setColor(Color.rgb(80, 80, 80));
            cds.setShadowColor(Color.DKGRAY);
            cds.setBarSpace(1f);
            cds.setDecreasingColor(Color.parseColor("#151B54"));
            cds.setDecreasingPaintStyle(Paint.Style.FILL);
            cds.setIncreasingColor(Color.parseColor("#151B54"));
            cds.setIncreasingPaintStyle(Paint.Style.STROKE);
            cds.setNeutralColor(Color.BLUE);
//            cds.setValueTextColor(Color.BLACK);
            // Set color as per the mode - Dark mode/Light mode.
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    cds.setValueTextColor(Color.WHITE);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    cds.setValueTextColor(Color.BLACK);
                    break;
            }
            cds.setValueTextSize(10);
            CandleData cd = new CandleData(cds);
            candleStick.setData(cd);
            candleStick.getDescription().setEnabled(false);


            //X axis
            XAxis xAxis = candleStick.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setLabelCount(timeList.size());
            xAxis.setValueFormatter(new IndexAxisValueFormatter(timeList));
            xAxis.setAvoidFirstLastClipping(true);
            xAxis.setLabelRotationAngle(-45);
            xAxis.setDrawGridLines(false);
            xAxis.setDrawAxisLine(false);
            xAxis.setGranularity(1f);
            xAxis.setGranularityEnabled(true);
            xAxis.setCenterAxisLabels(false);
            xAxis.setEnabled(true);
            CustomMarkerView mv = new CustomMarkerView(Statistics2.this, R.layout.marker_view);
            candleStick.setMarkerView(mv);

            //Y axis
            YAxis yAxisRight = candleStick.getAxisRight();
            yAxisRight.setEnabled(false);
            YAxis yAxisLeft = candleStick.getAxisLeft();
            yAxisLeft.setLabelCount(6,true);
            yAxisLeft.setDrawAxisLine(false);
            yAxisLeft.setAxisMinimum(50);
            yAxisLeft.setAxisMaximum(200);

            // Set color as per the mode - Dark mode/Light mode.
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    xAxis.setTextColor(Color.WHITE);
                    yAxisLeft.setTextColor(Color.WHITE);
                    yAxisRight.setTextColor(Color.WHITE);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    xAxis.setTextColor(Color.BLACK);
                    yAxisLeft.setTextColor(Color.BLACK);
                    yAxisRight.setTextColor(Color.BLACK);
                    break;
            }

            if (yVal.size() > 1){
                Entry lastEntry = yVal.get(yVal.size()-1);
                Highlight highlight = new Highlight(lastEntry.getX(), lastEntry.getY(), 0);
                highlight.setDataIndex(0);
                candleStick.highlightValue(highlight);
                candleStick.moveViewToX(timeList.size()-1);
            }
            else
            {
                Log.i(TAG, "No data found!!!");
            }

            if (yVal.size() >= 6) {
                candleStick.setVisibleXRangeMaximum(6);
            }
            else
            {
                candleStick.invalidate();
            }
            candleStick.invalidate();
            candleStick.notifyDataSetChanged();
            candleStick.animateXY(1000,1000);
        }
        else {
            Log.d(TAG, "Data not found");
        }
    }
}