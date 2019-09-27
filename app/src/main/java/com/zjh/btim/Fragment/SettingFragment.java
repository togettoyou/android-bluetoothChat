package com.zjh.btim.Fragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.zjh.btim.CallBack.BlueToothInterface;
import com.zjh.btim.R;
import com.zjh.btim.Receiver.BluetoothStateBroadcastReceive;
import com.zjh.btim.Util.BluetoothUtil;
import com.zjh.btim.Util.PermissionUtil;

public class SettingFragment extends Fragment implements View.OnClickListener {

    private BluetoothUtil bluetoothUtil;
    private BluetoothStateBroadcastReceive broadcastReceive;
    private CardView cvBtSwitch;
    private TextView tvBt;
    private CardView cvBtSetting;
    private CardView cvBtQxSetting;
    private static final int COLOR_OPEN = Color.parseColor("#4DB6AC");
    private static final int COLOR_CLOSE = Color.parseColor("#a2a3a3");
    private static final String VALUE_OPEN = "蓝牙已打开\n点击关闭";
    private static final String VALUE_CLOSE = "蓝牙未打开\n点击开启";
    private BlueToothInterface blueToothInterface = new BlueToothInterface() {

        @Override
        public void getBlueToothDevices(BluetoothDevice device) {

        }

        @Override
        public void getConnectedBlueToothDevices(BluetoothDevice device) {

        }

        @Override
        public void getDisConnectedBlueToothDevices(BluetoothDevice device) {

        }

        @Override
        public void searchFinish() {

        }

        @Override
        public void open() {
            cvBtSwitch.setCardBackgroundColor(COLOR_OPEN);
            tvBt.setText(VALUE_OPEN);
        }

        @Override
        public void disable() {
            cvBtSwitch.setCardBackgroundColor(COLOR_CLOSE);
            tvBt.setText(VALUE_CLOSE);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bluetoothUtil = new BluetoothUtil(getContext());
        cvBtSwitch = view.findViewById(R.id.cv_bt_switch);
        cvBtSwitch.setOnClickListener(this);
        tvBt = view.findViewById(R.id.tv_bt);
        cvBtSetting = view.findViewById(R.id.cv_bt_setting);
        cvBtSetting.setOnClickListener(this);
        cvBtQxSetting = view.findViewById(R.id.cv_bt_qx_setting);
        cvBtQxSetting.setOnClickListener(this);
        initBtSwitch();
        registerBluetoothReceiver();
    }

    private void initBtSwitch() {
        if (bluetoothUtil.isBluetoothEnable()) {
            cvBtSwitch.setCardBackgroundColor(COLOR_OPEN);
            tvBt.setText(VALUE_OPEN);
        } else {
            cvBtSwitch.setCardBackgroundColor(COLOR_CLOSE);
            tvBt.setText(VALUE_CLOSE);
        }
    }

    private void registerBluetoothReceiver() {
        Log.i("zjh", "蓝牙广播监听启动");
        if (broadcastReceive == null) {
            broadcastReceive = new BluetoothStateBroadcastReceive(blueToothInterface);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        getContext().registerReceiver(broadcastReceive, intentFilter);
    }

    private void unregisterBluetoothReceiver() {
        Log.i("zjh", "蓝牙广播监听关闭");
        if (broadcastReceive != null) {
            getContext().unregisterReceiver(broadcastReceive);
            broadcastReceive = null;
        }
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cv_bt_switch:
                if (bluetoothUtil.isBluetoothEnable() && tvBt.getText().equals(VALUE_OPEN))
                    bluetoothUtil.disableBluetooth();
                else
                    bluetoothUtil.openBluetooth();
                break;
            case R.id.cv_bt_setting:
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
                break;
            case R.id.cv_bt_qx_setting:
                new PermissionUtil(getContext()).open();
                break;
        }
    }

    @Override
    public void onDestroy() {
        Log.i("zjh-onDestroy", "Setting关闭");
        super.onDestroy();
        unregisterBluetoothReceiver();
        bluetoothUtil.close();
    }
}
