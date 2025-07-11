package com.unade.lsm.utils;

public class GForceHandler {

    public enum DEVICE_MODEL {
        GFORCE_200,
        GFORCE_PRO
    }

    public static final int CMD_SET_EMG_RAWDATA_CONFIG = 0x3F;
    public static final int CMD_SET_DATA_NOTIF_SWITCH = 0x4F;
    public static final int CMD_GET_EMG_RAWDATA_CONFIG = 0x46;
    public static final int EMG_DATA_LEN = 8;
    public static final int EMG_NUM_CHANNELS = 8;

    public static byte[] setDataNotifSwitch(int flags) {
        byte[] data = new byte[5];
        data[0] = CMD_SET_DATA_NOTIF_SWITCH;
        data[1] = (byte) (0xFF & flags);
        data[2] = (byte) (0xFF & flags >> 8);
        data[3] = (byte) (0xFF & flags >> 16);
        data[4] = (byte) (0xFF & flags >> 24);
        return data;
    }

    public static byte[] setEmgRawDataConfig() {

        int sampRate = 200;
        int channelMask = 0xFF;
        int resolution = 8;

        byte[] data = new byte[7];
        data[0] = CMD_SET_EMG_RAWDATA_CONFIG;
        data[1] = (byte) (0xFF & sampRate);
        data[2] = (byte) (0xFF & sampRate >> 8);
        data[3] = (byte) (0xFF & channelMask);
        data[4] = (byte) (0xFF & channelMask >> 8);
        data[5] = (byte) (0xFF & EMG_DATA_LEN);
        data[6] = (byte) (0xFF & resolution);
        return data;
    }

    public static class NotifDataType {
        public static final int NTF_COMMAND = 0x00;
        public static final int NTF_ACC_DATA = 0x01;
        public static final int NTF_GYRO_DATA = 0x02;
        public static final int NTF_EULER_DATA = 0x04;
        public static final int NTF_QUAT_FLOAT_DATA = 0x05;
        public static final int NTF_EMG_ADC_DATA = 0x08;
    }

    public static class DataNotifFlags {
        public static final int DNF_OFF = 0;

        public static final int DNF_ACCELERATE = 1;

        public static final int DNF_GYROSCOPE = 2;

        public static final int DNF_MAGNETOMETER = 4;

        public static final int DNF_EULERANGLE = 8;

        public static final int DNF_QUATERNION = 16;

        public static final int DNF_ROTATIONMATRIX = 32;

        public static final int DNF_EMG_GESTURE = 64;

        public static final int DNF_EMG_RAW = 128;

        public static final int DNF_HID_MOUSE = 256;

        public static final int DNF_HID_JOYSTICK = 512;

        public static final int DNF_DEVICE_STATUS = 1024;

        public static final int DNF_LOG = 2048;

        public static final int DNF_ALL = -1;
    }

    public static class ResponseResult {
        public static final int RSP_CODE_SUCCESS = 0;

        public static final int RSP_CODE_NOT_SUPPORT = 1;

        public static final int RSP_CODE_BAD_PARAM = 2;

        public static final int RSP_CODE_FAILED = 3;

        public static final int RSP_CODE_TIMEOUT = 4;

        public static final int RSP_CODE_PARTIAL_PACKET = 255;
    }

}
