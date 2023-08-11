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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import androidx.collection.SparseArrayCompat;

import com.vocsy.fakecall.BuildConfig;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicBoolean;


@SuppressWarnings("deprecation")
class Camera1 extends CameraViewImpl {

    private static final int INVALID_CAMERA_ID = -1;

    private static final SparseArrayCompat<String> FLASH_MODES = new SparseArrayCompat<>();

    static {
        FLASH_MODES.put(Constants.FLASH_OFF, Camera.Parameters.FLASH_MODE_OFF);
        FLASH_MODES.put(Constants.FLASH_ON, Camera.Parameters.FLASH_MODE_ON);
        FLASH_MODES.put(Constants.FLASH_TORCH, Camera.Parameters.FLASH_MODE_TORCH);
        FLASH_MODES.put(Constants.FLASH_AUTO, Camera.Parameters.FLASH_MODE_AUTO);
        FLASH_MODES.put(Constants.FLASH_RED_EYE, Camera.Parameters.FLASH_MODE_RED_EYE);
    }

    private int mCameraId;

    private final AtomicBoolean isPictureCaptureInProgress = new AtomicBoolean(false);

    private Camera mCamera;
    private Camera.Parameters mCameraParameters;

    private final Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();

    private final SizeMap mPreviewSizes = new SizeMap();
    private final SizeMap mPictureSizes = new SizeMap();

    private AspectRatio mAspectRatio;

    private boolean mShowingPreview;

    private boolean mAutoFocus;

    private int mFacing;

    private int mFlash;

    private int mDisplayOrientation;

    private Handler mFrameHandler;
    private HandlerThread mFrameThread;

    protected Float mZoomDistance;

    Camera1(PreviewImpl preview, Context context) {
        super(preview, context);
        preview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                if (mCamera != null) {
                    setUpPreview();
                    adjustCameraParameters();
                    setupPreviewCallback();
                }
            }
        });
    }

    @Override
    boolean start() {
        orientation.startListening(orientationListener);
        chooseCamera();
        openCamera();
        if (mPreview.isReady()) {
            setUpPreview();
            setupPreviewCallback();
        }
        mShowingPreview = true;
        startBackgroundThread();
        mCamera.startPreview();
        return true;
    }

    @Override
    void stop() {
        orientation.stopListening();
        latestFrameWidth = 0;
        latestFrameHeight = 0;
        stopBackgroundThread();
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        mShowingPreview = false;
        releaseCamera();
    }

    // Suppresses Camera#setPreviewTexture
    @SuppressLint("NewApi")
    void setUpPreview() {
        try {
            if (mPreview.getOutputClass() == SurfaceHolder.class) {
                mCamera.setPreviewDisplay(mPreview.getSurfaceHolder());
            } else {
                mCamera.setPreviewTexture((SurfaceTexture) mPreview.getSurfaceTexture());
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
        }
    }

    private byte[] latestFrameData;
    private int latestFrameWidth;
    private int latestFrameHeight;
    void setupPreviewCallback () {
        try {
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(final byte[] data, final Camera camera) {
                    if (data == null || isPictureCaptureInProgress.get() || mFrameHandler == null) return;
                    if (onFrameCallback != null)  {
                        latestFrameData = data;
                        if (latestFrameWidth == 0) latestFrameWidth = camera.getParameters().getPreviewSize().width;
                        if (latestFrameHeight == 0)latestFrameHeight = camera.getParameters().getPreviewSize().height;
                        mFrameHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                onFrameCallback.onFrame(latestFrameData,
                                        latestFrameWidth,
                                        latestFrameHeight,
                                        getRotationDegrees());
                            }
                        });
                    }
                }
            });
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
        }
    }

    @Override
    boolean isCameraOpened() {
        return mCamera != null;
    }

    @Override
    void setFacing(int facing) {
        if (mFacing == facing) {
            return;
        }
        mFacing = facing;
        if (isCameraOpened()) {
            stop();
            start();
        }
    }

    @Override
    int getFacing() {
        return mFacing;
    }

    @Override
    Set<AspectRatio> getSupportedAspectRatios() {
        SizeMap idealAspectRatios = mPreviewSizes;
        for (AspectRatio aspectRatio : idealAspectRatios.ratios()) {
            if (mPictureSizes.sizes(aspectRatio) == null) {
                idealAspectRatios.remove(aspectRatio);
            }
        }
        return idealAspectRatios.ratios();
    }

    @Override
    boolean setAspectRatio(AspectRatio ratio, boolean isInitializing) {
        if (mAspectRatio == null || !isCameraOpened()) {
            // Handle this later when camera is opened
            mAspectRatio = ratio;
            return true;
        } else if (!mAspectRatio.equals(ratio)) {
            final Set<Size> sizes = mPreviewSizes.sizes(ratio);
            if (sizes == null) {
                throw new UnsupportedOperationException(ratio + " is not supported");
            } else {
                mAspectRatio = ratio;
                adjustCameraParameters();
                return true;
            }
        }
        return false;
    }

    @Override
    AspectRatio getAspectRatio() {
        return mAspectRatio;
    }

    @Override
    void setAutoFocus(boolean autoFocus) {
        if (mAutoFocus == autoFocus) {
            return;
        }
        if (setAutoFocusInternal(autoFocus)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    boolean getAutoFocus() {
        if (!isCameraOpened()) {
            return mAutoFocus;
        }
        String focusMode = mCameraParameters.getFocusMode();
        return focusMode != null && focusMode.contains("continuous");
    }

    @Override
    void setFlash(int flash) {
        if (flash == mFlash) {
            return;
        }
        if (setFlashInternal(flash)) {
            mCamera.setParameters(mCameraParameters);
        }
    }

    @Override
    int getFlash() {
        return mFlash;
    }

    @Override
    void takePicture() {
        if (!isCameraOpened()) {
            throw new IllegalStateException(
                    "Camera is not ready. Call start() before takePicture().");
        }
        if (getAutoFocus()) {
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    takePictureInternal();
                }
            });
        } else {
            takePictureInternal();
        }
    }

    void takePictureInternal() {
        stopBackgroundThread();
        try {
            if (!isPictureCaptureInProgress.getAndSet(true)) {
                mCamera.takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {
                        if (focusLockedCallback != null) focusLockedCallback.onFocusLocked();
                    }
                }, null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        isPictureCaptureInProgress.set(false);
                        if (pictureBytesCallback != null) pictureBytesCallback.onPictureBytesAvailable(data, getRotationDegrees());
                        byteArrayToBitmap(data);
                        camera.cancelAutoFocus();
                        camera.startPreview();
                        startBackgroundThread();
                    }
                });
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
            startBackgroundThread();
        }
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        try {
            if (mDisplayOrientation == displayOrientation) {
                return;
            }
            mDisplayOrientation = displayOrientation;
            if (isCameraOpened()) {
                mCameraParameters.setRotation(displayOrientation);
                mCamera.setParameters(mCameraParameters);
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
        }
    }

    @Override
    int getCameraDefaultOrientation() {
        if (mCameraInfo != null) {
            return getFacing() == CameraView.FACING_FRONT ? mCameraInfo.orientation - 180 : mCameraInfo.orientation;
        } else {
            return 0;
        }
    }

    /**
     * This rewrites {@link #mCameraId} and {@link #mCameraInfo}.
     */
    private void chooseCamera() {
        for (int i = 0, count = Camera.getNumberOfCameras(); i < count; i++) {
            Camera.getCameraInfo(i, mCameraInfo);
            if (mCameraInfo.facing == mFacing) {
                mCameraId = i;
                return;
            }
        }
        mCameraId = INVALID_CAMERA_ID;
        if (turnFailCallback != null) turnFailCallback.onTurnCameraFail(new RuntimeException("Cannot find suitable camera."));
    }

    private void openCamera() {
        try {
            if (mCamera != null) {
                releaseCamera();
            }
            mCamera = Camera.open(mCameraId);
            mCameraParameters = mCamera.getParameters();
            // Supported preview sizes
            mPreviewSizes.clear();
            for (Camera.Size size : mCameraParameters.getSupportedPreviewSizes()) {
                if (maximumPreviewWidth == 0) {
                    mPreviewSizes.add(new Size(size.width, size.height));
                } else if (size.width <= maximumPreviewWidth && size.height <= maximumPreviewWidth) {
                    mPreviewSizes.add(new Size(size.width, size.height));
                }
            }
            // Supported picture sizes;
            mPictureSizes.clear();
            for (Camera.Size size : mCameraParameters.getSupportedPictureSizes()) {
                Log.i("CameraView2", "Picture Size: " + size.toString());
                if (maximumWidth == 0) {
                    mPictureSizes.add(new Size(size.width, size.height));
                } else if (size.width <= maximumWidth && size.height <= maximumWidth) {
                    mPictureSizes.add(new Size(size.width, size.height));
                }
            }
            // AspectRatio
            if (mAspectRatio == null) {
                mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;
            }
            adjustCameraParameters();
            mCamera.setDisplayOrientation(calcDisplayOrientation(mDisplayOrientation));
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
        }
    }

    private AspectRatio chooseAspectRatio() {
        AspectRatio r = null;
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            r = ratio;
            if (ratio.equals(Constants.DEFAULT_ASPECT_RATIO)) {
                return ratio;
            }
        }
        return r;
    }

    void adjustCameraParameters() {
        try {
            SortedSet<Size> sizes = mPreviewSizes.sizes(mAspectRatio);
            if (sizes == null) { // Not supported
                mAspectRatio = chooseAspectRatio();
                sizes = mPreviewSizes.sizes(mAspectRatio);
            }
            mPreviewSizeSelected = chooseOptimalSize(sizes);

            // Always re-apply camera parameters
            // Largest picture size in this ratio
            mPictureSizeSelected = mPictureSizes.sizes(mAspectRatio).last();
            if (mShowingPreview) {
                mCamera.stopPreview();
            }
            mCameraParameters.setPreviewSize(mPreviewSizeSelected.getWidth(), mPreviewSizeSelected.getHeight());
            mCameraParameters.setPictureSize(mPictureSizeSelected.getWidth(), mPictureSizeSelected.getHeight());
            mCameraParameters.setRotation(mDisplayOrientation);
            setAutoFocusInternal(mAutoFocus);
            setFlashInternal(mFlash);
            mCamera.setParameters(mCameraParameters);
            if (mShowingPreview) {
                mCamera.startPreview();
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
        }
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private Size chooseOptimalSize(SortedSet<Size> sizes) {
        if (!mPreview.isReady()) { // Not yet laid out
            return sizes.first(); // Return the smallest size
        }
        int desiredWidth;
        int desiredHeight;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (isLandscape(mDisplayOrientation)) {
            desiredWidth = surfaceHeight;
            desiredHeight = surfaceWidth;
        } else {
            desiredWidth = surfaceWidth;
            desiredHeight = surfaceHeight;
        }
        Size result = null;
        for (Size size : sizes) { // Iterate from small to large
            if (desiredWidth <= size.getWidth() && desiredHeight <= size.getHeight()) {
                return size;

            }
            result = size;
        }
        return result;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Calculate display orientation
     * https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
     *
     * This calculation is used for orienting the preview
     *
     * Note: This is not the same calculation as the camera rotation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees required to rotate preview
     */
    private int calcDisplayOrientation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (360 - (mCameraInfo.orientation + screenOrientationDegrees) % 360) % 360;
        } else {  // back-facing
            return (mCameraInfo.orientation - screenOrientationDegrees + 360) % 360;
        }
    }

    /**
     * Calculate camera rotation
     *
     * This calculation is applied to the output JPEG either via Exif Orientation tag
     * or by actually transforming the bitmap. (Determined by vendor camera API implementation)
     *
     * Note: This is not the same calculation as the display orientation
     *
     * @param screenOrientationDegrees Screen orientation in degrees
     * @return Number of degrees to rotate image in order for it to view correctly.
     */
    private int calcCameraRotation(int screenOrientationDegrees) {
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (mCameraInfo.orientation + screenOrientationDegrees) % 360;
        } else {  // back-facing
            final int landscapeFlip = isLandscape(screenOrientationDegrees) ? 180 : 0;
            return (mCameraInfo.orientation + screenOrientationDegrees + landscapeFlip) % 360;
        }
    }

    /**
     * Test if the supplied orientation is in landscape.
     *
     * @param orientationDegrees Orientation in degrees (0,90,180,270)
     * @return True if in landscape, false if portrait
     */
    private boolean isLandscape(int orientationDegrees) {
        return (orientationDegrees == Constants.LANDSCAPE_90 ||
                orientationDegrees == Constants.LANDSCAPE_270);
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setAutoFocusInternal(boolean autoFocus) {
        try {
            mAutoFocus = autoFocus;
            if (isCameraOpened()) {
                final List<String> modes = mCameraParameters.getSupportedFocusModes();
                if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                    mCameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
                } else {
                    mCameraParameters.setFocusMode(modes.get(0));
                }
                return true;
            } else {
                return false;
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
            return false;
        }
    }

    /**
     * @return {@code true} if {@link #mCameraParameters} was modified.
     */
    private boolean setFlashInternal(int flash) {
        try {
            if (isCameraOpened()) {
                List<String> modes = mCameraParameters.getSupportedFlashModes();
                String mode = FLASH_MODES.get(flash);
                if (modes != null && modes.contains(mode)) {
                    mCameraParameters.setFlashMode(mode);
                    mFlash = flash;
                    return true;
                }
                String currentMode = FLASH_MODES.get(mFlash);
                if (modes == null || !modes.contains(currentMode)) {
                    mCameraParameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mFlash = Constants.FLASH_OFF;
                    return true;
                }
                return false;
            } else {
                mFlash = flash;
                return false;
            }
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
            return false;
        }
    }

    private void startBackgroundThread() {
        mFrameThread = new HandlerThread("CameraFrameBackground");
        mFrameThread.start();
        mFrameHandler = new Handler(mFrameThread.getLooper());
    }

    private void stopBackgroundThread() {
        try {
            if (mFrameThread != null) mFrameThread.quit();
            if (mFrameThread != null) mFrameThread.join();
            mFrameThread = null;
            mFrameHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    boolean zoom(MotionEvent event) {
        try {
            Camera.Parameters params = mCamera.getParameters();
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            float realTimeDistance = getFingerSpacing(event);
            if (mZoomDistance == null) {
                mZoomDistance = realTimeDistance;
                return true;
            }

            boolean needZoom = false;
            int deltaZoom = (maxZoom / 30) + 1;
            if (realTimeDistance - mZoomDistance >= pixelsPerOneZoomLevel) {
                //zoom in
                if (zoom < maxZoom) {
                    if (zoom + deltaZoom > maxZoom) deltaZoom = maxZoom - zoom;
                    zoom = zoom + deltaZoom;
                }
                needZoom = true;
            } else if (mZoomDistance - realTimeDistance >= pixelsPerOneZoomLevel) {
                //zoom out
                if (zoom > 0) {
                    if (zoom - deltaZoom < 1) deltaZoom = zoom - 1;
                    zoom = zoom - deltaZoom;
                }
                needZoom = true;
            } else {
                //Do nothing since the difference is not large enough
            }
            if (needZoom) {
                mZoomDistance = realTimeDistance;
                params.setZoom(zoom);
                mCamera.setParameters(params);
            }
            return true;
        } catch (final Exception e) {
            if (BuildConfig.DEBUG) e.printStackTrace();
            if (cameraErrorCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        cameraErrorCallback.onCameraError(e);
                    }
                });
            }
            return false;
        }
    }

    @Override
    void onPinchFingerUp() {
        mZoomDistance = null; //Reset zoom memory if finger is up
    }

}
