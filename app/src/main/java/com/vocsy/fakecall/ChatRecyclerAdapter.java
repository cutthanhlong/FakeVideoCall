package com.vocsy.fakecall;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.vocsy.fakecall.R;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

public class ChatRecyclerAdapter extends Adapter<ChatRecord> {
    private Context c;
    private List<ChatMessage> chatlist;

    public ChatRecyclerAdapter(Context c, List<ChatMessage> chatlist) {
        this.c = c;
        this.chatlist = chatlist;
    }

    public ChatRecord onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ChatRecord(LayoutInflater.from(parent.getContext()).inflate(R.layout.msglist, parent, false));
    }

    public void onBindViewHolder(ChatRecord holder, int position) {
        final ChatMessage chat = (ChatMessage) this.chatlist.get(position);
        if (chat.getMsgUser().equals("user")) {
            holder.rightText.setText(chat.getMsgText());
            holder.rightText.setVisibility(View.VISIBLE);
            holder.leftText.setVisibility(View.GONE);
        }
        if (chat.getMsgUser().equals("bot")) {
            holder.leftText.setText(chat.getMsgText());
            holder.rightText.setVisibility(View.GONE);
            holder.leftText.setVisibility(View.VISIBLE);
            if (isValid(chat.getMsgText())) {
                holder.leftText.setTextColor(-16776961);
                holder.leftText.setOnClickListener(new OnClickListener() {
                    public void onClick(View view) {
                        String url = chat.getMsgText();
                        Intent i = new Intent("android.intent.action.VIEW");
                        i.setData(Uri.parse(url));
                        ChatRecyclerAdapter.this.c.startActivity(i);
                    }
                });
            }
        }
    }

    public static boolean isValid(String url) {
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean checkifurl(String url) {
        if (Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$").matcher(url).find()) {
            return true;
        }
        return false;
    }

    public int getItemCount() {
        return this.chatlist.size();
    }
}
