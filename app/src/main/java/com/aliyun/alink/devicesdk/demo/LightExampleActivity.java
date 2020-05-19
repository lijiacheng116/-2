package com.aliyun.alink.devicesdk.demo;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.devicesdk.Switch;
import com.aliyun.alink.dm.model.RequestModel;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.tmp.api.MapInputParams;
import com.aliyun.alink.linksdk.tmp.api.OutputParams;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tmp.devicemodel.Service;
import com.aliyun.alink.linksdk.tmp.listener.IPublishResourceListener;
import com.aliyun.alink.linksdk.tmp.listener.ITResRequestHandler;
import com.aliyun.alink.linksdk.tmp.listener.ITResResponseCallback;
import com.aliyun.alink.linksdk.tmp.utils.ErrorInfo;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Copyright (c) 2014-2016 Alibaba Group. All rights reserved.
 * License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */





/**
 * 注意！！！！
 * 1.该示例只共快速接入使用，只适用于有 Status、Data属性的快速接入测试设备；
 * 2.真实设备可以参考 ControlPanelActivity 里面有数据上下行示例；
 */


public class LightExampleActivity extends BaseActivity {
   //上报间隔 interval秒
   static int interval = 3;
   //初始默认状态为零
   static  int stauts = 0 ;
    private class myButton1 implements View.OnClickListener{
        @Override
        public void onClick(View view){

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
//                    tools.initAliyunIoTClient();
//                    tools.getDeviceOrder();
                    button1.setEnabled(false);
                    button1.setText("已在连接状态中");
                    button4.setEnabled(true);
                    button4.setText("断开与阿里云的连接");
                }
            };
            runnable.run();


        }
    }



    private class myButton2 implements View.OnClickListener{
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View view){
            openLight();
        }
    }

    private class myButton3 implements View.OnClickListener{
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onClick(View view){
            closeLight();
        }
    }
    private class myButton4 implements View.OnClickListener{
        @Override
        public void onClick(View view){
//            try{
//                if(mqttClient.isConnected()){
//                    mqttClient.disconnect();
//                    Toast.makeText(getApplicationContext(),"已断开连接", Toast.LENGTH_SHORT).show();
//                    button1.setEnabled(true);
//                    button1.setText("连接到阿里云");
//                    button4.setEnabled(false);
//                    button4.setText("当前不可断开连接");
//                }
//            }catch (Exception e){
//                e.printStackTrace();
//                Toast.makeText(getApplicationContext(),"发生错误，断开连接失败。", Toast.LENGTH_SHORT).show();
//            }
        }
    }


    private Button button1, button2, button3, button4;
    private final static int REPORT_MSG = 0x100;

    HandlerThread mBackgroundThread;
    Handler mBackgroundHandler;

    TextView consoleTV;
    String consoleStr;
    private InternalHandler mHandler = new InternalHandler();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 在初始化的时候可以设置 灯的初始状态，或者等初始化完成之后 上报一次设备所有属性的状态
        // 注意在调用云端接口之前确保初始化完成了
        setContentView(R.layout.activity_light_example);

        consoleTV = (TextView) findViewById(R.id.textview_console);
        setDownStreamListener();
        showToast("已启动每5秒上报一次状态");
        log("已启动每5秒上报一次状态");
        mHandler.sendEmptyMessageDelayed(REPORT_MSG, 2 * 1000);



        /* 设置界面*/
        button1 = findViewById(R.id.button);
        button1.setOnClickListener(new myButton1());
        button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new myButton2());
        button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new myButton3());
        button4 = findViewById(R.id.button4);
            button4.setOnClickListener(new myButton4());
//        textView = findViewById(R.id.tv1);
        //第一次启动
//        isFirstStart = true;
        //关闭闪光灯不可用
        button3.setEnabled(false);
        button3.setText("闪光灯已关闭");
        //按钮4未连接=不可用
        button4.setEnabled(false);
        button4.setText("当前不可断开连接");

//        //授权闪光灯  安卓6以上版本需要检查权限，不能直接使用摄像头权限
//
//        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
//            button1.setEnabled(false);
//            button2.setEnabled(false);
//            button3.setEnabled(false);
//            button4.setEnabled(false);
//            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.CAMERA},requestCameraPermission);
//        }
        startBackgroundThread();
        CameraManager mCameraManager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mCameraManager.registerTorchCallback(new CameraManager.TorchCallback() {
                @Override
                public void onTorchModeUnavailable(@NonNull String cameraId) {
                    super.onTorchModeUnavailable(cameraId);

                }

                @Override
                public void onTorchModeChanged(@NonNull String cameraId, boolean enabled) {
                    super.onTorchModeChanged(cameraId, enabled);
                    //状态为零
                    reportLightStatus(stauts);
                    //给下次状态改变
                    if(stauts == 1){
                        stauts = 0;
                    }else{
                        stauts = 1;
                    }
                }
            },mBackgroundHandler);
        }
    }


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    /**
     * 数据上行
     * 上报灯的状态
     */
    public void reportHelloWorld() {
        log("上报 Hello, World！");
        try {

            Map<String, ValueWrapper> reportData = new HashMap<>();
//            Camera cam = Camera.open();
//            Camera.Parameters p = cam.getParameters();
//            log( p.getFlashMode());
            //获取闪光灯的状态

            reportData.put("LightSwitch", new ValueWrapper.BooleanValueWrapper(1));
//            reportData.put("Status", new ValueWrapper.BooleanValueWrapper(1)); // 1开 0 关
//            reportData.put("Data", new ValueWrapper.StringValueWrapper("Hello, World!")); //
            LinkKit.getInstance().getDeviceThing().thingPropertyPost(reportData, new IPublishResourceListener() {
                @Override
                public void onSuccess(String s, Object o) {
                    Log.d(TAG, "onSuccess() called with: s = [" + s + "], o = [" + o + "]");
                    showToast("设备上报状态成功");
                    log("上报 Hello, World! 成功。");
                }

                @Override
                public void onError(String s, AError aError) {
                    Log.d(TAG, "onError() called with: s = [" + s + "], aError = [" + aError + "]");
                    showToast("设备上报状态失败");
                    log("上报 Hello, World! 失败。");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void reportLightStatus(int status) {
        log("上报 Light ,Status！");
        try {

            Map<String, ValueWrapper> reportData = new HashMap<>();
//            Camera cam = Camera.open();
//            Camera.Parameters p = cam.getParameters();
//            log( p.getFlashMode());
            //获取闪光灯的状态

            reportData.put("LightSwitch", new ValueWrapper.BooleanValueWrapper(status));
//            reportData.put("Status", new ValueWrapper.BooleanValueWrapper(1)); // 1开 0 关
//            reportData.put("Data", new ValueWrapper.StringValueWrapper("Hello, World!")); //
            LinkKit.getInstance().getDeviceThing().thingPropertyPost(reportData, new IPublishResourceListener() {
                @Override
                public void onSuccess(String s, Object o) {
                    Log.d(TAG, "onSuccess() called with: s = [" + s + "], o = [" + o + "]");
                    showToast("设备上报状态成功");
//                    log("上报 Hello, World! 成功。");
                }

                @Override
                public void onError(String s, AError aError) {
                    Log.d(TAG, "onError() called with: s = [" + s + "], aError = [" + aError + "]");
                    showToast("设备上报状态失败");
//                    log("上报 Hello, World! 失败。");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDownStreamListener(){
        LinkKit.getInstance().registerOnPushListener(notifyListener);
    }
//设置 通知
    private IConnectNotifyListener notifyListener = new IConnectNotifyListener() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onNotify(String s, String s1, AMessage aMessage) {
            try {
//                log(s1);
                if (s1 != null && s1.contains("service/property/set")) {
                    String result = new String((byte[]) aMessage.data, "UTF-8");
                    RequestModel<Switch> receiveObj = JSONObject.parseObject(result, new TypeReference<RequestModel<Switch>>() {
                    }.getType());
                    if( receiveObj.params.getLightSwitch() == 0){
                        closeLight();
                    }else{
                        openLight();
                    }

                    log("Received a message: " + (receiveObj==null?"":receiveObj.params));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public boolean shouldHandle(String s, String s1) {
            Log.d(TAG, "shouldHandle() called with: s = [" + s + "], s1 = [" + s1 + "]");
            return true;
        }

        @Override
        public void onConnectStateChange(String s, ConnectState connectState) {
            Log.d(TAG, "onConnectStateChange() called with: s = [" + s + "], connectState = [" + connectState + "]");
        }
    };

    private void log(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ALog.d(TAG, "log(), " + str);
                if (TextUtils.isEmpty(str))
                    return;
                consoleStr = consoleStr + "\n \n" + (getTime()) + " " + str;
                consoleTV.setText(consoleStr);
            }
        });

    }

    private void clearMsg() {
        consoleStr = "";
        consoleTV.setText(consoleStr);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHandler != null) {
            mHandler.removeMessages(REPORT_MSG);
            mHandler.removeCallbacksAndMessages(null);
            showToast("停止定时上报");
        }
        LinkKit.getInstance().unRegisterOnPushListener(notifyListener);
        clearMsg();
    }

    private class InternalHandler extends Handler {
        public InternalHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg == null) {
                return;
            }
            int what = msg.what;
            switch (what) {
                case REPORT_MSG:
                    reportHelloWorld();

                    mHandler.sendEmptyMessageDelayed(REPORT_MSG, interval*1000);
                    break;
            }

        }
    }








    /*开关灯*/

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openLight() {
        try {
            //判断API是否大于24（安卓7.0系统对应的API）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //获取CameraManager
                CameraManager mCameraManager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
                //获取当前手机所有摄像头设备ID
                assert mCameraManager != null;
                String[] ids = mCameraManager.getCameraIdList();
                for (String id : ids) {
                    CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                    //查询该摄像头组件是否包含闪光灯
                    Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                    if (flashAvailable != null && flashAvailable && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        //打开手电筒
                        mCameraManager.setTorchMode(id, true);
                    }
                }
            }
            Toast.makeText(getApplicationContext(), "已打开闪光灯", Toast.LENGTH_SHORT).show();
            button2.setEnabled(false);
            button2.setText("闪光灯已打开");
            button3.setEnabled(true);
            button3.setText("关闭闪光灯");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //关闭闪光灯
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void closeLight() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //获取CameraManager
                CameraManager mCameraManager = (CameraManager) getApplicationContext().getSystemService(Context.CAMERA_SERVICE);
                //获取当前手机所有摄像头设备ID
                assert mCameraManager != null;
                String[] ids = mCameraManager.getCameraIdList();
                for (String id : ids) {
                    CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                    //查询该摄像头组件是否包含闪光灯
                    Boolean flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                    if (flashAvailable != null && flashAvailable
                            && lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                        //关闭手电筒
                        mCameraManager.setTorchMode(id, false);

                    }
                }
            }

            Toast.makeText(getApplicationContext(), "已关闭闪光灯", Toast.LENGTH_SHORT).show();
            button2.setEnabled(true);
            button2.setText("打开闪光灯");
            button3.setEnabled(false);
            button3.setText("闪光灯已关闭");
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }







}
