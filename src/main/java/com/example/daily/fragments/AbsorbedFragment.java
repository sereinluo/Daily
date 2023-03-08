package com.example.daily.fragments;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.daily.R;
import com.example.daily.service.MyService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AbsorbedFragment extends Fragment {

    private static final String TAG = AbsorbedFragment.class.getName();
    private View view;
    private MyService.LocalBinder localBinder;
    private ImageView resumePauseTimeImage;
    private TextView timeText;
    private MySQLiteOpenHelper mySQLiteOpenHelper;
    private SQLiteDatabase writeDatabase;
    private SQLiteDatabase readDatabase;
    private List<TimeText> timeTextList = new ArrayList<>();;
    private TimeAdapter timeAdapter;
    private ListView timeListView;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.absorbed, container, false);
        timeText = (TextView) view.findViewById(R.id.timeText);

        mySQLiteOpenHelper = new MySQLiteOpenHelper(getActivity());
        writeDatabase = mySQLiteOpenHelper.getWritableDatabase();
        readDatabase = mySQLiteOpenHelper.getReadableDatabase();

        timeListView = (ListView) view.findViewById(R.id.timeListView);
        timeAdapter = new TimeAdapter();
        timeListView.setAdapter(timeAdapter);


        //绑定计时服务
        BindTimeService();

        //注册接收秒数的本地广播
        RegisterSecondLocalBroadcast();

        // 暂停取消按钮
        SetResumePauseTimeButton();
        //取消计时按钮
        SetCancelTimeButton();

        SetCompleteTimeButton();

        ReadTimeTextDatabase();

        /*** onCreateView End ***/
        return view;
    }

    public void BindTimeService(){
        Intent intent = new Intent(getActivity(), MyService.class);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                localBinder = (MyService.LocalBinder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "onServiceDisconnected: ");
            }
        };
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);

    }

    public void RegisterSecondLocalBroadcast(){
        //注册接收计时秒数的本地广播
        IntentFilter timeIntentFilter = new IntentFilter();
        timeIntentFilter.addAction("SECONDS_CHANGED");
        BroadcastReceiver timeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int second = localBinder.getService().getSecond();
                ShowTimeSecond(second);
            }
        };
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(timeBroadcastReceiver, timeIntentFilter);
    }

    public void SetResumePauseTimeButton(){
        resumePauseTimeImage = (ImageView) view.findViewById(R.id.resumeTimeImage);
        resumePauseTimeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //正在计时,此时为暂停按钮样式
                if(localBinder.getService().isRunning()){
                    localBinder.getService().PauseTime();   //暂停计时
                    resumePauseTimeImage.setImageResource(R.drawable.resume_time);
                }else{
                    if(localBinder.getService().getTimeThread() == null){
                        Intent intent = new Intent();
                        intent.setClass(getActivity(), MyService.class);
                        getActivity().startService(intent);
                        resumePauseTimeImage.setImageResource(R.drawable.pause_time);
                    }else {
                        localBinder.getService().ResumeTime();
                        resumePauseTimeImage.setImageResource(R.drawable.pause_time);
                    }
                }
            }
        });

    }

    public void SetCancelTimeButton(){
        ImageView cancelTimeImage = (ImageView) view.findViewById(R.id.cancelTimeImage);
        cancelTimeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localBinder.getService().CancelTime();
                ShowTimeSecond(0);
                resumePauseTimeImage.setImageResource(R.drawable.resume_time);
            }
        });
    }

    public void SetCompleteTimeButton(){
        ImageView completeImage = (ImageView) view.findViewById(R.id.completeTimeImage);
        completeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InsertTimeTextDatabase(timeText.getText().toString());

                //结束当前计时
                localBinder.getService().CancelTime();
                ShowTimeSecond(0);
                resumePauseTimeImage.setImageResource(R.drawable.resume_time);
            }
        });
    }

    public void ShowTimeSecond(int second){
        TextView secondText = (TextView) view.findViewById(R.id.timeText);
        secondText.setText(handleSecondToString(second));
    }

    public String handleSecondToString(int second){
        int hour = second / 3600;
        int min = (second - (hour*3600)) / 60;
        int sec = second % 60;

        String hh = String.valueOf(hour);
        if(hour<10){
            hh = "0" + hh;
        }

        String mm = String.valueOf(min);
        if(min<10){
            mm = "0" + mm;
        }

        String ss = String.valueOf(sec);
        if(sec<10){
            ss = "0" + ss;
        }

        return hh+" : "+mm+" : "+ss;
    }


    public class TimeText {
        private int id;
        private String text;
        private String date;

        public TimeText(int id, String text, String date){
            this.id = id;
            this.text = text;
            this.date = date;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
    //数据库管理类
    public class MySQLiteOpenHelper extends SQLiteOpenHelper{

        public MySQLiteOpenHelper(@Nullable Context context) {
            super(context, "time.db", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String create_sql =
                    "create table timeTable(id INTEGER PRIMARY KEY AUTOINCREMENT, text varchar(50), date varchar(50));";
            db.execSQL(create_sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    public void ReadTimeTextDatabase(){
        timeTextList.clear();

        Cursor cursor = readDatabase.query("timeTable", new String[]{"id", "text","date"}, null, null, null, null, null);
        while(cursor.moveToNext()){
            TimeText time = new TimeText(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2)
            );
            timeTextList.add(time);
            timeAdapter.notifyDataSetChanged();
        }
    }
    public void InsertTimeTextDatabase(String text){
        ContentValues contentValues = new ContentValues();
        contentValues.put("text", text);
        contentValues.put("date", GetTimeNowString());
        writeDatabase.insert("timeTable", null, contentValues);

        ReadTimeTextDatabase();
    }
    public void DeleteTimeTextDatabse(TimeText timeText){
        writeDatabase.delete("timeTable", "id=?", new String[]{""+timeText.getId()});
        ReadTimeTextDatabase();
    }

    public class TimeAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return timeTextList.size();
        }

        @Override
        public Object getItem(int position) {
            return timeTextList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TimeText time = (TimeText) getItem(position);
            ViewHolder viewHolder;
            if(convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.time_item, null);
                viewHolder = new ViewHolder();
                viewHolder.text = (TextView) convertView.findViewById(R.id.itemTimeText);
                viewHolder.date = (TextView) convertView.findViewById(R.id.itemTimeDate);
                viewHolder.deleteImage = (ImageView) convertView.findViewById(R.id.deleteImage);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.text.setText(time.getId()+"    "+time.getText());
            viewHolder.date.setText(time.getDate());
            viewHolder.deleteImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeleteTimeTextDatabse(time);
                }
            });

            return convertView;
        }
    }

    class ViewHolder{
        TextView text;
        TextView date;
        ImageView deleteImage;
    }

    public String GetTimeNowString(){
        Calendar calendar = Calendar.getInstance();

        Integer year = calendar.get(Calendar.YEAR);
        Integer month = calendar.get(Calendar.MONTH) + 1;
        Integer day = calendar.get(Calendar.DAY_OF_MONTH);

        String yy = String.valueOf(year);
        String mm = String.valueOf(month);
        if(month<10){
            mm = "0"+mm;
        }
        String dd = String.valueOf(day);
        if(day<10){
            dd = "0"+dd;
        }
        return yy+"-"+mm+"-"+dd;

    }
}
