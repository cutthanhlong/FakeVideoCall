/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.cameraview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.view.View;

import java.util.Set;

public abstract class CameraViewImpl {

    /**
     * The distance between 2 fingers (in pixel) needed in order for zoom level to increase by 1x.
     */
    protected int pixelsPerOneZoomLevel = 80;

    protected OnPictureTakenListener pictureCallback;
    protected OnPictureBytesAvailableListener pictureBytesCallback;
    protected OnTurnCameraFailListener turnFailCallback;
    protected OnCameraErrorListener cameraErrorCallback;
    protected OnFocusLockedListener focusLockedCallback;
    protected OnFrameListener onFrameCallback;

    protected final PreviewImpl mPreview;

    protected int maximumWidth = 0;
    protected int maximumPreviewWidth = 0;

    protected Orientation orientation;
    protected int currentOrientationDegrees;
    protected Orientation.Listener orientationListener = new Orientation.Listener() {
        @Override
        public void onOrientationChanged(float pitch, float roll) {
            currentOrientationDegrees = pitchAndRollToDegrees(pitch, roll);
        }
    };

    protected Size mPreviewSizeSelected;
    protected Size mPictureSizeSelected;

    CameraViewImpl(PreviewImpl preview, Context context) {
        mPreview = preview;
        orientation = new Orientation(context, 100);
    }

    View getView() {
        return mPreview.getView();
    }

    private Bitmap mirrorBitmap (Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public void setOnPictureTakenListener (OnPictureTakenListener pictureCallback) {
        this.pictureCallback = pictureCallback;
    }

    public void setOnPictureBytesAvailableListener (OnPictureBytesAvailableListener bytesCallback) {
        this.pictureBytesCallback = bytesCallback;
    }

    public void setOnFocusLockedListener (OnFocusLockedListener focusLockedListener) {
        this.focusLockedCallback = focusLockedListener;
    }

    public void setOnTurnCameraFailListener (OnTurnCameraFailListener turnCameraFailListener) {
        this.turnFailCallback = turnCameraFailListener;
    }

    public void setOnCameraErrorListener (OnCameraErrorListener onCameraErrorListener) {
        this.cameraErrorCallback = onCameraErrorListener;
    }

    public void setOnFrameListener (OnFrameListener onFrameListener) {
        this.onFrameCallback = onFrameListener;
    }

    /**
     * @return {@code true} if the implementation was able to start the camera session.
     */
    abstract boolean start();

    abstract void stop();

    abstract boolean isCameraOpened();

    abstract void setFacing(int facing);

    abstract int getFacing();

    abstract Set<AspectRatio> getSupportedAspectRatios();

    /**
     * @return {@code true} if the aspect ratio was changed.
     */
    abstract boolean setAspectRatio(AspectRatio ratio, boolean isInitializing);

    abstract AspectRatio getAspectRatio();

    abstract void setAutoFocus(boolean autoFocus);

    abstract boolean getAutoFocus();

    abstract void setFlash(int flash);

    abstract int getFlash();

    abstract void takePicture();

    abstract void setDisplayOrientation(int displayOrientation);

    /**
     * Different devices have different default orientations,
     * Therefore we need to take into account this value before passing the rotation
     * in the callback.
     */
    abstract int getCameraDefaultOrientation ();

    /**
     * @return {@code true} if the motionEvent is consumed.
     */
    abstract boolean zoom (MotionEvent event);

    abstract void onPinchFingerUp ();

    protected float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    protected void byteArrayToBitmap (final byte[] data) {
        if (pictureCallback == null) return; //There's no point of wasting resources if there is no callback registered
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (getFacing() == CameraView.FACING_FRONT) {
                    if (pictureCallback != null) pictureCallback.onPictureTaken(mirrorBitmap(bitmap), getRotationDegrees());
                } else {
                    if (pictureCallback != null) pictureCallback.onPictureTaken(bitmap, getRotationDegrees());
                }
            }
        });
    }

    public void setPixelsPerOneZoomLevel (int pixels) {
        if (pixels <= 0) return;
        pixelsPerOneZoomLevel = pixels;
    }

    public int getMaximumWidth() {
        return maximumWidth;
    }

    public void setMaximumWidth(int mMaximumWidth) {
        this.maximumWidth = mMaximumWidth;
    }

    public int getMaximumPreviewWidth() {
        return maximumPreviewWidth;
    }

    public void setMaximumPreviewWidth(int maximumPreviewWidth) {
        this.maximumPreviewWidth = maximumPreviewWidth;
    }

    public Size getPreviewSize() {
        return mPreviewSizeSelected;
    }

    public Size getPictureSize() {
        return mPictureSizeSelected;
    }

    protected int getRotationDegrees () {
        return -(currentOrientationDegrees + getCameraDefaultOrientation());
    }

    private int pitchAndRollToDegrees (float pitch, float roll) {
        if (roll < -135 || roll > 135) {
            return 180; //Home button on the top
        } else if (roll > 45 && roll <= 135) {
            return 270; //Home button on the right
        } else if (roll >= -135 && roll < -45) {
            return 90; //Home button on the left
        } else {
            return 0; //Portrait
        }
    }

    public interface OnPictureTakenListener {
        void onPictureTaken (Bitmap bitmap, int rotationDegrees);
    }

    public interface OnPictureBytesAvailableListener {
        void onPictureBytesAvailable (byte[] bytes, int rotationDegrees);
    }

    public interface OnTurnCameraFailListener {
        void onTurnCameraFail (Exception e);
    }

    public interface OnCameraErrorListener {
        void onCameraError (Exception e);
    }

    public interface OnFocusLockedListener {
        void onFocusLocked ();
    }

    public interface OnFrameListener {
        void onFrame (byte[] data, int width, int height, int rotationDegrees);
    }

}
