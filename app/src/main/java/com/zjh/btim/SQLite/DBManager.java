package com.zjh.btim.SQLite;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.zjh.btim.Bean.ChatRecord;

import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private DBHelper helper;
    private SQLiteDatabase db;

    public DBManager(Context context) {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
    }

    public void add(ChatRecord chatRecord) {
        db.beginTransaction();
        try {
            db.execSQL("INSERT INTO ChatRecord VALUES(?,?, ?, ?)",
                    new Object[]{chatRecord.getMac(), chatRecord.getTag(),
                            chatRecord.getName(), chatRecord.getContent()});
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<ChatRecord> query(String mac) {
        ArrayList<ChatRecord> list = new ArrayList<>();
        Cursor c = queryTheCursor(mac);
        while (c.moveToNext()) {
            ChatRecord chatRecord = new ChatRecord();
            chatRecord.setMac(c.getString(c.getColumnIndex("mac")));
            chatRecord.setTag(c.getInt(c.getColumnIndex("tag")));
            chatRecord.setName(c.getString(c.getColumnIndex("name")));
            chatRecord.setContent(c.getString(c.getColumnIndex("content")));
            list.add(chatRecord);
        }
        c.close();
        return list;
    }

    public Cursor queryTheCursor(String mac) {
        Cursor c = db.rawQuery("SELECT * FROM ChatRecord WHERE mac=?", new String[]{mac});
        return c;
    }


    public void closeDB() {
        db.close();
    }
}
