package com.hackathon.autolift;

import com.thalmic.myo.*;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private DeviceListener mListener = new AbstractDeviceListener() {
        @Override
        public void onConnect(Myo myo, long timestamp) {
            Toast.makeText(MainActivity.this, "Myo Connected!", Toast.LENGTH_SHORT).show();
            myo.unlock(Myo.UnlockType.HOLD);
        }

        @Override
        public void onDisconnect(Myo myo, long timestamp) {
            Toast.makeText(MainActivity.this, "Myo Disconnected!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
            // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
            float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
            float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
            float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
            // Adjust roll and pitch for the orientation of the Myo on the arm.
            if (myo.getXDirection() == XDirection.TOWARD_ELBOW) {
                roll *= -1;
                pitch *= -1;
            }
            System.out.println("Orientation at t = " + timestamp + ": " + roll + ", " + pitch + ", " + yaw);
        }

        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
            System.out.println("Acceleration at t = " + timestamp + ": " + accel.x() + ", " + accel.y() + ", " + accel.z());
        }

        @Override
        public void onGyroscopeData (Myo myo, long timestamp, Vector3 gyro) {
            System.out.println("Acceleration at t = " + timestamp + ": " + gyro.x() + ", " + gyro.y() + ", " + gyro.z());
            myo.requestRssi();
        }

        @Override
        public void onRssi(Myo myo, long timestamp, int rssi) {
            System.out.println("RSSI at t = " + timestamp + ": " + rssi);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Hub for Myo
        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            Toast.makeText(MainActivity.this, "Hub loaded unsuccessfully", Toast.LENGTH_SHORT).show();
            System.out.println("Hub loaded unsuccessfully");
            finish();
            return;
        }
        Toast.makeText(MainActivity.this, "Hub loaded successfully", Toast.LENGTH_SHORT).show();
        System.out.println("Hub loaded successfully");


        // Disable standard Myo locking policy. All poses will be delivered.
        hub.setLockingPolicy(Hub.LockingPolicy.NONE);

        // Next, register for DeviceListener callbacks.
        hub.addListener(mListener);

        // Finally, scan for Myo devices and connect to the first one found that is very near.
        hub.attachToAdjacentMyo();

    }
}
