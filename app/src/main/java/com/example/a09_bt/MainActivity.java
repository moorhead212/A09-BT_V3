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
import android.os.AsyncTask;
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

    private boolean isConnected = false, forwardDown = false, leftDown = false, rightDown = false, reverseDown = false, motor3LeftDown = false, motor3RightDown = false;
    private AsyncTask asyncTask = null;

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
        binding.slPower2.setProgress(50);

        // Set initial value of TextView to match default value of SeekBar
        binding.vvSlider1.setText("Power: " + binding.slPower.getProgress());
        binding.vvSlider2.setText("Power: " + binding.slPower2.getProgress());

        binding.slPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged1 = 0;


            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged1 = progress;
                binding.vvSlider1.setText("Power: " + progressChanged1);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        binding.slPower2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged2 = 0;


            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                progressChanged2 = progress;
                binding.vvSlider2.setText("Power: " + progressChanged2);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Saving the hex code for default button color
        int defaultButtonColor = 0xD6D6D6D6;

        binding.grantPermissions.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {


                    case MotionEvent.ACTION_DOWN:

                        cpf_requestBTPermissions();
                        binding.vvTvOut2.setText("Last Command: Grant Permissions");
                        binding.grantPermissions.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        binding.grantPermissions.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                        return true;
                }
                return false;
            }
        });

        binding.findEV3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {


                    case MotionEvent.ACTION_DOWN:

                        cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
                        binding.vvTvOut2.setText("Last Command: Search for " + CV_ROBOTNAME);
                        binding.findEV3.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:

                        binding.findEV3.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                        return true;
                }
                return false;
            }
        });

        binding.connection.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                switch (motionEvent.getAction()) {


                    case MotionEvent.ACTION_DOWN:

                        if (isConnected == false && cv_btDevice != null) {
                            cpf_connectToEV3(cv_btDevice);
                            binding.vvTvOut2.setText("Last Command: Connect to " + CV_ROBOTNAME);
                            binding.connection.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                            binding.connection.setImageDrawable(getDrawable(R.drawable.baseline_power_24));
                            isConnected = true;
                        } else {
                            cpf_disconnFromEV3(cv_btDevice);
                            binding.vvTvOut2.setText("Last Command: Disconnect from " + CV_ROBOTNAME);
                            binding.connection.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                            binding.connection.setImageDrawable(getDrawable(R.drawable.baseline_power_off_24));
                            isConnected = false;
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                }
                return false;
            }
        });

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
                                binding.vvTvOut2.setText("Last Command: Move Reverse");
                                binding.reverse.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                reverseDown = true;
                                startTask(binding.slPower.getProgress());
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
                                reverseDown = false;
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
                                binding.vvTvOut2.setText("Last Command: Move Forward");
                                binding.forward.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                forwardDown = true;
                                startTask(binding.slPower.getProgress());
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
                                forwardDown = false;
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
                                binding.vvTvOut2.setText("Last Command: Move Right");
                                binding.right.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                rightDown = true;
                                startTask(binding.slPower.getProgress());
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
                                rightDown = false;
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
                                binding.vvTvOut2.setText("Last Command: Move Left");
                                binding.left.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                leftDown = true;
                                startTask(binding.slPower.getProgress());
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
                                leftDown = false;
                                return true;
                        }
                        return false;
                    }
                }
        );

        binding.m3Right.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.m3Right, "scaleX", SCALE_FACTOR),
                                        ObjectAnimator.ofFloat(binding.m3Right, "scaleY", SCALE_FACTOR)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.vvTvOut2.setText("Last Command: Motor 3 Move Right");
                                binding.m3Right.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                motor3RightDown = true;
                                startTask(binding.slPower2.getProgress());
                                return true;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.m3Right, "scaleX", 1f),
                                        ObjectAnimator.ofFloat(binding.m3Right, "scaleY", 1f)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.m3Right.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                                motor3RightDown = false;
                                return true;
                        }
                        return false;
                    }
                }
        );

        binding.m3Left.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {

                        switch (motionEvent.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.m3Left, "scaleX", SCALE_FACTOR),
                                        ObjectAnimator.ofFloat(binding.m3Left, "scaleY", SCALE_FACTOR)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.vvTvOut2.setText("Last Command: Motor 3 Move Left");
                                binding.m3Left.setBackgroundTintList(ColorStateList.valueOf(Color.GREEN));
                                motor3LeftDown = true;
                                startTask(binding.slPower2.getProgress());
                                return true;
                            case MotionEvent.ACTION_UP:
                            case MotionEvent.ACTION_CANCEL:
                                set.playTogether(
                                        ObjectAnimator.ofFloat(binding.m3Left, "scaleX", 1f),
                                        ObjectAnimator.ofFloat(binding.m3Left, "scaleY", 1f)
                                );
                                set.setDuration(150);
                                set.start();
                                binding.m3Left.setBackgroundTintList(ColorStateList.valueOf(defaultButtonColor));
                                motor3LeftDown = false;
                                return true;
                        }
                        return false;
                    }
                }
        );
    }

    public void startTask(int power) {
        asyncTask = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                if (forwardDown || reverseDown || leftDown || rightDown || motor3RightDown || motor3LeftDown) {
                    while (forwardDown) {
                        try {
                            Thread.sleep(100);
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    cpf_forward(power);
                                }
                            });

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    while (reverseDown) {
                        try {
                            Thread.sleep(100);
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    cpf_goBackward(power);
                                }
                            });

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    while (rightDown) {
                        try {
                            Thread.sleep(10);
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    cpf_goRight(power);
                                }
                            });

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    while (leftDown) {
                        try {
                            Thread.sleep(10);
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    cpf_goLeft(power);
                                }
                            });

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    while (motor3RightDown) {
                        try {
                            Thread.sleep(10);
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    cpf_motor3clockwise(power);
                                }
                            });

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    while (motor3LeftDown) {
                        try {
                            Thread.sleep(10);
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    cpf_motor3cc(power);
                                }
                            });

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }

        }.execute();
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
            case R.id.menu_fourth: cpf_forward(binding.slPower.getProgress());
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
    private void cpf_forward(int power) {
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
            buffer[11] = (byte) power;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0xB4;
            buffer[15] = (byte) 0x00;

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

    private void cpf_goBackward(int power) {
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
            buffer[11] = (byte) -power;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0xB4;
            buffer[15] = (byte) 0x00;

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

    private void cpf_goLeft(int power) {
        try {
            byte[] buffer = new byte[33];       // 0x12 command length

            buffer[0] = (byte) (33-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 31;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xae;
            buffer[8] = 0;

            buffer[9] = (byte) 0x04;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) power;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0x05;
            buffer[15] = (byte) 0x00;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0x05;
            buffer[18] = (byte) 0x00;

            buffer[19] = 1;

            buffer[20] = (byte) 0xae;
            buffer[21] = 0;

            buffer[22] = (byte) 0x02;

            buffer[23] = (byte) 0x81;
            buffer[24] = (byte) -power;

            buffer[25] = 0;

            buffer[26] = (byte) 0x82;
            buffer[27] = (byte) 0x05;
            buffer[28] = (byte) 0x00;

            buffer[29] = (byte) 0x82;
            buffer[30] = (byte) 0x05;
            buffer[31] = (byte) 0x00;

            buffer[32] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            binding.vvTvOut1.setText("Error in TurnLeft(" + e.getMessage() + ")");
        }
    }

    private void cpf_goRight(int power) {
        try {
            byte[] buffer = new byte[33];       // 0x12 command length

            buffer[0] = (byte) (33-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 31;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xae;
            buffer[8] = 0;

            buffer[9] = (byte) 0x02;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) power;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0x05;
            buffer[15] = (byte) 0x00;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0x05;
            buffer[18] = (byte) 0x00;

            buffer[19] = 1;

            buffer[20] = (byte) 0xae;
            buffer[21] = 0;

            buffer[22] = (byte) 0x04;

            buffer[23] = (byte) 0x81;
            buffer[24] = (byte) -power;

            buffer[25] = 0;

            buffer[26] = (byte) 0x82;
            buffer[27] = (byte) 0x05;
            buffer[28] = (byte) 0x00;

            buffer[29] = (byte) 0x82;
            buffer[30] = (byte) 0x05;
            buffer[31] = (byte) 0x00;

            buffer[32] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            binding.vvTvOut1.setText("Error in TurnRight(" + e.getMessage() + ")");
        }
    }

    // 4.2.5 Play a 1Kz tone at level 2 for 1 sec.

    private void cpf_motor3clockwise(int power) {
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

            buffer[9] = (byte) 0x01;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) power;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0xB4;
            buffer[15] = (byte) 0x00;

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

    private void cpf_motor3cc(int power) {
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

            buffer[9] = (byte) 0x01;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) -power;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0xB4;
            buffer[15] = (byte) 0x00;

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