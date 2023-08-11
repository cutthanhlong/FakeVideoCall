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

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.vocsy.fakecall.BuildConfig;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedSet;

@SuppressWarnings("MissingPermission")
@androidx.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.LOLLIPOP)
class Camera2 extends CameraViewImpl {

    private static final String TAG = "Camera2";

    private static final SparseIntArray INTERNAL_FACINGS = new SparseIntArray();

    static {
        INTERNAL_FACINGS.put(Constants.FACING_BACK, CameraCharacteristics.LENS_FACING_BACK);
        INTERNAL_FACINGS.put(Constants.FACING_FRONT, CameraCharacteristics.LENS_FACING_FRONT);
    }

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private final CameraManager mCameraManager;

    private final CameraDevice.StateCallback mCameraDeviceCallback
            = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCamera = camera;
            startCaptureSession();
        }

        @Override
        public void onClosed(@NonNull CameraDevice camera) {
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            mCamera = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(TAG, "onError: " + camera.getId() + " (" + error + ")");
            mCamera = null;
        }

    };

    private final CameraCaptureSession.StateCallback mSessionCallback
            = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            if (mCamera == null) {
                return;
            }
            mCaptureSession = session;
            updateAutoFocus();
            updateFlash();
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                        mCaptureCallback, mBackgroundHandler);
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
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "Failed to configure capture session.");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            if (mCaptureSession != null && mCaptureSession.equals(session)) {
                mCaptureSession = null;
            }
        }

    };

    PictureCaptureCallback mCaptureCallback = new PictureCaptureCallback() {

        @Override
        public void onPrecaptureRequired() {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            setState(STATE_PRECAPTURE);
            try {
                mCaptureSession.capture(mPreviewRequestBuilder.build(), this, mBackgroundHandler);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
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
        public void onReady() {
            captureStillPicture();
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = reader.acquireNextImage()) {
                Image.Plane[] planes = image.getPlanes();
                if (planes.length > 0) {
                    ByteBuffer buffer = planes[0].getBuffer();
                    byte[] data = new byte[buffer.capacity()];
                    buffer.get(data);
                    byteArrayToBitmap(data);
                    if (pictureBytesCallback != null) pictureBytesCallback.onPictureBytesAvailable(data, getRotationDegrees());
                }
                image.close();
            }
        }
    };
    private byte[] latestFrameData;
    private int latestFrameWidth;
    private int latestFrameHeight;
    private final ImageReader.OnImageAvailableListener mOnFrameAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            if (mFrameHandler == null) return;
            mFrameHandler.post(new Runnable() {
                @Override
                public void run() {
                    final Image image = reader.acquireNextImage();
                    if (image == null) return;
                    try {
                        if (onFrameCallback != null) {
                            latestFrameData = Utils.YUV420toNV21(image);
                            latestFrameWidth = image.getWidth();
                            latestFrameHeight = image.getHeight();
                            mFrameProcessHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    onFrameCallback.onFrame(latestFrameData,
                                            latestFrameWidth,
                                            latestFrameHeight,
                                            getRotationDegrees());
                                }
                            });
                        }
                    } finally {
                        image.close();
                    }
                }
            });
        }
    };


    private String mCameraId;

    private CameraCharacteristics mCameraCharacteristics;

    CameraDevice mCamera;

    CameraCaptureSession mCaptureSession;

    CaptureRequest.Builder mPreviewRequestBuilder;

    private ImageReader mImageReader;
    private ImageReader mFrameImageReader;

    private final SizeMap mPreviewSizes = new SizeMap();

    private final SizeMap mPictureSizes = new SizeMap();

    private int mFacing;

    private AspectRatio mAspectRatio = Constants.DEFAULT_ASPECT_RATIO;

    private boolean mAutoFocus;
    private boolean mAutoFocusSetting;

    private int mFlash;

    private int mDisplayOrientation;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    private Handler mFrameHandler;
    private HandlerThread mFrameThread;

    private Handler mFrameProcessHandler;
    private HandlerThread mFrameProcessThread;

    Camera2(PreviewImpl preview, Context context) {
        super(preview, context);
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mPreview.setCallback(new PreviewImpl.Callback() {
            @Override
            public void onSurfaceChanged() {
                startCaptureSession();
            }
        });
    }

    @Override
    boolean start() {
        orientation.startListening(orientationListener);
        if (!chooseCameraIdByFacing()) {
            return false;
        }
        collectCameraInfo();
        prepareImageReader();
        startOpeningCamera();
        startBackgroundThread();
        return true;
    }

    @Override
    void stop() {
        orientation.stopListening();
        if (mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        if (mFrameImageReader != null) {
            mFrameImageReader.close();
            mFrameImageReader = null;
        }
        stopBackgroundThread();
        resetZoom();
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
        if (mPreviewSizes.isEmpty()) {
            chooseCameraIdByFacing();
            StreamConfigurationMap map = mCameraCharacteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) return mPreviewSizes.ratios();
            for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
                int width = size.getWidth();
                int height = size.getHeight();
                if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                    if (maximumPreviewWidth == 0) {
                        mPreviewSizes.add(new Size(width, height));
                    } else if (width <= maximumPreviewWidth && height <= maximumPreviewWidth) {
                        mPreviewSizes.add(new Size(width, height));
                    }
                }
            }
        }
        return mPreviewSizes.ratios();
    }

    @Override
    boolean setAspectRatio(AspectRatio ratio, boolean isInitializing) {
        if (ratio == null || ratio.equals(mAspectRatio)) return false;
        if (!getSupportedAspectRatios().contains(ratio)) {
            return false;
        }
        mAspectRatio = ratio;
        if (!isInitializing) {
            prepareImageReader();
            if (mCaptureSession != null) {
                mCaptureSession.close();
                mCaptureSession = null;
                startCaptureSession();
            }
        }
        return true;
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
        mAutoFocus = autoFocus;
        mAutoFocusSetting = autoFocus;
        if (mPreviewRequestBuilder != null) {
            updateAutoFocus();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    mAutoFocus = !mAutoFocus; // Revert
                }
            }
        }
    }

    @Override
    boolean getAutoFocus() {
        return mAutoFocusSetting;
    }

    @Override
    void setFlash(int flash) {
        if (mFlash == flash) {
            return;
        }
        int saved = mFlash;
        mFlash = flash;
        if (mPreviewRequestBuilder != null) {
            updateFlash();
            if (mCaptureSession != null) {
                try {
                    mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(),
                            mCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    mFlash = saved; // Revert
                }
            }
        }
    }

    @Override
    int getFlash() {
        return mFlash;
    }

    @Override
    void takePicture() {
        if (mAutoFocus) {
            lockFocus();
        } else {
            captureStillPicture();
        }
    }

    @Override
    void setDisplayOrientation(int displayOrientation) {
        mDisplayOrientation = displayOrientation;
        mPreview.setDisplayOrientation(mDisplayOrientation);
    }

    @Override
    int getCameraDefaultOrientation() {
        try {
            int orientationDegrees = getFacing() == CameraView.FACING_FRONT ?
                    mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) - 180 :
                    mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            return orientationDegrees;
        } catch (Exception e) {
            Log.w("Camera2", "Failed to get CameraDefaultOrientation");
            return 0;
        }
    }

    /**
     * <p>Chooses a camera ID by the specified camera facing ({@link #mFacing}).</p>
     * <p>This rewrites {@link #mCameraId}, {@link #mCameraCharacteristics}, and optionally
     * {@link #mFacing}.</p>
     */
    private boolean chooseCameraIdByFacing() {
        try {
            int internalFacing = INTERNAL_FACINGS.get(mFacing);
            final String[] ids = mCameraManager.getCameraIdList();
            if (ids.length == 0) { // No camera
                throw new RuntimeException("No camera available.");
            }
            for (String id : ids) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(id);
                Integer level = characteristics.get(
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                if (level == null ||
                        level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                    continue;
                }
                Integer internal = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (internal == null) {
                    throw new NullPointerException("Unexpected state: LENS_FACING null");
                }
                if (internal == internalFacing) {
                    mCameraId = id;
                    mCameraCharacteristics = characteristics;
                    return true;
                }
            }
            // Not found
            // Added at 2017-12-13 by Sira: Actually I don't understand why the below is needed
            mCameraId = ids[0];
            mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            Integer level = mCameraCharacteristics.get(
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
            if (level == null ||
                    level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                return false;
            }
            Integer internal = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (internal == null) {
                throw new NullPointerException("Unexpected state: LENS_FACING null");
            }
            for (int i = 0, count = INTERNAL_FACINGS.size(); i < count; i++) {
                if (INTERNAL_FACINGS.valueAt(i) == internal) {
                    mFacing = INTERNAL_FACINGS.keyAt(i);
                    return true;
                }
            }
            // The operation can reach here when the only camera device is an external one.
            // We treat it as facing back.
            if (turnFailCallback != null)
                turnFailCallback.onTurnCameraFail(new RuntimeException("Cannot find suitable Camera"));
            mFacing = Constants.FACING_BACK;
            return true;
        } catch (CameraAccessException e) {
            throw new RuntimeException("Failed to get a list of camera devices", e);
        }
    }

    /**
     * <p>Collects some information from {@link #mCameraCharacteristics}.</p>
     * <p>This rewrites {@link #mPreviewSizes}, {@link #mPictureSizes}, and optionally,
     * {@link #mAspectRatio}.</p>
     */
    private void collectCameraInfo() {
        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            throw new IllegalStateException("Failed to get configuration map: " + mCameraId);
        }
        mPreviewSizes.clear();
        for (android.util.Size size : map.getOutputSizes(mPreview.getOutputClass())) {
            int width = size.getWidth();
            int height = size.getHeight();
            if (width <= MAX_PREVIEW_WIDTH && height <= MAX_PREVIEW_HEIGHT) {
                if (maximumPreviewWidth == 0) {
                    mPreviewSizes.add(new Size(width, height));
                } else if (width <= maximumPreviewWidth && height <= maximumPreviewWidth) {
                    mPreviewSizes.add(new Size(width, height));
                }
            }
        }
        mPictureSizes.clear();
        collectPictureSizes(mPictureSizes, map);
        for (AspectRatio ratio : mPreviewSizes.ratios()) {
            if (!mPictureSizes.ratios().contains(ratio)) {
                mPreviewSizes.remove(ratio);
            }
        }

        if (!mPreviewSizes.ratios().contains(mAspectRatio)) {
            mAspectRatio = mPreviewSizes.ratios().iterator().next();
        }
    }

    protected void collectPictureSizes(SizeMap sizes, StreamConfigurationMap map) {
        for (android.util.Size size : map.getOutputSizes(ImageFormat.JPEG)) {
            Log.i("CameraView2", "Picture Size: " + size.toString());
            if (maximumWidth == 0) {
                mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
            } else if (size.getWidth() <= maximumWidth && size.getHeight() <= maximumWidth) {
                mPictureSizes.add(new Size(size.getWidth(), size.getHeight()));
            }
        }
    }

    private void prepareImageReader() {
        if (mImageReader != null) {
            mImageReader.close();
        }
        if (mFrameImageReader != null) {
            mFrameImageReader.close();
        }
        mPictureSizeSelected = mPictureSizes.sizes(mAspectRatio).last();
        mImageReader = ImageReader.newInstance(mPictureSizeSelected.getWidth(), mPictureSizeSelected.getHeight(),
                ImageFormat.JPEG, /* maxImages */ 1);
        Size previewLargest = mPreviewSizes.sizes(mAspectRatio).last();
        mFrameImageReader = ImageReader.newInstance(previewLargest.getWidth(), previewLargest.getHeight(),
                ImageFormat.YUV_420_888, 1);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);
        mFrameImageReader.setOnImageAvailableListener(mOnFrameAvailableListener, mBackgroundHandler);
    }

    /**
     * <p>Starts opening a camera device.</p>
     * <p>The result will be processed in {@link #mCameraDeviceCallback}.</p>
     */
    private void startOpeningCamera() {
        try {
            mCameraManager.openCamera(mCameraId, mCameraDeviceCallback, mBackgroundHandler);
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

    /**
     * <p>Starts a capture session for camera preview.</p>
     * <p>This rewrites {@link #mPreviewRequestBuilder}.</p>
     * <p>The result will be continuously processed in {@link #mSessionCallback}.</p>
     */
    void startCaptureSession() {
        if (!isCameraOpened() || !mPreview.isReady() || mImageReader == null || mFrameImageReader == null) {
            return;
        }
        mPreviewSizeSelected = chooseOptimalSize();

        mPreview.setBufferSize(mPreviewSizeSelected.getWidth(), mPreviewSizeSelected.getHeight());
        Surface surface = mPreview.getSurface();
        Surface frameSurface = mFrameImageReader.getSurface();
        try {
            mPreviewRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(frameSurface);
            mCamera.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface(), mFrameImageReader.getSurface()),
                    mSessionCallback, mBackgroundHandler);
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

    /**
     * Chooses the optimal preview size based on {@link #mPreviewSizes} and the surface size.
     *
     * @return The picked size for camera preview.
     */
    private Size chooseOptimalSize() {
        int surfaceLonger, surfaceShorter;
        final int surfaceWidth = mPreview.getWidth();
        final int surfaceHeight = mPreview.getHeight();
        if (surfaceWidth < surfaceHeight) {
            surfaceLonger = surfaceHeight;
            surfaceShorter = surfaceWidth;
        } else {
            surfaceLonger = surfaceWidth;
            surfaceShorter = surfaceHeight;
        }
        SortedSet<Size> candidates = mPreviewSizes.sizes(mAspectRatio);

        // Pick the smallest of those big enough
        for (Size size : candidates) {
            if (size.getWidth() >= surfaceLonger && size.getHeight() >= surfaceShorter) {
                return size;
            }
        }
        // If no size is big enough, pick the largest one.
        return candidates.last();
    }

    /**
     * Updates the internal state of auto-focus to {@link #mAutoFocus}.
     */
    void updateAutoFocus() {
        if (mAutoFocusSetting) {
            int[] modes = mCameraCharacteristics.get(
                    CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            // Auto focus is not supported
            if (modes == null || modes.length == 0 ||
                    (modes.length == 1 && modes[0] == CameraCharacteristics.CONTROL_AF_MODE_OFF)) {
                mAutoFocus = false;
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF);
            } else {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            }
        } else {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF);
        }
    }

    /**
     * Updates the internal state of flash to {@link #mFlash}.
     */
    void updateFlash() {
        switch (mFlash) {
            case Constants.FLASH_OFF:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_ON:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_TORCH:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_TORCH);
                break;
            case Constants.FLASH_AUTO:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
            case Constants.FLASH_RED_EYE:
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE);
                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE,
                        CaptureRequest.FLASH_MODE_OFF);
                break;
        }
    }

    /**
     * Locks the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mCaptureCallback.setState(PictureCaptureCallback.STATE_LOCKING);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
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

    /**
     * Captures a still picture.
     */
    void captureStillPicture() {
        try {
            CaptureRequest.Builder captureRequestBuilder = mCamera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            if (zoom != null) {
                captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            }
            switch (mFlash) {
                case Constants.FLASH_OFF:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_OFF);
                    break;
                case Constants.FLASH_ON:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    break;
                case Constants.FLASH_TORCH:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE,
                            CaptureRequest.FLASH_MODE_TORCH);
                    break;
                case Constants.FLASH_AUTO:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
                case Constants.FLASH_RED_EYE:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    break;
            }
            // Stop preview and capture a still picture.
            mCaptureSession.stopRepeating();

            //Call Activity to do things when focus is locked
            if (focusLockedCallback != null) {
                mPreview.getView().post(new Runnable() {
                    @Override
                    public void run() {
                        focusLockedCallback.onFocusLocked();
                    }
                });
            }

            mCaptureSession.capture(captureRequestBuilder.build(),
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            unlockFocus();
                        }
                    }, mBackgroundHandler);
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

    /**
     * Unlocks the auto-focus and restart camera preview. This is supposed to be called after
     * capturing a still picture.
     */
    void unlockFocus() {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
        try {
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            updateAutoFocus();
            updateFlash();
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            mCaptureCallback.setState(PictureCaptureCallback.STATE_PREVIEW);
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

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mFrameThread = new HandlerThread("CameraFrameBackground");
        mFrameThread.start();
        mFrameHandler = new Handler(mFrameThread.getLooper());
        mFrameProcessThread = new HandlerThread("CameraFrameProcessBackground");
        mFrameProcessThread.start();
        mFrameProcessHandler = new Handler(mFrameProcessThread.getLooper());
    }

    private void stopBackgroundThread() {
        try {
            if (mBackgroundThread != null) mBackgroundThread.quitSafely();
            if (mBackgroundThread != null) mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            if (mFrameThread != null) mFrameThread.quit();
            if (mFrameThread != null) mFrameThread.join();
            mFrameThread = null;
            mFrameHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            if (mFrameProcessThread != null) mFrameProcessThread.quit();
            if (mFrameProcessThread != null) mFrameProcessThread.join();
            mFrameProcessThread = null;
            mFrameProcessHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //Zooming
    protected Float mZoomDistance;
    protected float zoomLevel = 1f;
    protected Float maximumZoomLevel;
    protected Rect zoom;

    @Override
    boolean zoom(MotionEvent event) {
        try {
            //Gather camera information for zooming
            Rect rect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (rect == null) return false;
            if (maximumZoomLevel == null) {
                maximumZoomLevel = (mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM));
            }

            //Prepare finger distance for zooming
            float realTimeDistance = getFingerSpacing(event);
            if (mZoomDistance == null) {
                mZoomDistance = realTimeDistance;
                return true;
            }
            float distanceDelta;
            boolean isZoomIn;
            if (realTimeDistance >= mZoomDistance) {
                isZoomIn = true;
                distanceDelta = realTimeDistance - mZoomDistance;
            } else { //Zoom out
                isZoomIn = false;
                distanceDelta = mZoomDistance - realTimeDistance;
            }

            //Calculate zoom ratio delta
            float ratioDelta = distanceDelta / pixelsPerOneZoomLevel;

            //Prevent over zooming
            if (isZoomIn && zoomLevel + ratioDelta > maximumZoomLevel) {
                ratioDelta = maximumZoomLevel - zoomLevel;
            } else if (!isZoomIn && zoomLevel - ratioDelta < 1f) {
                ratioDelta = zoomLevel - 1f;
            }

            //Modify current zoomLevel
            if (isZoomIn) {
                zoomLevel += ratioDelta;
            } else {
                zoomLevel -= ratioDelta;
            }

            //Finally we can zoom
            float ratio = (float) 1 / zoomLevel;
            int croppedWidth = rect.width() - Math.round((float) rect.width() * ratio);
            int croppedHeight = rect.height() - Math.round((float) rect.height() * ratio);
            zoom = new Rect(croppedWidth / 2, croppedHeight / 2,
                    rect.width() - croppedWidth / 2, rect.height() - croppedHeight / 2);
            mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoom);
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);

            mZoomDistance = realTimeDistance;
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
        mZoomDistance = null;
    }

    void resetZoom() {
        zoomLevel = 1f;
        zoom = null;
        mZoomDistance = 0f;
    }

    /**
     * A {@link CameraCaptureSession.CaptureCallback} for capturing a still picture.
     */
    private static abstract class PictureCaptureCallback
            extends CameraCaptureSession.CaptureCallback {

        static final int STATE_PREVIEW = 0;
        static final int STATE_LOCKING = 1;
        static final int STATE_LOCKED = 2;
        static final int STATE_PRECAPTURE = 3;
        static final int STATE_WAITING = 4;
        static final int STATE_CAPTURING = 5;

        private int mState;

        PictureCaptureCallback() {
        }

        void setState(int state) {
            mState = state;
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

        private void process(@NonNull CaptureResult result) {
            switch (mState) {
                case STATE_LOCKING: {
                    Integer af = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (af == null) {
                        break;
                    }
                    if (af == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED ||
                            af == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED) {
                        Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            setState(STATE_CAPTURING);
                            onReady();
                        } else {
                            setState(STATE_LOCKED);
                            onPrecaptureRequired();
                        }
                    }
                    break;
                }
                case STATE_PRECAPTURE: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            ae == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED ||
                            ae == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        setState(STATE_WAITING);
                    }
                    break;
                }
                case STATE_WAITING: {
                    Integer ae = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (ae == null || ae != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        setState(STATE_CAPTURING);
                        onReady();
                    }
                    break;
                }
            }
        }

        /**
         * Called when it is ready to take a still picture.
         */
        public abstract void onReady();

        /**
         * Called when it is necessary to run the precapture sequence.
         */
        public abstract void onPrecaptureRequired();

    }

}
