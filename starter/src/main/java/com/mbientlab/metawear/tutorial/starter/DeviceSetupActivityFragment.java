/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.mbientlab.metawear.tutorial.starter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.*;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.SensorFusionBosch;

import bolts.Continuation;
import bolts.Task;

/**
 * A placeholder fragment containing a simple view.
 */
public class DeviceSetupActivityFragment extends Fragment implements ServiceConnection {

    /**
     * Add any new variables in the lines below
     */
    // ****************************************
    public Accelerometer accelerometer;
    public SensorFusionBosch sensorFusionBosch;
    TextView acclReadOut;
    TextView angleReadOut;
    TextView countReadOut;
    TextView timerReadOut;
    double xAccl;
    double yAccl;
    double zAccl;
    double roll;
    double pitch;
    double yaw;
    int count;
    int minutes;
    int seconds;
    long countdownLength = 15000;
    long timeInMilliSeconds = countdownLength;
    boolean timerRunning = false;
    // this is a media player to play sounds
    MediaPlayer alert;
    CountDownTimer timer;
    CountDownTimer timer2;

    // ****************************************


    public interface FragmentSettings {
        BluetoothDevice getBtDevice();
    }

    private MetaWearBoard metawear = null;
    private FragmentSettings settings;

    public DeviceSetupActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        alert = MediaPlayer.create(getContext(), R.raw.sanford);
        count = 0;
        timerRunning = false;
        timer2 = new CountDownTimer(timeInMilliSeconds, 1000) {
            @Override
            public void onTick(long l) {
                timeInMilliSeconds = l;
                minutes = (int) timeInMilliSeconds / 60000;
                seconds = (int) timeInMilliSeconds % 60000 / 1000;

                String timeLeft = "" + minutes;
                timeLeft += ":";
                if(seconds < 10) {
                    timeLeft += "0";
                }
                timeLeft += seconds;
                if(timeInMilliSeconds < 2000) {
                    alert.start();
                }
                System.out.println("time in milis: " + timeInMilliSeconds);
                timerReadOut.setText(timeLeft);
            }

            @Override
            public void onFinish() {
                timeInMilliSeconds = countdownLength;
            }
        };


        Activity owner= getActivity();
        if (!(owner instanceof FragmentSettings)) {
            throw new ClassCastException("Owning activity must implement the FragmentSettings interface");
        }

        settings= (FragmentSettings) owner;
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ///< Unbind the service when the activity is destroyed
        getActivity().getApplicationContext().unbindService(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_device_setup, container, false);

    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        metawear = ((BtleService.LocalBinder) service).getMetaWearBoard(settings.getBtDevice());

        //adding an accelerometer

        accelerometer = metawear.getModule(Accelerometer.class);
        accelerometer.configure()
                .odr(25f)       // Set sampling frequency to 25Hz, or closest valid ODR
                .commit();

        // adding a sensorFusion to read Euler Angles

        sensorFusionBosch = metawear.getModule(SensorFusionBosch.class);
        sensorFusionBosch.configure()
                .mode(SensorFusionBosch.Mode.NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_8G)
                .commit();


    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /**
     * Called when the app has reconnected to the board
     */
    public void reconnected() { }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        acclReadOut = (TextView) view.findViewById(R.id.accelerationReadOut);
        angleReadOut =  (TextView) view.findViewById(R.id.angleReadOut);
        countReadOut = (TextView) view.findViewById(R.id.countReadOut);
        timerReadOut = (TextView) view.findViewById(R.id.timerReadOut);
        countReadOut.setText("");
        angleReadOut.setBackgroundColor(getResources().getColor(R.color.bigXAcceleration));

        view.findViewById(R.id.acc_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //startTimer();

                // ******************************
                // start accelerometer
                // ******************************
                accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                Log.i("MainActivity", data.value(Acceleration.class).toString());
                                count++;
                                //acclReadOut.setText(data.value(Acceleration.class).toString());




                                xAccl = data.value(Acceleration.class).x();
                                yAccl = data.value(Acceleration.class).y();
                                zAccl = data.value(Acceleration.class).z();

                                if(xAccl > 1 || xAccl < -1) {
                                    acclReadOut.setBackgroundColor(getResources().getColor(R.color.bigXAcceleration));
                                    //alert.start();
                                }
                                else {
                                    acclReadOut.setBackgroundColor(getResources().getColor(R.color.white));
                                }


                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        accelerometer.acceleration().start();
                        accelerometer.start();
                        return null;

                    }
                });

                // ******************************
                // Finish accelerometer
                //******************************
                // *****************************
                // start euler angles
                // *****************************
                sensorFusionBosch.eulerAngles().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                Log.i("MainActivity", data.value(EulerAngles.class).toString());

                                roll = data.value(EulerAngles.class).roll();
                                pitch = data.value(EulerAngles.class).pitch();
                                yaw = data.value(EulerAngles.class).yaw();
                                angleReadOut.setText("Roll: " + Math.round(roll));
                                //timerReadOut.setText("" + timeInMilliSeconds);
                                //timerReadOut.setText(updateTimer());
                                //System.out.println(timeInMilliSeconds);


                                if(roll < -10 ) {
                                    angleReadOut.setBackgroundColor(getResources().getColor(R.color.bigYAcceleration));
                                    timerReadOut.setBackgroundColor(getResources().getColor(R.color.test));
                                    angleReadOut.setText("Roll: " + Math.round(roll));

                                    if(!timerRunning) {

                                        timerRunning = true; //shouldn't need this...
                                        timer2.start();
                                    }





                                }
                                else {
                                    angleReadOut.setBackgroundColor(getResources().getColor(R.color.white));
                                    timerReadOut.setBackgroundColor(getResources().getColor(R.color.white));
                                    if(timerRunning) {
                                        timer2.cancel();
                                        timerRunning = false;
                                        alert.stop();
                                        alert.prepareAsync();
                                    }

                                }
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {
                        sensorFusionBosch.eulerAngles().start();
                        sensorFusionBosch.start();
                        return null;

                    }
                });

                // *****************************
                // finish euler angles
                // *****************************



            }
        });
        view.findViewById(R.id.acc_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorFusionBosch.stop();
                sensorFusionBosch.eulerAngles().stop();
                accelerometer.stop();
                accelerometer.acceleration().stop();
                metawear.tearDown();
                timeInMilliSeconds = countdownLength;
            }
        });
    }

}
