package com.example.volumecontroller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaRecorder;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class Controller extends AppCompatActivity {

    private Button proximityButton, noiseButton, raiseVolume, lowerVolume;
    private SensorManager sensorManager;
    private Sensor sensor;
    private MediaRecorder mediaRecorder = null;
    private AudioManager audioManager = null;

    private int swipeCounter = 0, noiseCounter = 0;
    private int currentMediaVolume, maxMediaVolume, maxCallVolume;
    private int meanNoiseValue = 0, prevNoiseValue = 0;

    TimerTask timerTask;
    Timer timer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controller);


        proximityButton = (Button) findViewById(R.id.proximityButton);
        noiseButton = (Button) findViewById(R.id.noiseButton);
//        accelerationButton = (Button) findViewById(R.id.accelerationButton);
//        raiseVolume = (Button) findViewById(R.id.raiseVolume);
//        lowerVolume = (Button) findViewById(R.id.lowerVolume);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        currentMediaVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//        currentCallVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        maxMediaVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        maxCallVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);

        timerTask = new TimerTask() {
            @Override
            public void run() {
//                Log.i("get", "vol");
                int soundLevel = mediaRecorder.getMaxAmplitude();
                noiseCounter++;
                meanNoiseValue += soundLevel;
                if(noiseCounter == 4){
                    meanNoiseValue /= 4;
                    if(meanNoiseValue < 1000){
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxCallVolume / 4 , AudioManager.FLAG_SHOW_UI);
                    }
                    else if(meanNoiseValue >= 1000 && meanNoiseValue < 1500){
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxCallVolume / 2, AudioManager.FLAG_SHOW_UI);
                    }
                    else if(meanNoiseValue >= 1500 && meanNoiseValue < 2000){
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, (maxCallVolume * 3) / 4, AudioManager.FLAG_SHOW_UI);
                    }
                    else{
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxCallVolume, AudioManager.FLAG_SHOW_UI);
                    }
                    noiseCounter = 0;
                    meanNoiseValue = 0;
//                    prevNoiseValue = meanNoiseValue
                }
                Log.i("Media Level ", String.valueOf(soundLevel));
            }
        };

//        timer = new Timer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {
                        Manifest.permission.RECORD_AUDIO
                }, 10);
                return;
            }
        }
        else{
            sensorEventManager();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("inside", "reqpermission");
        switch (requestCode){
            case 10:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.i("Permissions", "Granted");
                    sensorEventManager();
                }
                return;
        }
    }

    private void volumeHandler(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                swipeCounter++;
                if(swipeCounter == 1){
                    try {
                        sleep(1200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.i("exception", e.getMessage());
                    }
                }
                else{
                    //two swipe so dec vol
                    Log.i("two swipe", "dec vol");
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, --currentMediaVolume, AudioManager.FLAG_SHOW_UI);
                    swipeCounter = 0;
                }

                if(swipeCounter == 1){
                    //only one swipe so inc vol
                    Log.i("one swipe", "inc vol");
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ++currentMediaVolume, AudioManager.FLAG_SHOW_UI);
                    swipeCounter = 0;
                }
            }
        }.start();
    }

    private void noiseVolumeHandle(){
//        if(timer != null){
//            timer = null;
//        }
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 1500);
    }



    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if(sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY){
                if(sensorEvent.values[0] == 0){
                    volumeHandler();
//                    Log.i("Sensor Change", "Near");
                }
                else{
//                    Log.i("Sensor Change", "Away");
                }
            }
//            else if(sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
//                double xAcceleration = sensorEvent.values[0], yAcceleration = sensorEvent.values[1], zAcceleration = sensorEvent.values[2];
//                Log.i("Acceleration ", xAcceleration + " - " + yAcceleration + " - " + zAcceleration);
////                Toast.makeText(getApplicationContext(), "Acceleration: " + xAcceleration + " - " + yAcceleration + " - " + zAcceleration, Toast.LENGTH_SHORT);
//            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            if(sensor.getType() == Sensor.TYPE_PROXIMITY){
                Log.i("Proximity Changing ", String.valueOf(i));
                Log.i("Proximity ", sensor + " ");
            }
//            else if(sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
//                Log.i("Acceleration Changing ", String.valueOf(i));
//            }
        }
    };

    private void sensorEventManager(){
        proximityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(timer != null){
                    timer.cancel();
                }
                Log.i("Proximity", "Clicked");
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
                Toast.makeText(getApplicationContext(), "Proximity Started", Toast.LENGTH_SHORT).show();
                if(sensor != null){
//                    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);//try to set sensor accuracy here
                    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_UI);

                }
                else{
                    Log.i("Sensor", "No Proximity Sensor");
                }
            }
        });

        noiseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sensorManager != null){
                    sensorManager.unregisterListener(sensorEventListener, sensor);
                }
                Log.i("Noise", "Clicked");

                if(mediaRecorder != null){
                    mediaRecorder = null;
                }

                    try {
                        mediaRecorder = new MediaRecorder();
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        mediaRecorder.setOutputFile("/dev/null");
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        Toast.makeText(getApplicationContext(), "Media Recorder Started", Toast.LENGTH_SHORT).show();
                        noiseVolumeHandle();
                        Log.i("Media Recoeder ", "Started");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("Exception", String.valueOf(e));
                    }

            }
        });

//        accelerationButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.i("Acceleration", "Clicked");
//                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
//
////                if(sensor != null){
////                    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);//try to set sensor accuracy here
////                }
////                else{
////                    Log.i("Sensor", "No Linear Acceleration");
////                }
//                sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);//try to set sensor accuracy here
//
//
//            }
//        });

//        raiseVolume.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.i("Click", "Raise Vol");
//                Log.i("Curr media vol value ", "" + currentMediaVolume);
//                Log.i("Max media vol value ", "" + maxMediaVolume);
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, ++currentMediaVolume, AudioManager.FLAG_SHOW_UI);
//            }
//        });
//
//        lowerVolume.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Log.i("Click", "Lower Vol");
//                Log.i("Curr media vol value ", "" + currentMediaVolume);
//                Log.i("Max media vol value ", "" + maxMediaVolume);
//                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, --currentMediaVolume, AudioManager.FLAG_SHOW_UI);
//            }
//        });

    }
}
