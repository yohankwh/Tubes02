package com.example.tubes02;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

//import com.example.tubes02.TempRepo.AppConstants;
//import com.example.tubes02.TempRepo.GameActivity;

public class MainActivity extends AppCompatActivity implements FragmentListener, View.OnClickListener, SensorEventListener, OrientationManager.OrientationListener, IMainActivity {
    private FragmentManager fragmentManager;

    private HomeFragment homeFragment;
    private GameFragment gameFragment;
    private MediaPlayers mediaPlayer;
    private UIThreadHandler uiThreadHandler;
    private MoveThread mvThread;
    private boolean back;
    private boolean gameStart;

    private static final float VALUE_DRIFT = 0.05f;
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private float[] accelerometerReading = new float[3];
    private float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private int oritentation = -1;
    private OrientationManager orientationManager;

    // variables for shake detection
    private static final float SHAKE_THRESHOLD = 0.5f; // m/S**2
    private static final int MIN_TIME_BETWEEN_SHAKES_MILLISECS = 1000;
    private long mLastShakeTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.back = true;
        this.gameStart = false;

        this.fragmentManager = this.getSupportFragmentManager();
        FragmentTransaction ft = this.fragmentManager.beginTransaction();

        this.uiThreadHandler = new UIThreadHandler(this);

        this.homeFragment = new HomeFragment();
        this.gameFragment = new GameFragment();
        this.gameFragment.setUIThreadHandler(this.uiThreadHandler);
        this.mediaPlayer = new MediaPlayers();

        ft.add(R.id.fragment_container, this.homeFragment)
                .addToBackStack(null)
                .commit();

//        sensor
        this.mSensorManager = (SensorManager) getSystemService(
                Context.SENSOR_SERVICE);

        orientationManager = new OrientationManager(this, SensorManager.SENSOR_DELAY_NORMAL, this);
        orientationManager.enable();

        this.accelerometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.magnetometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (savedInstanceState != null) {
            savedInstanceState.getInt("count");
        }

    }

    @Override
    public void changePage(int page) {
        FragmentTransaction ft = this.fragmentManager.beginTransaction();
        if (page == 2) {
            this.playMusic(!back);
            if (this.gameFragment.isAdded()) {
                ft.show(this.gameFragment);


            } else {
                ft.add(R.id.fragment_container, this.gameFragment)
                        .addToBackStack(null);
            }
            if (this.homeFragment.isAdded()) {
                ft.hide(this.homeFragment);
            }
        } else if (page == 1) {
            if (this.homeFragment.isAdded()) {
                ft.show(this.homeFragment);
                ft.remove(this.gameFragment);
            }
        }
        ft.commit();
    }

    public void playMusic(boolean check) {
        Log.d("debugg", "laguintent");
        Intent intent = new Intent(this, MediaPlayers.class);
        if (check == true) {
            stopService(intent);
        } else {
            startService(intent);
        }

    }

    @Override
    public void closeApplication() {
        this.moveTaskToBack(true);
        this.finish();
    }

    @Override
    public void updateHighScore(int val) {
        saveToFile(val+"");
    }

    @Override
    public void loadHighScore() {
        String content = readFile();
        int val = 0;

        if(!content.equals("")){
            val = Integer.parseInt(content);
        }

        setHighScore(val);
    }

    @Override
    public void setHighScore(int val) {
        this.gameFragment.setHighScore(val);
    }

    public void move(int x, int y) {
        this.gameFragment.move(x, y);
    }

    @Override
    public void onBackPressed() {
//        this.moveThread.thread.interrupt();
        this.back = true;
        this.playMusic(this.back);
        this.gameFragment.stopThread();


//        finish();
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.d(this.getClass().getName(), "back button pressed");
            this.back = true;
            this.playMusic(this.back);
        }

        return super.onKeyDown(keyCode, event);
    }

    public boolean getBack() {
        return this.back;
    }

    public boolean isPassAllowed() {
        return this.gameFragment.allowPass;
    }

    public void resetGameCheck() {
        this.gameFragment.resetAllowPass();
    }

    @Override
    public void onClick(View v) {
        this.back = false;
    }

    @Override
    protected void onDestroy() {
        this.back = true;
        this.playMusic(this.back);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.accelerometer != null) {
            this.mSensorManager.registerListener(this, this.accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }

        if (this.magnetometer != null) {
            this.mSensorManager.registerListener(this, this.magnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        int sensorType = event.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                this.accelerometerReading = event.values.clone();
                if(gameStart){
                    long curTime = System.currentTimeMillis();
                    if ((curTime - mLastShakeTime) > MIN_TIME_BETWEEN_SHAKES_MILLISECS) {

                        float x = event.values[0];
                        float y = event.values[1];
                        float z = event.values[2];

                        double acceleration = Math.sqrt(Math.pow(x, 2) +
                                Math.pow(y, 2) +
                                Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH;

                        if (acceleration > SHAKE_THRESHOLD) {
                            mLastShakeTime = curTime;
                            this.uiThreadHandler.shake();
                        }
                    }
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                this.magnetometerReading = event.values.clone();
                break;
        }

        if (event.values[1] < 6.5 && event.values[1] > -6.5) {
            oritentation = 1;
        } else {
            oritentation = 0;
        }

        this.mSensorManager.getRotationMatrix(rotationMatrix, null, this.accelerometerReading, this.magnetometerReading);
        this.mSensorManager.getOrientation(rotationMatrix, orientationAngles);

        float azimuth = orientationAngles[0];
        float pitch = orientationAngles[1];
        float roll = orientationAngles[2];

        if (Math.abs(azimuth) < VALUE_DRIFT) {
            azimuth = 0;
        }
        if (Math.abs(pitch) < VALUE_DRIFT) {
            pitch = 0;
        }
        if (Math.abs(roll) < VALUE_DRIFT) {
            roll = 0;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onOrientationChange(OrientationManager.ScreenOrientation screenOrientation) {
        switch (screenOrientation) {
            case PORTRAIT:
                Log.d("oritentation", "reversed_potrait");
            case REVERSED_PORTRAIT:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                Log.d("oritentation", "potrait");
                break;
            case REVERSED_LANDSCAPE:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                Log.d("oritentation", "reversed_landscape");
                break;
            case LANDSCAPE:
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                Log.d("oritentation", "landscape");
                break;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);

    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        savedInstanceState.getInt("count");
    }

    @Override
    public void saveToFile(String content) {
        File file = new File(this.getFilesDir(),"score.txt");

        try (FileOutputStream fop = new FileOutputStream(file)) {
            if (!file.exists()) {
                file.createNewFile();
            }

            byte[] contentInBytes = content.getBytes();

            fop.write(contentInBytes);
            fop.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String readFile() {
        File file = new File(this.getFilesDir(),"score.txt");

        try (FileInputStream fis = new FileInputStream(file)) {

            int content;
            String msg = "";
            while ((content = fis.read()) != -1) {
                msg=msg+(char)content;
            }
            return msg;

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public void addBonusPoint(){
        this.gameFragment.addBonusScore();
    }

    public void setGameStart(){
        this.gameStart = true;
    }
    public void setGameStop(){
        this.gameStart = false;
        Toast toast = Toast.makeText(this,"Game Over!\nYour Score: "+this.gameFragment.getScore(), Toast.LENGTH_SHORT);
        toast.show();
    }
    public boolean getGameState(){
        return this.gameStart;
    }
}