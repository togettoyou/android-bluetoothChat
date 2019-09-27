package com.zjh.btim.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.zjh.btim.Adapter.RecyclerChatAdapter;
import com.zjh.btim.Bean.ChatInfo;
import com.zjh.btim.Bean.ChatRecord;
import com.zjh.btim.R;
import com.zjh.btim.SQLite.DBManager;
import com.zjh.btim.Service.BluetoothChatService;
import com.zjh.btim.Util.FileUtils;
import com.zjh.btim.Util.XPermissionUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_READ;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_READ_FILE;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_READ_FILE_NOW;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_TOAST;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_WRAITE;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_WRAITE_FILE;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_WRAITE_FILE_NOW;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String DEVICE_NAME_INTENT = "device_name";
    public static final String DEVICE_MAC_INTENT = "device_mac";
    private static final int UPDATE_DATA = 0x666;
    private String deviceName;
    private String deviceMac;
    private BluetoothChatService bluetoothChatService;
    private ProgressDialog dialog;
    private DBManager dbManager;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BLUE_TOOTH_TOAST:
                    Snackbar.make(et_write, (String) msg.obj, Snackbar.LENGTH_LONG).show();
                    Timer timer = new Timer();
                    TimerTask tast = new TimerTask() {
                        @Override
                        public void run() {
                            finish();
                            dbManager.closeDB();
                        }
                    };
                    timer.schedule(tast, 1500);
                    break;
                case BLUE_TOOTH_READ:
                    String readMessage = (String) msg.obj;
                    Log.i("zjh聊天", deviceName + ":" + readMessage);
                    list.add(new ChatInfo(ChatInfo.TAG_LEFT, deviceName, readMessage));
                    recyclerChatAdapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(list.size());
                    dbManager.add(new ChatRecord(deviceMac, ChatInfo.TAG_LEFT, deviceName, readMessage));
                    break;
                case BLUE_TOOTH_WRAITE:
                    String writeMessage = (String) msg.obj;
                    Log.i("zjh聊天", "我" + ":" + writeMessage);
                    list.add(new ChatInfo(ChatInfo.TAG_RIGHT, "我", writeMessage));
                    recyclerChatAdapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(list.size());
                    dbManager.add(new ChatRecord(deviceMac, ChatInfo.TAG_RIGHT, "我", writeMessage));
                    break;
                case BLUE_TOOTH_READ_FILE_NOW:
                    Log.i("zjh蓝牙文件传输", msg.obj + "");
                    Snackbar.make(et_write, (String) msg.obj, Snackbar.LENGTH_LONG).show();
                    break;
                case BLUE_TOOTH_WRAITE_FILE_NOW:
                    Log.i("zjh蓝牙文件传输", msg.obj + "");
                    if (msg.obj.toString().equals("文件发送失败")) {
                        dialog.dismiss();
                        Snackbar.make(et_write, (String) msg.obj, Snackbar.LENGTH_LONG).show();
                    } else {
                        dialog.setMessage(msg.obj + "");
                        dialog.setCancelable(false);
                        dialog.show();
                    }
                    break;
                case BLUE_TOOTH_READ_FILE:
                    Log.i("zjh蓝牙文件传输", "文件接收完成(" + msg.obj + ")");
                    list.add(new ChatInfo(ChatInfo.TAG_FILE_LEFT, deviceName, msg.obj + ""));
                    recyclerChatAdapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(list.size());
                    dbManager.add(new ChatRecord(deviceMac, ChatInfo.TAG_FILE_LEFT, deviceName, msg.obj + ""));
                    break;
                case BLUE_TOOTH_WRAITE_FILE:
                    Log.i("zjh蓝牙文件传输", "文件发送完成");
                    dialog.dismiss();
                    list.add(new ChatInfo(ChatInfo.TAG_FILE_RIGHT, "我", msg.obj + ""));
                    recyclerChatAdapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(list.size());
                    dbManager.add(new ChatRecord(deviceMac, ChatInfo.TAG_FILE_RIGHT, "我", msg.obj + ""));
                    break;
                case UPDATE_DATA:
                    recyclerChatAdapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(list.size());
                    break;
            }
        }
    };

    private boolean is_bt_add = true;
    private RecyclerView recyclerView;
    private List<ChatInfo> list;
    private RecyclerChatAdapter recyclerChatAdapter;
    private ImageButton bt_send;
    private ImageButton bt_add;
    private RelativeLayout layout_add;
    private EditText et_write;
    private ImageView ivFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        deviceName = intent.getStringExtra(DEVICE_NAME_INTENT);
        deviceMac = intent.getStringExtra(DEVICE_MAC_INTENT);
        setTitle(deviceName);
        bluetoothChatService = BluetoothChatService.getInstance(handler);
        dbManager = new DBManager(this);
        initView();
        List<ChatRecord> chatRecordList = dbManager.query(deviceMac);
        for (ChatRecord i : chatRecordList)
            list.add(new ChatInfo(i.getTag(), i.getName(), i.getContent()));
        recyclerChatAdapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(list.size());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            exit();
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothChatService.stop();
        dbManager.closeDB();
    }

    private void initView() {
        dialog = new ProgressDialog(this);
        list = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setFocusable(true);
        recyclerView.setFocusableInTouchMode(true);
        recyclerView.requestFocus();
        recyclerChatAdapter = new RecyclerChatAdapter(this);
        recyclerChatAdapter.setList(list);
        recyclerView.setAdapter(recyclerChatAdapter);
        et_write = findViewById(R.id.et_write);
        et_write.setOnClickListener(this);
        et_write.setFocusable(true);
        et_write.setFocusableInTouchMode(true);
        et_write.requestFocus();
        bt_send = findViewById(R.id.bt_send);
        bt_send.setOnClickListener(this);
        bt_send.setClickable(false);
        bt_add = findViewById(R.id.bt_add);
        bt_add.setOnClickListener(this);
        bt_add.setBackgroundResource(R.drawable.add);
        layout_add = findViewById(R.id.layout_add);
        layout_add.setVisibility(View.GONE);
        ivFile = findViewById(R.id.ivFile);
        ivFile.setOnClickListener(this);
        et_write.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                bt_send.setBackgroundResource(R.drawable.nosend);
                bt_send.setClickable(false);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String write = et_write.getText().toString().trim();
                if (TextUtils.isEmpty(write))
                    return;
                bt_send.setBackgroundResource(R.drawable.send);
                bt_send.setClickable(true);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_send:
                send();
                break;
            case R.id.bt_add:
                hintKeyboard();
                if (is_bt_add) {
                    bt_add.setBackgroundResource(R.drawable.noadd);
                    layout_add.setVisibility(View.VISIBLE);
                    is_bt_add = false;
                } else {
                    bt_add.setBackgroundResource(R.drawable.add);
                    layout_add.setVisibility(View.GONE);
                    is_bt_add = true;
                }
                recyclerChatAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(list.size());
                break;
            case R.id.et_write:
                bt_add.setBackgroundResource(R.drawable.add);
                layout_add.setVisibility(View.GONE);
                is_bt_add = true;
                break;
            case R.id.ivFile:
                showFileChooser();
                break;
        }
    }

    private void hintKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive() && getCurrentFocus() != null) {
            if (getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    private void send() {
        String write = et_write.getText().toString().trim();
        if (TextUtils.isEmpty(write)) {
            Snackbar.make(et_write, "发送内容不能为空", Snackbar.LENGTH_LONG).show();
            return;
        }
        bluetoothChatService.sendData(write);
        et_write.setText("");
    }

    private void sendFile(final String file) {
        XPermissionUtil.requestPermissions(this, 1,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                new XPermissionUtil.OnPermissionListener() {
                    @Override
                    public void onPermissionGranted() {
                        //权限获取成功
                        bluetoothChatService.sendFile(file);
                    }

                    @Override
                    public void onPermissionDenied() {
                        //权限获取失败
                        Snackbar.make(et_write, "请手动到设置界面给予相关权限", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private static final int FILE_SELECT_CODE = 0;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    final String path = FileUtils.getInstance(this).getChooseFileResultPath(uri);
                    Log.i("zjh-选择文件", "File Path: " + path);
                    if (path != null) {
                        AlertDialog.Builder ad = new AlertDialog.Builder(ChatActivity.this);
                        ad.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                sendFile(path);
                            }
                        });
                        ad.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        ad.setMessage("你确定要发送" + path + "吗?");
                        ad.setTitle("提示");
                        ad.setCancelable(false);
                        ad.show();
                    }
                }
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
            exit();
        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        AlertDialog.Builder ad = new AlertDialog.Builder(ChatActivity.this);
        ad.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                dbManager.closeDB();
            }
        });
        ad.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setNeutralButton("最小化", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                home.addCategory(Intent.CATEGORY_HOME);
                startActivity(home);
            }
        });
        ad.setMessage("你确定要断开连接吗？");
        ad.setTitle("提示");
        ad.setCancelable(false);
        ad.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        XPermissionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
