package com.vocsy.fakecall;

import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.TextView;

import com.vocsy.fakecall.R;

public class ChatRecord extends ViewHolder {
    public TextView leftText;
    public TextView rightText;

    public ChatRecord(View itemView) {
        super(itemView);
        this.leftText = (TextView) itemView.findViewById(R.id.leftText);
        this.rightText = (TextView) itemView.findViewById(R.id.rightText);
    }
}
