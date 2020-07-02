package com.example.newmap2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
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
import com.amap.api.location.DPoint;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.navi.services.search.model.LatLonPoint;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements AMap.OnMarkerClickListener, AMap.InfoWindowAdapter, AMap.OnMapClickListener, LocationSource, AMapLocationListener {


    /**
     * 地图相关变量
     */
    private MapView mMapView;//地图控件
    private AMap aMap;//地图对象
    private AMapLocationClient mLocationClient = null;//声明AMapLocationClient类对象
    private UiSettings uiSettings;
    private MyLocationStyle myLocationStyle;
    public AMapLocationClientOption mLocationOption = null;//声明mLocationOption对象，定位参数
    private LocationSource.OnLocationChangedListener mListener = null;//声明mListener对象，定位监听器
    private boolean isFirstLoc = true;//标识，用于判断是否只显示一次定位信息和用户重新定位

    Polygon polygon = null;

    double latitude = -1;
    double longitude = -1;

    /**
     * 地图数据
     * */
    public ArrayList<ArrayList<LatLng>> coordinates = new ArrayList<ArrayList<LatLng>>();
    //ArrayList<String> commoditiesList = new ArrayList<String>();

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

    public class NetRequest {
        public static final String host = "http://120.78.73.158/girl_hackathon/";
        public static final String coordinatesUploadUrl = host + "uploadCoordinates.php";
        public static final String getPolygonUrl = host + "getPolygons.php";
        public static final String shopPositionUploadUrl = host + "shopPositionUpload.php";
        public static final String getShopsUrl = host + "getShops.php";

        protected String responseString;

        public void afterGet() {}
        public void afterPost() {}

        /**
         * get异步请求
         * */
        public void getDataAsync(String url) {
            OkHttpClient client = new OkHttpClient();
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

                    /*tmpPoints.clear();
                    getDataAsync(getPolygonUrl);
                    drawMap(tmpPoints);*/
                }
            });

        }
    }

    /**
     * 绘制坐标标记
     */
    public void drawMarker(LatLng latLng, String title, String snippet, boolean draggable) {
        final Marker marker = aMap.addMarker(new MarkerOptions().position(latLng).title(title).snippet(snippet).draggable(draggable));
    }

    /**
     * 假数据数据插入，现在已经从数据库中获取了，所以此段没有用到
     * */
    /*public void dataInsert() {
        for (int i = 0; i < 5; ++i) {
            coordinates.add(new ArrayList<LatLng>());
            for (int j = 0; j < 10; ++j) {
                double x = Math.random(), y = Math.random();
                if (x > 0.5) x -= 1;
                if (y > 0.5) y -= 1;
                coordinates.get(i).add(new LatLng(30.620184979761223 + x * 0.005, 104.04852122882592 + y * 0.005));
                //System.out.println(x + " " + y);
            }
        }
        commoditiesList.add("口罩");
        commoditiesList.add("洗手液");
        commoditiesList.add("消毒液");
        commoditiesList.add("护目镜");
        commoditiesList.add("一次性手套");
    }*/

    /**
     * 地图展示
     */
    public void renderMap(Bundle savedInstanceState) {
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.map);
        //在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mMapView.onCreate(savedInstanceState);
        if (aMap == null) {
            //获取地图对象
            aMap = mMapView.getMap();
            //设置显示定位按钮 并且可以点击
            uiSettings = aMap.getUiSettings();
            aMap.setLocationSource(this);//设置了定位的监听
            uiSettings.setMyLocationButtonEnabled(true);// 是否显示定位按钮
            aMap.setMyLocationEnabled(true);//显示定位层并且可以触发定位,默认是flase
            setMapAttribute();//设置地图属性
        }
        //开始定位
        System.out.println("before location");
        location();
    }

    /**
     * 设置地图属性
     */
    private void setMapAttribute() {
        //设置默认缩放级别
        aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
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

    private void drawMap(Vector<LatLng>points) {
        aMap.clear();
        for (LatLng point : points) {
            drawMarker(point, "", "", false);
        }
        drawPolygon(points);
        //drawGetPoints();
    }

    private Vector<LatLng>tmpPoints = new Vector<>();

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
        //aMap.clear();

        //点击改变蓝圆标位置，在控制台打印点击位置
        System.out.println(latLng.latitude + "," + latLng.longitude);
        tmpPoints.add(latLng);
        drawMap(tmpPoints);
        MarkerOptions markerOptions = new MarkerOptions();
        //markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.placeholder));
        markerOptions.position(latLng);
        Marker marker = aMap.addMarker(markerOptions);
        //System.out.println(marker.getPosition().latitude + ", " + marker.getPosition().longitude);

        //aMap.moveCamera(CameraUpdateFactory.changeLatLng(latLng));
    }

    class GetPolgonRequest extends NetRequest {
        class Point {
            public String polygonId;
            public String latitude;
            public String longitude;
            public Point(String latitude, String longitude, String polygonId) {
                super();
                this.latitude = latitude;
                this.longitude = longitude;
                this.polygonId  = polygonId;
            }
        }
        /**
         * 用来存储get得到的解析好的多边形数组信息
         * */
        private Vector<Point> getPolygonArray = new Vector<>();
        /**
         * 用来绘制get得到的多边形
         * */
        private void drawGetPoints() {
            Vector<LatLng>tmp = new Vector<>();
            for (int i = 0; i < getPolygonArray.size(); ++i) {
                if (i != 0 && Integer.parseInt(getPolygonArray.get(i).polygonId) != Integer.parseInt(getPolygonArray.get(i - 1).polygonId)) {
                    //System.out.println(i + " : " + Integer.parseInt(getJsonArray.get(i).polygonId));
                    drawPolygon(tmp);
                    tmp.clear();
                }
                tmp.add(new LatLng(Double.parseDouble(getPolygonArray.get(i).latitude), Double.parseDouble(getPolygonArray.get(i).longitude)));
            }
            drawPolygon(tmp);
            System.out.println(tmp.size());
            System.out.println(new Gson().toJson(getPolygonArray));
            tmp.clear();
        }
        @Override
        public void afterGet() {
            getPolygonArray.clear();
            JsonParser parser = new JsonParser();
            //将JSON的String 转成一个JsonArray对象
            JsonArray jsonArray = parser.parse(responseString).getAsJsonArray();
            Gson gson = new Gson();
            ArrayList<Point> shopList = new ArrayList<>();
            //加强for循环遍历JsonArray
            for (JsonElement user : jsonArray) {
                //使用GSON，直接转成Bean对象
                Point userBean = gson.fromJson(user, Point.class);
                //System.out.println(userBean.latitude);
                getPolygonArray.add(userBean);
            }
            //mainLView.setAdapter(new UserAdapter(this, userBeanList));
            drawGetPoints();
        }
    }

    class GetShopsRequest extends NetRequest {
        class Shop {
            public String latitude;
            public String longitude;
            public String name;
            public String remain;
        }
        private Vector<Shop> getShopArray = new Vector<>();
        public void drawShopMarker() {
            for (Shop shop : getShopArray) {
                drawMarker(new LatLng(Double.parseDouble(shop.latitude), Double.parseDouble(shop.longitude)), shop.name, shop.remain, false);
            }
        }
        @Override
        public void afterGet() {
            super.afterGet();
            //Json的解析类对象
            JsonParser parser = new JsonParser();
            //将JSON的String 转成一个JsonArray对象
            JsonArray jsonArray = parser.parse(responseString).getAsJsonArray();
            Gson gson = new Gson();
            ArrayList<Shop> shopList = new ArrayList<>();
            //加强for循环遍历JsonArray
            for (JsonElement user : jsonArray) {
                //使用GSON，直接转成Bean对象
                Shop userBean = gson.fromJson(user, Shop.class);
                //System.out.println(userBean.latitude);
                getShopArray.add(userBean);
            }
            drawShopMarker();
            //mainLView.setAdapter(new UserAdapter(this, userBeanList));
        }
    }

    /**
     * 搜索按钮绑定事件
     * */
    public void searchButton(View view) {
        Log.d("kwwl","enter searchButton");
        String searchContent = searchText.getText().toString();
        System.out.println("search content == " + searchContent);
        aMap.clear(true);
        new GetShopsRequest().getDataAsync(GetShopsRequest.getShopsUrl + "?searchContent=" + searchContent);
    }

    /**
     * 判断是否需要隐藏输入法
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
     * 点击其他地方后隐藏输入法
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
     * 下拉框相关设置，太丑所以没有用到
     * 后面觉得下拉框写的太丑了就注释掉了
     * 以后需要还可以用所以没有删掉
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
        //printTime();
        searchText = (EditText)findViewById(R.id.search_text);
    }

    private void location() {
        System.out.println("location");
        //初始化定位
        mLocationClient = new AMapLocationClient(getApplicationContext());
        //设置定位回调监听
        mLocationClient.setLocationListener(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位模式为Hight_Accuracy高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        //设置是否只定位一次,默认为false
        mLocationOption.setOnceLocation(false);
        //设置是否强制刷新WIFI，默认为强制刷新
        //mLocationOption.setWifiActiveScan(true);
        //设置是否允许模拟位置,默认为false，不允许模拟位置
        mLocationOption.setMockEnable(true);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        //给定位客户端对象设置定位参数
        mLocationClient.setLocationOption(mLocationOption);
        //启动定位
        mLocationClient.startLocation();
    }

    /**
     * 测试用，输出当前时间
     * */
    private void printTime() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH)+1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        System.out.println(year+"年"+month+"月"+day+"日"+hour+":"+minute+":"+second);
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            //System.out.println("errorCode" + aMapLocation.getErrorCode());
            if (aMapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                aMapLocation.getLatitude();//获取纬度
                aMapLocation.getLongitude();//获取经度
                aMapLocation.getAccuracy();//获取精度信息
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(aMapLocation.getTime());
                df.format(date);//定位时间
                aMapLocation.getAddress();//地址，如果option中设置isNeedAddress为false，则没有此结果，网络定位结果中会有地址信息，GPS定位不返回地址信息。
                aMapLocation.getCountry();//国家信息
                aMapLocation.getProvince();//省信息
                aMapLocation.getCity();//城市信息
                aMapLocation.getDistrict();//城区信息
                aMapLocation.getStreet();//街道信息
                aMapLocation.getStreetNum();//街道门牌号信息
                aMapLocation.getCityCode();//城市编码
                aMapLocation.getAdCode();//地区编码

                // 如果不设置标志位，此时再拖动地图时，它会不断将地图移动到当前的位置
                if (isFirstLoc) {
                    //设置缩放级别
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(17));
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude())));
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(aMapLocation);
                    //添加图钉
                    // aMap.addMarker(getMarkerOptions(amapLocation));
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(aMapLocation.getCountry() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getCity() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getDistrict() + ""
                            + aMapLocation.getStreet() + ""
                            + aMapLocation.getStreetNum());
                    Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    isFirstLoc = false;
                }
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
                Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    private int pos;
    /**
     * 计算两个坐标距离的平方
     * */
    private double squareOfDistance(LatLng point1, LatLng point2) {
        return (point1.latitude - point2.latitude) * (point1.latitude - point2.latitude)
                + (point1.longitude -point2.longitude) * (point1.longitude -point1.longitude);
    }
    /**
     * 绘制多边形
     * 按顺序连接points中的点并填充
     * */
    public void drawPolygon(Vector<LatLng> points) {
        PolygonOptions polygonOptions = new PolygonOptions();// 多边形参数对象
        // 添加 多边形的每个顶点（顺序添加）
        for (LatLng point : points) {
            polygonOptions.add(point);
        }
        polygonOptions.strokeWidth(4) // 多边形的边框
                .strokeColor(Color.argb(50, 1, 1, 1)) // 边框颜色
                .fillColor(Color.argb(50, 1, 0, 0));   // 多边形的填充色
        aMap.addPolygon(polygonOptions);
        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                pos = -1;
                System.out.println(marker.getPosition().latitude + ", " + marker.getPosition().longitude);
                //判断哪个已经marker离点击的位置距离小于0.0005且最靠近点击的位置，就是我们实际要改变坐标的点
                for (int i = 0; i < tmpPoints.size(); ++i) {
                    if (Math.sqrt(squareOfDistance(marker.getPosition(), tmpPoints.get(i))) < 0.0005) {
                        if (pos == -1) pos = i;
                        else if (squareOfDistance(tmpPoints.get(i), marker.getPosition())
                                < squareOfDistance(tmpPoints.get(pos), marker.getPosition())) pos = i;
                    }
                }
                System.out.println("移除第" + pos + "个");
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (pos != -1) {
                    tmpPoints.remove(pos);
                    //拖拽结束时，创建新点
                    tmpPoints.add(pos, marker.getPosition());
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (pos != -1) {
                    tmpPoints.remove(pos);
                    tmpPoints.add(pos, marker.getPosition());
                    drawMap(tmpPoints);
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        componentBinding();//组件绑定

        renderMap(savedInstanceState);//地图展示
}

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        mListener = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
        mMapView.onDestroy();
        mLocationClient.stopLocation();//停止定位
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



