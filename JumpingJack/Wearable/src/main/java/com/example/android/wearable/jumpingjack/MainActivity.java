/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.jumpingjack;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager.widget.ViewPager;
import androidx.wear.ambient.AmbientModeSupport;
import androidx.wear.widget.CircularProgressLayout;

import com.example.android.wearable.jumpingjack.fragments.FunctionOneFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionThreeFragment;
import com.example.android.wearable.jumpingjack.fragments.FunctionTwoFragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static java.lang.Math.abs;
import static java.lang.Math.log;

/**
 * The main activity for the Jumping Jack application. This activity registers itself to receive
 * sensor values.
 *
 * This activity includes a {@link ViewPager} with two pages, one that
 * shows the current count and one that allows user to reset the counter. the current value of the
 * counter is persisted so that upon re-launch, the counter picks up from the last value. At any
 * stage, user can set this counter to 0.
 */
public class MainActivity extends FragmentActivity
        implements AmbientModeSupport.AmbientCallbackProvider{

    private static final String TAG = "MainActivity";

    private SensorManager mSensorManager;
    private Sensor mAcceleratorSensor;
    private Sensor mGyroSensor;
    private Sensor mMagnetSensor;
    private int SENSOR_RATE_NORMAL = 20000;//Sensor sample rate =50Hz
    private long mLastTime = 0;
    private int timer=0;
    private Timer scrollTimer;
    private TimerTask scrollTask;
    private Timer waitTimer;
    private TimerTask waitTask;
    private boolean isHold=false;
    private int isTop=0;
    private int isBottom=0;
    private TextView gestureText;
    private TextView counterText;
    private PagerAdapter adapter;
    private int previousTime=0;
    private float roll;
    private int i=0;
    private int session = 0;
    private int previous_session=0;
    private int functionOrder=1;
    private int functionTime =1;
    private int task = 0;
    private int previous_task=0;
    private int block =0;
    private int previous_block=0;
    private Integer[][] blocks_StudyOne;
    private List<List<Integer>> randomBlocks_StudyOne;
    private List<Integer> random_block;
    private String[] block_name;
    private int viewIndex=0;
    private boolean isDeviceMenu=true;
    private boolean isFunctionMenu=false;
    private boolean isTrialView=false;
    private int previousPressed=0;
    private int selectedSemantic=0;
    private int[] target_function_senario1;
    private int[] target_function_senario2;
    private int[] target_function_senario3;
    MediaPlayer wrong_sound_player;
    MediaPlayer correct_sound_player;

    private ViewPager mPager;
    private FunctionOneFragment mCounterPage;
    private FunctionThreeFragment mLeftSwipeCounterPage;
    private FunctionTwoFragment mSettingPage;
    private ImageView mSecondIndicator;
    private ImageView mFirstIndicator;
    private ImageView mThirdIndicator;

    /**Bluetooth setup*/
    // Initializes Bluetooth adapter.
    private BluetoothAdapter bluetoothAdapter;
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean mScanning;
    private Handler handler;
    private BluetoothLeScanner lescanner;
    private ScanSettings settings;
    private List<ScanFilter> filters = new ArrayList<ScanFilter>();
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 300000;
    private byte[] manudata= new byte[4];
    private byte  BUTTON_LEFT = 0;
    private byte  BUTTON_RIGHT= 0;
    private byte  BUTTON_TRIGGER = 0;
    private byte  SLIDER_TOUCH = 0;
    private byte  SLIDER_VALUE = 0;
    private byte TOGGLE = 0;
    private byte PREVIOUS_BUTTON_LEFT=0;
    private byte PREVIOUS_BUTTON_RIGHT=0;
    private byte PREVIOUS_SLIDER_TOUCH=0;
    private byte PREVIOUS_SLIDER_VALUE=0;
    private byte PREVIOUS_TOGGLE=0;

    private float[] gravity= new float[3];
    private float[] linear_acceleration= new float[3];
    private float[] gyro=new float[3];
    private float[] accelerator=new float[3];
    private float[] magnet=new float[3];
    private double[] RA=new double[3]; //Relative Accelerator
    private float AO=0.0f; //Absolute Orientation
    private double[] acc=new double[3];
    private String mPosition = POSITION_UNKNOWN;

    public static final String POSITION_UNKNOWN = "Unkown";
    private final String POSITION_TOP="up";
    private final String POSITION_BOTTOM="down";
    private final String POSITION_LEFT="left";
    private final String POSITION_RIGHT="right";
    private final String POSITION_FORWARD="push";
    private final String STATION_DISCRETE_DETECTING="Detecting discrete gestures";
    private final String STATION_SELECTING="Selecting functions";
    private final String STATION_CONTINUOUS_SELECTING="Selecting continuous functions";
    private final String STATION_CONTINUOUS_DETECTING="Detecting continuous gestures";
    private String mStation=STATION_DISCRETE_DETECTING;

    private CircularProgressLayout circularProgress;
    private int layoutId;
    private function current_function;
    private boolean stopfunction = false;
    List<function> all_functions = new ArrayList<>();
    List<function> all_tasks=new ArrayList<>();
    int[] device_states;

    // wifi
    static int PORT = 11121;
    Socket socket = null;
    BufferedReader reader;
    PrintWriter writer;
    boolean listening;
    String tmp_s;
//    String ip = "192.168.43.224";
    String ip = "192.168.1.103";
 //   String ip = "10.127.44.126";
    log_data log_trial = new log_data();
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AmbientModeSupport.attach(this);
        context = getApplicationContext();

        //blocks_StudyOne = new Integer[][]{{1, 0}, {1, 2}, {1, 4},{2,0},{2,2},{2,4},{3,0},{3,2},{3,4}};
        //randomBlocks_StudyOne = new ArrayList<>();
        //for (Integer[] ints : blocks_StudyOne) {
        //    randomBlocks_StudyOne.add(Arrays.asList(ints));
        //}

        Integer[] blocks=new Integer[]{1,2,3};
        block_name=new String[]{"Meeting room","Living home","Smart home"};
        random_block=new ArrayList<Integer>();
        random_block= Arrays.asList(blocks);
        Collections.shuffle(random_block);


        target_function_senario1=new int[]{8,9,7,3,2,2,1,6};
        target_function_senario2=new int[]{1,0,3,5,6,7,8,3};
        target_function_senario3=new int[]{0,2,9,7,4,5,1,4};

        correct_sound_player = MediaPlayer.create(context, R.raw.correct);
        wrong_sound_player = MediaPlayer.create(context, R.raw.wrong);

        handler= new Handler();
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if(socket == null) {
            new NetworkAsyncTask().execute(ip);
        }
        send("New Experiment\n");
        setupstartview(block);

        Log.e("speaker", Boolean.toString(checkspeaker()));
    }

    private boolean checkspeaker(){
        PackageManager packageManager = context.getPackageManager();
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // Check whether the device has a speaker.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                // Check FEATURE_AUDIO_OUTPUT to guard against false positives.
                packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    return true;
                }
            }
        }
        return false;
    }

    /**Register sensor listener*/
    @Override
    protected void onResume() {
        super.onResume();
        Log.i("resume", "resume");
        if (socket == null){
            new NetworkAsyncTask().execute(ip);
        }
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else{
            lescanner = bluetoothAdapter.getBluetoothLeScanner();
            settings = new ScanSettings.Builder()//
                           .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)//
                           .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)//
                           .build();

            ScanFilter namefilter = new ScanFilter.Builder().setManufacturerData(0x0059, new byte[]{0x00, 0x00, 0x00, 0x00}, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}).build();
            //ScanFilter namefilter = new ScanFilter.Builder().setDeviceName("BoldMove").build();

            filters.add(namefilter);
            scanLeDevice(true);

        }
    }

    /**Unregister sensor listener*/
    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        disconnect();
    }

    @Override
    public void setContentView(int layoutResID) {
        this.layoutId = layoutResID;
        super.setContentView(layoutResID);
    }

    private void setupstartview(int block_num) {
        isTrialView=false;
        setContentView(R.layout.session_start);
        TextView block_textview = findViewById(R.id.block_num);
        TextView session_textview = findViewById(R.id.session_num);
        TextView block_name_textview=findViewById(R.id.block_name);

        // display block number
        /**Study 2 has 3 blocks*/
        if (block_num < 3) {
            block_textview.setText("Block" + block_num);
            block_name_textview.setText(block_name[random_block.get(block_num)-1]);
            try {
                /**Study 2 functions*/
                all_functions = assembly_functions("functions_study.json",2, 0, random_block.get(block), 0);
                all_tasks=assembly_functions("task_list.json",2, 0, random_block.get(block), 0);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            Log.d("all function size", String.valueOf(all_functions.size()));
            device_states = new int[all_functions.size()];
            // reinitialize states of all devices
            for (function f:all_functions
            ) {
                device_states[f.get_id()] = f.get_initstateid();
            }
        }
        else{
            block_textview.setText("Session "+session+"Finished!");
            block = 0;
            block_name_textview.setText(block_name[random_block.get(block)-1]);
            previous_session=session;
            session = session + 1;
            try {
                /**Study 2 functions*/
                all_functions = assembly_functions("functions_study.json",2, 0, random_block.get(block), 0);
                all_tasks=assembly_functions("task_list.json",2, 0, random_block.get(block), 0);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            device_states = new int[all_functions.size()];
            // reinitialize states of all devices
            for (function f:all_functions
            ) {
                device_states[f.get_id()] = f.get_initstateid();
            }
        }
        // display session number
        if (session < 2){
            session_textview.setText("Session "+session);
        }
        else{
            session_textview.setText("Experiment Finished");
            send("Experiment Finished!\n");
            disconnect();
        }

        Button start_button = findViewById(R.id.button_start);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int cnt = 10;
                if (socket == null){
                    new NetworkAsyncTask().execute(ip);
                }
                send(random_block.toString());
//                while (socket == null && cnt > 0) {
//                    new NetworkAsyncTask().execute(ip);
//                    if (socket.isConnected()){
//                        break;
//                    }
//                    Log.e("wifi", "socket not connected!");
//                    cnt -= 1;
//                }

                setupTrialview(block, task,-1,-1);
            }
        });

        //Collections.shuffle(randomBlocks_StudyOne);
        View func_view = findViewById(R.id.block_view);
        func_view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                log_trial = new log_data();
                task=previous_task;
                block=previous_block;
                session=previous_session;
                try {
                    all_functions = assembly_functions("functions_study.json",2, 0, random_block.get(block), 0);
                    all_tasks=assembly_functions("task_list.json",2, 0, random_block.get(block), 0);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                device_states[current_function.get_id()]=current_function.get_initstateid();
                setupTrialview(block,task,-1,-1);
                return true;
            }
        });
    }

    private void setupTrialview(int block_num, int task_num,int semantic, int pressed){
        isTrialView=true;
        setContentView(R.layout.block_layout);
        TextView block_textview = findViewById(R.id.block);
        TextView task_textview = findViewById(R.id.task);
        TextView task_name_textview=findViewById(R.id.task_name);
        Button start_button=findViewById(R.id.button_start);
        String blocktext = "Block "+ block_num;
        String tasktext = "Task "+ task_num;

        block_textview.setText(blocktext);
        task_textview.setText(tasktext);
        task_name_textview.setText(all_tasks.get(task).get_device()[0]+" "+all_tasks.get(task).get_name());

        /**Study 2*/
       /* if(session==0)
            start_button.setVisibility(View.INVISIBLE);
        else{
            start_button.setVisibility(View.VISIBLE);
            start_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int cnt = 10;
                    if (socket == null){
                        new NetworkAsyncTask().execute(ip);
                    }

                    setUpMenuView(true,"",all_functions);
                    log_trial.timestamp_pressed=System.currentTimeMillis();
                }
            });
        }*/

        View func_view = findViewById(R.id.task_view);
        func_view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                log_trial = new log_data();
                task=previous_task;
                block=previous_block;
                session=previous_session;
                try {
                    all_functions = assembly_functions("functions_study.json",2, 0, random_block.get(block),0);
                    all_tasks=assembly_functions("task_list.json",2, 0, random_block.get(block), 0);
                    device_states[current_function.get_id()]=current_function.get_initstateid();
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
                setupTrialview(block,task,-1,-1);
                return true;
            }
        });

       if((semantic==0||semantic==1)&&pressed==1)
       {
           if (socket == null){
               new NetworkAsyncTask().execute(ip);
           }
           log_trial.timestamp_pressed=System.currentTimeMillis();
           isTrialView=false;

           setUpMenuView(true,"",all_functions);
       }

        Log.d("view", Integer.toString(layoutId));

    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupfunctionview(int task_num, int semantic, int pressed, int slider_value) {
        isTrialView=false;
        Log.e("display", Integer.toString(semantic));
        int view_func_select;
        //int viewid;
        int cp;
        int deviceid;
        int funcid;
        switch (semantic) {
            case 2:
                view_func_select = R.layout.toggle_func_select;
                //viewid = R.id.toggle_func_select;
                cp = R.id.circular_progress2;
                deviceid = R.id.device2;
                funcid = R.id.function2;
                break;

            case 3:
                view_func_select = R.layout.slider_func_select;
                //viewid = R.id.slider_func_select;
                cp = R.id.circular_progress3;
                deviceid = R.id.device3;
                funcid = R.id.function3;
                break;

            default:
                view_func_select = R.layout.pn_func_select;
                //viewid = R.id.pn_func_select;
                cp = R.id.circular_progress1;
                deviceid = R.id.device1;
                funcid = R.id.function1;
        }

        int[] target_functions;
        switch (random_block.get(block)){
            case 1:
                target_functions=target_function_senario1;
                break;
            case 2:
                target_functions=target_function_senario2;
                break;
            case 3:
                target_functions=target_function_senario3;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + random_block.get(block));
        }

        if (pressed == 1 && layoutId == R.layout.block_layout) {
            log_trial.timestamp_pressed = System.currentTimeMillis();

            setContentView(view_func_select);

            /**Study 2 fixed function time*/
            //int functionOrder= randomBlocks_StudyOne.get(task_num).get(1);
            int functionTime = 2;
            Integer[] function_order;
            if(semantic==0||semantic==1)
                function_order  =new Integer[]{0,1};
            else
                function_order  =new Integer[]{0,1,2};
            List<Integer>random_order=Arrays.asList(function_order);
            Collections.shuffle(random_order);

           // log_trial.configure = new int[]{functionOrder, functionTime};
            int index = 0;

            final List<function> functions = functionList(semantic, task_num, random_order.get(0),target_functions[task_num]);
            log_trial.func_id=new int[functions.size()];
            for (int j = 0; j < functions.size(); j++) {
                log_trial.func_id[j] = functions.get(j).get_id();
            }
            log_trial.funcid_target = target_functions[task_num];

            circularProgress = (CircularProgressLayout) findViewById(cp);
            Log.e("display", Integer.toString(circularProgress.getId()));

            circularProgress.setTotalTime(functionTime * 1000);
            stopfunction = false;

            updatefunctionview(index, functions, circularProgress, semantic, funcid, deviceid);

        }

        if (pressed == 0 && layoutId == view_func_select){
            if(semantic != 2) {
                log_trial.timestamp_selected = System.currentTimeMillis();
            }
            log_trial.funcid_selected = current_function.get_id();
            circularProgress.stopTimer();
            circularProgress.setVisibility(View.INVISIBLE);

            stopfunction = true;
            // deal with state display
            final int functionid = current_function.get_id();
            if(functionid==target_functions[task_num])
                correct_sound_player.start();
            else
                wrong_sound_player.start();

            int temp_stateid = device_states[functionid];
            if (semantic == 0){
                temp_stateid -= 1;
                if (temp_stateid < 0){
                    temp_stateid = current_function.get_state().length - 1;
                }
            }

            if (semantic == 1 || semantic == 2){
                temp_stateid += 1;
                if (temp_stateid > current_function.get_state().length - 1){
                    temp_stateid = 0;
                }
            }

            // update displayed state
            if (semantic == 2){
                Switch toggle = findViewById(R.id.switch2);
                toggle.setChecked(temp_stateid == 0);
                TextView state_text = findViewById(R.id.state_text2);
                state_text.setText(current_function.get_state()[temp_stateid]);
            }

            if (semantic == 0 || semantic == 1){
                TextView state_text = findViewById(R.id.state_text1);
                state_text.setText(current_function.get_state()[temp_stateid]);
            }

            if (semantic == 3){
                SeekBar slider = findViewById(R.id.seekBar3);
                slider.setEnabled(false);
            }

            Log.d("display", Integer.toString(view_func_select));
            final int finalTemp_stateid = temp_stateid;

            device_states[functionid] = finalTemp_stateid;
            log_trial.session = session;
            log_trial.block = block;
            log_trial.trial = task;
            if (socket == null){
                new NetworkAsyncTask().execute(ip);
            }
            else{
                Log.d("socket", String.valueOf(socket.isConnected()));
            }
            send(log_trial.assemby_send_string());

            scrollTimer = new Timer();

            /**Timer: page scroll every 2s*/
            scrollTask = new TimerTask() {
                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            log_trial = new log_data();

                            previous_task=task;
                            previous_block=block;

                            task = task + 1;
                            /**Study 2 total trial number*/
                            if (task == all_tasks.size()){
                                block = block + 1;
                                task = 0;
                                setupstartview(block);
                            }
                            else{
                                setupTrialview(block, task,-1,-1);
                            }
                        }});}
            };

            scrollTimer.schedule(scrollTask, 2000);//every 2 seconds
            //func_view.setOnClickListener(new View.OnClickListener() {
               // @Override
                //public void onClick(View v) {

             //   }
            //});

           /* func_view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    log_trial = new log_data();
                    setupTrialview(block, task);
                    return true;
                }
            });*/
        }

        // for slider selection
        if (pressed == 2 && layoutId == view_func_select){
            SeekBar slider = findViewById(R.id.seekBar3);
            if (slider.isEnabled()) {
                log_trial.timestamp_selected = System.currentTimeMillis();
                circularProgress.stopTimer();
                circularProgress.setVisibility(View.INVISIBLE);
                stopfunction = true;
                // make buttons visible
                TextView svalue = findViewById(R.id.state3_text);
                String[] scale = current_function.get_state();

                int min = Integer.parseInt(scale[0], 10);
                int max = Integer.parseInt(scale[scale.length - 1], 10);
                int scaled_value = min + (max - min) * (SLIDER_VALUE) / 15;

                device_states[current_function.get_id()] = scaled_value - min;
                //Log.d("scale", scale[0]+"-"+scale[scale.length-1]+"-"+min+"-"+max+"-"+scaled_value);
                svalue.setText(Integer.toString(scaled_value));

                slider.setMax(max);
                slider.setMin(min);
                slider.setProgress(scaled_value);

                log_trial.session = session;
                log_trial.block = block;
                log_trial.trial = task;
                if (socket == null){
                    new NetworkAsyncTask().execute(ip);
                }
                else{
                    Log.d("socket", String.valueOf(socket.isConnected()));
                }
                send(log_trial.assemby_send_string());
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupfunctionview_study2(int index,int semantic, int pressed, int slider_value) {
        int view_func_select;
        int viewid;
        int deviceid;
        int funcid;
        switch (semantic) {
            case 2:
                view_func_select = R.layout.toggle_func_select;
                viewid = R.id.toggle_func_select;
                deviceid = R.id.device2;
                funcid = R.id.function2;
                break;

            case 3:
                view_func_select = R.layout.slider_func_select;
                viewid = R.id.slider_func_select;
                deviceid = R.id.device3;
                funcid = R.id.function3;
                break;

            default:
                view_func_select = R.layout.pn_func_select;
                viewid = R.id.pn_func_select;
                deviceid = R.id.device1;
                funcid = R.id.function1;
        }

        int[] target_functions;
        switch (random_block.get(block)){
            case 1:
                target_functions=target_function_senario1;
                break;
            case 2:
                target_functions=target_function_senario2;
                break;
            case 3:
                target_functions=target_function_senario3;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + random_block.get(block));
        }



        if (pressed == -1) {
            //send log to server
            log_trial.funcid_target = target_functions[task];
            log_trial.timestamp_selected = System.currentTimeMillis();
            log_trial.session = session;
            log_trial.block = block;
            log_trial.trial = task;

            if (socket == null){
                new NetworkAsyncTask().execute(ip);
            }
            else{
                Log.d("socket", String.valueOf(socket.isConnected()));
            }
            send(log_trial.assemby_send_string());

            setContentView(view_func_select);
            updatefunctionview_study2(index, all_functions, semantic, funcid, deviceid);
        }

        if (pressed == 0){

            final int functionid = current_function.get_id();
            if(functionid==target_functions[task])
                correct_sound_player.start();
            else
                wrong_sound_player.start();
            int temp_stateid = device_states[functionid];
            if(selectedSemantic==0){
            if (semantic == 0){
                temp_stateid -= 1;
                if (temp_stateid < 0){
                    temp_stateid = current_function.get_state().length - 1;
                }
            }

            if (semantic == 1){
                temp_stateid += 1;
                if (temp_stateid > current_function.get_state().length - 1){
                    temp_stateid = 0;
                }
            }}

            if(selectedSemantic==2){
                if(semantic==2){
                    temp_stateid += 1;
                    if (temp_stateid > current_function.get_state().length - 1){
                        temp_stateid = 0;
                    }}
            }

            // update displayed state
            if (semantic == 2 && selectedSemantic==2){
                Switch toggle = findViewById(R.id.switch2);
                toggle.setChecked(temp_stateid == 0);
                TextView state_text = findViewById(R.id.state_text2);
                state_text.setText(current_function.get_state()[temp_stateid]);
            }

            if ((semantic == 0 || semantic == 1)&&selectedSemantic==0 ){
                TextView state_text = findViewById(R.id.state_text1);
                state_text.setText(current_function.get_state()[temp_stateid]);
            }

            if (semantic == 3 && selectedSemantic==3){
                SeekBar slider = findViewById(R.id.seekBar3);
                slider.setEnabled(false);
            }

            Log.d("display", Integer.toString(view_func_select));
            View func_view = findViewById(viewid);
            if(func_view!=null){
            final int finalTemp_stateid = temp_stateid;
                device_states[functionid] = finalTemp_stateid;
//                log_trial.session = session;
//                log_trial.block = block;
//                log_trial.trial = task;
//                if (socket == null){
//                    new NetworkAsyncTask().execute(ip);
//                }
//                else{
//                    Log.d("socket", String.valueOf(socket.isConnected()));
//                }
//                if (semantic != 2){
//                    send(log_trial.assemby_send_string());
//                }
                scrollTimer = new Timer();

                /**Timer: page scroll every 2s*/
                scrollTask = new TimerTask() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                isFunctionMenu=false;

                                log_trial = new log_data();

                                previous_task=task;
                                previous_block=block;
                                previous_session=session;
                                task = task + 1;
                                /**Study 2 total trial number*/
                                if (task == all_tasks.size()){
                                    block = block + 1;
                                    task = 0;
                                    setupstartview(block);
                                }
                                else{
                                    setupTrialview(block, task,-1,-1);
                                }
                            }});}
                };

                scrollTimer.schedule(scrollTask, 2000);//every 2 seconds

            }
        }


        // for slider selection
        if (pressed == 2 && layoutId == view_func_select && selectedSemantic==3){
            SeekBar slider = findViewById(R.id.seekBar3);
            if (slider.isEnabled()) {
                log_trial.timestamp_selected = System.currentTimeMillis();
                stopfunction = true;
                // make buttons visible
                TextView svalue = findViewById(R.id.state3_text);
                String[] scale = current_function.get_state();

                int min = Integer.parseInt(scale[0], 10);
                int max = Integer.parseInt(scale[scale.length - 1], 10);
                int scaled_value = min + (max - min) * (SLIDER_VALUE) / 15;

                device_states[current_function.get_id()] = scaled_value - min;
                //Log.d("scale", scale[0]+"-"+scale[scale.length-1]+"-"+min+"-"+max+"-"+scaled_value);
                svalue.setText(Integer.toString(scaled_value));

                slider.setMax(max);
                slider.setMin(min);
                slider.setProgress(scaled_value);
//
//                log_trial.session = session;
//                log_trial.block = block;
//                log_trial.trial = task;
//                if (socket == null){
//                    new NetworkAsyncTask().execute(ip);
//                }
//                else{
//                    Log.d("socket", String.valueOf(socket.isConnected()));
//                }
//                send(log_trial.assemby_send_string());

            }
        }

    }

    private void  updatefunctionview(final int index, final List<function> functions, CircularProgressLayout layout, final int sem, final int funcid, final int deviceid){
        if (!stopfunction) {
            current_function = functions.get(index);
            layout.stopTimer();

            Log.d("functionlist",functions.get(index).get_device()[0]);

            int current_state = device_states[current_function.get_id()];
            String image_src;
            if(sem == 2) {
                image_src = functions.get(index).get_name() + "_" + current_state;
            }
            else{
                image_src = functions.get(index).get_name();
            }
            Log.d("funcupdate", Integer.toString(index));
            Log.d("funcupdate", Integer.toString(sem));
            Log.d("funcupdate",image_src);

            ImageView image = findViewById(funcid);
            if(image!=null){
            int drawableId = context.getResources().getIdentifier(image_src, "drawable", context.getPackageName());
            image.setImageResource(drawableId);}

//            if (functions.get(index).get_imageid() != null) {
//                func_image.setImageResource(functions.get(index).get_imageid());
//            }
            TextView device = findViewById(deviceid);
            if(device!=null)
                device.setText(Arrays.toString(functions.get(index).get_device()).replace("[", " ").replace("]", " "));

            if (sem == 0) {
                ImageView limage  = findViewById(R.id.semanticl);
                String limage_src = "previous_l";
                limage.setImageResource(context.getResources().getIdentifier(limage_src, "drawable", context.getPackageName()));
                ImageView rimage = findViewById(R.id.semanticr);
                String rimage_src = "previous_r";
                rimage.setImageResource(context.getResources().getIdentifier(rimage_src, "drawable", context.getPackageName()));
                TextView state_text = findViewById(R.id.state_text1);
                Log.d("current_state", String.valueOf(current_state));
                state_text.setText(current_function.get_state()[current_state]);
            }

            if (sem == 1) {
                ImageView limage  = findViewById(R.id.semanticl);
                String limage_src = "next_l";
                if(limage!=null)
                    limage.setImageResource(context.getResources().getIdentifier(limage_src, "drawable", context.getPackageName()));
                ImageView rimage = findViewById(R.id.semanticr);
                String rimage_src = "next_r";
                if(rimage!=null)
                    rimage.setImageResource(context.getResources().getIdentifier(rimage_src, "drawable", context.getPackageName()));
                TextView state_text = findViewById(R.id.state_text1);
                if(state_text!=null)
                    state_text.setText(current_function.get_state()[current_state]);
            }

            if (sem == 2) {
                Switch toggle = findViewById(R.id.switch2);
                if(toggle!=null){
                toggle.setChecked(current_state == 0);
                toggle.jumpDrawablesToCurrentState();}
                TextView state_text = findViewById(R.id.state_text2);
                if(state_text!=null)
                state_text.setText(current_function.get_state()[current_state]);
            }

            if (sem == 3) {
                SeekBar slider = findViewById(R.id.seekBar3);
                //slider.setEnabled(false);
                String[] scale = current_function.get_state();
                int min  = Integer.parseInt(scale[0], 10);
                int max = Integer.parseInt(scale[scale.length-1], 10);
                //int scaled_value = min + (max-min) * (SLIDER_VALUE)/ 15;
                if(slider!=null){
                slider.setMax(max);
                slider.setMin(min);
                slider.setProgress(Integer.parseInt(scale[current_state]));}

                TextView slider_value = findViewById(R.id.state3_text);
                slider_value.setText(scale[current_state]);

            }


//            TextView state = findViewById(stateid);
//            state.setText(current_function.get_state()[current_state]);

            //final ImageView finalImage = func_image;
            layout.setOnTimerFinishedListener(new CircularProgressLayout.OnTimerFinishedListener() {
                @Override
                public void onTimerFinished(CircularProgressLayout layout) {
                    if (index == functions.size() - 1) {
                        updatefunctionview(0, functions, layout, sem, funcid, deviceid);
                    } else {
                        updatefunctionview(index + 1, functions, layout, sem, funcid, deviceid);
                    }
                }
            });
            layout.startTimer();
            log_trial.timestamp_func_start[index] = System.currentTimeMillis();
        }
        else{
            layout.stopTimer();
        }
    };

    private void  updatefunctionview_study2(final int index, final List<function> functions, final int sem, final int funcid, final int deviceid){
            current_function = functions.get(index);

            int current_state = device_states[current_function.get_id()];
            String image_src;
            if(sem == 2) {
                image_src = functions.get(index).get_name() + "_" + current_state;
            }
            else{
                image_src = functions.get(index).get_name();

            }
            Log.d("funcupdate", Integer.toString(index));
            Log.d("funcupdate", Integer.toString(sem));
            Log.d("funcupdate",image_src);

            ImageView image = findViewById(funcid);
            int drawableId = context.getResources().getIdentifier(image_src, "drawable", context.getPackageName());
            image.setImageResource(drawableId);

            TextView device = findViewById(deviceid);
            device.setText(Arrays.toString(functions.get(index).get_device()).replace("[", " ").replace("]", " "));

            if (sem == 0) {
                ImageView limage  = findViewById(R.id.semanticl);
                String limage_src = "previous_l";
                limage.setImageResource(context.getResources().getIdentifier(limage_src, "drawable", context.getPackageName()));
                ImageView rimage = findViewById(R.id.semanticr);
                String rimage_src = "previous_r";
                rimage.setImageResource(context.getResources().getIdentifier(rimage_src, "drawable", context.getPackageName()));
                TextView state_text = findViewById(R.id.state_text1);
                state_text.setText(current_function.get_state()[current_state]);
            }

            if (sem == 1) {
                ImageView limage  = findViewById(R.id.semanticl);
                String limage_src = "next_l";
                limage.setImageResource(context.getResources().getIdentifier(limage_src, "drawable", context.getPackageName()));
                ImageView rimage = findViewById(R.id.semanticr);
                String rimage_src = "next_r";
                rimage.setImageResource(context.getResources().getIdentifier(rimage_src, "drawable", context.getPackageName()));
                TextView state_text = findViewById(R.id.state_text1);
                state_text.setText(current_function.get_state()[current_state]);
            }

            if (sem == 2) {
                Switch toggle = findViewById(R.id.switch2);
                toggle.setChecked(current_state == 0);
                toggle.jumpDrawablesToCurrentState();
                TextView state_text = findViewById(R.id.state_text2);
                state_text.setText(current_function.get_state()[current_state]);
            }

            if (sem == 3) {
                SeekBar slider = findViewById(R.id.seekBar3);
                //slider.setEnabled(false);
                String[] scale = current_function.get_state();
                int min  = Integer.parseInt(scale[0], 10);
                int max = Integer.parseInt(scale[scale.length-1], 10);
                //int scaled_value = min + (max-min) * (SLIDER_VALUE)/ 15;
                slider.setMax(max);
                slider.setMin(min);
                slider.setProgress(Integer.parseInt(scale[current_state]));

                TextView slider_value = findViewById(R.id.state3_text);
                slider_value.setText(scale[current_state]);

            }

    };

    /**Study 2: baseline study - device and function scroll menu setup*/
    private void setUpMenuView(boolean isDeviceMenu, String selectedDevice, final List<function> functions){
        setContentView(R.layout.scroll_menu);
        final ScrollView scrollView=findViewById(R.id.scrollView);
        final LinearLayout scrollList=findViewById(R.id.scrollList);
        //添加列表
        for(int i=0;i<functions.size();i++){
            TextView textView;
            //如果是设备选择页，获取所有设备
            if(isDeviceMenu){
                if(i==0||(i>0&&!functions.get(i).get_device()[0].equals(functions.get(i-1).get_device()[0]))){
                textView=new TextView(this);
                textView.setText(functions.get(i).get_device()[0]);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,25);
                textView.setGravity(Gravity.CENTER);
                scrollList.addView(textView);
                textView.getLayoutParams().height = 100;
                textView.getLayoutParams().width=250;
                }
            }
            //如果是功能选择页，获取该设备的所有功能
            if(!isDeviceMenu){
                if(functions.get(i).get_device()[0].equals(selectedDevice)){
                    textView=new TextView(this);
                    textView.setId(functions.get(i).get_id());
                    textView.setText(functions.get(i).get_name());
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,25);
                    textView.setGravity(Gravity.CENTER);
                    scrollList.addView(textView);
                    textView.getLayoutParams().height = 100;
                    textView.getLayoutParams().width=250;
                }
            }

        }
        TextView device= (TextView) scrollList.getChildAt(0);
        device.setTextColor(Color.BLUE);
    }

    /**Study 2: baseline study - device and function scroll menu update*/
    private void updateMenuView(int semantic, int pressed, int SLIDER_VALUE){
        final ScrollView scrollView=findViewById(R.id.scrollView);
        final LinearLayout scrollList=findViewById(R.id.scrollList);
         if(scrollList==null||scrollView==null)
             return;

        if(pressed==0&&previousPressed==1){
                switch (semantic){
                    case 0: //向上滚动
                        if(viewIndex>0){
                            TextView previousDevice=(TextView)scrollList.getChildAt(viewIndex);
                            previousDevice.setTextColor(Color.GRAY);
                            viewIndex--;}
                        break;
                    case 1://向下滚动
                        if(viewIndex<scrollList.getChildCount()-1){
                            TextView previousDevice=(TextView)scrollList.getChildAt(viewIndex);
                            previousDevice.setTextColor(Color.GRAY);
                            viewIndex++;}
                        break;
                    case 2:
                        if(isDeviceMenu){
                            //如果当前页面是设备选择页，进入功能选择页
                            log_trial.timestamp_device_selected=System.currentTimeMillis();
                            TextView selectedDevice=(TextView)scrollList.getChildAt(viewIndex);
                            setUpMenuView(false, (String) selectedDevice.getText(),all_functions);
                            isDeviceMenu=false;
                            viewIndex=0;
                        } else{
                            //如果当前页面是功能选择页，进入功能调节页
                            TextView selectedFunction=(TextView)scrollList.getChildAt(viewIndex);
                            selectedSemantic=all_functions.get(selectedFunction.getId()).get_semantic()[0];
                            log_trial.funcid_selected = selectedFunction.getId();
                            setupfunctionview_study2(selectedFunction.getId(),selectedSemantic,-1,SLIDER_VALUE);
                            isFunctionMenu=true;
                            isDeviceMenu=true;
                            viewIndex=0;
                        }

                        break;
                }
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        TextView device= (TextView) scrollList.getChildAt(viewIndex);
                        scrollView.smoothScrollTo(0,device.getTop()-150);
                        device.setTextColor(Color.BLUE);
                    }});

            }
        previousPressed=pressed;
    }

    ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                // Should not happen.
                Log.e(TAG, "LE Scan has already started");
                return;
            }
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                return;
            }
//            Log.d("blescan", scanRecord.toString());
             int[] inputs = new int[]{-1,-1,-1,-1};
          manudata = scanRecord.getManufacturerSpecificData(0x0059);
//          if (scanRecord.getDeviceName().equals("BoldMove_test")){
//                inputs = getTouchInput(manudata);
//            }
            switch(random_block.get(block)){
                case 1:
                    if (scanRecord.getDeviceName().equals("BoldMove1")){
                        inputs = getTouchInput(manudata);
                    }
                    break;
                case 2:
                    if (task < 3 && scanRecord.getDeviceName().equals("BoldMove_Cup")){
                        inputs = getTouchInput(manudata);
                    }
                    if (task >= 3 && scanRecord.getDeviceName().equals("BoldMove_Book")){
                        inputs = getTouchInput(manudata);
                    }
                    break;
                case 3:
                    if (scanRecord.getDeviceName().equals("BoldMove_FG")){
                        inputs = getTouchInput(manudata);
                    }
                    break;
            }
            /*manudata = scanRecord.getManufacturerSpecificData(0x0059);

            int[] inputs = getTouchInput(manudata);*/

            if (inputs[0] > -1 && inputs[1] > -1) {
                Log.d("manudata",Integer.toString(inputs[0])+Integer.toString(inputs[1]));
                if(session==0)
                    setupfunctionview(task, inputs[0], inputs[1], SLIDER_VALUE);
                /**Study 2 Session 2*/
                else{
                    if(isTrialView==true)
                        setupTrialview(block,task,inputs[0],inputs[1]);
                        //Log.d("test","test");
                    else if(isFunctionMenu==false)
                        updateMenuView(inputs[0],inputs[1],SLIDER_VALUE);
                    else
                        setupfunctionview_study2(0,inputs[0],inputs[1],SLIDER_VALUE);
                }

            }
//            try {
//                ubiTouchStatus();
//            } catch (IOException | JSONException e) {
//                e.printStackTrace();
//            }
//            callback.onLeScan(result.getDevice(), result.getRssi(),
//                    scanRecord.getBytes());
        }
    };

    private int[] getTouchInput(byte[] advdata){
        int semantic = -1;
        int pressed = -1; //1 pressed, 0 released, 2 dragged
        // Previous
        if (BUTTON_LEFT != advdata[0]){
            semantic = 0;
            BUTTON_LEFT = advdata[0];
            if (advdata[0] == 0){
                // button released
                pressed = 0;
            }
            else{
                // button pressed
                pressed = 1;
            }
        };
        // Next
        if (BUTTON_RIGHT != advdata[1]){
            semantic = 1;
            BUTTON_RIGHT = advdata[1];
            if (advdata[1] == 0){
                // button released
                pressed = 0;
            }
            else{
                // button pressed
                pressed = 1;
            }
        };
        // Tigger
        if (BUTTON_TRIGGER != advdata[2]){
            semantic = 2;
            BUTTON_TRIGGER = advdata[2];
            if (advdata[2] == 0){
                // button released
                pressed = 0;
            }
            else{
                // button pressed
                pressed = 1;
            }
        };
        // Slider
        if (SLIDER_TOUCH != getByteValues(advdata[3])[0]){
            semantic = 3;
            SLIDER_TOUCH = getByteValues(advdata[3])[0];//获得前四位值
            SLIDER_VALUE = getByteValues(advdata[3])[1];//获得后四位值
            if (SLIDER_TOUCH == 0){
                // slider released
                pressed = 0;
            }
            else{
                // slider pressed
                pressed = 1;
            }
        };

        if (abs(SLIDER_VALUE-getByteValues(advdata[3])[1])>2){  // deal with noises
            semantic = 3;
            SLIDER_VALUE = getByteValues(advdata[3])[1];//获得后四位值
            // slider pressed
            pressed = 2;
        };

        return new int[] {semantic, pressed};
    }
    /**Get first four bits and last four bits values*/
    public static byte[] getByteValues(byte b) {
        byte[] array = new byte[2];
        for (int i = 1; i >= 0; i--) {
            array[i] = (byte)(b & 15);
            b = (byte) (b >> 4);
        }
        return array;
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;
            lescanner.startScan(filters, settings, scanCallback);
            //lescanner.startScan(scanCallback);

        } else {
            mScanning = false;
            lescanner.stopScan(scanCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**Redefined function list*/
    private List<function> assembly_functions(String jason_file,int study, int session, int block, int semantic) throws IOException, JSONException {
        InputStream jsonStream = getAssets().open(jason_file);//"functions_study.json"
        JSONObject jsonObject = new JSONObject(Utils.convertStreamToString(jsonStream));
        JSONArray json_scenarios = new JSONArray();
        if (study == 1) {
             json_scenarios = jsonObject.getJSONArray("functions_study" + Integer.toString(study));
        }
        else if (study == 2){
            json_scenarios = jsonObject.getJSONArray("functions_study" + Integer.toString(study) + "_scenario"+Integer.toString(block));
        }
        List<function> functions = new ArrayList<function>();
        for (int i = 0; i < json_scenarios.length(); i++) {
            functions.add(new function(json_scenarios.getJSONObject(i), context));
        }
        return functions;
    }

    private List<function> extract_semantic_functions (List<function> block_functions, int semantic){
        List<function> mapping_functions = new ArrayList<>();
        for (function item:
             block_functions) {
            for (int i:item.get_semantic()
                 ) {
                if (i == semantic){
                    mapping_functions.add(item);
                    break;
                }
            }
        }
        return mapping_functions;
    }


    /**Predefined function list*/
    private List<function> functionList(int semantic,int task_num, int functionOrder,int target_function_id){
        boolean isTargetFunction=false;
        List<function> semantic_functions = extract_semantic_functions(all_functions, semantic);
        Collections.shuffle(semantic_functions);
        function target_function = new function();
        for (function item:semantic_functions) {
            if (item.get_id() ==target_function_id){
                isTargetFunction=true;
                target_function = item;
                semantic_functions.remove(item);
                break;
            }
        }
        if(isTargetFunction)
            semantic_functions.add(functionOrder, target_function);
        return semantic_functions;
    }



//    /**Real-time gesture display*/
//    private void function_display(List<function> functions){
//        setContentView(R.layout.circular_timer);
//
//        gestureText = findViewById(R.id.gesture);
//        gestureText.setText(initialText);
//
//        counterText=  findViewById(R.id.counter);
//        counterText.setText("第"+(block==9?1:(block+1))+"次");
//
//        if (scrollTimer != null) {
//            scrollTimer.cancel();
//            scrollTimer = null;
//        }
//        if (scrollTask != null) {
//            scrollTask.cancel();
//            scrollTask = null;
//        }
//
//        Button remove = (Button) this.findViewById(R.id.removeButton);
//        remove.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //TODO Auto-generated method stub
//                if(block >0)
//                    block--;
//                counterText.setText("第"+(block==9?1:(block+1))+"次");
//                Log.i("buttonEvent", "removeButton被用户点击了。");
//            }
//        });
//    }
//
//    /**Scroll function list*/
//    private void setupScrollViews(String function1, String function2, String function3) {
//        setContentView(R.layout.jumping_jack_layout);
//        mPager = findViewById(R.id.pager);
//        mFirstIndicator = findViewById(R.id.indicator_0);
//        mSecondIndicator = findViewById(R.id.indicator_1);
//        mThirdIndicator=findViewById(R.id.indicator_2);
//
//        adapter = new PagerAdapter(getSupportFragmentManager());
//
//        mCounterPage = new FunctionOneFragment(function1);
//        mSettingPage = new FunctionTwoFragment(function2);
//        mLeftSwipeCounterPage=new FunctionThreeFragment(function3);
//
//        adapter.addFragment(mCounterPage);
//        adapter.addFragment(mSettingPage);
//        adapter.addFragment(mLeftSwipeCounterPage);
//
//        setIndicator(0);
//        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int i, float v, int i2) {
//                // No-op.
//                Log.d(TAG, String.valueOf(i));
//            }
//
//            @Override
//            public void onPageSelected(int i) {
//
//                setIndicator(i);
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int i) {
//                // No-op.
//            }
//        });
//
//        circularProgress = (CircularProgressLayout) findViewById(R.id.circular_progress);
////        circularProgress.setwidt(50);
//        circularProgress.setOnTimerFinishedListener(this);
//
//        circularProgress.setTotalTime(functionTime *1000);
//        // Start the timer
//        circularProgress.startTimer();
//
//        scrollTimer = new Timer();
//
//        /**Timer: page scroll every 2s*/
//        scrollTask = new TimerTask() {
//            @Override
//            public void run() {
//                // TODO Auto-generated method stub
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        circularProgress.stopTimer();
//                        mPager.setCurrentItem(mPager.getCurrentItem()==2?0:mPager.getCurrentItem()+1);
//                        //写入Log文件，每个选项出现时间
//                        Log.d("everyFunctionTime",String.valueOf(System.currentTimeMillis()));
//                        // Start the timer
//                        circularProgress.startTimer();
//                    }});}
//        };
//
//        scrollTimer.schedule(scrollTask, functionTime *1000, functionTime *1000);//every 2 seconds
//
//        mPager.setAdapter(adapter);
//    }
//
//    private void setText(String text)
//    {
//        if(gestureText!=null)
//        {
//            gestureText.setText(text);
//        }
//    }
//
//    private void stopTimer(){
//        if (waitTimer != null) {
//            waitTimer.cancel();
//            waitTimer = null;
//        }
//        if (waitTask != null) {
//            waitTask.cancel();
//            waitTask = null;
//        }
//    }
//
//    /**Sets the page indicator for the ViewPager.*/
//    private void setIndicator(int i) {
//        switch (i) {
//            case 0:
//                mFirstIndicator.setImageResource(R.drawable.full_10);
//                mSecondIndicator.setImageResource(R.drawable.empty_10);
//                mThirdIndicator.setImageResource(R.drawable.empty_10);
//                break;
//            case 1:
//                mFirstIndicator.setImageResource(R.drawable.empty_10);
//                mSecondIndicator.setImageResource(R.drawable.full_10);
//                mThirdIndicator.setImageResource(R.drawable.empty_10);
//                break;
//            case 2:
//                mFirstIndicator.setImageResource(R.drawable.empty_10);
//                mSecondIndicator.setImageResource(R.drawable.empty_10);
//                mThirdIndicator.setImageResource(R.drawable.full_10);
//                break;
//        }
//    }

    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }


    /** Customizes appearance for Ambient mode. (We don't do anything minus default.) */
    private static class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        /** Prepares the UI for ambient mode. */
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            super.onEnterAmbient(ambientDetails);
        }

        /**
         * Updates the display in ambient mode on the standard interval. Since we're using a custom
         * refresh cycle, this method does NOT update the data in the display. Rather, this method
         * simply updates the positioning of the data in the screen to avoid burn-in, if the display
         * requires it.
         */
        @Override
        public void onUpdateAmbient() {
            super.onUpdateAmbient();
        }

        /** Restores the UI to active (non-ambient) mode. */
        @Override
        public void onExitAmbient() {
            super.onExitAmbient();
        }
    }

    void disconnect() {
        try {
            //if (reader != null) reader.close();
            if (writer != null) writer.close();
            socket.close();
            socket = null;
            //text_connect_info.setText("disconnected");
        } catch (Exception e) {
            //text_connect_info.setText(e.toString());
        }
    }

    void send(String s) {
        tmp_s = s;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (socket != null) {
                    writer.write(tmp_s);
                    writer.flush();
                }
            }
        }).start();
    }
//
//    void recv(String s) {
//        Log.d("b2wdebug", "receive: " + s);
//        tmp_s = s;
//        activity_uithread.runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                //text_0.setText(tmp_s);
//            }
//        });
//    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }


    @SuppressLint("StaticFieldLeak")
    class NetworkAsyncTask extends AsyncTask<String, Integer, String> {

        protected String doInBackground(String... params) {
            try {
                socket = new Socket(params[0], PORT);
                //reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                Thread.sleep(300);
                writer.print("Client Send!");
                writer.flush();
                listening = false;
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Log.d("b2wdebug", "listening");
//                        while (listening) {
//                            try {
//                                String s = reader.readLine();
//                                if (s == null) listening = false;
////                                recv(s);
//                            } catch (Exception e) {
//                                Log.d("b2wdebug", "listen thread error: " + e.toString());
//                                listening = false;
//                                break;
//                            }
//                        }
////                        activity_uithread.runOnUiThread(new Runnable() {
////                            @Override
////                            public void run() {
////                                disconnect();
////                            }
////                        }
////                        );
//                    }
//                }).start();
                return socket.toString();
            } catch (Exception e) {
                socket = null;
                if (writer != null){
                    writer.close();
                }
                return e.toString();
            }
        }

        protected void onPostExecute(String string) {
            Log.d("b2wdebug", "connect info: " + string);
            //text_connect_info.setText(string);
        }
    }

}
