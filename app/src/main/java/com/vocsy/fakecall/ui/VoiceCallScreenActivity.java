package com.vocsy.fakecall.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.vocsy.fakecall.utils.Globals;
import com.vocsy.fakecall.utils.MyPreferences;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.model.UserModel;
import com.vocsy.fakecall.adapter.VideoAdapter;
import com.vocsy.fakecall.data.CallHistoryHelper;
import com.vocsy.fakecall.data.UserDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class VoiceCallScreenActivity extends AppCompatActivity {
    public Chronometer chronometer;
    public CountDownTimer yourCountDownTimer2;
    LinearLayout Lineraccep;
    LinearLayout accepte;
    MediaPlayer mMediaPlayer = new MediaPlayer();
    MediaPlayer mMediaPlayer2 = new MediaPlayer();
    LinearLayout refcall;
    LinearLayout messageLay1;
    TextView personname, callername;
    LinearLayout main_background;
    LinearLayout endCallLay;
    RelativeLayout recCallLay;
    ImageView endCall;
    List<UserModel> userModels = new ArrayList<>();
    UserDatabase database;
    private MyPreferences preferences;
    ImageView personphoto, personimageforbg, mikeBT, speakerCall, vidCallBT;
    int disheight;
    int diswidth;
    int mikeOnOff = 0;
    private static Handler handler = new Handler();
    private static Runnable runnable;

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public Bitmap getBitmapFromAssetold(String path) {

        AssetManager am = VoiceCallScreenActivity.this.getAssets();
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = am.open(path);

            BitmapFactory.Options options = new BitmapFactory.Options();
            BitmapFactory.decodeFile(path, options);
            options.inSampleSize = calculateInSampleSize(options, 300, 300);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;


            bitmap = BitmapFactory.decodeStream(is, null, options);
        } catch (final IOException e) {
            bitmap = null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return bitmap;
    }

    public Bitmap getBitmap(String path) {
        Bitmap bitmap = null;
        try {

            BitmapFactory.Options options = new BitmapFactory.Options();
            BitmapFactory.decodeFile(path, options);
            options.inSampleSize = calculateInSampleSize(options, diswidth, disheight);
            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            bitmap = BitmapFactory.decodeFile(path, options);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @SuppressLint({"WrongConstant", "MissingInflatedId"})
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activtity_voice_call);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        preferences = new MyPreferences(this);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        disheight = displayMetrics.heightPixels;
        diswidth = displayMetrics.widthPixels;


        if (preferences.isVibrate()) {
            Globals.startVibrate(VoiceCallScreenActivity.this);
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                if (refcall != null) {
                    refcall.performClick();
                }
            }
        };

        handler.postDelayed(runnable, (long) preferences.getAutoCutSecond() * 1000);

        database = new UserDatabase(getApplicationContext());
        userModels = database.retriveData();

        if (ContactScreenFragment.behavior != null) {
            ContactScreenFragment.behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        personphoto = findViewById(R.id.personphoto);
        personname = (TextView) findViewById(R.id.personname);
        callername = (TextView) findViewById(R.id.callername);
        //  personnumber = (TextView) findViewById(R.id.personnumber);

        recCallLay = findViewById(R.id.recCallLay);
        endCallLay = findViewById(R.id.endCallLay);
        endCall = findViewById(R.id.endCall);
        personimageforbg = findViewById(R.id.personimageforbg);
        mikeBT = findViewById(R.id.MikeBT);
        speakerCall = findViewById(R.id.speakerCall);
        vidCallBT = findViewById(R.id.vidCallBT);

        mikeBT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mikeOnOff == 0) {
                    mikeBT.setImageResource(R.drawable.ic_mike_off);
                    mikeOnOff = 1;
                } else {
                    mikeBT.setImageResource(R.drawable.ic_mike_on);
                    mikeOnOff = 0;
                }
            }
        });

        this.speakerCall.setOnClickListener(view -> {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.isSpeakerphoneOn()) {
                audioManager.setSpeakerphoneOn(false);
                speakerCall.setImageResource(R.drawable.ic_call_not_speaker);
            } else {
                audioManager.setSpeakerphoneOn(true);
                speakerCall.setImageResource(R.drawable.ic_call_speaker);
            }
        });

        vidCallBT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(VoiceCallScreenActivity.this, VideoCallScreenActivity.class);
                intent.putExtra("userId", String.valueOf(VideoAdapter.selectedPerson));
                startActivity(intent);
                finish();
            }
        });

        Fresco.initialize(getApplication());

        if (userModels.get(VideoAdapter.selectedPerson).type.equals("Asset")) {

            //mainbackground.setImageBitmap(getBitmapFromAsset("background/" + voicePersonModels.get(0).Background));
            personphoto.setImageBitmap(getBitmapFromAssetold("person/" + userModels.get(VideoAdapter.selectedPerson).photo));
            personimageforbg.setImageBitmap(getBitmapFromAssetold("person/" + userModels.get(VideoAdapter.selectedPerson).photo));

        } else {

            //mainbackground.setImageBitmap(getBitmap(voicePersonModels.get(0).Background));
            personphoto.setImageBitmap(getBitmap(userModels.get(VideoAdapter.selectedPerson).photo));
            personimageforbg.setImageBitmap(getBitmap(userModels.get(VideoAdapter.selectedPerson).photo));

        }


        personname.setText(userModels.get(VideoAdapter.selectedPerson).name);
        callername.setText(userModels.get(VideoAdapter.selectedPerson).name);
        //  personnumber.setText(userModels.get(VideoAdapter.selectedPerson).phonenumber);


        if (userModels.get(VideoAdapter.selectedPerson).type.equals("Asset")) {
            AudioManager mAudioManager = ((AudioManager) VoiceCallScreenActivity.this.getSystemService(Context.AUDIO_SERVICE));

            mAudioManager.setMode(AudioManager.STREAM_VOICE_CALL);
            mAudioManager.setSpeakerphoneOn(false);

            AssetFileDescriptor descriptor = null;

            mMediaPlayer2 = new MediaPlayer();
            VoiceCallScreenActivity.this.mMediaPlayer2.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            VoiceCallScreenActivity.this.mMediaPlayer2.setLooping(true);

            try {
                descriptor = getAssets().openFd("voice/" + userModels.get(VideoAdapter.selectedPerson).getAudio());
                mMediaPlayer2.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                VoiceCallScreenActivity.this.mMediaPlayer2.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            AudioManager mAudioManager = ((AudioManager) VoiceCallScreenActivity.this.getSystemService(Context.AUDIO_SERVICE));

            mAudioManager.setMode(AudioManager.STREAM_VOICE_CALL);
            mAudioManager.setSpeakerphoneOn(false);

            mMediaPlayer2 = new MediaPlayer();
            VoiceCallScreenActivity.this.mMediaPlayer2.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            VoiceCallScreenActivity.this.mMediaPlayer2.setLooping(true);

            try {
                mMediaPlayer2.setDataSource(userModels.get(VideoAdapter.selectedPerson).getAudio());
                VoiceCallScreenActivity.this.mMediaPlayer2.prepare();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        this.accepte = (LinearLayout) findViewById(R.id.acceptcell);
        this.refcall = (LinearLayout) findViewById(R.id.refcall);
        this.messageLay1 = (LinearLayout) findViewById(R.id.messageLay1);
        this.Lineraccep = (LinearLayout) findViewById(R.id.Lineraccep);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                VoiceCallScreenActivity.this.mMediaPlayer = MediaPlayer.create(VoiceCallScreenActivity.this, Settings.System.DEFAULT_RINGTONE_URI);
                VoiceCallScreenActivity.this.mMediaPlayer.setAudioStreamType(3);
                VoiceCallScreenActivity.this.mMediaPlayer.setLooping(true);
                VoiceCallScreenActivity.this.mMediaPlayer.start();
            }
        }, 200);

        this.yourCountDownTimer2 = new CountDownTimer(Long.MAX_VALUE, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                VoiceCallScreenActivity.this.mMediaPlayer.stop();
                VoiceCallScreenActivity.this.mMediaPlayer2.stop();
                VoiceCallScreenActivity.this.finish();
            }
        }.start();
        this.accepte.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (handler != null & runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                if (preferences.isVibrate()) {
                    Globals.stopVibrate();
                }

                VoiceCallScreenActivity.this.mMediaPlayer.stop();
                VoiceCallScreenActivity.this.yourCountDownTimer2.start();
                recCallLay.setVisibility(View.GONE);
                endCallLay.setVisibility(View.VISIBLE);
                VoiceCallScreenActivity.this.chronometer = (Chronometer) VoiceCallScreenActivity.this.findViewById(R.id.chronometerd);
                VoiceCallScreenActivity.this.chronometer.setVisibility(View.VISIBLE);
                ((Chronometer) VoiceCallScreenActivity.this.findViewById(R.id.chronometerd)).setBase(SystemClock.elapsedRealtime());
                ((Chronometer) VoiceCallScreenActivity.this.findViewById(R.id.chronometerd)).start();
                mMediaPlayer2.start();

                addHistory(1);


            }
        });
        this.refcall.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (handler != null & runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                if (preferences.isVibrate()) {
                    Globals.stopVibrate();
                }

                yourCountDownTimer2.cancel();
                mMediaPlayer.stop();
                mMediaPlayer2.stop();
                addHistory(0);
                finish();

            }
        });
        this.endCall.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                VoiceCallScreenActivity.this.yourCountDownTimer2.cancel();
                VoiceCallScreenActivity.this.mMediaPlayer.stop();
                VoiceCallScreenActivity.this.mMediaPlayer2.stop();

                VoiceCallScreenActivity.this.finish();

            }
        });
    }

    private void otherButtons(View view) {

        switch (view.getId()) {
            case R.id.MikeBT:
                if (mikeOnOff == 0) {
                    mikeBT.setImageResource(R.drawable.ic_mike_off);
                    mikeOnOff = 1;
                } else {
                    mikeBT.setImageResource(R.drawable.ic_mike_on);
                    mikeOnOff = 0;
                }
                break;
            default:
                break;
        }

    }

    private void addHistory(int i) {
        CallHistoryHelper helper = new CallHistoryHelper(getApplicationContext());
        Date date = new Date();
        CharSequence s = DateFormat.format("dd/MM", date.getTime());
        CharSequence time = DateFormat.format("HH:mm:ss a", date.getTime());
        //Log.e("TAG", "addHistory: "+time );
        helper.insertData(userModels.get(VideoAdapter.selectedPerson).name, userModels.get(VideoAdapter.selectedPerson).phonenumber, String.valueOf(s), String.valueOf(time), 0, i);

    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 25) {
            this.mMediaPlayer.stop();
        }
        if (keyCode == 24) {
            this.mMediaPlayer.stop();
        }
        if (keyCode == 4) {
            this.mMediaPlayer.stop();
            this.mMediaPlayer2.stop();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        if (mMediaPlayer2.isPlaying()) {
            mMediaPlayer2.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }
        if (mMediaPlayer2.isPlaying()) {
            mMediaPlayer2.stop();
        }
        finish();
    }
}




