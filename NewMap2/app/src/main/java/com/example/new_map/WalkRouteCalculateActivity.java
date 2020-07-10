package com.example.new_map;

import android.content.Intent;
import android.os.Bundle;

import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.NaviLatLng;
//import com.amap.navi.demo.R;


public class WalkRouteCalculateActivity extends BaseActivity {
    private NaviLatLng start, end;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_navi);

        Intent intent = getIntent();//声明一个对象，并获得跳转过来的Intent对象
        // 从intent对象中获得数据
        double startlat = intent.getDoubleExtra("startlat", 30.620224219303456);
        double startlng = intent.getDoubleExtra("startlng", 104.0484742901689);
        double endlat = intent.getDoubleExtra("endlat", 30.6216079800);
        double endlng = intent.getDoubleExtra("endlng", 104.0499830327);
        start = new NaviLatLng(startlat, startlng);
        end = new NaviLatLng(endlat, endlng);
        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);
        mAMapNaviView.setAMapNaviViewListener(this);
        mAMapNaviView.setNaviMode(AMapNaviView.NORTH_UP_MODE);

    }

    @Override
    public void onInitNaviSuccess() {
        super.onInitNaviSuccess();
        mAMapNavi.calculateWalkRoute(start, end);
    }

    @Override
    public void onCalculateRouteSuccess(int[] ids) {
        super.onCalculateRouteSuccess(ids);
        mAMapNavi.startNavi(NaviType.GPS);
    }
}
