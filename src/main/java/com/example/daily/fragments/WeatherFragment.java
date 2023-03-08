package com.example.daily.fragments;

import android.animation.PropertyValuesHolder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.daily.R;
import com.example.daily.weathers.News;
import com.example.daily.weathers.RequestNewsData;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dalvik.system.DelegateLastClassLoader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherFragment extends Fragment {
    private static final String NewsUrl = "https://way.jd.com/jisuapi/get?channel=头条&num=15&start=0&appkey=0d06c61b38f391b2ff3aeaad09bf8661";
    private static final String TAG = WeatherFragment.class.getName();
    private View view;
    private List<News> newsList = new ArrayList<>();
    private NewsHandler newsHandler;
    private NewsAdapter newsAdapter;
    private ListView newsListView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.weather, null);
        newsListView = (ListView) view.findViewById(R.id.newsListView);



        newsHandler = new NewsHandler();
        newsAdapter = new NewsAdapter();
        RequestNewsDataByOkHttp();

        return view;
    }
    public void RequestNewsDataByOkHttp(){
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(NewsUrl).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.i(TAG, "onFailure: ");
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String newsJson = response.body().string();

                RequestNewsData newsData = new Gson().fromJson(newsJson, RequestNewsData.class);

                Message message = new Message();
                message.what = 0;
                message.obj = newsData.getResult().getResult().getList();
                newsHandler.sendMessage(message);
            }
        });
    }
    public class NewsHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what == 0){
                newsList = (List<News>) msg.obj;

                newsListView.setAdapter(newsAdapter);
            }
        }
    }

    public class NewsAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return newsList.size();
        }

        @Override
        public Object getItem(int position) {
            return newsList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView titleText;
            News news = (News) getItem(position);
            if(convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.news_item, null);
                titleText = (TextView) convertView.findViewById(R.id.newsTitleText);
                convertView.setTag(titleText);
            }else{
                titleText = (TextView) convertView.getTag();
            }

            titleText.setText(news.getTitle());

            return convertView;
        }
    }

}
