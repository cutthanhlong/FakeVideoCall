
package com.google.android.cameraview;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;

/**
 * https://github.com/kplatfoot/android-rotation-sensor-sample/blob/master/app/src/main/java/com/kviation/sample/orientation/Orientation.java
 */
public class Orientation implements SensorEventListener {

  public interface Listener {
    void onOrientationChanged(float pitch, float roll);
  }

  private int sensorInterval = 50;

  private WindowManager mWindowManager;

  private final SensorManager mSensorManager;

  @Nullable
  private Sensor mRotationSensor;

  private int mLastAccuracy;
  private Listener mListener;

  public Orientation(Activity activity, int sensorInterval) {
    mWindowManager = activity.getWindow().getWindowManager();
    mSensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);

    // Can be null if the sensor hardware is not available
    if (mSensorManager != null) mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    this.sensorInterval = sensorInterval;
  }

  public Orientation(Context context, int sensorInterval) {
    mSensorManager = (SensorManager) context.getSystemService(Activity.SENSOR_SERVICE);

    // Can be null if the sensor hardware is not available
      if (mSensorManager != null) mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
      this.sensorInterval = sensorInterval;
  }

  public void startListening(Listener listener) {
    if (mListener == listener) {
      return;
    }
    mListener = listener;
    if (mRotationSensor == null) {
      Log.w("Orientation", "Rotation vector sensor not available; will not provide orientation data.");
      return;
    }
    mSensorManager.registerListener(this, mRotationSensor, sensorInterval);
  }

  public void stopListening() {
    mSensorManager.unregisterListener(this);
    mListener = null;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    if (mLastAccuracy != accuracy) {
      mLastAccuracy = accuracy;
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (mListener == null) {
      return;
    }
    if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
      return;
    }
    if (event.sensor == mRotationSensor) {
      updateOrientation(event.values);
    }
  }

  @SuppressWarnings("SuspiciousNameCombination")
  private void updateOrientation(float[] rotationVector) {
    float[] rotationMatrix = new float[9];
    SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

    final int worldAxisForDeviceAxisX;
    final int worldAxisForDeviceAxisY;

    if (mWindowManager != null) {
        // Remap the axes as if the device screen was the instrument panel,
        // and adjust the rotation matrix for the device orientation.
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_0:
            default:
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
        }
    } else {
        worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
    }

    float[] adjustedRotationMatrix = new float[9];
    SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
        worldAxisForDeviceAxisY, adjustedRotationMatrix);

    // Transform rotation matrix into azimuth/pitch/roll
    float[] orientation = new float[3];
    SensorManager.getOrientation(adjustedRotationMatrix, orientation);

    // Convert radians to degrees
    float pitch = orientation[1] * -57;
    float roll = orientation[2] * -57;

    mListener.onOrientationChanged(pitch, roll);
  }
}
