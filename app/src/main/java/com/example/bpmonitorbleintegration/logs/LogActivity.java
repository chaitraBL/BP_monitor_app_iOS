package com.example.bpmonitorbleintegration.logs;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bpmonitorbleintegration.R;
import com.example.bpmonitorbleintegration.database.BloodPressureDB;
import com.example.bpmonitorbleintegration.database.DatabaseClient;
import com.example.bpmonitorbleintegration.home.HomePage;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LogActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private final String TAG = LogActivity.class.getName();
    List<BloodPressureDB> newTask = new ArrayList<>();
    ArrayList<BloodPressureDB> recylerDateBP = new ArrayList<>();
    BottomNavigationView logBottomNavigationView;
    List<Date> resultDate = new ArrayList<>();

    RecyclerView logRecycleView;
    Button selectBtn,startBtn,endBtn;
    DatePickerDialog picker;
    TextView no_data_found,startDate,endDate;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_log);

        //To change status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(LogActivity.this, R.color.blue_200));
        View decorView = getWindow().getDecorView(); //set status background black
        decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); //set status text  light

        ActionBar actioBar = getSupportActionBar();
        Objects.requireNonNull(actioBar).setTitle(R.string.logs);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#151B54")));

//        actioBar.setHomeAsUpIndicator(R.drawable.ic_baseline_keyboard_arrow_left_24);
//        actioBar.setDisplayHomeAsUpEnabled(true);
        logRecycleView = findViewById(R.id.log_list);
        logBottomNavigationView = findViewById(R.id.log_bottomNavigationView);
        startDate = findViewById(R.id.txt_start_date);
        endDate = findViewById(R.id.txt_end_date);
        selectBtn = findViewById(R.id.btn_filter_logs);
        startBtn = findViewById(R.id.btn_start_date);
        endBtn = findViewById(R.id.btn_end_date);

        no_data_found = findViewById(R.id.txt_no_data_found);
        logBottomNavigationView.setOnNavigationItemSelectedListener(LogActivity.this);
        logBottomNavigationView.setSelectedItemId(R.id.logs);

        logRecycleView.setLayoutManager(new LinearLayoutManager(this));

        getManualTasks();

        //Selected start date
        startDate.setInputType(InputType.TYPE_NULL);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startDateCalendar();
            }
        });

        // Selected end date
        endDate.setInputType(InputType.TYPE_NULL);
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               endDateCalendar();
            }
        });
    }

    //Formatting the dates.
//    Start date calendar
    private void startDateCalendar(){
        Calendar c=Calendar.getInstance();
        int month=c.get(Calendar.MONTH);
        int day=c.get(Calendar.DAY_OF_MONTH);
        int year=c.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog =new DatePickerDialog(LogActivity.this, new DatePickerDialog.OnDateSetListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                c.set(year,month,dayOfMonth);
                int get_month = month+1;
                if(dayOfMonth < 10){
                    String day = "0"+dayOfMonth;
                    Log.d(TAG,"day:::"+day);
                    if(get_month < 10){
                        startDate.setText(day+"-"+"0"+get_month+"-"+year);
                        startDate.setTextColor(getResources().getColor(R.color.black));
                    }else {
                        startDate.setText(day+"-"+get_month+"-"+year);
                        startDate.setTextColor(getResources().getColor(R.color.black));
                    }
                }else {
                    if(get_month < 10){
                        startDate.setText(dayOfMonth+"-"+"0"+get_month+"-"+year);
                        startDate.setTextColor(getResources().getColor(R.color.black));
                    }else {
                        startDate.setText(dayOfMonth+"-"+get_month+"-"+year);
                        startDate.setTextColor(getResources().getColor(R.color.black));
                    }
                }
            }
        },year,month,day);
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()-1000);
        datePickerDialog.show();
}

//End date calendar
private  void endDateCalendar() {
    Calendar c=Calendar.getInstance();
    int month=c.get(Calendar.MONTH);
    int day=c.get(Calendar.DAY_OF_MONTH);
    int year=c.get(Calendar.YEAR);
    DatePickerDialog datePickerDialog =new DatePickerDialog(LogActivity.this, new DatePickerDialog.OnDateSetListener() {
        @SuppressLint("SetTextI18n")
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            c.set(year,month,dayOfMonth);
            int get_month = month+1;
            if(dayOfMonth < 10){
                String day = "0"+dayOfMonth;
                Log.d(TAG,"day:::"+day);
                if(get_month < 10){
                    endDate.setText(day+"-"+"0"+get_month+"-"+year);
                    endDate.setTextColor(getResources().getColor(R.color.black));
                }else {
                    endDate.setText(day+"-"+get_month+"-"+year);
                    endDate.setTextColor(getResources().getColor(R.color.black));
                }
            }else {
                if(get_month < 10){
                    endDate.setText(dayOfMonth+"-"+"0"+get_month+"-"+year);
                    endDate.setTextColor(getResources().getColor(R.color.black));
                }else {
                    endDate.setText(dayOfMonth+"-"+get_month+"-"+year);
                    endDate.setTextColor(getResources().getColor(R.color.black));
                }
            }
        }
    },year,month,day);
    datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis()-1000);
    datePickerDialog.show();
}

//bottom bar item
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.home:
                startActivity(new Intent(LogActivity.this, HomePage.class));
                break;
            case R.id.profile:
                break;
            case R.id.logs:
                break;
        }
        return true;
    }

    //To retrieve data from Room DB.
    private void getManualTasks() {
        @SuppressLint("StaticFieldLeak")
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

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<BloodPressureDB> tasks) {
                super.onPostExecute(tasks);

                @SuppressLint("SimpleDateFormat") DateFormat df1 = new SimpleDateFormat("dd-MM-yyyy"); // Format date
                // Display all values in recycler view.
                if (tasks.size() == 0) {
                    no_data_found.setVisibility(View.VISIBLE);
                    logRecycleView.setVisibility(View.INVISIBLE);
                }
                else {

                    //Sort the arraylist in ascending order
                    Collections.sort(tasks, new Comparator<BloodPressureDB>() {
                        @Override
                        public int compare(BloodPressureDB bloodPressureDB, BloodPressureDB t1) {
                            try {
                                if (Objects.requireNonNull(df1.parse(bloodPressureDB.getDate())).before(df1.parse(t1.getDate()))){
                                    return -1;
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            return 1;
                        }
                    });
//                    Collections.reverse(tasks);
                    no_data_found.setVisibility(View.INVISIBLE);
                    logRecycleView.setVisibility(View.VISIBLE);
                        ReadingsAdapter adapter = new ReadingsAdapter(LogActivity.this, tasks);
                        logRecycleView.setAdapter(adapter);
                        adapter.notifyDataSetChanged();

//                        To filter data with 2 dates
                    selectBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {

                            if (startDate.getText().toString().isEmpty()) {
                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.select_start_date), Toast.LENGTH_SHORT).show();
                            }
                            else if (endDate.getText().toString().isEmpty()){
                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.select_end_date), Toast.LENGTH_SHORT).show();
                            }
                            else {

                                newTask.clear();

                                //Method 1: to filter the values between 2 dates.

//                                String start_date = startDate.getText().toString().replaceAll("-","");
//                                String end_date = endDate.getText().toString().replaceAll("-","");
//                                        for (int i = 0; i < tasks.size(); i++) {
//                                            String date = tasks.get(i).getDate().replaceAll("-", "");
//
//                                            Log.i(TAG, "onClick: start_date " + start_date + " end_date " + end_date + " date " + date);
//
//                                    if (Integer.parseInt(date) >= Integer.parseInt(start_date) && Integer.parseInt(date) <= Integer.parseInt(end_date))
//                                            {
//                                                newTask.add(tasks.get(i));
//                                            }
//                                            Log.i(TAG, "new task size " + newTask.size());
//
////                                            if (i == tasks.size() - 1) {
//
//                                                if (newTask.size() == 0) {
//                                                    no_data_found.setVisibility(View.VISIBLE);
//                                                    logRecycleView.setVisibility(View.INVISIBLE);
//                                                } else {
//                                                    no_data_found.setVisibility(View.INVISIBLE);
//                                                    logRecycleView.setVisibility(View.VISIBLE);
//                                                    ReadingsAdapter adapter = new ReadingsAdapter(LogActivity.this, newTask);
//                                                    logRecycleView.setAdapter(adapter);
//                                                    adapter.notifyDataSetChanged();
//                                                }
////                                            }
//                                }

                                //Method 2: Filter the dates between start and end date.
                                List<Date> dates = getDates(startDate.getText().toString(), endDate.getText().toString());

                                //Compare start and end date and filter values display in recycler view.
                                for (BloodPressureDB i : tasks) {
                                    recylerDateBP.add(i);
                                    if (dates.size() > 0) {
                                        for (Date d : dates) {
//                                            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                                            String strDate = df1.format(d);
//                                            System.out.println("Converted String: " + strDate);
                                            if (i.getDate().equals(strDate)) {
                                                newTask.add(i);
                                            }

                                            if (newTask.size() == 0) {
                                                    no_data_found.setVisibility(View.VISIBLE);
                                                    logRecycleView.setVisibility(View.INVISIBLE);
                                                } else {
//                                                @SuppressLint("SimpleDateFormat") DateFormat df1 = new SimpleDateFormat("dd-MM-yyyy"); // Format date
                                                //Sort the arraylist in ascending order
                                                Collections.sort(newTask, new Comparator<BloodPressureDB>() {
                                                    @Override
                                                    public int compare(BloodPressureDB bloodPressureDB, BloodPressureDB t1) {
                                                        try {
                                                            if (Objects.requireNonNull(df1.parse(bloodPressureDB.getDate())).before(df1.parse(t1.getDate()))){
                                                                return -1;
                                                            }
                                                        } catch (ParseException e) {
                                                            e.printStackTrace();
                                                        }
                                                        return 1;
                                                    }
                                                });
//                                                Collections.reverse(newTask);
                                                    no_data_found.setVisibility(View.INVISIBLE);
                                                    logRecycleView.setVisibility(View.VISIBLE);
                                                    ReadingsAdapter adapter = new ReadingsAdapter(LogActivity.this, newTask);
                                                    logRecycleView.setAdapter(adapter);
                                                    adapter.notifyDataSetChanged();
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            }
        }
        GetTasks gt = new GetTasks();
        gt.execute();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        bottomNavigationView.setOnNavigationItemSelectedListener(HomePage.this);
        logBottomNavigationView.setSelectedItemId(R.id.home);
    }

    //Filter dates between 2 dates.
    private static List<Date> getDates(String dateString1, String dateString2)
    {
        ArrayList<Date> dates = new ArrayList<Date>();
        @SuppressLint("SimpleDateFormat") DateFormat df1 = new SimpleDateFormat("dd-MM-yyyy");

        Date date1 = null;
        Date date2 = null;

        try {
            date1 = df1 .parse(dateString1);
            date2 = df1 .parse(dateString2);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(Objects.requireNonNull(date1));


        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(Objects.requireNonNull(date2));

        while(!cal1.after(cal2))
        {
            dates.add(cal1.getTime());
            cal1.add(Calendar.DATE, 1);
        }
        return dates;
    }

    //Adapter to display data in recycler view.
    public class ReadingsAdapter extends RecyclerView.Adapter<ReadingsAdapter.ReadingViewHolder> {
        private final Context mCtx;
        private final List<BloodPressureDB> readingList;

        public ReadingsAdapter(Context mCtx, List<BloodPressureDB> taskList) {
            this.mCtx = mCtx;
            this.readingList = taskList;
        }

        @NonNull
        @Override
        public ReadingsAdapter.ReadingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mCtx).inflate(R.layout.recyclerview_tasks, parent, false);
            return new ReadingsAdapter.ReadingViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ReadingViewHolder holder, int position) {
            BloodPressureDB t = readingList.get(position);

            String date = t.getDate();
            String[] showDate = date.split("-");

            // Changing the date format
            if (showDate[1].equalsIgnoreCase("01")) {
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.jan));
            }else if(showDate[1].equalsIgnoreCase("02")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.feb));
            }else if(showDate[1].equalsIgnoreCase("03")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.mar));
            }else if(showDate[1].equalsIgnoreCase("04")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.apr));
            }else if(showDate[1].equalsIgnoreCase("05")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.may));
            }else if(showDate[1].equalsIgnoreCase("06")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.jun));
            }else if(showDate[1].equalsIgnoreCase("07")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.jly));
            }else if(showDate[1].equalsIgnoreCase("08")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.aug));
            }else if(showDate[1].equalsIgnoreCase("09")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.sep));
            }else if(showDate[1].equalsIgnoreCase("10")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.oct));
            }else if(showDate[1].equalsIgnoreCase("11")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.nov));
            }else if(showDate[1].equalsIgnoreCase("12")){
                holder.textViewDate.setText(showDate[0]+"-"+getString(R.string.dec));
            }
            holder.textViewTime.setText(t.getTime());
            holder.textViewSysta.setText(String.valueOf(t.getSystolic()));
            holder.textViewDiasta.setText(String.valueOf(t.getDystolic()));
            holder.textViewRate.setText(String.valueOf(t.getHeartRate()));
        }

        @Override
        public int getItemCount() {
            return readingList.size();
        }
        public class ReadingViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

//            Button textViewDate, textViewTime, textViewSysta, textViewDiasta, textViewRate, textViewRange;
            TextView textViewDate, textViewTime, textViewSysta, textViewDiasta, textViewRate, textViewRange;

            public ReadingViewHolder(View itemView) {
                super(itemView);

                textViewDate = itemView.findViewById(R.id.date);
                textViewTime = itemView.findViewById(R.id.time1);
                textViewSysta = itemView.findViewById(R.id.systalic);
                textViewDiasta = itemView.findViewById(R.id.dystalic);
                textViewRate = itemView.findViewById(R.id.heartRate);

                itemView.setOnClickListener(this);
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                BloodPressureDB task = readingList.get(getAdapterPosition());

                // Share dialog to share the data through different apps/media.
                Dialog shareDialog = new Dialog(mCtx);
                shareDialog.setContentView(R.layout.share_readings);
                shareDialog.setCancelable(false);
                TextView systolicText = shareDialog.findViewById(R.id.reading_sys_txt);
                TextView diastolicText = shareDialog.findViewById(R.id.reading_dia_txt);
                TextView heartRateText = shareDialog.findViewById(R.id.reading_rate_txt);
                TextView statusText = shareDialog.findViewById(R.id.reading_status_txt);
                TextView dateText = shareDialog.findViewById(R.id.reading_date_txt);
                Button btnShare = shareDialog.findViewById(R.id.btn_share_reading);
                Button btnCancel = shareDialog.findViewById(R.id.btn_share_reading_cancel);
                systolicText.setText(task.getSystolic() + " mmHg");
                diastolicText.setText(task.getDystolic() + " mmHg");
                heartRateText.setText(task.getHeartRate() + " bpm");
                String status = changeStatus(task.getSystolic(),task.getDystolic());
                statusText.setText(status);
                dateText.setText(task.getDate());
                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        shareDialog.dismiss();
                    }
                });
                btnShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        shareDialog.dismiss();
                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        String shareBody = getString(R.string.logName) +"            " + task.getName()+"\r \n"+
                                getString(R.string.log_date)+ "              " + dateText.getText().toString()+"\r \n"+
                                getString(R.string.log_time)+ "              " + task.getTime()+"\r \n"+
                                getString(R.string.systolic_log) + "         " + systolicText.getText().toString() + "\r \n" +
                                getString(R.string.diastolic_log) +"        " + diastolicText.getText().toString() + "\r \n" +
                                getString(R.string.heartrate_log) +"     " + heartRateText.getText().toString() +"\r \n"+
                                getString(R.string.status_1)+"             " + statusText.getText().toString();
                        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.bp_for)+ "   " + task.getName() +"\n");
                        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)));
                    }
                });
                shareDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                Window window = shareDialog.getWindow();
                WindowManager.LayoutParams wlp = window.getAttributes();
                wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
                wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                wlp.gravity = Gravity.CENTER;
                shareDialog.create();
                shareDialog.show();
            }

            // Status according to sys/dia ranges.
            private String changeStatus(int systolic, int diastolic) {
                String msg = null;
                if ((systolic < 80) || (diastolic < 60)) {
                    msg = getString(R.string.low_bp);
                }
                else if ((systolic > 80 && systolic < 120) || (diastolic > 60 && diastolic < 80)) {
                    msg = getString(R.string.normal_bp);
                }
                else if ((systolic > 120 && systolic < 139) || (diastolic > 80 && diastolic < 89)) {
                    msg = getString(R.string.high_normal_bp);
                }
                else if ((systolic > 139 && systolic < 159) || (diastolic > 89 && diastolic < 99)) {
                    msg = getString(R.string.hypertension_stage_1);
                }
                else if ((systolic > 159 && systolic < 179) || (diastolic > 99 && diastolic < 109)) {
                    msg = getString(R.string.hypertension_stage_2);
                }
                else if ((systolic > 179 && systolic < 189) || (diastolic > 109 && diastolic < 119)) {
                    msg = getString(R.string.hypertension_stage_3);
                }
                else {
                    msg = getString(R.string.high_bp_crisis);
                }
                return msg;
            }
        }
    }
}