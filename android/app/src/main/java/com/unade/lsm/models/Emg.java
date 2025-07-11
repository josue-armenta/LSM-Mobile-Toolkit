package com.unade.lsm.models;

import static com.unade.lsm.utils.GForceHandler.EMG_NUM_CHANNELS;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public class Emg {
    private static final int CH0 = 0;
    private static final int CH1 = 1;
    private static final int CH2 = 2;
    private static final int CH3 = 3;
    private static final int CH4 = 4;
    private static final int CH5 = 5;
    private static final int CH6 = 6;
    private static final int CH7 = 7;

    @Nullable
    public static double[] getFromRawBytes(byte[] data) {
        if (data.length != 9) return null;

        double[] channels = new double[EMG_NUM_CHANNELS];
        for (int i = 0; i < channels.length; i++) {
//            ByteBuffer bf = ByteBuffer.wrap(new byte[]{data[i * 2 + 2], data[i * 2 + 1]});
            channels[i] = data[i + 1];

        }
        return channels;
    }
}
