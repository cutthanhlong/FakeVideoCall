package com.vocsy.fakecall;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.io.FileDescriptor;
import java.io.IOException;

public class Globals {

    private static Vibrator vibrator;
    private static final long[] VIBRATE_PATTERN = {1000, 1000};

    public static void startVibrate(Activity activity) {
        if (vibrator == null) {
            vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, VibrationEffect.EFFECT_CLICK));
            } else {
                vibrator.vibrate(VIBRATE_PATTERN, 0);
            }
        }
    }

    public static void stopVibrate() {
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri, float width, float height) throws IOException {
        OutOfMemoryError e;
        Bitmap bitmap = null;
        try {
            ParcelFileDescriptor openFileDescriptor = context.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = openFileDescriptor.getFileDescriptor();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            if (width <= height) {
                width = height;
            }
            int i = (int) width;
            options2.inSampleSize = ImageUtils.getClosestResampleSize(options.outWidth, options.outHeight, i);
            Bitmap decodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options2);
            try {
                Matrix matrix = new Matrix();
                if (decodeFileDescriptor.getWidth() > i || decodeFileDescriptor.getHeight() > i) {
                    BitmapFactory.Options resampling = ImageUtils.getResampling(decodeFileDescriptor.getWidth(), decodeFileDescriptor.getHeight(), i);
                    matrix.postScale(((float) resampling.outWidth) / ((float) decodeFileDescriptor.getWidth()), ((float) resampling.outHeight) / ((float) decodeFileDescriptor.getHeight()));
                }
                String realPathFromURI = ImageUtils.getRealPathFromURI(uri, context);
                if (new Integer(Build.VERSION.SDK).intValue() > 4) {
                    int exifRotation = ExifUtils.getExifRotation(realPathFromURI);
                    if (exifRotation != 0) {
                        matrix.postRotate((float) exifRotation);
                    }
                }
                bitmap = Bitmap.createBitmap(decodeFileDescriptor, 0, 0, decodeFileDescriptor.getWidth(), decodeFileDescriptor.getHeight(), matrix, true);
                openFileDescriptor.close();
            } catch (OutOfMemoryError e2) {
                e = e2;
                bitmap = decodeFileDescriptor;
                e.printStackTrace();
                return bitmap;
            }
        } catch (OutOfMemoryError e3) {
            e = e3;
        }
        return bitmap;
    }

}
