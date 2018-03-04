package com.hackathon.autolift;

import com.thalmic.myo.Vector3;

/**
 * Created by Nathan on 3/3/18.
 */

public class DataPoint {
    public long time;
    public Vector3 acceleration;
    public Vector3 orientation;
    public Vector3 gryo;

    public DataPoint(long time, Vector3 gryo) {
        this.time = time;
        this.gryo = gryo;
    }
}
