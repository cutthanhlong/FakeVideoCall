package com.vocsy.fakecall.permission;

public interface PermissionListener {

    void onRequest(int requestCode);

    void onPermissionDenied(int requestCode, String permission);

}
