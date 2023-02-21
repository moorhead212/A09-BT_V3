package com.example.a09_bt;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import com.example.a09_bt.databinding.ActivityMainBinding;

import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

//// HERE
import java.io.InputStream;
import java.io.OutputStream;

// ver02 -- permission and connect
// ver03 -- connect to EV3
// ver04 -- sending Direct Command

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;


    // BT Variables
    private final String CV_ROBOTNAME = "WALL-E";
    private BluetoothAdapter cv_btInterface = null;
    private Set<BluetoothDevice> cv_pairedDevices = null;
    private BluetoothDevice cv_btDevice = null;
    private BluetoothSocket cv_btSocket = null;

    //// HERE
    // Data stream to/from NXT bluetooth
    private InputStream cv_is = null;
    private OutputStream cv_os = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setting up the Animation for click and release buttons
        final float SCALE_FACTOR = 1.2f;
        final AnimatorSet set = new AnimatorSet();

        // Need grant permission once per install
        cpf_checkBTPermissions();

        // Set default value for SeekBar
        binding.slPower.setProgress(50);

        // Set initial value of TextView to match default value of SeekBar
        binding.vvSlider.setText("Power: " + binding.slPower.getProgress());

        binding.slPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged = progress;
                binding.vvSlider.setText("Power: " + progressChanged);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Saving the hex code for default button color
        int defaultButtonColor = 0xD6D6D6D6;

        binding.reverse.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {

                        switch (motionEvent.getAction()) {


                            case MotionEvent.ACTION_DOWN:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.reverse, "scaleX", SCALE_FACTOR),
                                        ObjectAnimator.ofFloat(binding.reverse, "scaleY", SCALE_FACTOR)
                                );
                                set.setDuration(150);
                                set.start();
                                cpf_goBackward();
                                binding.vvTvOut2.setText("Last Command: Move Backward");
                                binding.reverse.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                return true;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.reverse, "scaleX", 1f),
                                        ObjectAnimator.ofFloat(binding.reverse, "scaleY", 1f)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.reverse.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                                return true;
                        }
                        return false;
                    }
                }
        );

        binding.forward.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.forward, "scaleX", SCALE_FACTOR),
                                        ObjectAnimator.ofFloat(binding.forward, "scaleY", SCALE_FACTOR)
                                );
                                set.setDuration(150);
                                set.start();
                                cpf_forward();
                                binding.vvTvOut2.setText("Last Command: Move Forward");
                                binding.forward.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                return true;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.forward, "scaleX", 1f),
                                        ObjectAnimator.ofFloat(binding.forward, "scaleY", 1f)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.forward.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                                return true;
                        }
                        return false;
                    }
                }
        );

        binding.right.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.right, "scaleX", SCALE_FACTOR),
                                        ObjectAnimator.ofFloat(binding.right, "scaleY", SCALE_FACTOR)
                                );
                                set.setDuration(150);
                                set.start();
                                cpf_goRight();
                                binding.vvTvOut2.setText("Last Command: Move right");
                                binding.right.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                return true;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.right, "scaleX", 1f),
                                        ObjectAnimator.ofFloat(binding.right, "scaleY", 1f)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.right.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                                return true;
                        }
                        return false;
                    }
                }
        );

        binding.left.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.left, "scaleX", SCALE_FACTOR),
                                        ObjectAnimator.ofFloat(binding.left, "scaleY", SCALE_FACTOR)
                                );
                                set.setDuration(150);
                                set.start();
                                cpf_goLeft();
                                binding.vvTvOut2.setText("Last Command: Move Left");
                                binding.left.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                return true;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.left, "scaleX", 1f),
                                        ObjectAnimator.ofFloat(binding.left, "scaleY", 1f)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.left.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                                return true;
                        }
                        return false;
                    }
                }
        );


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_first: cpf_requestBTPermissions();
                return true;
            case R.id.menu_second: cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
                return true;
            case R.id.menu_third: cpf_connectToEV3(cv_btDevice);
                return true;
            case R.id.menu_fourth: cpf_forward();
                return true;
            case R.id.menu_fifth: cpf_EV3PlayTone();
                return true;
            case R.id.menu_sixth: cpf_disconnFromEV3(cv_btDevice);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void cpf_checkBTPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            binding.vvTvOut1.setText("BLUETOOTH_SCAN already granted.\n");
        }
        else {
            binding.vvTvOut1.setText("BLUETOOTH_SCAN NOT granted.\n");
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            binding.vvTvOut2.setText("BLUETOOTH_CONNECT NOT granted.\n");
        }
        else {
            binding.vvTvOut2.setText("BLUETOOTH_CONNECT already granted.\n");
        }
    }

    // https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
    private void cpf_requestBTPermissions() {
        // We can give any value but unique for each permission.
        final int BLUETOOTH_SCAN_CODE = 100;
        final int BLUETOOTH_CONNECT_CODE = 101;

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    BLUETOOTH_SCAN_CODE);
        }
        else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_SCAN already granted", Toast.LENGTH_SHORT) .show();
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_CONNECT_CODE);
        }
        else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_CONNECT already granted", Toast.LENGTH_SHORT) .show();
        }
    }

    // Modify from chap14, pp390 findRobot()
    private BluetoothDevice cpf_locateInPairedBTList(String name) {
        BluetoothDevice lv_bd = null;
        try {
            cv_btInterface = BluetoothAdapter.getDefaultAdapter();
            cv_pairedDevices = cv_btInterface.getBondedDevices();
            Iterator<BluetoothDevice> lv_it = cv_pairedDevices.iterator();
            while (lv_it.hasNext())  {
                lv_bd = lv_it.next();
                if (lv_bd.getName().equalsIgnoreCase(name)) {
                    binding.vvTvOut1.setText(name + " is in paired list");
                    return lv_bd;
                }
            }
            binding.vvTvOut1.setText(name + " is NOT in paired list");
        }
        catch (Exception e) {
            binding.vvTvOut1.setText("Failed in findRobot() " + e.getMessage());
        }
        return null;
    }

    // Modify frmo chap14, pp391 connectToRobot()
    private void cpf_connectToEV3(BluetoothDevice bd) {
        try  {
            cv_btSocket = bd.createRfcommSocketToServiceRecord
                    (UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            cv_btSocket.connect();

            //// HERE
            cv_is = cv_btSocket.getInputStream();
            cv_os = cv_btSocket.getOutputStream();
            binding.vvTvOut2.setText("Connect to " + bd.getName() + " at " + bd.getAddress());
        }
        catch (Exception e) {
            binding.vvTvOut2.setText("Error interacting with remote device [" +
                    e.getMessage() + "]");
        }
    }

    private void cpf_disconnFromEV3(BluetoothDevice bd) {
        try {
            cv_btSocket.close();
            cv_is.close();
            cv_os.close();
            binding.vvTvOut2.setText(bd.getName() + " is disconnect " );
        } catch (Exception e) {
            binding.vvTvOut2.setText("Error in disconnect -> " + e.getMessage());
        }
    }

    // Communication Developer Kit Page 27
    // 4.2.2 Start motor B & C forward at power 50 for 3 rotation and braking at destination
    private void cpf_forward() {
        try {
            byte[] buffer = new byte[20];       // 0x12 command length

            buffer[0] = (byte) (20-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xae;
            buffer[8] = 0;

            buffer[9] = (byte) 0x06;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) 0x32;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0x84;
            buffer[15] = (byte) 0x03;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0xB4;
            buffer[18] = (byte) 0x00;

            buffer[19] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            binding.vvTvOut1.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    private void cpf_goBackward() {
        try {
            byte[] buffer = new byte[20];       // 0x12 command length

            buffer[0] = (byte) (20-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xae;
            buffer[8] = 0;

            buffer[9] = (byte) 0x06;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) -0x32;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0x84;
            buffer[15] = (byte) 0x03;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0xB4;
            buffer[18] = (byte) 0x00;

            buffer[19] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            binding.vvTvOut1.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    private void cpf_goLeft() {
        try {
            byte[] buffer = new byte[20];       // 0x12 command length

            buffer[0] = (byte) (20-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xae;
            buffer[8] = 0;

            buffer[9] = (byte) 0x04;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) 0x32;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0xC2;
            buffer[15] = (byte) 0x01;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0x5A;
            buffer[18] = (byte) 0x00;

            buffer[19] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            binding.vvTvOut1.setText("Error in TurnLeft(" + e.getMessage() + ")");
        }
    }

    private void cpf_goRight() {
        try {
            byte[] buffer = new byte[20];       // 0x12 command length

            buffer[0] = (byte) (20-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xae;
            buffer[8] = 0;

            buffer[9] = (byte) 0x02;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) 0x32;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0xC2;
            buffer[15] = (byte) 0x01;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0x5A;
            buffer[18] = (byte) 0x00;

            buffer[19] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            binding.vvTvOut1.setText("Error in TurnRight(" + e.getMessage() + ")");
        }
    }

    // 4.2.5 Play a 1Kz tone at level 2 for 1 sec.
    private void cpf_EV3PlayTone() {
        try {
            byte[] buffer = new byte[17];       // 0x0f command length

            buffer[0] = (byte) (17-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0x94;
            buffer[8] = 1;

            buffer[9] = (byte) 0x81;
            buffer[10] = (byte) 0x02;

            buffer[11] = (byte) 0x82;
            buffer[12] = (byte) 0xe8;
            buffer[13] = (byte) 0x03;

            buffer[14] = (byte) 0x82;
            buffer[15] = (byte) 0xe8;
            buffer[16] = (byte) 0x03;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            binding.vvTvOut2.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    private void go_forward() {

    }
}