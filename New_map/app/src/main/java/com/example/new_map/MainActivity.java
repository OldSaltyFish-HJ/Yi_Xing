package com.example.new_map;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.navi.services.search.model.LatLonPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.autonavi.base.amap.mapcore.AMapNativeBuildingRenderer.render;


public class MainActivity extends AppCompatActivity implements AMap.OnMarkerClickListener, AMap.InfoWindowAdapter, AMap.OnMapClickListener {

    /**
     * 地图相关变量
     */
    private MapView mMapView;//地图控件
    private AMap aMap;//地图对象
    AMapLocationClient mLocationClient = null;//声明AMapLocationClient类对象
    public AMapLocationClientOption mLocationOption = null;//声明AMapLocationClientOption对象
    MyLocationStyle myLocationStyle;
    private double currentLat,currentLon;//声明当前经纬度
    LatLng currentLatLng;// 当前位置
    public UiSettings uiSettings;
    private boolean flag = true;// 设置一个flag 当 第一次进入地图 去将当前位置作为屏幕中心点

    /**
     * 地图数据
     * */
    public ArrayList<ArrayList<Pair<Double, Double>>> coordinates = new ArrayList<ArrayList<Pair<Double, Double>>>();
    ArrayList<String> commoditiesList = new ArrayList<String>();

    /**
     * 点标记相关变量
     * */
    private Marker clickMaker;//当前点击的marker
    View infoWindow = null;//自定义窗体

    /**
     * 下拉框相关变量
     */
    private Spinner spinnerButton = null;
    private Spinner spinner = null;

    /**
     * 需要绑定的组件
     * */
    private EditText searchText;
    private Button searchButton;

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
        }
        else {
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
                }
                else {
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
     * 绘制坐标标记
     */
    public void drawMarker(Double x, Double y, String title, String snippet) {
        LatLng latLng = new LatLng(x,y);
        final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(snippet));
    }

    /**
    * 数据插入，目前是假数据，实际应该从数据库中获取
    * */
    public void dataInsert() {
        for (int i = 0; i < 5; ++i) {
            coordinates.add(new ArrayList<Pair<Double, Double>>());
            for (int j = 0; j < 5; ++j) {
                double x = Math.random(), y = Math.random();
                if (x > 0.5) x -= 1;
                if (y > 0.5) y -= 1;
                coordinates.get(i).add(new Pair<>(39.906901 + x * 0.03, 116.397972 + y * 0.03));
                //System.out.println(x + " " + y);
            }
        }
        commoditiesList.add("口罩");
        commoditiesList.add("洗手液");
        commoditiesList.add("消毒液");
        commoditiesList.add("护目镜");
        commoditiesList.add("一次性手套");
    }

    /**
     * 地图展示
     */
    public void renderMap(Bundle savedInstanceState) {
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        //获取地图对象
        aMap = mMapView.getMap();uiSettings = aMap.getUiSettings();
        //设置地图属性
        setMapAttribute();
    }

    /**
     * 设置地图属性
     */
    private void setMapAttribute() {
        //设置默认缩放级别
        aMap.moveCamera(CameraUpdateFactory.zoomTo(14));
        //隐藏的右下角缩放按钮
        //uiSettings.setZoomControlsEnabled(false);
        //设置marker点击事件监听
        aMap.setOnMarkerClickListener(this);
        //设置自定义信息窗口
        aMap.setInfoWindowAdapter(this);
        //设置地图点击事件监听
        aMap.setOnMapClickListener(this);
    }

    /**
     * marker点击事件
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
        clickMaker = marker;
        //点击当前marker展示自定义窗体
        marker.showInfoWindow();
        //返回true 表示接口已响应事,无需继续传递
        return true;
    }

    /**
     * 监听自定义窗口infoWindow事件回调
     */
    @Override
    public View getInfoWindow(Marker marker) {
        if (infoWindow == null) {
            System.out.println(6666666);
            infoWindow = LayoutInflater.from(this).inflate(R.layout.amap_info_window, null);
        }
        render(marker, infoWindow);
        return infoWindow;
    }

    /**
     * 自定义infoWindow窗口
     */
    private void render(Marker marker, View infoWindow) {
        TextView title = infoWindow.findViewById(R.id.info_window_title);
        TextView content = infoWindow.findViewById(R.id.info_window_content);
        title.setText(marker.getTitle());
        content.setText(marker.getSnippet());
    }

    /**
     * 不能修改整个InfoWindow的背景和边框，返回null
     */
    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 地图点击事件
     * 点击地图区域让当前展示的窗体隐藏
     */
    @Override
    public void onMapClick(LatLng latLng) {
        //判断当前marker信息窗口是否显示
        if (clickMaker != null && clickMaker.isInfoWindowShown()) {
            clickMaker.hideInfoWindow();
        }

    }

    /**
     * 搜索按钮绑定事件
     * */
    public void searchButton(View view) {
        String searchContent = searchText.getText().toString();
        System.out.println(searchContent);
        aMap.clear(true);
        int id = Integer.parseInt(searchContent);
        for (Pair<Double, Double> pair : coordinates.get(id)) {
            drawMarker(pair.first, pair.second, commoditiesList.get(id) + ":", "余量：" + (int)(Math.random() * 100));
        }
    }

    /**
     * 判断是否需要隐藏输入框
     * */
    private boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] location = {0, 0};
            v.getLocationOnScreen(location);
            int left = location[0];
            int top = location[1];
            //Log.d(TAG, "getLocationOnScreen(): left = " + location[0] + "  top=" + location[1]);
            if (event.getX() < left || (event.getX() > left + v.getWidth())
                    || event.getY() < top || (event.getY() > top + v.getHeight())) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * 点击其他地方后隐藏输入框
     * */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideInput(v, ev)) {

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
            return super.dispatchTouchEvent(ev);
        }
        if (getWindow().superDispatchTouchEvent(ev)) {
            return true;
        }
        return onTouchEvent(ev);
    }

    /**
     * 下拉框相关设置
     * */
    /*class spinnerForCommoditiesListener implements android.widget.AdapterView.OnItemSelectedListener{
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int position, long id) {
            String selected = parent.getItemAtPosition(position).toString();
            System.out.println(id);
            aMap.clear(true);
            for (Pair<Double, Double> pair : coordinates.get((int)id)) {
                drawMarker(pair.first, pair.second, commoditiesList.get((int)id) + ":", "余量：" + (int)(Math.random() * 100));
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            System.out.println("nothingSelect");
        }
    }
    public void SpinnerSetting() {
        spinner = (Spinner) findViewById(R.id.spinner_commodities);
        ArrayAdapter spinnerForCommodities = new ArrayAdapter(this, R.layout.item, R.id.textview, commoditiesList);
        spinner.setAdapter(spinnerForCommodities);
        spinner.setOnItemSelectedListener(new spinnerForCommoditiesListener());
    }*/

    /**
     * 绑定组件
     * */
    private void componentBinding() {
        searchText = (EditText)findViewById(R.id.search_text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        componentBinding();//组件绑定
        dataInsert();//插入假数据
        renderMap(savedInstanceState);//地图展示
        //SpinnerSetting();//设置下拉框

        //aMap.getUiSettings().setZoomControlsEnabled(false);
        //uiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_CENTER);
        //getCurrentLocationLatLng();
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



