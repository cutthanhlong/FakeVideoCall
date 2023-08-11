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

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.ParcelableCompat;
import androidx.core.os.ParcelableCompatCreatorCallbacks;
import androidx.core.view.ViewCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

public class CameraView extends FrameLayout {

    /** The camera device faces the opposite direction as the device's screen. */
    public static final int FACING_BACK = Constants.FACING_BACK;

    /** The camera device faces the same direction as the device's screen. */
    public static final int FACING_FRONT = Constants.FACING_FRONT;

    /** Direction the camera faces relative to device screen. */
    @IntDef({FACING_BACK, FACING_FRONT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Facing {
    }

    /** Flash will not be fired. */
    public static final int FLASH_OFF = Constants.FLASH_OFF;

    /** Flash will always be fired during snapshot. */
    public static final int FLASH_ON = Constants.FLASH_ON;

    /** Constant emission of light during preview, auto-focus and snapshot. */
    public static final int FLASH_TORCH = Constants.FLASH_TORCH;

    /** Flash will be fired automatically when required. */
    public static final int FLASH_AUTO = Constants.FLASH_AUTO;

    /** Flash will be fired in red-eye reduction mode. */
    public static final int FLASH_RED_EYE = Constants.FLASH_RED_EYE;

    /** The mode for for the camera device's flash control */
    @IntDef({FLASH_OFF, FLASH_ON, FLASH_TORCH, FLASH_AUTO, FLASH_RED_EYE})
    public @interface Flash {
    }

    CameraViewImpl mImpl;

    private boolean mAdjustViewBounds;
    private boolean mZoomEnabled = true;
    private int maximumWidth = 0;
    private int maximumPreviewWidth = 0;

    private final DisplayOrientationDetector mDisplayOrientationDetector;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressWarnings("WrongConstant")
    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()){
            mDisplayOrientationDetector = null;
            return;
        }

        // Attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CameraView, defStyleAttr,
                R.style.Widget_CameraView);
        mAdjustViewBounds = a.getBoolean(R.styleable.CameraView_android_adjustViewBounds, false);
        String aspectRatio = a.getString(R.styleable.CameraView_cameraAspectRatio);
        maximumWidth = a.getInt(R.styleable.CameraView_maximumWidth, 0);
        maximumPreviewWidth = a.getInt(R.styleable.CameraView_maximumPreviewWidth, 0);
        boolean useHighResPicture = a.getBoolean(R.styleable.CameraView_useHighResPicture, maximumWidth == 0);
        int facing = a.getInt(R.styleable.CameraView_facing, FACING_BACK);
        boolean autoFocus = a.getBoolean(R.styleable.CameraView_autoFocus, true);
        int flash = a.getInt(R.styleable.CameraView_flash, Constants.FLASH_AUTO);
        boolean zoomEnabled = a.getBoolean(R.styleable.CameraView_enableZoom, true);
        a.recycle();

        // Internal setup
        final PreviewImpl preview = createPreviewImpl(context, false);
        if (CameraViewConfig.isForceCamera1 || Build.VERSION.SDK_INT < 21) {
            mImpl = new Camera1(preview, context);
        } else if (Build.VERSION.SDK_INT >= 23 && useHighResPicture) {
            mImpl = new Camera2Api23(preview, context);
        } else {
            mImpl = new Camera2(preview, context);
        }
        mImpl.setMaximumWidth(maximumWidth);
        mImpl.setMaximumPreviewWidth(maximumPreviewWidth);

        setFacing(facing);
        setAutoFocus(autoFocus);
        setFlash(flash);
        setZoomEnabled(zoomEnabled);
        if (aspectRatio != null) {
            setAspectRatio(AspectRatio.parse(aspectRatio), true);
        } else {
            setAspectRatio(Constants.DEFAULT_ASPECT_RATIO, true);
        }

        // Display orientation detector
        mDisplayOrientationDetector = new DisplayOrientationDetector(context) {
            @Override
            public void onDisplayOrientationChanged(int displayOrientation) {
                mImpl.setDisplayOrientation(displayOrientation);
            }
        };

    }

    @NonNull
    private PreviewImpl createPreviewImpl(Context context, boolean isLegacy) {
        PreviewImpl preview;
        if (Build.VERSION.SDK_INT < 21 || CameraViewConfig.isForceCamera1 || isLegacy) {
            preview = new SurfaceViewPreview(context, this);
        } else {
            preview = new TextureViewPreview(context, this);
        }
        return preview;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!isInEditMode()) {
            mDisplayOrientationDetector.enable(ViewCompat.getDisplay(this));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (!isInEditMode()) {
            mDisplayOrientationDetector.disable();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isInEditMode()){
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // Handle android:adjustViewBounds
        if (mAdjustViewBounds) {
            if (!isCameraOpened()) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int height = (int) (MeasureSpec.getSize(widthMeasureSpec) * ratio.toFloat());
                if (heightMode == MeasureSpec.AT_MOST) {
                    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
                }
                super.onMeasure(widthMeasureSpec,
                        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
            } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
                final AspectRatio ratio = getAspectRatio();
                assert ratio != null;
                int width = (int) (MeasureSpec.getSize(heightMeasureSpec) * ratio.toFloat());
                if (widthMode == MeasureSpec.AT_MOST) {
                    width = Math.min(width, MeasureSpec.getSize(widthMeasureSpec));
                }
                super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                        heightMeasureSpec);
            } else {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
        // Measure the TextureView
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        AspectRatio ratio = getAspectRatio();
        if (mDisplayOrientationDetector.getLastKnownDisplayOrientation() % 180 == 0) {
            ratio = ratio.inverse();
        }
        assert ratio != null;
        if (height < width * ratio.getY() / ratio.getX()) {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(width * ratio.getY() / ratio.getX(),
                            MeasureSpec.EXACTLY));
        } else {
            mImpl.getView().measure(
                    MeasureSpec.makeMeasureSpec(height * ratio.getX() / ratio.getY(),
                            MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    //Handle Pinch Zoom
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mImpl == null) return super.onTouchEvent(event);

        int action = event.getActionMasked();
        if (event.getPointerCount() == 2) { //Multi touch.
            if (mZoomEnabled) {
                if (action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP) {
                    mImpl.onPinchFingerUp();
                } else {
                    mImpl.zoom(event);
                }
            }
            return true;
        } else { //Single touch point, needs to return true in order to detect one more touch point
            return true;
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.facing = getFacing();
        state.ratio = getAspectRatio();
        state.autoFocus = getAutoFocus();
        state.flash = getFlash();
        state.zoom = isZoomEnabled();
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setFacing(ss.facing);
        setAspectRatio(ss.ratio, true);
        setAutoFocus(ss.autoFocus);
        setFlash(ss.flash);
        setZoomEnabled(ss.zoom);
    }

    /**
     * Open a camera device and start showing camera preview. This is typically called from
     * {@link Activity#onResume()}.
     */
    public void start() {
        if (!mImpl.start()) {
            //store the state ,and restore this state after fall back o Camera1
            Parcelable state = onSaveInstanceState();
            // Camera2 uses legacy hardware layer; fall back to Camera1
            mImpl = new Camera1(createPreviewImpl(getContext(), true), getContext());
            mImpl.setMaximumWidth(maximumWidth);
            mImpl.setMaximumPreviewWidth(maximumPreviewWidth);
            onRestoreInstanceState(state);
            mImpl.start();
        }
    }

    /**
     * Stop camera preview and close the device. This is typically called from
     * {@link Activity#onPause()}.
     */
    public void stop() {
        mImpl.stop();
    }

    /**
     * @return {@code true} if the camera is opened.
     */
    public boolean isCameraOpened() {
        return mImpl.isCameraOpened();
    }

    /**
     * @param adjustViewBounds {@code true} if you want the CameraView to adjust its bounds to
     *                         preserve the aspect ratio of camera.
     * @see #getAdjustViewBounds()
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        if (mAdjustViewBounds != adjustViewBounds) {
            mAdjustViewBounds = adjustViewBounds;
            requestLayout();
        }
    }

    /**
     * @return True when this CameraView is adjusting its bounds to preserve the aspect ratio of
     * camera.
     * @see #setAdjustViewBounds(boolean)
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    /**
     * Chooses camera by the direction it faces.
     *
     * @param facing The camera facing. Must be either {@link #FACING_BACK} or
     *               {@link #FACING_FRONT}.
     */
    public void setFacing(@Facing int facing) {
        mImpl.setFacing(facing);
    }

    /**
     * A more user-friendly version of setFacing.
     */
    public void switchCamera () {
        if (getFacing() == FACING_BACK) {
            setFacing(FACING_FRONT);
        } else {
            setFacing(FACING_BACK);
        }
    }

    /**
     * Gets the direction that the current camera faces.
     *
     * @return The camera facing.
     */
    @Facing
    public int getFacing() {
        //noinspection WrongConstant
        return mImpl.getFacing();
    }

    /**
     * Gets all the aspect ratios supported by the current camera.
     */
    public Set<AspectRatio> getSupportedAspectRatios() {
        return mImpl.getSupportedAspectRatios();
    }

    /**
     * Sets the aspect ratio of camera.
     *
     * @param ratio The {@link AspectRatio} to be set.
     */
    public void setAspectRatio(@NonNull AspectRatio ratio, boolean isInitializing) {
        if (mImpl.setAspectRatio(ratio, isInitializing)) {
            requestLayout();
        }
    }

    /**
     * Gets the current aspect ratio of camera.
     *
     * @return The current {@link AspectRatio}. Can be {@code null} if no camera is opened yet.
     */
    @Nullable
    public AspectRatio getAspectRatio() {
        return mImpl.getAspectRatio();
    }

    /**
     * Enables or disables the continuous auto-focus mode. When the current camera doesn't support
     * auto-focus, calling this method will be ignored.
     *
     * @param autoFocus {@code true} to enable continuous auto-focus mode. {@code false} to
     *                  disable it.
     */
    public void setAutoFocus(boolean autoFocus) {
        mImpl.setAutoFocus(autoFocus);
    }

    /**
     * Returns whether the continuous auto-focus mode is enabled.
     *
     * @return {@code true} if the continuous auto-focus mode is enabled. {@code false} if it is
     * disabled, or if it is not supported by the current camera.
     */
    public boolean getAutoFocus() {
        return mImpl.getAutoFocus();
    }

    /**
     * Sets the flash mode.
     *
     * @param flash The desired flash mode.
     */
    public void setFlash(@Flash int flash) {
        mImpl.setFlash(flash);
    }

    /**
     * Gets the current flash mode.
     *
     * @return The current flash mode.
     */
    @Flash
    public int getFlash() {
        //noinspection WrongConstant
        return mImpl.getFlash();
    }

    /**
     * Returns the dimensions setting for preview pictures.
     *
     * @return a Size object with the width and height setting
     *          for the preview picture
     */
    public Size getPreviewSize() {
        return mImpl.getPreviewSize();
    }

    /**
     * Returns the dimensions setting for pictures.
     *
     * @return a Size object with the width and height setting
     *          for the picture
     */
    public Size getPictureSize() {
        return mImpl.getPictureSize();
    }

    public void setOnPictureTakenListener (CameraViewImpl.OnPictureTakenListener pictureTakenListener) {
        if (mImpl != null) {
            mImpl.setOnPictureTakenListener(pictureTakenListener);
        }
    }

    public void setOnPictureBytesAvailableListener (CameraViewImpl.OnPictureBytesAvailableListener pictureBytesAvailableListener) {
        if (mImpl != null) {
            mImpl.setOnPictureBytesAvailableListener(pictureBytesAvailableListener);
        }
    }

    public void setOnFocusLockedListener (CameraViewImpl.OnFocusLockedListener focusLockedListener) {
        if (mImpl != null) {
            mImpl.setOnFocusLockedListener(focusLockedListener);
        }
    }

    public void setOnTurnCameraFailListener (CameraViewImpl.OnTurnCameraFailListener turnCameraFailListener) {
        if (mImpl != null) {
            mImpl.setOnTurnCameraFailListener(turnCameraFailListener);
        }
    }

    public void setOnCameraErrorListener (CameraViewImpl.OnCameraErrorListener cameraErrorListener) {
        if (mImpl != null) {
            mImpl.setOnCameraErrorListener(cameraErrorListener);
        }
    }

    public void setOnFrameListener (CameraViewImpl.OnFrameListener onFrameListener) {
        if (mImpl != null) {
            mImpl.setOnFrameListener(onFrameListener);
        }
    }

    public void takePicture() {
        mImpl.takePicture();
    }

    public void setPixelsPerOneZoomLevel (int pixels) {
        if (mImpl != null) {
            mImpl.setPixelsPerOneZoomLevel(pixels);
        }
    }

    /**
     * Enables or disables the zoom on pitch.
     *
     * @param zoomOnPitch {@code true} to enable zoom on pitch. {@code false} to disable it.
     */
    public void setZoomEnabled(boolean zoomOnPitch) {
        mZoomEnabled = zoomOnPitch;
    }

    /**
     * Returns whether the zoom on pitch is enabled.
     *
     * @return {@code true} if the zoom on pitch is enabled. {@code false} if it is
     * disabled.
     */
    public boolean isZoomEnabled() {
        return mZoomEnabled;
    }

    public int getDefaultOrientation () {
        if (mImpl != null) {
            return mImpl.getCameraDefaultOrientation();
        } else {
            return 0;
        }
    }

    protected static class SavedState extends BaseSavedState {

        @Facing
        int facing;

        AspectRatio ratio;

        boolean autoFocus;

        @Flash
        int flash;

        boolean zoom;

        @SuppressWarnings("WrongConstant")
        public SavedState(Parcel source, ClassLoader loader) {
            super(source);
            facing = source.readInt();
            ratio = source.readParcelable(loader);
            autoFocus = source.readByte() != 0;
            flash = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(facing);
            out.writeParcelable(ratio, 0);
            out.writeByte((byte) (autoFocus ? 1 : 0));
            out.writeInt(flash);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }

        });

    }

}
