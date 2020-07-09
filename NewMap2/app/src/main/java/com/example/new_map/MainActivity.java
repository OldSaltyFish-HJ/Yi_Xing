package com.example.new_map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements AMap.OnMarkerClickListener,
        AMap.InfoWindowAdapter, AMap.OnMapClickListener, LocationSource, AMapLocationListener {

    private int isAdmin = 0;

    /**
     * 地图相关变量
     */
    private MapView mMapView;//地图控件
    private AMap aMap;//地图对象
    private AMapLocationClient mLocationClient = null;//声明AMapLocationClient类对象
    private UiSettings uiSettings;
    private MyLocationStyle myLocationStyle = new MyLocationStyle();
    public AMapLocationClientOption mLocationOption = null;//声明mLocationOption对象，定位参数
    private LocationSource.OnLocationChangedListener mListener = null;//声明mListener对象，定位监听器
    private boolean isFirstLoc = true;//标识，用于判断是否只显示一次定位信息和用户重新定位

    int prePolygonId = -1;//前一次onLoacationChange时所在区域ID
    double latitude, longitude;//当前位置经纬度
    double preLatitude = -1, preLongitude = -1;


    /**
     * 点标记相关变量
     * */
    private Marker clickMaker;//当前点击的marker
    View infoWindow = null;//自定义窗体



    /**
     * 需要绑定的组件
     * */
    private EditText searchText;
    private LinearLayout menu;
    private Button submitButton;

    /**
     * 管理员绘制新的多边形
     * */
    public class MyMarker {
        public Marker marker;
        public LatLng latLng;
        public MyMarker(Marker _marker, LatLng _latLng) {
            this.marker = _marker;
            this.latLng = _latLng;
        }
    }
    private class PolygonDrawer {
        Polygon newPolygon = null;
        public Vector<MyMarker> clickedPoints = new Vector<>();
        public void addPoint(LatLng latLng) {
            if (newPolygon != null) newPolygon.remove();
            Marker marker = drawMarker(latLng, "绘制多边形", "第" + clickedPoints.size() + "点", true);
            clickedPoints.add(new MyMarker(marker, latLng));
            PolygonOptions polygonOptions = new PolygonOptions();// 多边形参数对象
            // 添加 多边形的每个顶点（顺序添加）
            for (MyMarker point : clickedPoints) {
                polygonOptions.add(point.latLng);
            }
            polygonOptions.strokeWidth(4) // 多边形的边框
                    .strokeColor(Color.argb(50, 1, 1, 1)) // 边框颜色
                    .fillColor(Color.argb(100, 0, 0, 0));   // 多边形的填充色
            newPolygon = aMap.addPolygon(polygonOptions);
        }
        public void moveTo(int pos, LatLng latLng) {
            clickedPoints.get(pos).marker.remove();
            clickedPoints.remove(pos);
            Marker marker = drawMarker(latLng, "绘制多边形", "第" + pos + "点", true);
            clickedPoints.add(pos, new MyMarker(marker, latLng));
            PolygonOptions polygonOptions = new PolygonOptions();// 多边形参数对象
            // 添加 多边形的每个顶点（顺序添加）
            for (MyMarker point : clickedPoints) {
                polygonOptions.add(point.latLng);
            }
            polygonOptions.strokeWidth(4) // 多边形的边框
                    .strokeColor(Color.argb(50, 1, 1, 1)) // 边框颜色
                    .fillColor(Color.argb(100, 0, 0, 0));   // 多边形的填充色
            newPolygon.remove();
            newPolygon = aMap.addPolygon(polygonOptions);
        }
        public void clear() {
            newPolygon = null;
            for (MyMarker marker : clickedPoints) {
                marker.marker.remove();
            }
            clickedPoints.clear();
        }
    }

    /**
     * 管理危险区域多边形
     * */
    private class MyPolygon {
        public Polygon polygon;
        public int polygonId;
        public int color;
        MyPolygon(Polygon _polygon, int _polygonId, int _color) {
            this.polygon = _polygon;
            this.polygonId = _polygonId;
            this.color = _color;
        }
    }
    private class PolygonManager {
        private Vector<MyPolygon> myPolygonList = new Vector<>();
        private HashMap<Pair<Integer, Integer>, Polygon> myPolygonMap = new HashMap<>();
        /**
         * 绘制多边形
         * 按顺序连接points中的点并填充
         * */
        public void drawPolygon(Vector<LatLng> points, int polygonId, int colorSet) {
            if (!myPolygonMap.containsKey(Pair.create(polygonId, colorSet))) {
                PolygonOptions polygonOptions = new PolygonOptions();// 多边形参数对象
                // 添加 多边形的每个顶点（顺序添加）
                for (LatLng point : points) {
                    polygonOptions.add(point);
                }
                polygonOptions.strokeWidth(4) // 多边形的边框
                        .strokeColor(Color.argb(50, 1, 1, 1)) // 边框颜色
                        .fillColor(colorSet);   // 多边形的填充色
                Polygon polygon = aMap.addPolygon(polygonOptions);
                myPolygonMap.put(Pair.create(polygonId, colorSet), polygon);
            }
        }
        public void clear() {
            myPolygonMap.clear();
            myPolygonList.clear();
            aMap.clear();
            getShopsRequest.drawShopMarker();
            location();
        }
    }
    //用于获取多边形区域信息的类
    private class GetPolgonRequest extends NetRequest {
        public int getPolygonId = -1;
        class Polygon {
            int polygonId;
            double risk;
            Vector<LatLng> pointList;
        }
        class Result {
            public int getPolygonId;
            Vector<Polygon> arr;
        }

        /**
         * 用来存储get得到的解析好的多边形数组信息
         * */
        private Vector<Polygon> getPolygonArray = new Vector<>();
        /**
         * 用来绘制get得到的多边形
         * */
        private void drawGetPolygons() {
            Vector<Polygon>tmpGetPolygonArray = new Vector<>(getPolygonArray);
            for (int i = 0; i < tmpGetPolygonArray.size(); ++i) {
                //System.out.println("polygonId == " + tmpGetPolygonArray.get(i).polygonId);
                if (isAdmin == 0) {
                    if (tmpGetPolygonArray.get(i).risk > 0.6) {
                        polygonManager.drawPolygon(tmpGetPolygonArray.get(i).pointList,
                                tmpGetPolygonArray.get(i).polygonId,
                                Color.argb(170, 200, 0, 0));
                    } else if (tmpGetPolygonArray.get(i).risk > 0.3) {
                        polygonManager.drawPolygon(tmpGetPolygonArray.get(i).pointList,
                                tmpGetPolygonArray.get(i).polygonId,
                                Color.argb(130, 200, 0, 0));
                    }
                } else {
                    polygonManager.drawPolygon(tmpGetPolygonArray.get(i).pointList,
                            tmpGetPolygonArray.get(i).polygonId,
                            Color.argb(100, 0, 0, 0));
                }
            }
        }
        public void clearPolgon() {
            getPolygonArray.clear();
        }
        @Override
        public void afterGet() {
            getPolygonArray.clear();
            Result result = new Gson().fromJson(responseString, Result.class);
            getPolygonArray = result.arr;
            prePolygonId = result.getPolygonId;
            drawGetPolygons();
        }
    }
    //用于获取拥有指定物品的类
    private class GetShopsRequest extends NetRequest {
        class Shop {
            private String latitude;
            private String longitude;
            private String name;
            private String remain;
        }
        private Vector<Shop> getShopArray = new Vector<>();
        private Vector<Marker>markerList = new Vector<>();
        public void drawShopMarker() {
            for (Marker marker : markerList) {
                marker.remove();
            }
            markerList.clear();
            for (Shop shop : getShopArray) {
                Marker marker = drawMarker(new LatLng(Double.parseDouble(shop.latitude), Double.parseDouble(shop.longitude)),
                        shop.name, "商品余量: " + shop.remain, false);
                markerList.add(marker);
                //System.out.println(new Gson().toJson(shop));
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
            getShopArray.clear();
            for (JsonElement user : jsonArray) {
                //使用GSON，直接转成Bean对象
                Shop userBean = gson.fromJson(user, Shop.class);
                //System.out.println(userBean.latitude);
                getShopArray.add(userBean);
            }
            drawShopMarker();
        }
    }
    private class PostPolygonInfo extends NetRequest {
        @Override
        public void afterPost() {
            super.afterPost();
            System.out.println(Integer.parseInt(responseString));
            polygonManager.myPolygonList.add(new MyPolygon(polygonDrawer.newPolygon,
                                                            Integer.parseInt(responseString),
                                                            Color.argb(100, 0, 0, 0)));
            polygonDrawer.clear();
        }
    }

    private PolygonManager polygonManager = new PolygonManager();
    private PostPolygonInfo postPolygonInfo = new PostPolygonInfo();
    public GetPolgonRequest getPolgonRequest = new GetPolgonRequest();
    public GetShopsRequest getShopsRequest = new GetShopsRequest();
    public PolygonDrawer polygonDrawer = new PolygonDrawer();

    /**
     * 绘制坐标标记
     */
    public Marker drawMarker(LatLng latLng, String title, String snippet, boolean draggable) {
        return aMap.addMarker(new MarkerOptions()
                                            .position(latLng)
                                            .title(title)
                                            .snippet(snippet)
                                            .draggable(draggable));
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
            uiSettings.setZoomControlsEnabled(false);
            aMap.setMyLocationEnabled(true);//显示定位层并且可以触发定位,默认是flase
            setMapAttribute();//设置地图属性
            aMap.clear();
        }
        //开始定位
        location();
    }
    /**
     * 设置地图属性
     */
    private void setMapAttribute() {
        //设置默认缩放级别
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        //隐藏的右下角缩放按钮
        //uiSettings.setZoomControlsEnabled(false);
        //设置marker点击事件监听
        aMap.setOnMarkerClickListener(this);
        //设置自定义信息窗口
        aMap.setInfoWindowAdapter(this);
        //设置地图点击事件监听
        aMap.setOnMapClickListener(this);
        aMap.setOnMarkerDragListener(new AMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                pos = -1;
                System.out.println(marker.getPosition().latitude + ", " + marker.getPosition().longitude);
                //判断哪个已经marker离点击的位置距离小于0.0005且最靠近点击的位置，就是我们实际要改变坐标的点
                for (int i = 0; i < polygonDrawer.clickedPoints.size(); ++i) {
                    if (Distance(marker.getPosition(), polygonDrawer.clickedPoints.get(i).latLng) < 100) {
                        if (pos == -1) pos = i;
                        else if (Distance(polygonDrawer.clickedPoints.get(i).latLng, marker.getPosition())
                                < Distance(polygonDrawer.clickedPoints.get(pos).latLng, marker.getPosition())) pos = i;
                    }
                }
                System.out.println("移除第" + pos + "个");
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                if (pos != -1) {
                    polygonDrawer.moveTo(pos, marker.getPosition());
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                if (pos != -1) {
                    polygonDrawer.moveTo(pos, marker.getPosition());
                }
            }
        });
        aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {}
            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
                Log.v("test","当前地图缩放级别: " + cameraPosition.zoom);
                //System.out.println(new Gson().toJson(cameraPosition));
                if (cameraPosition.zoom > 15) {
                    getPolgonRequest.getDataAsync(NetRequest.getPolygonUrl
                            + "?prePolygonId=" + prePolygonId
                            + "&latitude=" + latitude
                            + "&longitude=" + longitude
                            + "&isAdmin=" + isAdmin);
                    prePolygonId = getPolgonRequest.getPolygonId;
                }
                else {
                    polygonManager.clear();
                }
            }
        });
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

    /*private void drawMap() {
        aMap.clear();
        location();
        getShopsRequest.drawShopMarker();
        getPolgonRequest.drawGetPolygons();
    }*/


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
        //点击改变蓝圆标位置，在控制台打印点击位置
        System.out.println(latLng.latitude + "," + latLng.longitude);
        if (isAdmin == 1) {
            polygonDrawer.addPoint(latLng);
        }

    }

    /**
     * 搜索按钮绑定事件
     * */
    public void searchButton(View view) {
        Log.d("kwwl","enter searchButton");
        String searchContent = searchText.getText().toString();
        System.out.println("search content == " + searchContent);
        getShopsRequest.getDataAsync(GetShopsRequest.getShopsUrl + "?searchContent=" + searchContent);
    }

    /**
     * 跳转按钮绑定事件
     * */
    public void navigateButton(View view) {
        Intent intent = new Intent();
        intent.setClass(MainActivity.this, WalkRouteCalculateActivity.class);
        startActivity(intent);
    }

    /**
     * 多边形提交按钮绑定事件
     * */
    public void submitButton(View view) {
        Vector<LatLng> points = new Vector<>();
        for (MyMarker point : polygonDrawer.clickedPoints) {
            points.add(point.latLng);
        }
        postPolygonInfo.postDataWithParame(NetRequest.coordinatesUploadUrl, new Gson().toJson(points));
    }

    /**
     * 取消按钮绑定事件
     * */
    public void cancelButton(View view) {
        if (clickMaker != null && clickMaker.isInfoWindowShown()) {
            clickMaker.hideInfoWindow();
        }
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

    private int rvHeight;
    public void clickIcon(View view) {
        ImageView icon = (ImageView) view;
        RecyclerView rv = findViewById(R.id.iconlists);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) rv.getLayoutParams();
        if (params.height != 0) rvHeight = params.height;
        searchText.setText(icon.getTag().toString());
        searchButton(null);
        iconflag=true;
        rv.setLayoutParams(params);
    }

    /**
         * 绑定组件
         * */
    private void componentBinding() {
        searchText = (EditText)findViewById(R.id.search_text);
        menu = findViewById(R.id.menu);
        submitButton = (Button)findViewById(R.id.submit_button);
        if (isAdmin == 1) submitButton.setVisibility(View.VISIBLE);

        initicons();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.iconlists);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        iconadapter adapter = new iconadapter(iconList);
        recyclerView.setAdapter(adapter);
        searchText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) showMenu(null);
            }
        });
    }
    boolean menuflag = false,iconflag = false;
    public void showMenu(View view) {
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) menu.getLayoutParams();
        params.height = findViewById(R.id.allsearch).getHeight()/56*(56+4+8+80+8);
        menu.setLayoutParams(params);
        menuflag=true;
    }

    /**
     * 定位设置
     * */
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
        //设置定位自带的圆圈透明
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));
        aMap.setMyLocationStyle(myLocationStyle);
        //启动定位
        mLocationClient.startLocation();
    }
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见官方定位类型表
                double tmpLatitude = latitude;
                double tmpLongitude = longitude;
                latitude = aMapLocation.getLatitude();//获取纬度
                longitude = aMapLocation.getLongitude();//获取经度
                if (isFirstLoc ||
                        !(clickMaker != null && clickMaker.isInfoWindowShown())
                        && Distance(new LatLng(latitude, longitude), new LatLng(preLatitude, preLongitude)) > 1000) {
                    System.out.println(latitude + ", " + longitude);
                    getPolgonRequest.getDataAsync(NetRequest.getPolygonUrl
                                                    + "?prePolygonId=" + prePolygonId
                                                    + "&latitude=" + latitude
                                                    + "&longitude=" + longitude
                                                    + "&isAdmin=" + isAdmin);
                    prePolygonId = getPolgonRequest.getPolygonId;
                }
                preLatitude = tmpLatitude;
                preLongitude = tmpLongitude;
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
                    aMap.moveCamera(CameraUpdateFactory.zoomTo(19));
                    //将地图移动到定位点
                    aMap.moveCamera(CameraUpdateFactory.changeLatLng(
                            new LatLng(aMapLocation.getLatitude(), aMapLocation.getLongitude())));
                    //点击定位按钮 能够将地图的中心移动到定位点
                    mListener.onLocationChanged(aMapLocation);
                    //获取定位信息
                    StringBuffer buffer = new StringBuffer();
                    buffer.append(aMapLocation.getCountry() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getCity() + ""
                            + aMapLocation.getProvince() + ""
                            + aMapLocation.getDistrict() + ""
                            + aMapLocation.getStreet() + ""
                            + aMapLocation.getStreetNum());
                    System.out.println("buffer == " + buffer.toString());
                    //Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_LONG).show();
                    isFirstLoc = false;
                }
            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
                //Toast.makeText(getApplicationContext(), "定位失败", Toast.LENGTH_LONG).show();
            }
        }
    }

    private int pos;
    /**
     * 计算两个坐标距离的平方
     * */
    private double Distance(LatLng point1, LatLng point2) {
        return AMapUtils.calculateLineDistance(point1, point2);
    }

    private List<icon> iconList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);
        System.out.println(new DebugNeeds().getCertificateSHA1Fingerprint(this));
        componentBinding();//组件绑定
        renderMap(savedInstanceState);//地图展示
    }

    private void initicons() {
        iconList.add(new icon("口罩", R.drawable.mask));
        iconList.add(new icon("手套", R.drawable.gloves));
        iconList.add(new icon("护目镜", R.drawable.goggles));
        iconList.add(new icon("消毒液", R.drawable.disinfectant));
        iconList.add(new icon("洗手液", R.drawable.liquidsoap));
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
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK) {
            if (iconflag) {
                RecyclerView rv =  findViewById(R.id.iconlists);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) rv.getLayoutParams();
                params.height = rvHeight;
                rv.setLayoutParams(params);
                searchText.setHint("");
                iconflag = false;
                return false;
            } else
            if (menuflag) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) menu.getLayoutParams();
                params.height = findViewById(R.id.allsearch).getHeight() / 56 * 64;
                menu.setLayoutParams(params);
                menuflag = false;
                return false;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
    public class icon {
        private String name;
        private int imageId;
        public icon(String name, int imageId){
            this.name = name;
            this.imageId = imageId;

        }
        public String getName() {
            return name;
        }
        public int getImageId() {
            return imageId;
        }
    }
    public class iconadapter extends  RecyclerView.Adapter<iconadapter.ViewHolder> {
        private List<icon> micon;
        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView iconimage;
            TextView iconname;
            public ViewHolder(View view)  {
                super(view);
                iconimage =(ImageView) view.findViewById(R.id.icon_image);
                iconname=(TextView) view.findViewById(R.id.icon_name);
            }
        }
        public iconadapter(List<icon> iconlist){
            micon=iconlist;
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.icon_item,parent,false);
            ViewHolder holder = new ViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position){
            icon icon = micon.get(position);
            holder.iconimage.setImageResource(icon.getImageId());
            holder.iconimage.setTag(icon.getName());
            holder.iconname.setText(icon.getName());
        }

        @Override
        public int getItemCount(){
            return micon.size();
        }
    }
}
