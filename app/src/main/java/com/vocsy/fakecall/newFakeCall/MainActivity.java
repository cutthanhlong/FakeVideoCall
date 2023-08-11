package com.vocsy.fakecall.newFakeCall;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.BillingClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.UserModel;
import com.vocsy.fakecall.ui.ContactsBookFragment;
import com.vocsy.fakecall.ui.HistoryFragment;
import com.vocsy.fakecall.permission.MyPermission;
import com.vocsy.fakecall.permission.PermissionListener;
import com.vocsy.fakecall.ui.SettingActivity;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends MyPermission implements myInterface, PermissionListener {

    public static boolean favouriteOrNot = false;
    public static boolean historyOrNot = false;
    public static int callOption;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout container;
    private UserDatabase database;
    private List<UserModel> userModels = new ArrayList<>();
    private TextView titleText;
    private ImageView crown;
    private BillingClient billingClient;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public void add_default_person_data() {
        database.insertUSER(getString(R.string.name1), getString(R.string.phonenumber1), getString(R.string.profile1), getString(R.string.video1), "Asset", getString(R.string.email1), getString(R.string.voice1), "0", "both");
        database.insertUSER(getString(R.string.name2), getString(R.string.phonenumber2), getString(R.string.profile2), getString(R.string.video2), "Asset", getString(R.string.email2), getString(R.string.voice2), "0", "both");
        database.insertUSER(getString(R.string.name3), getString(R.string.phonenumber3), getString(R.string.profile3), getString(R.string.video3), "Asset", getString(R.string.email3), getString(R.string.voice3), "0", "both");
        database.insertUSER(getString(R.string.name4), getString(R.string.phonenumber4), getString(R.string.profile4), getString(R.string.video4), "Asset", getString(R.string.email4), getString(R.string.voice4), "0", "both");
        database.insertUSER(getString(R.string.name5), getString(R.string.phonenumber5), getString(R.string.profile5), getString(R.string.video5), "Asset", getString(R.string.email5), getString(R.string.voice5), "0", "both");
        database.insertUSER(getString(R.string.name6), getString(R.string.phonenumber6), getString(R.string.profile6), getString(R.string.video6), "Asset", getString(R.string.email6), getString(R.string.voice6), "0", "both");
    }

    private void init() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        container = findViewById(R.id.container);
        titleText = findViewById(R.id.titleText);
        crown = findViewById(R.id.crown);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        preferences = getSharedPreferences("AppPref", MODE_PRIVATE);
        editor = preferences.edit();


        crown.setVisibility(View.GONE);

        database = new UserDatabase(getApplicationContext());

        if (!Settings.canDrawOverlays(this)) {

            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("OVERLAY Permission")
                    .setMessage("This app need to grant OVERLAY ON OTHER APP permission for displaying calling screen ")
                    .setPositiveButton("GRANT", (dialog, var2) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 0);
                    })
                    .setNegativeButton("No Thanks", (dialog, var2) -> {
                        dialog.dismiss();
                        finishAffinity();
                    })
                    .setCancelable(false)
                    .show();

        }

        userModels = database.retriveData();
        if (userModels.size() < 5) {
            add_default_person_data();
        }

        initBottomNavigation();


        replaceFragment(0);

    }


    private void replaceFragment(int value) {
//        editor.putInt("lastFrag", value);
//        editor.apply();

        switch (value) {
            case 0:
                titleText.setText("Recent");
                historyOrNot = true;
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new HistoryFragment(), "historyFragment").commit();
                break;
            case 1:
                String[] permissions;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO};
                } else {
                    permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
                }
                startPermission(MainActivity.this, permissions, 251);
                break;
            case 2:
                titleText.setText("Favorite");
                favouriteOrNot = true;
                historyOrNot = false;
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new ContactsBookFragment(), "contactsBookFragment").commit();
                break;

        }
    }


    private void initBottomNavigation() {

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.history:
                        replaceFragment(0);
                        return true;
                    case R.id.contacts:
                        replaceFragment(1);
                        return true;
                    case R.id.favourites:
                        replaceFragment(2);
                        return true;
                    case R.id.setting:
                        startActivity(new Intent(MainActivity.this, SettingActivity.class));
                        break;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {

        ContactsBookFragment contactsBookFragment = (ContactsBookFragment) getSupportFragmentManager().findFragmentByTag("contactsBookFragment");

        if (contactsBookFragment != null && contactsBookFragment.isVisible()) {
            // add your code here
            historyOrNot = true;
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new HistoryFragment(), "historyFragment").commit();
            bottomNavigationView.setSelectedItemId(R.id.history);
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void showDetailFragment() {
        favouriteOrNot = false;
        historyOrNot = false;
        HistoryAdapter.historySelectDetail = true;
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new ContactsBookFragment(), "contactsBookFragment").commit();
        bottomNavigationView.setSelectedItemId(R.id.contacts);
    }

    @Override
    public void onRequest(int requestCode) {

        if (requestCode == 251) {
            titleText.setText("Contact");
            favouriteOrNot = false;
            historyOrNot = false;
            if (!HistoryAdapter.historySelectDetail) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, new ContactsBookFragment(), "contactsBookFragment").commit();
            }
        }

    }

    @Override
    public void onPermissionDenied(int requestCode, String permission) {

    }
}