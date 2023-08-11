package com.vocsy.fakecall.adapter;

import static android.content.Context.ALARM_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.vocsy.fakecall.BuildConfig;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.model.UserModel;
import com.vocsy.fakecall.data.CallHistoryHelper;
import com.vocsy.fakecall.model.HistoryModels;
import com.vocsy.fakecall.ui.ContactScreenFragment;
import com.vocsy.fakecall.ui.MainScreenActivity;
import com.vocsy.fakecall.data.UserDatabase;
import com.vocsy.fakecall.receiver.VideoReceiver;
import com.vocsy.fakecall.receiver.VoiceReceiver;
import com.vocsy.fakecall.ui.AddContactScreenActivity;
import com.vocsy.fakecall.ui.VoiceCallScreenActivity;
import com.vocsy.fakecall.ui.VideoCallScreenActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {
    public static int selectedPerson = 0;
    public int mSelectedItem = -1;
    Activity mContext;
    List<UserModel> userModels = new ArrayList<>();
    List<UserModel> noChangeUserModels = new ArrayList<>();
    UserDatabase userDatabase;
    String TAG = "VideoAdapter";

    public VideoAdapter(Activity context, List<UserModel> userModels) {
        mContext = context;
        userDatabase = new UserDatabase(context);
        noChangeUserModels = userDatabase.retriveData();
        this.userModels = userModels;
    }

    public Bitmap getBitmapFromAsset(String path) {

        AssetManager am = mContext.getAssets();
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = am.open(path);
            bitmap = BitmapFactory.decodeStream(is);
        } catch (final IOException e) {
            bitmap = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return bitmap;
    }

    public Bitmap getBitmap(String path) {
        Bitmap bitmap = null;
        try {
            File f = new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    public void onBindViewHolder(VideoAdapter.ViewHolder viewHolder, @SuppressLint("RecyclerView") int i) {

        if (userModels.get(i).type.equals("Asset")) {
            RequestOptions requestOptions = new RequestOptions();
            requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners(40));


            Log.e("bitmap", "" + userModels.get(i).photo);
            Glide.with(mContext)
                    .load(getBitmapFromAsset("person/" + userModels.get(i).photo))
                    .apply(requestOptions)
                    .into(viewHolder.imageperson);

        } else {

            //viewHolder.imageperson.setImageBitmap(getBitmap(mItems.get(i).photo));
            RequestOptions requestOptions = new RequestOptions();
            requestOptions = requestOptions.transforms(new CenterCrop(), new RoundedCorners(40));

            Uri uri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".provider", new File(userModels.get(i).photo));

            Glide.with(mContext)
                    .load(uri)
                    .apply(requestOptions)
                    .into(viewHolder.imageperson);
        }

        viewHolder.nameofperson.setText(userModels.get(i).name);
        viewHolder.numberofperson.setText(userModels.get(i).phonenumber);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v) {
                mSelectedItem = i;

                Log.e(TAG, "onClick:old i " + i);
                for (int j = 0; j < noChangeUserModels.size(); j++) {
                    if (noChangeUserModels.get(j).getName().matches(userModels.get(i).getName())) {
                        selectedPerson = j;
                        Log.e(TAG, "onClick: for i " + i);
                        break;
                    }
                }

                ContactScreenFragment.myPosition = userModels.get(i).getId();

                Log.e(TAG, "onClick: avb " + userModels.get(i).getAvb());
                clickMethod(i);
                ContactScreenFragment.behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (i > 5) {
                    MenuHandling(viewHolder, i);
                }
                return false;
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void clickMethod(int i) {

        if (ContactScreenFragment.singleUserPosition < 6) {
            //ContactsBookFragment.editContactIV.setVisibility(View.GONE);
        }

        ContactScreenFragment.favouriteIV = ContactScreenFragment.bottomSheet.findViewById(R.id.favouriteIV);
        if (userModels.get(i).getFavourite().matches("0")) {
            ContactScreenFragment.favouriteIV.setImageResource(R.drawable.ic_without_fill_favourite);
        } else {
            ContactScreenFragment.favouriteIV.setImageResource(R.drawable.ic_fill_favourite);
        }

        ContactScreenFragment.favouriteLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userModels.get(i).getFavourite().matches("0")) {
                    ContactScreenFragment.favouriteIV.setImageResource(R.drawable.ic_without_fill_favourite);
                    ContactScreenFragment.database.updateFavorite(String.valueOf(userModels.get(i).getId()), "0");
                    userModels.get(i).setFavourite("0");
                    Log.e("TAG", "onClick: if");
                } else {
                    ContactScreenFragment.favouriteIV.setImageResource(R.drawable.ic_fill_favourite);
                    ContactScreenFragment.database.updateFavorite(String.valueOf(userModels.get(i).getId()), "1");
                    userModels.get(i).setFavourite("1");
                    Log.e("TAG", "onClick: else");
                }
            }
        });

        try {
            if (userModels.get(i).getType().equals("Asset")) {
                ContactScreenFragment.personImage.setImageBitmap(getBitmapFromAsset("person/" + userModels.get(i).getPhoto()));
            } else {
                Glide.with(mContext).load(new File(userModels.get(i).getPhoto())).into(ContactScreenFragment.personImage);
//                ContactsBookFragment.personImage.setImageBitmap(getBitmap(userModels.get(i).getPhoto()));
            }
        } catch (Exception e) {
            Log.e("exceptoin", "" + e.getMessage());
        }

        ContactScreenFragment.personName.setText(userModels.get(i).getName());
        ContactScreenFragment.numberofperson.setText(userModels.get(i).getPhonenumber());


        ((LinearLayout) ContactScreenFragment.bottomSheet.findViewById(R.id.personVidCallLay)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "onClick:zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz ");
                if (userModels.get(i).getAvb().equals("video") || userModels.get(i).getAvb().equals("both")) {
                    mContext.startActivity(new Intent(mContext, VideoCallScreenActivity.class));
                } else {
                    Toast.makeText(mContext, "Please add Video", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ((LinearLayout) ContactScreenFragment.bottomSheet.findViewById(R.id.personCallLay)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (userModels.get(i).getAvb().matches("audio") || userModels.get(i).getAvb().matches("both")) {
                    mContext.startActivity(new Intent(mContext, VoiceCallScreenActivity.class));
                } else {
                    Toast.makeText(mContext, "Please add Audio", Toast.LENGTH_SHORT).show();
                }

            }
        });

        ContactScreenFragment.TimerLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dialog = new Dialog(mContext);
                dialog.setContentView(R.layout.dialog_start_call_after);
                dialog.show();
                ContactScreenFragment.timeET = dialog.findViewById(R.id.timeET);
                ContactScreenFragment.submitvideocall = dialog.findViewById(R.id.submitvideocall);
                ContactScreenFragment.submitvoicecall = dialog.findViewById(R.id.submitvoicecall);

                ContactScreenFragment.submitvideocall.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v) {
                        Log.e(TAG, "onClick: avb " + userModels.get(i).getAvb());
                        if (userModels.get(i).getAvb().equals("video") || userModels.get(i).getAvb().equals("both")) {
                            callVidOrVoiceAlarm(0);
                            Log.e(TAG, "onClick: submitvideocall");
                        } else {
                            Toast.makeText(mContext, "Please add Video", Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    }
                });

                ContactScreenFragment.submitvoicecall.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v) {
                        if (userModels.get(i).getAvb().equals("audio") || userModels.get(i).getAvb().equals("both")) {
                            callVidOrVoiceAlarm(1);
                            Log.e(TAG, "onClick: submitvoicecall");
                        } else {
                            Toast.makeText(mContext, "Please add Audio", Toast.LENGTH_SHORT).show();
                        }

                        dialog.dismiss();

                    }
                });


            }
        });

        MainScreenActivity.historyOrNot = false;
        ContactScreenFragment.historyDatabase = new CallHistoryHelper(mContext);
        ContactScreenFragment.historyModels = ContactScreenFragment.historyDatabase.retriveData();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext, RecyclerView.VERTICAL, false);
        ContactScreenFragment.historyRecyclerView.setLayoutManager(layoutManager);
        ContactScreenFragment.historyAdapter = new HistoryAdapter(mContext, ContactScreenFragment.historyModels);

        ContactScreenFragment.historyRecyclerView.setAdapter(ContactScreenFragment.historyAdapter);
        setFilterData(userModels.get(i).getName());

        ContactScreenFragment.editContactIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i < 6) {
                    Toast.makeText(mContext, "Default persons cannot editable", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(mContext, AddContactScreenActivity.class);
                    intent.putExtra("setMode", "editContact");
                    intent.putExtra("userId", i);
                    mContext.startActivity(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return userModels.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        final View view = inflater.inflate(R.layout.contacts_item, viewGroup, false);

        return new ViewHolder(view);
    }

    private void MenuHandling(ViewHolder holder, int position) {
        PopupMenu popup = new PopupMenu(mContext, holder.nameofperson);
        popup.getMenuInflater().inflate(R.menu.menu_contact_detail, popup.getMenu());
        popup.getMenu().removeItem(R.id.action_settings);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @SuppressLint("NonConstantResourceId")
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.delete:
                        userDatabase.deleteData(String.valueOf(userModels.get(position).getId()));
                        userModels.remove(position);
                        notifyDataSetChanged();
                        break;
                    default:
                        Toast.makeText(mContext, "Something Want Wrong....!", Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });
        popup.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void callVidOrVoiceAlarm(int i) {

        try {
            ContactScreenFragment.delayTime = Integer.parseInt(String.valueOf(ContactScreenFragment.timeET.getText()));
        } catch (Exception e) {
            Log.e("exception", "" + e.getMessage());
            ContactScreenFragment.delayTime = 15;
        }

        if (i == 0) {
            ContactScreenFragment.pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, VideoReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_IMMUTABLE);
            Toast.makeText(mContext, "You will receive a video call at : " + ContactScreenFragment.delayTime + " Seconds", Toast.LENGTH_SHORT).show();
        } else {
            ContactScreenFragment.pendingIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, VoiceReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Toast.makeText(mContext, "You will receive a voice call at : " + ContactScreenFragment.delayTime + " Seconds", Toast.LENGTH_SHORT).show();
        }
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(ALARM_SERVICE);

        Log.e("TAG", "callVidOrVoiceAlarm: " + 1000 * ContactScreenFragment.delayTime);
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * ContactScreenFragment.delayTime), ContactScreenFragment.pendingIntent);
        } else if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * ContactScreenFragment.delayTime), ContactScreenFragment.pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000 * ContactScreenFragment.delayTime), ContactScreenFragment.pendingIntent);
        }

    }

    public void setFilterData(String text) {
        Log.e("TAG", "setFilterData: " + text);
        List<HistoryModels> songList = new ArrayList<>();
        for (HistoryModels song : ContactScreenFragment.historyModels) {
            if (song.getName().toLowerCase().contains(text.toLowerCase())) {
                songList.add(song);
            }
        }
        ContactScreenFragment.historyAdapter.FilterData(songList);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        public CircleImageView imageperson;
        TextView nameofperson;
        TextView numberofperson;

        public ViewHolder(final View inflate) {
            super(inflate);
            imageperson = (CircleImageView) inflate.findViewById(R.id.imageperson);
            nameofperson = (TextView) inflate.findViewById(R.id.nameofperson);
            numberofperson = (TextView) inflate.findViewById(R.id.numberofperson);
        }
    }

}
