package com.unade.lsm.models;

import androidx.annotation.Nullable;

public class Quaternion {

    public static final int W = 0;
    public static final int X = 1;
    public static final int Y = 2;
    public static final int Z = 3;

    @Nullable
    public static double[] getFromRawBytes(byte[] data) {

        if (data.length != 17) return null;

        byte[] w = new byte[4];
        byte[] x = new byte[4];
        byte[] y = new byte[4];
        byte[] z = new byte[4];

        System.arraycopy(data, 1, w, 0, 4);
        System.arraycopy(data, 5, x, 0, 4);
        System.arraycopy(data, 9, y, 0, 4);
        System.arraycopy(data, 13, z, 0, 4);

        float W = getFloat(w);
        float X = getFloat(x);
        float Y = getFloat(y);
        float Z = getFloat(z);

        return new double[]{(double) W, (double) X, (double) Y, (double) Z};
    }

    private static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum | (b[0] & 0xff);
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }
}
