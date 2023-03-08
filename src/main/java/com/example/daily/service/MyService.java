package com.example.daily.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyService extends Service {

    private static final String TAG = MyService.class.getName();
    private TimeThread timeThread = null;
    private boolean isRunning = false;
    private MediaPlayer mediaPlayer;
    private MusicTimeThread musicTimeThread;
    private String musicName;

    public TimeThread getTimeThread() {
        return timeThread;
    }

    public void setTimeThread(TimeThread timeThread) {
        this.timeThread = timeThread;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    //计时秒数
    private int second = 0;

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "TimeService onCreate: ");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "TimeService onStartCommand: ");
        //创建计时线程实例
        timeThread = new TimeThread();
        timeThread.start();
        isRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "TimeService onDestroy: ");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "TimeService onUnbind: ");
        return super.onUnbind(intent);
    }

    //用于返回本地服务
    public class LocalBinder extends Binder{
        public MyService getService(){
            return MyService.this;
        }
    }

    //用于在MainActivity调用的方法
    public void PauseTime(){
        timeThread.pauseThread();
        isRunning = false;
    }
    public void ResumeTime(){
        timeThread.resumeThread();
        isRunning = true;
    }
    public void CancelTime(){
        timeThread.pauseThread();
        second = 0;
    }

    //广播发送模块
    public void SendSecondBroadcast(){
        Intent intent = new Intent();
        intent.setAction("SECONDS_CHANGED");
        intent.putExtra("second", second);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }



    public class TimeThread extends Thread{
        private final Object lock = new Object();
        private boolean pause = false;

        /**
         * 调用该方法实现线程的暂停
         */
        void pauseThread(){
            Log.i(TAG, "pauseTimeThread: ");
            pause = true;
        }
        /*
        调用该方法实现恢复线程的运行
         */
        void resumeThread(){
            Log.i(TAG, "resumeTimeThread: ");
            pause = false;
            synchronized (lock){
                lock.notify();
            }
        }

        /**
         * 这个方法只能在run 方法中实现，不然会阻塞主线程，导致页面无响应
         */
        void onPause() {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            super.run();
            try {
                while(true){
                    //当pause为true时，调用onPause挂起该线程
                    TimeUnit.SECONDS.sleep(1);

                    while(pause) {
                        onPause();
                    }
                    second++;
                    SendSecondBroadcast();
                    Log.i(TAG, "run: "+second);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }




















    /******* 音乐播放部分 *******/
    public boolean musicIsPlaying(){
        if(mediaPlayer != null){
            return mediaPlayer.isPlaying();
        }else{
            return false;
        }
    }

    public void servicePlayMusic(List<Music> musicList, int start) {
        try {
            int size = musicList.size();

            if(mediaPlayer == null){
                mediaPlayer = new MediaPlayer();
            }
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicList.get(start).getFile().getAbsolutePath());
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mediaPlayer.start();

                    musicName = musicList.get(start).getName();

                    if(musicTimeThread == null){
                        musicTimeThread = new MusicTimeThread();
                        musicTimeThread.start();
                    }else{
                        musicTimeThread.resumeThread();
                    }
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    servicePlayMusic(musicList, (start+1)%size );
                    musicTimeThread.pauseThread();
                }
            });
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void serviceSendProgressBroadcast(int duration, int current, String name){
        // 发送当前进度的本地广播
        Intent intent = new Intent();
        intent.setAction("PROGRESS");
        // 总时长 ms
        intent.putExtra("duration", mediaPlayer.getDuration());
        // 当前播放进度 ms
        intent.putExtra("current", mediaPlayer.getCurrentPosition());

        intent.putExtra("name", name);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void setMediaPlayerProgress(int current){
        Log.i(TAG, "setMediaPlayerProgress: ");
        mediaPlayer.seekTo(current);
    }

    public void servicePauseMusic(){
        if(mediaPlayer != null && mediaPlayer.isPlaying()){
            mediaPlayer.pause();
        }
    }
    public void serviceResumeMusic(){
        if(mediaPlayer!=null){
            mediaPlayer.start();
        }
    }
    public void serviceCancelMusic(){
        if(mediaPlayer!=null){
            mediaPlayer.stop();
        }
    }


    public class MusicTimeThread extends Thread{
        private final Object lock = new Object();
        private boolean pause = false;

        /**
         * 调用该方法实现线程的暂停
         */
        void pauseThread(){
            Log.i(TAG, "pauseTimeThread: ");
            pause = true;
        }
        /*
        调用该方法实现恢复线程的运行
         */
        void resumeThread(){
            Log.i(TAG, "resumeTimeThread: ");
            pause = false;
            synchronized (lock){
                lock.notify();
            }
        }

        /**
         * 这个方法只能在run 方法中实现，不然会阻塞主线程，导致页面无响应
         */
        void onPause() {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            super.run();
            try {
                while(true){
                    //当pause为true时，调用onPause挂起该线程
                    TimeUnit.SECONDS.sleep(1);
                    while(pause) {
                        onPause();
                    }
                    Log.i(TAG, "run: ");
                    serviceSendProgressBroadcast(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition(), musicName);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
