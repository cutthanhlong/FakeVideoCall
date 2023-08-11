package com.vocsy.fakecall.base;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.vocsy.fakecall.onclick.OnClickPermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyPermission extends AppCompatActivity {

    private int mRequestCode = 0;
    private final String permission = "";
    private OnClickPermission listener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        listener = (OnClickPermission) this;
    }

    private final ActivityResultLauncher<String[]> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
        boolean allPermissionsGranted = true;
        for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
            if (!entry.getValue()) {
                allPermissionsGranted = false;
                break;
            }
        }
        if (allPermissionsGranted) {
            afterPermission();
        } else {
            Toast.makeText(this, "Please allow all the required permissions", Toast.LENGTH_SHORT).show();
        }
    });

    public void startPermission(Activity activity, String[] permissions, int mRequestCode) {
        this.mRequestCode = mRequestCode;
        boolean allPermissionsGranted = true;
        List<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    permissionsToRequest.add(permission);
                }
            }

            if (allPermissionsGranted) {
                afterPermission();
            } else if (shouldShowRequestPermissionsRationale(activity, permissionsToRequest)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Permission");
                builder.setMessage("You need to grant the following permissions: " + TextUtils.join(", ", permissionsToRequest) + " for use our application");
                builder.setCancelable(false);
                builder.setPositiveButton("I UNDERSTAND", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                        intent.setData(uri);
                        activity.startActivity(intent);
                    }
                });
                builder.show();
            } else {
                requestPermissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
            }
        }
    }

    private boolean shouldShowRequestPermissionsRationale(Activity activity, List<String> permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    private void afterPermission() {
        listener.onRequest(mRequestCode);
    }
}
