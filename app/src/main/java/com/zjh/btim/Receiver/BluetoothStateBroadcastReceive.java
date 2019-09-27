package com.zjh.btim.Receiver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.zjh.btim.CallBack.BlueToothInterface;

public class BluetoothStateBroadcastReceive extends BroadcastReceiver {

    private BlueToothInterface blueToothInterface;

    public BluetoothStateBroadcastReceive(BlueToothInterface blueToothInterface) {
        this.blueToothInterface = blueToothInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                Log.i("zjh蓝牙设备", "扫描到设备" + device.getName());
                blueToothInterface.getBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.i("zjh蓝牙设备", "搜索完成");
                blueToothInterface.searchFinish();
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.i("zjh蓝牙设备", device.getName() + "已连接");
                blueToothInterface.getConnectedBlueToothDevices(device);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.i("zjh蓝牙设备", device.getName() + "已断开");
                blueToothInterface.getDisConnectedBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i("zjh蓝牙设备", "蓝牙已关闭");
                        blueToothInterface.disable();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i("zjh蓝牙设备", "蓝牙已开启");
                        blueToothInterface.open();
                        break;
                }
                break;
        }
    }
}
