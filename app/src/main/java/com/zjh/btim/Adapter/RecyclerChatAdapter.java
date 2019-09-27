package com.zjh.btim.Adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zjh.btim.Bean.ChatFileLeftHolder;
import com.zjh.btim.Bean.ChatFileRightHolder;
import com.zjh.btim.Bean.ChatInfo;
import com.zjh.btim.Bean.ChatLeftHolder;
import com.zjh.btim.Bean.ChatRightHolder;
import com.zjh.btim.R;
import com.zjh.btim.Util.OpenFileUtil;

import java.io.File;
import java.util.List;

public class RecyclerChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ChatInfo> list;
    private Context context;

    public List<ChatInfo> getList() {
        return list;
    }

    public void setList(List<ChatInfo> list) {
        this.list = list;
    }

    public RecyclerChatAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).getTag();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = null;
        switch (viewType) {
            case ChatInfo.TAG_LEFT:
                view = LayoutInflater.from(context).inflate(R.layout.recycler_chat_left, null);
                return new ChatLeftHolder(view);
            case ChatInfo.TAG_RIGHT:
                view = LayoutInflater.from(context).inflate(R.layout.recycler_chat_right, null);
                return new ChatRightHolder(view);
            case ChatInfo.TAG_FILE_LEFT:
                view = LayoutInflater.from(context).inflate(R.layout.recycler_file_chat_left, null);
                return new ChatFileLeftHolder(view);
            case ChatInfo.TAG_FILE_RIGHT:
                view = LayoutInflater.from(context).inflate(R.layout.recycler_file_chat_right, null);
                return new ChatFileRightHolder(view);
        }
        return new ChatLeftHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        String name = list.get(position).getName();
        final String content = list.get(position).getContent();
        switch (list.get(position).getTag()) {
            case ChatInfo.TAG_LEFT:
                ChatLeftHolder chatLeftHolder = (ChatLeftHolder) holder;
                chatLeftHolder.getTvName().setText(name);
                chatLeftHolder.getTvContent().setText(content);
                break;
            case ChatInfo.TAG_RIGHT:
                ChatRightHolder chatRightHolder = (ChatRightHolder) holder;
                chatRightHolder.getTvName().setText(name);
                chatRightHolder.getTvContent().setText(content);
                break;
            case ChatInfo.TAG_FILE_LEFT:
                ChatFileLeftHolder chatFileLeftHolder = (ChatFileLeftHolder) holder;
                chatFileLeftHolder.getTvName().setText(name);
                File mfile = new File(content);
                chatFileLeftHolder.getTvFileName().setText(mfile.getName());
                chatFileLeftHolder.getLayout_file().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OpenFileUtil.openFileByPath(context, content);
                    }
                });
                break;
            case ChatInfo.TAG_FILE_RIGHT:
                ChatFileRightHolder chatFileRightHolder = (ChatFileRightHolder) holder;
                chatFileRightHolder.getTvName().setText(name);
                File nfile = new File(content);
                chatFileRightHolder.getTvFileName().setText(nfile.getName());
                chatFileRightHolder.getLayout_file().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        OpenFileUtil.openFileByPath(context, content);
                    }
                });
                break;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
