package com.example.new_map;

import android.widget.Spinner;

import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;

public class MaybeNeed {
    /**
     * 下拉框相关变量
     */
    private Spinner spinnerButton = null;
    private Spinner spinner = null;
    /**
     * 地图数据
     * */
    public ArrayList<ArrayList<LatLng>> coordinates = new ArrayList<ArrayList<LatLng>>();
    ArrayList<String> commoditiesList = new ArrayList<String>();
    /**
     * 假数据数据插入，现在已经从数据库中获取了，所以此段没有用到
     * */
    public void dataInsert() {
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
}
