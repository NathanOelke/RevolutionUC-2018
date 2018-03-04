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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private boolean fistClinched = false;
    private ArrayList<DataPoint> exerciseData;
    private long lastClinchChange = 0;
    private long minMilisecondsBetweenClinch = 1000;
    private long miliSecDeleteAtEnd = 1000;
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
            //System.out.println("Orientation at t = " + timestamp + ": " + roll + ", " + pitch + ", " + yaw);
        }

        @Override
        public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {

        }

        @Override
        public void onGyroscopeData (Myo myo, long timestamp, Vector3 gyro) {
            if (fistClinched) {
                exerciseData.add(new DataPoint(timestamp, gyro));
                System.out.println("Gyro at t = " + timestamp + ": " + gyro.x() + ", " + gyro.y() + ", " + gyro.z());
            }
        }

        @Override
        public void onRssi(Myo myo, long timestamp, int rssi) {
            //System.out.println("RSSI at t = " + timestamp + ": " + rssi);
        }

        @Override
        public void onPose(Myo myo, long timestamp, Pose pose) {
            System.out.println("Pose: " + pose);
            if (pose == Pose.WAVE_IN && timestamp - lastClinchChange > minMilisecondsBetweenClinch) {
                if (!fistClinched) {
                    System.out.println("Reps: Clinched");
                    fistClinched = true;
                    exerciseData = new ArrayList<DataPoint>();
                    lastClinchChange = timestamp;
                }
            } else if (pose == Pose.WAVE_OUT && timestamp - lastClinchChange > minMilisecondsBetweenClinch) {
                System.out.println("Reps: Released");
                fistClinched = false;
                lastClinchChange = timestamp;

                // Send data to be processed
                if (exerciseData != null && exerciseData.size() > 0) {
                    // Delete end data
                    long lastTime = exerciseData.get(exerciseData.size()-1).time;
                    for (int i = 0; i < exerciseData.size(); i++) {
                        if (lastTime - exerciseData.get(i).time < miliSecDeleteAtEnd) {
                            exerciseData.remove(exerciseData.get(i));
                        }
                    }

                    if (exerciseData.size() > 1) {
                        System.out.println("Num reps: " + countReps(Exercise.DEADLIFT, exerciseData, 175000));
                    }
                }
                exerciseData = new ArrayList<DataPoint>();
            }
        }
    };

    private int countReps(Exercise e, ArrayList<DataPoint> data, double dropThreshold) {
        int numReps = 0;

        try {
            if (e == Exercise.DEADLIFT) {
                double maxZ = 0;
                double Z = 0;
                boolean nearPeak = false;
                long tPrev = data.get(0).time;

                for (int i = 0; i < data.size(); i++) {
                    long dt = (data.get(i).time - tPrev);
                    Z += 0.5 * data.get(i).gryo.y() * dt*dt;
                    //System.out.println("Reps: Estimated Z = " + Z);

                    tPrev = data.get(i).time;

                    if (Z > maxZ) {
                        maxZ = Z;
                    }

                    // Check if Z is going over a hump
                    if (maxZ-Z > dropThreshold && nearPeak && Z > 0) {
                        numReps++;
                        nearPeak = false;
                    }

                    // Check if Z is coming up to a hump
                    if (maxZ-Z < dropThreshold) {
                        nearPeak = true;
                    }
                }
            }
        } catch (Exception e1) {
            System.out.println("Reps: Error!");
        }
        return numReps;
    }

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
