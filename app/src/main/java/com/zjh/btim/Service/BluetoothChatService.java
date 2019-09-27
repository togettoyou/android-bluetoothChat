/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zjh.btim.Service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.zjh.btim.Activity.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private int mState;
    // 显示当前连接状态
    public static final int STATE_NONE = 0;       // 什么都不做
    public static final int STATE_LISTEN = 1;     // 监听连接
    public static final int STATE_CONNECTING = 2; // 正在建立连接
    public static final int STATE_TRANSFER = 3;  // 现在连接到一个远程的设备，可以进行传输

    //用来向主线程发送消息
    private static Handler uiHandler;
    private BluetoothAdapter bluetoothAdapter;
    //用来连接端口的线程
    private AcceptThread mAcceptThread;
    private TransferThread mTransferThread;
    private ConnectThread mConnectThread;
    private boolean isTransferError = false;


    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BTIMBluetooth/";
    private static final int FLAG_MSG = 0;  //消息标记
    private static final int FLAG_FILE = 1; //文件标记

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    //获取单例
    public static volatile BluetoothChatService instance = null;

    public static BluetoothChatService getInstance(Handler handler) {
        uiHandler = handler;
        if (instance == null) {
            synchronized (BluetoothChatService.class) {
                if (instance == null) {
                    instance = new BluetoothChatService();
                }
            }
        }
        return instance;
    }

    public BluetoothChatService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    /**
     * 开启服务监听
     */
    public synchronized void start() {
        if (mTransferThread != null) {
            mTransferThread.cancel();
            mTransferThread = null;
        }

        setState(STATE_LISTEN);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.e(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        if (mTransferThread != null) {
            mTransferThread.cancel();
            mTransferThread = null;
        }

        setState(STATE_NONE);
    }

    public void setState(int state) {
        this.mState = state;
    }

    /**
     * 连接访问
     *
     * @param device
     */
    public synchronized void connectDevice(BluetoothDevice device) {
        Log.e(TAG, "connectDevice: ");
        // 如果有正在传输的则先关闭
        if (mState == STATE_CONNECTING) {
            if (mTransferThread != null) {
                mTransferThread.cancel();
                mTransferThread = null;
            }
        }

        //如果有正在连接的则先关闭
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        sendMessageToUi(MainActivity.BLUE_TOOTH_DIALOG, "正在与" + device.getName() + "连接");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        //标志为正在连接
        setState(STATE_CONNECTING);
    }

    //连接等待线程
    class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            //获取服务器监听端口
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = tmp;
        }

        @Override
        public void run() {
            super.run();
            //监听端口
            BluetoothSocket socket = null;
            while (mState != STATE_TRANSFER) {
                try {
                    Log.e(TAG, "run: AcceptThread 阻塞调用，等待连接");
                    socket = serverSocket.accept();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: ActivityThread fail");
                    break;
                }
                //获取到连接Socket后则开始通信
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //传输数据，服务器端调用
                                Log.e(TAG, "run: 服务器AcceptThread传输");
                                sendMessageToUi(MainActivity.BLUE_TOOTH_DIALOG, "正在与" + socket.getRemoteDevice().getName() + "连接");
                                dataTransfer(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_TRANSFER:
                                // 没有准备好或者终止连接
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket" + e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            Log.e(TAG, "close: activity Thread");
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "close: activity Thread fail");
            }
        }
    }

    private void sendMessageToUi(int what, Object s) {
        Message message = uiHandler.obtainMessage();
        message.what = what;
        message.obj = s;
        uiHandler.sendMessage(message);
    }

    /**
     * 开始连接通讯
     *
     * @param socket
     * @param remoteDevice 远程设备
     */
    private void dataTransfer(BluetoothSocket socket, final BluetoothDevice remoteDevice) {
        //关闭连接线程，这里只能连接一个远程设备
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // 启动管理连接线程和开启传输
        mTransferThread = new TransferThread(socket);
        mTransferThread.start();
        //标志状态为连接
        setState(STATE_TRANSFER);
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isTransferError) {
                    sendMessageToUi(MainActivity.BLUE_TOOTH_SUCCESS, remoteDevice);
                }
            }
        }, 300);
    }

    /**
     * 传输数据
     *
     * @param msg
     */
    public void sendData(String msg) {
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        r.write(msg);
    }

    /**
     * 传输文件
     *
     * @param filePath
     */
    public void sendFile(String filePath) {
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        r.writeFile(filePath);
    }


    /**
     * 用来传输数据的线程
     */
    class TransferThread extends Thread {
        private final BluetoothSocket socket;
        private final OutputStream out;
        private final DataOutputStream OutData;
        private final InputStream in;
        private final DataInputStream inData;


        public TransferThread(BluetoothSocket mBluetoothSocket) {
            socket = mBluetoothSocket;
            OutputStream mOutputStream = null;
            InputStream mInputStream = null;
            try {
                if (socket != null) {
                    //获取连接的输入输出流
                    mOutputStream = socket.getOutputStream();
                    mInputStream = socket.getInputStream();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            out = mOutputStream;
            OutData = new DataOutputStream(out);
            in = mInputStream;
            inData = new DataInputStream(in);
            isTransferError = false;
        }

        @Override
        public void run() {
            super.run();
            //读取数据
            while (true) {
                try {
                    switch (inData.readInt()) {
                        case FLAG_MSG: //读取短消息
                            String msg = inData.readUTF();
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ, msg);
                            break;
                        case FLAG_FILE: //读取文件
                            File destDir = new File(FILE_PATH);
                            if (!destDir.exists())
                                destDir.mkdirs();
                            String fileName = inData.readUTF(); //文件名
                            long fileLen = inData.readLong(); //文件长度
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ_FILE_NOW, "正在接收文件(" + fileName + ")");
                            // 读取文件内容
                            long len = 0;
                            int r;
                            byte[] b = new byte[4 * 1024];
                            FileOutputStream out = new FileOutputStream(FILE_PATH + fileName);
                            while ((r = in.read(b)) != -1) {
                                out.write(b, 0, r);
                                len += r;
                                if (len >= fileLen)
                                    break;
                            }
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ_FILE, FILE_PATH + fileName);
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: Transform error" + e.toString());
                    BluetoothChatService.this.start();
                    //TODO 连接丢失显示并重新开始连接
                    sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "设备连接失败/传输关闭");
                    isTransferError = true;
                    break;
                }
            }
        }

        /**
         * 写入数据传输
         *
         * @param msg
         */
        public void write(final String msg) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        OutData.writeInt(FLAG_MSG); //消息标记
                        OutData.writeUTF(msg);
                    } catch (Throwable e) {
                        Log.i("zjh蓝牙消息传输", "发送失败");
                    }
                    sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE, msg);
                }
            });
        }

        /**
         * 发送文件
         */
        public void writeFile(final String filePath) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE_FILE_NOW, "正在发送文件(" + filePath + ")");
                        FileInputStream in = new FileInputStream(filePath);
                        File file = new File(filePath);
                        OutData.writeInt(FLAG_FILE); //文件标记
                        OutData.writeUTF(file.getName()); //文件名
                        OutData.writeLong(file.length()); //文件长度
                        int r;
                        byte[] b = new byte[4 * 1024];
                        while ((r = in.read(b)) != -1) {
                            OutData.write(b, 0, r);
                        }
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE_FILE, filePath);
                    } catch (Throwable e) {
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE_FILE_NOW, "文件发送失败");
                    }
                }
            });
        }


        public void cancel() {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed" + e);
            }
        }
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket mSocket = null;
            try {
                //建立通道
                mSocket = device.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ConnectThread: fail");
                sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "连接失败，请重新连接");
            }
            socket = mSocket;
        }

        @Override
        public void run() {
            super.run();
            //建立后取消扫描
            bluetoothAdapter.cancelDiscovery();

            try {
                Log.e(TAG, "run: connectThread 等待");
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    Log.e(TAG, "run: unable to close");
                }
                //TODO 连接失败显示
                sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "连接失败，请重新连接");
                BluetoothChatService.this.start();
            }


            // 重置
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }
            //Socket已经连接上了，默认安全,客户端才会调用
            Log.e(TAG, "run: connectThread 连接上了,准备传输");
            dataTransfer(socket, device);
        }

        public void cancel() {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
