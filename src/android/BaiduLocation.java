/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package cn.com.youlove.cordova.geo;

import android.Manifest;
import android.content.pm.PackageManager;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百度定位
 *
 * @author carrot on 2016/12/7.
 * @version 1.0.0
 */
public class BaiduLocation extends CordovaPlugin {

  /**
   * 日志标签
   */
  private String TAG = BaiduLocation.class.getSimpleName();

  /**
   * 百度手机状态
   */
  private static final int BAIDU_READ_PHONE_STATE = 100;

  /**
   * 回调上下文
   */
  private CallbackContext context;

  /**
   * 百度定位客户端
   */
  public LocationClient mLocationClient = null;

  /**
   * 需要的权限
   */
  private String[] permissions = {
    // 获取手机状态
    Manifest.permission.READ_PHONE_STATE,
    // 获取位置信息
    Manifest.permission.ACCESS_COARSE_LOCATION,
    Manifest.permission.ACCESS_FINE_LOCATION,
    // 读写SD卡
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
  };

  /**
   * 获取定位
   *
   * @param action          The action to execute.
   * @param args            The exec() arguments.
   * @param callbackContext The callback context used when calling back into JavaScript.
   * @return
   * @throws JSONException
   */
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    context = callbackContext;
    if (action.equals("getCurrentPosition")) {
      if (hasPermisssion()) {
        //请求定位
        requestLocation();
        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        context.sendPluginResult(r);
        return true;
      } else {
        PermissionHelper.requestPermissions(this, BAIDU_READ_PHONE_STATE, permissions);
      }
      return true;
    }
    return false;
  }

  /**
   * 权限请求回调
   *
   * @param requestCode
   * @param permissions
   * @param grantResults
   * @throws JSONException
   */
  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    PluginResult result;
    for (int i = 0; i < grantResults.length; i++) {
      final int r = grantResults[i];
      final String permission = permissions[i];
      if (r == PackageManager.PERMISSION_DENIED) {
        LOG.d(TAG, "Permission Denied!");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("permission", permission);
        jsonObject.put("message", "获取权限失败。");
        result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION, jsonObject);
        context.sendPluginResult(result);
        return;
      }
    }
    //请求定位
    requestLocation();
    result = new PluginResult(PluginResult.Status.OK);
    context.sendPluginResult(result);
  }

  /**
   * 判断是否具有权限
   *
   * @return
   */
  @Override
  public boolean hasPermisssion() {
    for (String p : permissions) {
      if (!PermissionHelper.hasPermission(this, p)) {
        return false;
      }
    }
    return true;
  }

  /**
   * 请求权限
   *
   * @param requestCode Passed to the activity to track the request
   */
  @Override
  public void requestPermissions(int requestCode) {
    PermissionHelper.requestPermissions(this, requestCode, permissions);
  }

  /**
   * 请求定位，异步返回。
   */
  private void requestLocation() {
    if (mLocationClient == null) {
      //实例化
      mLocationClient = new LocationClient(this.webView.getContext());
      //配置定位SDK参数
      initBaiduLocation(new HashMap<String, Object>());
      //注册监听器
      mLocationClient.registerLocationListener(new BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
          try {
            final JSONObject jsonObject = new JSONObject();
            //server返回的当前定位时间
            jsonObject.put("time", location.getTime());
            //获取定位类型: 参考 定位结果描述 相关的字段
            jsonObject.put("locType", location.getLocType());
            //获取纬度坐标
            jsonObject.put("latitude", location.getLatitude());
            //获取经度坐标
            jsonObject.put("lontitude", location.getLongitude());
            //获取定位精度,默认值0.0f
            jsonObject.put("radius", location.getRadius());

            switch (location.getLocType()) {
              //GPS定位结果
              case BDLocation.TypeGpsLocation:
                //单位：公里每小时
                jsonObject.put("speed", location.getSpeed());
                //GPS定位结果时，获取GPS锁定用的卫星数
                jsonObject.put("satellite", location.getSatelliteNumber());
                //获取高度信息，目前只有是GPS定位结果时或者设置LocationClientOption.setIsNeedAltitude(true)时才有效，单位米
                jsonObject.put("height", location.getAltitude());
                //GPS定位结果时，行进的方向，单位度
                jsonObject.put("direction", location.getDirection());
                //获取详细地址信息
                jsonObject.put("addr", location.getAddrStr());
                jsonObject.put("describe", "gps定位成功");
                break;
              //网络定位结果
              case BDLocation.TypeNetWorkLocation:
                //获取详细地址信息
                jsonObject.put("addr", location.getAddrStr());
                //获取运营商信息
                jsonObject.put("operationers", location.getOperators());
                jsonObject.put("describe", "网络定位成功");
                break;
              //离线定位结果
              case BDLocation.TypeOffLineLocation:
                jsonObject.put("describe", "离线定位成功，离线定位结果也是有效的");
                break;
              //服务端网络定位失败
              case BDLocation.TypeServerError:
                jsonObject.put("describe", "服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
                break;
              //网络不通导致定位失败
              case BDLocation.TypeNetWorkException:
                jsonObject.put("describe", "网络不通导致定位失败，请检查网络是否通畅");
                break;
              //无法获取有效定位依据导致定位失败
              case BDLocation.TypeCriteriaException:
                jsonObject.put("describe", "无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
                break;
            }

            //获取位置语义化信息，没有的话返回NULL
            jsonObject.put("locationDescribe", location.getLocationDescribe());

            // POI数据
            final List<Poi> poiList = location.getPoiList();
            if (poiList != null) {
              final JSONArray jsonPoiList = new JSONArray();
              for (final Poi poi : poiList) {
                final JSONObject jsonPoi = new JSONObject();
                jsonPoi.put("id", poi.getId());
                jsonPoi.put("name", poi.getName());
                jsonPoi.put("rank", poi.getRank());
                jsonPoiList.put(jsonPoi);
              }
              jsonObject.put("poiList", jsonPoiList);
            }

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
            context.sendPluginResult(pluginResult);
          } catch (JSONException e) {
            String errMsg = e.getMessage();
            LOG.e(TAG, errMsg, e);

            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, errMsg);
            context.sendPluginResult(pluginResult);
          } finally {
            mLocationClient.stop();
          }
        }
      });
    }
    //启动定位
    mLocationClient.start();
  }

  /**
   * 初始化定位定位SDK参数
   */
  private void initBaiduLocation(Map<String, Object> locationOption) {

    final LocationClientOption option = new LocationClientOption();
    //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
    option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
    //可选，默认gcj02，设置返回的定位结果坐标系
    option.setCoorType("bd09ll");
    //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
    option.setScanSpan(0);
    //可选，设置是否需要地址信息，默认不需要
    option.setIsNeedAddress(true);
    //可选，默认false,设置是否使用gps
    option.setOpenGps(true);
    //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
    option.setLocationNotify(false);
    //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
    option.setIsNeedLocationDescribe(false);
    //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
    option.setIsNeedLocationPoiList(false);
    //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
    option.setIgnoreKillProcess(false);
    //可选，默认false，设置是否收集CRASH信息，默认收集
    option.SetIgnoreCacheException(false);
    //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
    option.setEnableSimulateGps(false);

    //client相关的参数设定
    mLocationClient.setLocOption(option);
  }

}
