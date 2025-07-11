package com.unade.lsm.models;

import androidx.annotation.Nullable;

public class EulerAngles {

    public static final int P = 0;
    public static final int R = 1;
    public static final int Y = 2;

    @Nullable
    public static double[] getFromRawBytes(byte[] data) {

        if (data.length != 13) return null;

        byte[] p = new byte[4];
        byte[] r = new byte[4];
        byte[] y = new byte[4];

        System.arraycopy(data, 1, p, 0, 4);
        System.arraycopy(data, 5, r, 0, 4);
        System.arraycopy(data, 9, y, 0, 4);

        float pitch = getFloat(p);
        float roll = getFloat(r);
        float yaw = getFloat(y);

        return new double[]{(double) pitch, (double) roll, (double) yaw};
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
