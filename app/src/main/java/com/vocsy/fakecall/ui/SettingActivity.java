package com.vocsy.fakecall.ui;

import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.vocsy.fakecall.MyPreferences;
import com.vocsy.fakecall.R;

import java.util.Objects;

import vocsy.ads.AppUtil;

public class SettingActivity extends AppCompatActivity {

    private SwitchCompat premiumSwitch, vibrationSwitch;
    private ImageView back;
    private TextView vibrationText;
    private TextView autoCutText;
    private MyPreferences preferences;
    private Dialog dialog;
    private RelativeLayout autoCutLay;
    private RelativeLayout share_app;
    private RelativeLayout rate_app;
    private RelativeLayout more_app;
    private RelativeLayout privacy_policy;

    private void bindView() {
        share_app = findViewById(R.id.share_app);
        rate_app = findViewById(R.id.rate_app);
        more_app = findViewById(R.id.more_app);
        privacy_policy = findViewById(R.id.privacy_policy);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        premiumSwitch = findViewById(R.id.premiumSwitch);
        back = findViewById(R.id.back);
        vibrationText = findViewById(R.id.vibrationText);
        autoCutText = findViewById(R.id.autoCutText);
        autoCutLay = findViewById(R.id.autoCutLay);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        preferences = new MyPreferences(this);
        bindView();

        back.setOnClickListener(view -> onBackPressed());
        autoCutLay.setOnClickListener(view -> showDialog());

        handleSwitch();

        share_app.setOnClickListener(view -> AppUtil.shareApp(this));
        rate_app.setOnClickListener(view -> AppUtil.rateApp(this));
        more_app.setOnClickListener(view -> AppUtil.moreApp(this, getString(R.string.developer_ID)));
        privacy_policy.setOnClickListener(view -> AppUtil.privacyPolicy(this, getString(R.string.privacy_policy)));
    }

    private void handleSwitch() {

        premiumSwitch.setChecked(preferences.isUserHasPremium());
        vibrationSwitch.setChecked(preferences.isVibrate());
        handleVibrationText(preferences.isVibrate());
        handleAutoCutText(preferences.getAutoCutSecond());

        premiumSwitch.setOnCheckedChangeListener((compoundButton, value) -> {
            preferences.setPremium(value);
            showToast(value ? "Now you are premium user" : "Now You are normal user");
        });

        vibrationSwitch.setOnCheckedChangeListener((compoundButton, value) -> {
            preferences.setVibrate(value);
            handleVibrationText(value);
            showToast(value ? getString(R.string.vib_on) : getString(R.string.vib_off));
        });
    }

    private void showToast(String text) {
        Toast.makeText(SettingActivity.this,
                text
                , Toast.LENGTH_SHORT).show();
    }

    private void handleVibrationText(boolean value) {
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.vib_1));
        builder.append(value ? " ON " : " OFF ");
        builder.append(getString(R.string.vib_2));
        vibrationText.setText(builder.toString());
    }

    private void handleAutoCutText(int value) {
        StringBuilder builder = new StringBuilder();
        builder.append(getString(R.string.call_automatically_cut_after));
        builder.append(value);
        builder.append(" " + getString(R.string.second));
        autoCutText.setText(builder.toString());
    }

    public void showDialog() {
        dialog = new Dialog(SettingActivity.this);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // before
        dialog.setContentView(R.layout.auto_cut_layout);
        dialog.setCancelable(true);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(Objects.requireNonNull(dialog.getWindow()).getAttributes());
        lp.flags = WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.MATCH_PARENT;


        EditText editText = dialog.findViewById(R.id.editText);
        Button ok = dialog.findViewById(R.id.ok);

        ok.setOnClickListener(view -> {

            String text = editText.getText().toString().trim();

            if (text.isEmpty()) {
                editText.setError(getString(R.string.enter_value));
                editText.requestFocus();
            } else {
                int second = Integer.parseInt(text);

                if (second > 29 && second < 121) {
                    preferences.setAutoCutSecond(second);
                    handleAutoCutText(second);
                    dialog.dismiss();
                } else {
                    editText.setError(getString(R.string.error_1));
                    editText.requestFocus();
                }
            }
        });

        if (!dialog.isShowing() && !isFinishing()) {
            dialog.show();
//            dialog.getWindow().setAttributes(lp);
        }
    }


}