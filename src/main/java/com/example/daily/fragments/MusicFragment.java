package com.example.daily.fragments;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.daily.R;
import com.example.daily.service.Music;
import com.example.daily.service.MyService;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class MusicFragment extends Fragment {

    private static final String TAG = MusicFragment.class.getName();
    private View view;
    private List<Music> musicList;
    private ListView musicListView;
    private MusicAdapter musicAdapter;
    private MyService.LocalBinder localBinder;
    private TextView currentText;
    private TextView durationText;
    private SeekBar musicSeekBar;
    private TextView musicNameText;
    private ImageView musicCancelImage;
    private ImageView musicPauseResumeImage;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.music, null);
        currentText = (TextView) view.findViewById(R.id.currentText);
        durationText = (TextView) view.findViewById(R.id.durationText);
        musicSeekBar = (SeekBar) view.findViewById(R.id.musicSeekBar);
        musicNameText = (TextView) view.findViewById(R.id.musicNameText);
        musicCancelImage = (ImageView) view.findViewById(R.id.musicCancelImage);
        musicPauseResumeImage = (ImageView) view.findViewById(R.id.musicPauseResumeImage);

        // 绑定服务
        BindMyService();

        //读取本地音乐文件
        musicList = new ArrayList<>();
        ReadLocalMusicFileToList();

        // 创建适配器实例，显示音乐列表
        musicListView = (ListView) view.findViewById(R.id.musicListView);
        musicAdapter = new MusicAdapter();
        musicListView.setAdapter(musicAdapter);

        // 注册接收播放进度的本地广播
        RegisterProgressLocalBroadcast();

        //设置进度条拖动事件
        SetMusicSeekBarChangedListener();

        // 设置暂停和继续按钮
        SetPauseResumeImageOnClick();
        //设置取消播放按钮
        SetCancelImageOnClick();

        // *** onCreateView End *** //
        return view;
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
//            Log.i(TAG, "onRequestPermissionsResult: ");
//            ShowMusicList();
//        }
//    }

    public void BindMyService(){
        //绑定服务
        Intent intent = new Intent(getActivity(), MyService.class);
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                localBinder = (MyService.LocalBinder) service;
                Log.i(TAG, "onServiceConnected: ");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "onServiceDisconnected: ");
            }
        };
        getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public void ReadLocalMusicFileToList(){
        // 读取本地音乐文件
        File musicStorage = new File("/storage/11E9-360F/Music");
        File[] musicFiles = musicStorage.listFiles(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mp3");
            }
        });

        for(int i=0; i<musicFiles.length; i++){
            Music music = new Music();
            music.setName(musicFiles[i].getName());
            music.setFile(musicFiles[i]);
            musicList.add(music);
        }

    }

    public class MusicAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return musicList.size();
        }

        @Override
        public Object getItem(int position) {
            return musicList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            Music music = (Music) getItem(position);
            if(convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.music_item, null);
                viewHolder = new ViewHolder();
                viewHolder.musicId = (TextView) convertView.findViewById(R.id.musicIdText);
                viewHolder.musicName = (TextView) convertView.findViewById(R.id.musicNameText);
                convertView.setTag(viewHolder);
            }else{
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.musicId.setText(position+"");
            viewHolder.musicName.setText(handleMusicListName(music.getName()));

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    localBinder.getService().servicePlayMusic(musicList, position);
                }
            });

            return convertView;
        }
    }
    class ViewHolder {
        TextView musicId;
        TextView musicName;
    }

    public void RegisterProgressLocalBroadcast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("PROGRESS");
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int duration = intent.getIntExtra("duration", 0);
                int current  = intent.getIntExtra("current", 0);
                String musicName = intent.getStringExtra("name");
                Log.i(TAG, "onReceive: "+duration+"  "+current);
                ShowMusicProgress(duration, current, musicName);
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void ShowMusicProgress(int duration, int current, String musicName){
        currentText.setText(handleMusicTime(current));
        durationText.setText(handleMusicTime(duration));

        musicSeekBar.setMax(duration);
        musicSeekBar.setProgress(current);

        musicNameText.setText(handleMusicName(musicName));
    }
    public String handleMusicTime(int ms){
        int min = (ms/1000) / 60;
        int sec = (ms/1000) % 60;
        String mm = String.valueOf(min);
        String ss = String.valueOf(sec);
        if(min<10){
            mm = "0"+mm;
        }
        if(sec<10){
            ss = "0"+ss;
        }
        return mm+":"+ss;
    }

    public String handleMusicName(String name){
        String res = name.replace(".mp3", "");
        if(res.length() > 12){
            res = res.substring(0,12) + "...";
        }
        return res;
    }
    public String handleMusicListName(String name){
        String res = name.replace(".mp3", "");
        if(res.length() > 18){
            res = res.substring(0,18) + "...";
        }
        return res;
    }

    public void SetMusicSeekBarChangedListener(){
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                localBinder.getService().setMediaPlayerProgress(seekBar.getProgress());
            }
        });
    }

    public void SetPauseResumeImageOnClick(){
        musicPauseResumeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(localBinder.getService().musicIsPlaying()){
                    localBinder.getService().servicePauseMusic();
                    musicPauseResumeImage.setImageResource(R.drawable.resume_time);
                }else{
                    localBinder.getService().serviceResumeMusic();
                    musicPauseResumeImage.setImageResource(R.drawable.pause_time);
                }
            }
        });
    }
    public void SetCancelImageOnClick(){
        musicCancelImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                localBinder.getService().serviceCancelMusic();
            }
        });
    }

}
