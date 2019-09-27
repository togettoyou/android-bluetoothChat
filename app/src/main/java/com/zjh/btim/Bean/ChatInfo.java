package com.zjh.btim.Bean;

public class ChatInfo {
    public static final int TAG_LEFT = 0;
    public static final int TAG_RIGHT = 1;
    public static final int TAG_FILE_LEFT = 2;
    public static final int TAG_FILE_RIGHT = 3;
    private int tag;
    private String name;
    private String content;

    public ChatInfo(int tag, String name, String content) {
        this.tag = tag;
        this.name = name;
        this.content = content;
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
