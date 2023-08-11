package com.vocsy.fakecall.ui;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.vocsy.fakecall.utils.Globals;
import com.vocsy.fakecall.utils.MyPreferences;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.model.UserModel;
import com.vocsy.fakecall.adapter.VideoAdapter;
import com.vocsy.fakecall.data.CallHistoryHelper;
import com.vocsy.fakecall.data.UserDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class VideoCallScreenActivity extends AppCompatActivity implements Callback {

    private static int aux = 0;
    private static int aux2 = 0;
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    public Chronometer chronometer;
    public CountDownTimer yourCountDownTimer;
    LinearLayout Laccept;
    LinearLayout Lcancel;
    LinearLayout Limag;
    Button accepte;
    Camera camera;
    Camera camera1;
    LinearLayout camliner;
    LinearLayout camvv;
    MediaPlayer mMediaPlayer = new MediaPlayer();
    MediaPlayer mMediaPlayer2 = new MediaPlayer();
    boolean previewing = true;
    boolean previewing1 = true;
    Button refcall;
    SurfaceHolder surfaceHolder;
    SurfaceHolder surfaceHolder1;
    SurfaceView surfaceView;
    SurfaceView surfaceView1;
    LinearLayout vidvid;
    LinearLayout abovelayout;
    List<UserModel> persons = new ArrayList<>();
    ImageView mutevideocall;
    TextView nameofperson, numberofperson;
    ImageView personimage;
    UserDatabase database;
    private MyPreferences preferences;
    private static final Handler handler = new Handler();
    private static Runnable runnable;

    static /* synthetic */ int access$308() {
        int i = aux;
        aux = i + 1;
        return i;
    }

    static /* synthetic */ int access$508() {
        int i = aux2;
        aux2 = i + 1;
        return i;
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = new MyPreferences(this);
        setContentView(R.layout.activity_video_call);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        runnable = new Runnable() {
            @Override
            public void run() {
                if (refcall != null) {
                    refcall.performClick();
                }
            }
        };

        handler.postDelayed(runnable, (long) preferences.getAutoCutSecond() * 1000);


        if (preferences.isVibrate()) {
            Globals.startVibrate(VideoCallScreenActivity.this);
        }

        if (ContactScreenFragment.behavior != null) {
            ContactScreenFragment.behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        database = new UserDatabase(getApplicationContext());
        persons = database.retriveData();

        mutevideocall = findViewById(R.id.mutevideocall);
        nameofperson = findViewById(R.id.nameofperson);
        numberofperson = findViewById(R.id.numberofperson);
        personimage = findViewById(R.id.personimage);

        numberofperson.setText(persons.get(VideoAdapter.selectedPerson).phonenumber);
        nameofperson.setText(persons.get(VideoAdapter.selectedPerson).name);

        if (persons.get(VideoAdapter.selectedPerson).type.equals("Asset")) {
            personimage.setImageBitmap(getBitmapFromAsset("person/" + persons.get(VideoAdapter.selectedPerson).photo));
        } else {
            personimage.setImageBitmap(getBitmap(persons.get(VideoAdapter.selectedPerson).photo));
        }

        final VideoView videoview = findViewById(R.id.videoview);
        final ImageView mutevideomic = findViewById(R.id.mutemicvideo);
        ImageView endcallvideo = findViewById(R.id.endcallvideo);
        this.accepte = findViewById(R.id.acceptcell);
        this.refcall = findViewById(R.id.refcall);
        this.Limag = findViewById(R.id.imagecall);
        this.Laccept = findViewById(R.id.rependliner);
        abovelayout = findViewById(R.id.abovelayout);

        this.camliner = findViewById(R.id.camlineri);
        this.Lcancel = findViewById(R.id.canselliner);
        this.vidvid = findViewById(R.id.vidvid);
        this.camvv = findViewById(R.id.camevv);
        this.surfaceView = findViewById(R.id.cameravideo);
        this.surfaceHolder = this.surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
        this.surfaceHolder.setType(3);
        videoview.setVisibility(View.GONE);
        this.surfaceView1 = findViewById(R.id.cameravideo1);
        this.surfaceHolder1 = this.surfaceView1.getHolder();
        this.surfaceHolder1.addCallback(this);
        this.surfaceHolder1.setType(3);
        CameraInfo cameraInfo = new CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == 1) {
                try {
                    this.camera1 = Camera.open(camIdx);
                } catch (RuntimeException e) {
                }
            }
        }
        if (this.camera1 != null) {
            try {
                this.camera1.setPreviewDisplay(this.surfaceHolder1);
                this.camera1.startPreview();
                this.camera1.setDisplayOrientation(90);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                VideoCallScreenActivity.this.mMediaPlayer = MediaPlayer.create(VideoCallScreenActivity.this, Settings.System.DEFAULT_RINGTONE_URI);
                VideoCallScreenActivity.this.mMediaPlayer.setAudioStreamType(3);
                VideoCallScreenActivity.this.mMediaPlayer.setLooping(true);
                VideoCallScreenActivity.this.mMediaPlayer.start();
                VideoCallScreenActivity.this.showcam1();
                VideoCallScreenActivity.this.showcam1();
            }
        }, 200);
        this.yourCountDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                VideoCallScreenActivity.this.mMediaPlayer.stop();
                VideoCallScreenActivity.this.finish();
            }
        }.start();

        // startcameravideo();


        this.accepte.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (handler != null & runnable != null) {
                    handler.removeCallbacks(runnable);
                }
                if (preferences.isVibrate()) {
                    Globals.stopVibrate();
                }

                VideoCallScreenActivity.this.stopcam1();

                VideoCallScreenActivity.this.vidvid.setVisibility(View.GONE);
                videoview.setVisibility(View.VISIBLE);
                VideoCallScreenActivity.this.surfaceView1.setVisibility(View.GONE);

                VideoCallScreenActivity.this.mMediaPlayer.stop();

                VideoCallScreenActivity.this.yourCountDownTimer.cancel();
                VideoCallScreenActivity.this.camliner.setVisibility(View.VISIBLE);
                VideoCallScreenActivity.this.Lcancel.setVisibility(View.VISIBLE);
                VideoCallScreenActivity.this.Laccept.setVisibility(View.GONE);
                VideoCallScreenActivity.this.Limag.setVisibility(View.GONE);
                abovelayout.setVisibility(View.GONE);


                if (getIntent().hasExtra("DEFULT")) {
                    Intent intent = new Intent(VideoCallScreenActivity.this, FinalVideoCallScreenActivity.class);
                    intent.putExtra("DEFULT", "YES");
                    startActivity(intent);
                    finish();

                } else {


                    Intent intent = new Intent(VideoCallScreenActivity.this, FinalVideoCallScreenActivity.class);
                    startActivity(intent);
                    finish();
                }

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

                mMediaPlayer.stop();
                mMediaPlayer2.stop();
                addHistory(0);
                finish();
            }
        });
        endcallvideo.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                videoview.stopPlayback();
                videoview.pause();
                finish();
            }
        });
        mutevideocall.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (VideoCallScreenActivity.aux % 2 == 0) {
                    VideoCallScreenActivity.this.stopcam();
                    VideoCallScreenActivity.this.surfaceView.setVisibility(View.INVISIBLE);
                    VideoCallScreenActivity.access$308();
                    mutevideocall.setBackground(VideoCallScreenActivity.this.getResources().getDrawable(R.drawable.ic_vidcall_video_off));
                    return;
                }
                VideoCallScreenActivity.access$308();
                VideoCallScreenActivity.this.surfaceView.setVisibility(View.VISIBLE);
                if (!VideoCallScreenActivity.this.previewing) {
                    CameraInfo cameraInfo = new CameraInfo();
                    int cameraCount = Camera.getNumberOfCameras();
                    for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                        Camera.getCameraInfo(camIdx, cameraInfo);
                        if (cameraInfo.facing == 1) {
                            try {
                                VideoCallScreenActivity.this.camera = Camera.open(camIdx);
                            } catch (RuntimeException e) {
                            }
                        }
                    }
                    if (VideoCallScreenActivity.this.camera != null) {
                        try {
                            VideoCallScreenActivity.this.camera.setPreviewDisplay(VideoCallScreenActivity.this.surfaceHolder);
                            VideoCallScreenActivity.this.camera.startPreview();
                            VideoCallScreenActivity.this.previewing = true;
                            VideoCallScreenActivity.this.camera.setDisplayOrientation(90);
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
                mutevideocall.setBackground(VideoCallScreenActivity.this.getResources().getDrawable(R.drawable.ic_vidcall_video_on));
            }
        });
        mutevideomic.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (VideoCallScreenActivity.aux2 % 2 == 0) {
                    VideoCallScreenActivity.access$508();
                    mutevideomic.setBackground(VideoCallScreenActivity.this.getResources().getDrawable(R.drawable.ic_vidcall_mike_off));
                    return;
                }
                VideoCallScreenActivity.access$508();
                mutevideomic.setBackground(VideoCallScreenActivity.this.getResources().getDrawable(R.drawable.ic_vidcall_mike_on));
            }
        });
    }


    private void showcam1() {
        if (aux % 2 == 0) {
            stopcam1();
            this.surfaceView1.setVisibility(View.INVISIBLE);
            aux++;
            return;
        }
        aux++;
        //  starcam();
        this.surfaceView1.setVisibility(View.VISIBLE);
        if (!this.previewing) {
            CameraInfo cameraInfo = new CameraInfo();
            int cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == 1) {
                    try {
                        this.camera1 = Camera.open(camIdx);
                    } catch (RuntimeException e) {
                    }
                }
            }
            if (this.camera1 != null) {
                try {
                    this.camera1.setPreviewDisplay(this.surfaceHolder1);
                    this.camera1.startPreview();
                    this.previewing = true;
                    this.camera1.setDisplayOrientation(90);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    private void starcam() {
        if (!this.previewing) {
            this.camera = Camera.open();
            if (this.camera != null) {
                try {
                    this.camera.setPreviewDisplay(this.surfaceHolder);
                    this.camera.startPreview();
                    this.previewing = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void stopcam() {
        if (this.camera != null && this.previewing) {
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
            this.previewing = false;
        }
    }

    private void stopcam1() {
        if (this.camera1 != null && this.previewing1) {
            this.camera1.stopPreview();
            this.camera1.release();
            this.camera1 = null;
            this.previewing = false;
        }
        if (this.camera != null && this.previewing) {
            this.camera.stopPreview();
            this.camera.release();
            this.camera = null;
            this.previewing = false;
        }
    }


    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopcam1();
        stopcam();
    }


    public Bitmap getBitmapFromAsset(String path) {

        AssetManager am = VideoCallScreenActivity.this.getAssets();
        InputStream is = null;
        Bitmap bitmap = null;
        try {
            is = am.open(path);
            bitmap = BitmapFactory.decodeStream(is);
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
            File f = new File(path);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeStream(new FileInputStream(f), null, options);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void addHistory(int i) {
        CallHistoryHelper helper = new CallHistoryHelper(getApplicationContext());
        Date date = new Date();
        CharSequence s = DateFormat.format("dd/MM", date.getTime());
        CharSequence time = DateFormat.format("HH:mm:ss a", date.getTime());
        //Log.e("TAG", "addHistory: "+time );
        helper.insertData(persons.get(VideoAdapter.selectedPerson).name, persons.get(VideoAdapter.selectedPerson).phonenumber, String.valueOf(s), String.valueOf(time), 1, i);

    }
}