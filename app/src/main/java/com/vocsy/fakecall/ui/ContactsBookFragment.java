package com.vocsy.fakecall.ui;

import static android.content.Context.ALARM_SERVICE;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.UserModel;
import com.vocsy.fakecall.VideoAdapter;
import com.vocsy.fakecall.newFakeCall.CallHistoryHelper;
import com.vocsy.fakecall.newFakeCall.HistoryAdapter;
import com.vocsy.fakecall.newFakeCall.HistoryModels;
import com.vocsy.fakecall.newFakeCall.MainActivity;
import com.vocsy.fakecall.newFakeCall.UserDatabase;
import com.vocsy.fakecall.receiver.VideoReceiver;
import com.vocsy.fakecall.receiver.VoiceReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContactsBookFragment extends Fragment {

    public RecyclerView mRecyclerView;
    public static VideoAdapter mVoiceAdapter;
    FloatingActionButton addContactFAB;
    static List<UserModel> userModels = new ArrayList<>();
    public static UserDatabase database;
    public static View bottomSheet;
    public static BottomSheetBehavior behavior;
    public static TextView personName;
    CoordinatorLayout coordinatorLayout;
    public static ImageView personImage;
    public static final String[] REQUIRED_SDK_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    public static PendingIntent pendingIntent;
    public static ImageView favouriteIV;
    public static LinearLayout favouriteLay;
    public static ImageView submitvideocall;
    public static ImageView submitvoicecall;

    public static EditText timeET;
    public static int delayTime = 15;
    public static View view;
    public static LinearLayout TimerLay;
    public static TextView numberofperson;
    public static int myPosition = 0;
    public static List<UserModel> singleUserModel = new ArrayList<>();

    public static RecyclerView historyRecyclerView;
    public static HistoryAdapter historyAdapter;
    public static List<HistoryModels> historyModels = new ArrayList<>();
    public static CallHistoryHelper historyDatabase;
    public static CollapsingToolbarLayout toolBarLayout;
    public static int singleUserPosition = 0;
    public static LinearLayout editContactIV;
    public static LinearLayout messageLay;

    List<UserModel> favouriteUserModels = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts_book, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        database = new UserDatabase(getContext());
        userModels = database.retriveData();
        addContactFAB = view.findViewById(R.id.addContactFAB);

        bottomSheet = view.findViewById(R.id.bottom_sheet);
        coordinatorLayout = view.findViewById(R.id.cordinatorLayout);
        bottomSheetMethod();

        addContactFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(getContext(), AddPersonClickActivity.class));
                getActivity().finish();
            }
        });

        mRecyclerView = view.findViewById(R.id.classification_select);

        if (MainActivity.favouriteOrNot) {
            for (int i = 0; i < userModels.size(); i++) {
                if (userModels.get(i).getFavourite().matches("1")) {

                    UserModel model = new UserModel();
                    model.setId(userModels.get(i).getId());
                    model.setName(userModels.get(i).getName());
                    model.setPhonenumber(userModels.get(i).getPhonenumber());
                    model.setPhoto(userModels.get(i).getPhoto());
                    model.setBackground(userModels.get(i).getBackground());
                    model.setVideo(userModels.get(i).getVideo());
                    model.setType(userModels.get(i).getType());
                    model.setEmail(userModels.get(i).getEmail());
                    model.setAudio(userModels.get(i).getAudio());
                    model.setFavourite(userModels.get(i).getFavourite());
                    model.setAvb(userModels.get(i).getAvb());

                    favouriteUserModels.add(model);
                }
            }
            mVoiceAdapter = new VideoAdapter(getActivity(), favouriteUserModels);

        } else {
            mVoiceAdapter = new VideoAdapter(getActivity(), userModels);
        }

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mVoiceAdapter);

        // mRecyclerView.setAdapter(mVoiceAdapter);

        activityCode();

        // history condition
        if (HistoryAdapter.historySelectDetail) {
            clickMethod(VideoAdapter.selectedPerson);
            coordinatorLayout.setBackgroundColor(Color.parseColor("#4D000000"));
            coordinatorLayout.setVisibility(View.VISIBLE);
            ContactsBookFragment.behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            HistoryAdapter.historySelectDetail = false;
        }
    }


    public void activityCode() {

        historyRecyclerView = bottomSheet.findViewById(R.id.historyRecyclerView);
        editContactIV = bottomSheet.findViewById(R.id.editContactIV);
        favouriteLay = bottomSheet.findViewById(R.id.favouriteLay);
        personName = bottomSheet.findViewById(R.id.personName);
        personImage = bottomSheet.findViewById(R.id.personImage);
        numberofperson = bottomSheet.findViewById(R.id.personNumber);
        TimerLay = bottomSheet.findViewById(R.id.TimerLay);
        messageLay = bottomSheet.findViewById(R.id.messageLay);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        historyRecyclerView.setLayoutManager(layoutManager);

        historyDatabase = new CallHistoryHelper(getContext());
        historyModels = historyDatabase.retriveData();

        // myPosition = getActivity().getIntent().getIntExtra("id", 0);
        Log.e("TAG", "myPosition: " + myPosition);
        database = new UserDatabase(getContext());
        userModels = database.retriveData();

        checkPermissions();

    }

    public void bottomSheetMethod() {
        behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setHideable(true);
        behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                coordinatorLayout.setBackgroundColor(Color.parseColor("#4D000000"));
                coordinatorLayout.setVisibility(View.VISIBLE);
                myClick();
                Log.e("TAG", "onStateChanged: " + newState);
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    coordinatorLayout.setVisibility(View.GONE);
                    coordinatorLayout.setOnClickListener(null);
                    coordinatorLayout.setBackgroundColor(Color.parseColor("#00000000"));
                }

            }

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {


            }
        });
    }

    private void myClick() {
        coordinatorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
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

    public void checkPermissions() {
        List<String> missingPermissions = new ArrayList();
        for (String permission : REQUIRED_SDK_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != 0) {
                missingPermissions.add(permission);
            }
        }
        if (missingPermissions.isEmpty()) {
            int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, 0);
            onRequestPermissionsResult(1, REQUIRED_SDK_PERMISSIONS, grantResults);
            return;
        }
        ActivityCompat.requestPermissions(getActivity(), missingPermissions.toArray(new String[missingPermissions.size()]), 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void callVidOrVoiceAlarm(int i) {

        try {
            delayTime = Integer.parseInt(String.valueOf(timeET.getText()));
        } catch (Exception e) {
            Log.e("exception", "" + e.getMessage());
            delayTime = 15;
        }

        if (i == 0) {
            pendingIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(getContext(), VideoReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Toast.makeText(getContext(), "You will receive a video call at : " + delayTime + " Seconds", Toast.LENGTH_SHORT).show();
        } else {
            pendingIntent = PendingIntent.getBroadcast(getContext(), 0, new Intent(getContext(), VoiceReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Toast.makeText(getContext(), "You will receive a voice call at : " + delayTime + " Seconds", Toast.LENGTH_SHORT).show();
        }
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);

        Log.e("TAG", "callVidOrVoiceAlarm: " + 1000 * delayTime);
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000L * delayTime), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (1000L * delayTime), pendingIntent);
        }

    }

    public void setFilterData(String text) {
        Log.e("TAG", "setFilterData: " + text);
        List<HistoryModels> songList = new ArrayList<>();
        for (HistoryModels song : historyModels) {
            if (song.getName().toLowerCase().contains(text.toLowerCase())) {
                songList.add(song);
            }
        }
        historyAdapter.FilterData(songList);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        for (int index = permissions.length - 1; index >= 0; index--) {
            if (grantResults[index] != 0) {
                Toast.makeText(getContext(), "Required permission 'CAMERA' ", Toast.LENGTH_LONG).show();
                //finish();
                return;
            }
        }
    }

    public Bitmap getBitmapFromAsset(String path) {

        AssetManager am = getActivity().getAssets();
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


    private void clickMethod(int i) {

        ContactsBookFragment.favouriteIV = ContactsBookFragment.bottomSheet.findViewById(R.id.favouriteIV);
        if (userModels.get(i).getFavourite().matches("0")) {
            ContactsBookFragment.favouriteIV.setImageResource(R.drawable.ic_without_fill_favourite);
        } else {
            ContactsBookFragment.favouriteIV.setImageResource(R.drawable.ic_fill_favourite);
        }

        ContactsBookFragment.favouriteLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!userModels.get(i).getFavourite().matches("0")) {
                    ContactsBookFragment.favouriteIV.setImageResource(R.drawable.ic_without_fill_favourite);
                    ContactsBookFragment.database.updateFavorite(String.valueOf(userModels.get(i).getId()), "0");
                    userModels.get(i).setFavourite("0");
                    Log.e("TAG", "onClick: if");
                } else {
                    ContactsBookFragment.favouriteIV.setImageResource(R.drawable.ic_fill_favourite);
                    ContactsBookFragment.database.updateFavorite(String.valueOf(userModels.get(i).getId()), "1");
                    userModels.get(i).setFavourite("1");
                    Log.e("TAG", "onClick: else");
                }
            }
        });

        try {
            if (userModels.get(i).getType().equals("Asset")) {
                ContactsBookFragment.personImage.setImageBitmap(getBitmapFromAsset("person/" + userModels.get(i).getPhoto()));
            } else {
                ContactsBookFragment.personImage.setImageBitmap(getBitmap(userModels.get(i).getPhoto()));
            }
        } catch (Exception e) {
            Log.e("exceptoin", "" + e.getMessage());
        }

        ContactsBookFragment.personName.setText(userModels.get(i).getName());
        ContactsBookFragment.numberofperson.setText(userModels.get(i).getPhonenumber());

        ContactsBookFragment.bottomSheet.findViewById(R.id.personVidCallLay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().startActivity(new Intent(getActivity(), VideoCallActivity.class));

            }
        });

        ContactsBookFragment.bottomSheet.findViewById(R.id.personCallLay).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().startActivity(new Intent(getActivity(), AudioCallActivity.class));

            }
        });

        ContactsBookFragment.TimerLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {/*
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();*/
                Dialog dialog = new Dialog(getActivity());
                dialog.setContentView(R.layout.alarm_layout);
                dialog.show();
                ContactsBookFragment.timeET = dialog.findViewById(R.id.timeET);
                ContactsBookFragment.submitvideocall = dialog.findViewById(R.id.submitvideocall);
                ContactsBookFragment.submitvoicecall = dialog.findViewById(R.id.submitvoicecall);


                ContactsBookFragment.submitvideocall.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v) {
                        if (userModels.get(i).getAvb().equals("video") || userModels.get(i).getAvb().equals("both")) {
                            callVidOrVoiceAlarm(0);
                            Log.e("fragment", "onClick: submitvideocall");
                        } else {
                            Toast.makeText(getContext(), "Please add Video", Toast.LENGTH_SHORT).show();
                        }

                        dialog.dismiss();
                    }
                });

                ContactsBookFragment.submitvoicecall.setOnClickListener(new View.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View v) {
                        if (userModels.get(i).getAvb().equals("audio") || userModels.get(i).getAvb().equals("both")) {
                            callVidOrVoiceAlarm(1);
                            Log.e("fragment", "onClick: submitvoicecall");
                        } else {
                            Toast.makeText(getContext(), "Please add Video", Toast.LENGTH_SHORT).show();
                        }
                        dialog.dismiss();
                    }
                });
            }
        });

        MainActivity.historyOrNot = false;
        ContactsBookFragment.historyDatabase = new CallHistoryHelper(getActivity());
        ContactsBookFragment.historyModels = ContactsBookFragment.historyDatabase.retriveData();
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        ContactsBookFragment.historyRecyclerView.setLayoutManager(layoutManager);
        ContactsBookFragment.historyAdapter = new HistoryAdapter(getActivity(), ContactsBookFragment.historyModels);


        ContactsBookFragment.historyRecyclerView.setAdapter(ContactsBookFragment.historyAdapter);


        setFilterData(userModels.get(i).getName());

        ContactsBookFragment.editContactIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i < 6) {
                    Toast.makeText(getActivity(), "Default persons cannot editable", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getActivity(), AddPersonClickActivity.class);
                    intent.putExtra("setMode", "editContact");
                    intent.putExtra("userId", i);
                    getActivity().startActivity(intent);
                    //finish();
                }
            }
        });
        ContactsBookFragment.messageLay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("userId", String.valueOf(i));
                getActivity().startActivity(intent);
            }
        });


    }

}