package com.unade.lsm.models;

import androidx.annotation.Nullable;

public class Accelerometer {

    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;

    @Nullable
    public static double[] getFromRawBytes(byte[] data) {

        if (data.length != 13) return null;

        byte[] x = new byte[4];
        byte[] y = new byte[4];
        byte[] z = new byte[4];

        System.arraycopy(data, 1, x, 0, 4);
        System.arraycopy(data, 5, y, 0, 4);
        System.arraycopy(data, 9, z, 0, 4);

        float X = getFloat(x);
        float Y = getFloat(y);
        float Z = getFloat(z);

        return new double[]{(double) X, (double) Y, (double) Z};
    }

    private static float getFloat(byte[] b) {
        int accum = 0;
        accum = accum | (b[0] & 0xff);
        accum = accum | (b[1] & 0xff) << 8;
        accum = accum | (b[2] & 0xff) << 16;
        accum = accum | (b[3] & 0xff) << 24;
        return accum / 65536.0F;
    }
}
