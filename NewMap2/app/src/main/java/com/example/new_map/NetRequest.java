package com.example.new_map;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetRequest {
    public static final String host = "http://120.78.73.158/girl_hackathon/";
    public static final String coordinatesUploadUrl = host + "uploadCoordinates.php";
    public static final String getPolygonUrl = host + "getPolygons.php";
    public static final String shopPositionUploadUrl = host + "shopPositionUpload.php";
    public static final String getShopsUrl = host + "getShops.php";
    public static final String getPolygonIdUrl = host + "getPolygonId.php";

    protected String responseString;

    public void afterGet() {}
    public void afterPost() {}

    /**
     * get异步请求
     * */
    public void getDataAsync(String url) {
        OkHttpClient client = new OkHttpClient()/*.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()*/;
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println(e);
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){//回调的方法执行在子线程。
                    responseString = response.body().string();
                    Log.d("kwwl","获取数据成功了");
                    Log.d("kwwl","response.code()=="+response.code());
                    Log.d("kwwl","response.body().string()==" + responseString);
                    System.out.println("connect success");
                    afterGet();
                }
            }
        });
    }

    /**
     * post异步请求
     * */
    public void postDataWithParame(String url, String json) {
        System.out.println(url);
        System.out.println(json);
        OkHttpClient okHttpClient  = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10,TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        //MediaType  设置Content-Type 标头中包含的媒体类型值
        RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8")
                , json);

        Request request = new Request.Builder()
                .url(url)//请求的url
                .post(requestBody)
                .build();

        //创建/Call
        Call call = okHttpClient.newCall(request);
        //加入队列 异步操作
        call.enqueue(new Callback() {
            //请求错误回调方法
            @Override
            public void onFailure(Call call, IOException e) {
                System.out.println("连接失败");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                System.out.println("response print == " + response.body().string());
                Log.d("kwwl","获取数据成功了");
                Log.d("kwwl","response.code()=="+response.code());
                Log.d("kwwl","response.body().string()=="+response.body().string());
                afterPost();
            }
        });

    }
}
