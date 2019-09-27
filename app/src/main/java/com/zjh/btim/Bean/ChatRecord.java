package com.zjh.btim.Bean;

public class ChatRecord {
    private String mac;
    private int tag;
    private String name;
    private String content;

    public ChatRecord() {
    }

    public ChatRecord(String mac, int tag, String name, String content) {
        this.mac = mac;
        this.tag = tag;
        this.name = name;
        this.content = content;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
