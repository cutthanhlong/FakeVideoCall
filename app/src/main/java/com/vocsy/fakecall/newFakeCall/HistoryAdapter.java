package com.vocsy.fakecall.newFakeCall;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vocsy.fakecall.ui.AudioCallActivity;
import com.vocsy.fakecall.ui.VideoCallActivity;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.UserModel;
import com.vocsy.fakecall.VideoAdapter;
import com.vocsy.fakecall.ui.HistoryFragment;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.MyClass> {
    Context context;
    List<HistoryModels> historyModels;
    CallHistoryHelper helper;
    List<UserModel> userModels = new ArrayList<>();
    public int selectedPerson = 0;
    UserDatabase userDatabase;

    myInterface myInterface;
    public static Boolean historySelectDetail=false;


    public HistoryAdapter(Context context, List<HistoryModels> historyModels) {
        this.context = context;
        this.historyModels = historyModels;
        helper = new CallHistoryHelper(context);
        userDatabase = new UserDatabase(context);
        this.userModels = userDatabase.retriveData();
        myInterface= (com.vocsy.fakecall.newFakeCall.myInterface) context;
    }

    @NonNull
    @Override
    public MyClass onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false);
        MyClass m = new MyClass(view);
        return m;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(@NonNull MyClass holder, int position) {
        holder.name_history.setText(historyModels.get(position).getName());
        holder.number_history.setText(historyModels.get(position).getMobile_number());
        holder.date_and_time.setText(historyModels.get(position).getDate() + " - " + historyModels.get(position).getTime());

        if (historyModels.get(position).getIs_Miscall() == 0) {
            holder.misCall_history.setImageResource(R.drawable.ic_missed_call);
            holder.misCall_history.setImageTintMode(PorterDuff.Mode.SRC_IN);
            holder.misCall_history.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
        } else {
            holder.misCall_history.setImageResource(R.drawable.ic_incoming_call);
            holder.misCall_history.setImageTintMode(PorterDuff.Mode.SRC_IN);
            holder.misCall_history.setColorFilter(ContextCompat.getColor(context, R.color.green));
        }
        if (historyModels.get(position).getAv() == 0) {
            holder.av_history.setImageResource(R.drawable.ic_call);
            holder.av_history.setImageTintMode(PorterDuff.Mode.SRC_IN);
            holder.av_history.setColorFilter(ContextCompat.getColor(context, R.color.black));
        } else {
            holder.av_history.setImageResource(R.drawable.ic_new_video_call);
            holder.av_history.setImageTintMode(PorterDuff.Mode.SRC_IN);
            holder.av_history.setColorFilter(ContextCompat.getColor(context, R.color.black));
        }

        if (MainActivity.historyOrNot==true){
            holder.detail_history.setVisibility(View.VISIBLE);
            Log.e("TAG", "onBindViewHolder: if" );
        }else {
            holder.detail_history.setVisibility(View.GONE);
            Log.e("TAG", "onBindViewHolder: else" );
        }

        holder.detail_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = historyModels.get(position).getName();
                for (int i = 0; i < userModels.size(); i++) {
                    if (userModels.get(i).getName().matches(name)) {
                        VideoAdapter.selectedPerson=i;
                        break;
                    }
                }

                myInterface.showDetailFragment();
                historySelectDetail=true;

            }
        });



        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = historyModels.get(position).getName();
                for (int i = 0; i < userModels.size(); i++) {
                    if (userModels.get(i).getName().matches(name)) {
                        VideoAdapter.selectedPerson=i;
                        break;
                    }
                }
                if (historyModels.get(position).getAv() == 0) {
                    context.startActivity(new Intent(context, AudioCallActivity.class));
                } else {
                    context.startActivity(new Intent(context, VideoCallActivity.class));
                }
            }
        });

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                MenuHandling(holder, position);
                return false;
            }
        });


    }

    public void FilterData(List<HistoryModels> modelData) {
        historyModels = modelData;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return historyModels.size();
    }

    public class MyClass extends RecyclerView.ViewHolder {
        ImageView av_history, misCall_history, detail_history;
        TextView name_history, number_history, date_and_time;

        public MyClass(@NonNull View itemView) {
            super(itemView);
            misCall_history = itemView.findViewById(R.id.misCall_history);
            av_history = itemView.findViewById(R.id.av_history);
            name_history = itemView.findViewById(R.id.name_history);
            number_history = itemView.findViewById(R.id.number_history);
            date_and_time = itemView.findViewById(R.id.date_and_time);
            detail_history = itemView.findViewById(R.id.detail_history);
        }
    }


    private void MenuHandling(MyClass holder, int position) {
        PopupMenu popup = new PopupMenu(context, holder.detail_history);
        popup.getMenuInflater().inflate(R.menu.menu_contact_detail, popup.getMenu());
        popup.getMenu().removeItem(R.id.action_settings);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @SuppressLint("NonConstantResourceId")
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete:
                        helper.deleteData(String.valueOf(historyModels.get(position).getId()));
                        Toast.makeText(context, historyModels.get(position).getName() + " was Removed", Toast.LENGTH_SHORT).show();
                        historyModels.remove(position);
                        notifyDataSetChanged();

                        HistoryFragment.historyTextMethod();
                        Log.e("TAG", "onBindViewHolder size: " + historyModels.size());
                        break;
                    default:
                        Toast.makeText(context, "Something Want Wrong....!", Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });
        popup.show();
    }
}
