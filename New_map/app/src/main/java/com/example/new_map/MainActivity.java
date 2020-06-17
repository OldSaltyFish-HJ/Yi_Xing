package com.example.new_map;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.navi.services.search.model.LatLonPoint;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //地图相关变量
    private MapView mMapView;//地图控件
    private AMap aMap;//地图对象
    //声明AMapLocationClient类对象
    AMapLocationClient mLocationClient = null;
    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    MyLocationStyle myLocationStyle;
    //声明当前经纬度
    private double currentLat,currentLon;
    // 当前位置
    LatLng currentLatLng;
    public UiSettings mUiSettings;
    // 设置一个flag 当 第一次进入地图 去将当前位置作为屏幕中心点
    private boolean flag=true;


    //下拉框相关变量
    private Spinner spinnerButton = null;
    private Spinner spinner = null;

    public ArrayList<ArrayList<Pair<Double, Double>>> coordinates = new ArrayList<ArrayList<Pair<Double, Double>>>();
    ArrayList<String> list = new ArrayList<String>();

    private boolean checkGps(){
        if (!IsGpsWork.isGpsEnabled(this)){
            Toast toast = Toast.makeText(this, getString(R.string.hasNotOpenGps), Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return false;
        }else {
            return true;
        }
    }

    /**
     *  初始化定位并设置定位回调监听
     */
    private void getCurrentLocationLatLng(){
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(mLocationListener);
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();

        /* //设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景） 设置了场景就不用配置定位模式等
        option.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
        if(null != locationClient){
            locationClient.setLocationOption(option);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            locationClient.stopLocation();
            locationClient.startLocation();
        }*/
        // 同时使用网络定位和GPS定位,优先返回最高精度的定位结果,以及对应的地址描述信息
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //只会使用网络定位
        /* mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Battery_Saving);*/
        //只使用GPS进行定位
        /*mLocationOption.setLocationMode(AMapLocationMode.Device_Sensors);*/
        // 设置为单次定位 默认为false
        /*mLocationOption.setOnceLocation(true);*/
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。默认连续定位 切最低时间间隔为1000ms
        mLocationOption.setInterval(3500);
        //设置是否返回地址信息（默认返回地址信息）
        /*mLocationOption.setNeedAddress(true);*/
        //关闭缓存机制 默认开启 ，在高精度模式和低功耗模式下进行的网络定位结果均会生成本地缓存,不区分单次定位还是连续定位。GPS定位结果不会被缓存。
        /*mLocationOption.setLocationCacheEnable(false);*/
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    /**
     * 定位回调监听器
     */
    public AMapLocationListener mLocationListener = new AMapLocationListener() {
        @Override
        public void onLocationChanged(AMapLocation amapLocation) {
            if (!IsGpsWork.isGpsEnabled(getApplicationContext())) {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.hasNotOpenGps), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            } else {
                if (amapLocation != null) {
                    if (amapLocation.getErrorCode() == 0) {
                        //定位成功回调信息，设置相关消息
                        amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                        double currentLat = amapLocation.getLatitude();//获取纬度
                        double currentLon = amapLocation.getLongitude();//获取经度
                        Object latLonPoint = new LatLonPoint(currentLat, currentLon);  // latlng形式的
                        /*currentLatLng = new LatLng(currentLat, currentLon);*/   //latlng形式的
                        Log.i("currentLocation", "currentLat : " + currentLat + " currentLon : " + currentLon);
                        amapLocation.getAccuracy();//获取精度信息
                    } else {
                        //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                        Log.e("AmapError", "location Error, ErrCode:"
                                + amapLocation.getErrorCode() + ", errInfo:"
                                + amapLocation.getErrorInfo());
                    }
                }
            }
        }
    };

    /**
     * 坐标标记
     */
    public void drawMarker(Double x, Double y, String title, String snippet) {
        LatLng latLng = new LatLng(x,y);
        final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(snippet));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for (int i = 0; i < 5; ++i) {
            coordinates.add(new ArrayList<Pair<Double, Double>>());
            for (int j = 0; j < 5; ++j) {
                double x = Math.random(), y = Math.random();
                if (x > 0.5) x -= 1;
                if (y > 0.5) y -= 1;
                coordinates.get(i).add(new Pair<>(39.906901 + x * 0.5, 116.397972 + y * 0.5));
                //System.out.println(x + " " + y);
            }
        }

        list.add("口罩");
        list.add("洗手液");
        list.add("消毒液");
        list.add("护目镜");
        list.add("一次性手套");

        /*
        * 地图展示
        */
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        //获取地图对象
        aMap = mMapView.getMap();



        getCurrentLocationLatLng();

        class spinnerForCommoditiesListener implements android.widget.AdapterView.OnItemSelectedListener{
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                System.out.println(id);
                aMap.clear(true);
                for (Pair<Double, Double> pair : coordinates.get((int)id)) {
                    drawMarker(pair.first, pair.second, list.get((int)id) + ":", "余量：" + (int)(Math.random() * 100));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("nothingSelect");
            }
        }

        spinner = (Spinner) findViewById(R.id.spinner_commodities);

        /*
         * 动态添显示下来菜单的选项，可以动态添加元素
         */

        /*
         * 第二个参数是显示的布局
         * 第三个参数是在布局显示的位置id
         * 第四个参数是将要显示的数据
         */
        ArrayAdapter spinnerForCommodities = new ArrayAdapter(this, R.layout.item, R.id.textview,list);
        spinner.setAdapter(spinnerForCommodities);
        spinner.setOnItemSelectedListener(new spinnerForCommoditiesListener());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        mLocationClient.onDestroy();//销毁定位客户端。
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView.onResume ()，重新绘制加载地图
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO Auto-generated method stub
        //在activity执行onPause时执行mMapView.onPause ()，暂停地图的绘制
        mMapView.onPause();
        //mLocationClient.stopLocation();//停止定位
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        mMapView.onSaveInstanceState(outState);
    }
}



