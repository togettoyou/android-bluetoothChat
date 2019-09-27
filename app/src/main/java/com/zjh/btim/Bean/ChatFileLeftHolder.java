package com.zjh.btim.Bean;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zjh.btim.R;

public class ChatFileLeftHolder extends RecyclerView.ViewHolder {

    private LinearLayout layout_file;
    private TextView tvFileName;
    private TextView tvName;

    public ChatFileLeftHolder(View itemView) {
        super(itemView);
        layout_file = (LinearLayout) itemView.findViewById(R.id.layout_left);
        tvFileName = (TextView) itemView.findViewById(R.id.tv_left);
        tvName = (TextView) itemView.findViewById(R.id.tv_device);
    }

    public TextView getTvFileName() {
        return tvFileName;
    }

    public LinearLayout getLayout_file() {
        return layout_file;
    }

    public void setLayout_file(LinearLayout layout_file) {
        this.layout_file = layout_file;
    }

    public void setTvFileName(TextView tvFileName) {
        this.tvFileName = tvFileName;
    }

    public TextView getTvName() {
        return tvName;
    }

    public void setTvName(TextView tvName) {
        this.tvName = tvName;
    }
}
