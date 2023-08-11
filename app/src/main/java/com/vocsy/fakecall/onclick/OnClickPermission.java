package com.vocsy.fakecall.onclick;

public interface OnClickPermission {

    void onRequest(int requestCode);

    void onPermissionDenied(int requestCode, String permission);

}
