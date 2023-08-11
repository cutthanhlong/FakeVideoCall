package com.vocsy.fakecall.ui;


import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import com.vocsy.fakecall.R;
import com.vocsy.fakecall.model.UserModel;
import com.vocsy.fakecall.adapter.VideoAdapter;
import com.vocsy.fakecall.data.UserDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FinalVideoCallScreenActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static int aux = 0;
    private static int aux2 = 0;
    String TAG = "BTSFinal_video";
    int cameraPos = 0;
    LinearLayout Lcancel;
    ImageView rotate_camera;
    RelativeLayout camevv;
    Camera camera;
    boolean previewing = true;
    boolean previewing1 = true;
    SurfaceHolder surfaceHolder;
    SurfaceView surfaceView;
    LinearLayout vidvid;

    List<UserModel> userModels = new ArrayList<>();
    UserDatabase database;
    ImageView mutevideocall;
    VideoView videoview;
    int appNameStringRes = R.string.app_name;
    Uri uri;


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
        setContentView(R.layout.activity_final_video_call);

        database = new UserDatabase(this);
        userModels = database.retriveData();

        mutevideocall = (ImageView) findViewById(R.id.mutevideocall);
        videoview = findViewById(R.id.videoview);
        final ImageView mutevideomic = (ImageView) findViewById(R.id.mutemicvideo);
        ImageView endcallvideo = (ImageView) findViewById(R.id.endcallvideo);
        rotate_camera = findViewById(R.id.rotate_camera);
        camevv = findViewById(R.id.camevv);

        rotate_camera.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        this.Lcancel = (LinearLayout) findViewById(R.id.canselliner);
        this.vidvid = (LinearLayout) findViewById(R.id.vidvid);

        this.surfaceView = findViewById(R.id.cameravideo);
        this.surfaceHolder = surfaceView.getHolder();
        this.surfaceHolder.addCallback(this);
        this.surfaceHolder.setType(3);

        CameraInfo cameraInfo = new CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == 1) {
                try {
                    this.camera = Camera.open(camIdx);
                } catch (RuntimeException e) {
                }
            }
        }
        if (this.camera != null) {
            try {
                this.camera.setPreviewDisplay(this.surfaceHolder);
                this.camera.startPreview();
                this.camera.setDisplayOrientation(90);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }

        /* int starname = BTSPref.getPreference(BTSFinal_video.this, "BTSstar");*/


        if (userModels.get(VideoAdapter.selectedPerson).getType().equals("Asset")) {

            Log.e("video name", "====== " + userModels.get(VideoAdapter.selectedPerson).getVideo());
            videoview.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/raw/" + userModels.get(VideoAdapter.selectedPerson).getVideo()));

            uri = Uri.parse("android.resource://" + getPackageName() + "/raw/" + userModels.get(VideoAdapter.selectedPerson).getVideo());

        } else {

            Log.e("video audio", "====== " + userModels.get(VideoAdapter.selectedPerson).getVideo());

            try {
                // Uri uri = FileProvider.getUriForFile(this, getPackageName()+".provider", new File(userModels.get(VideoAdapter.selectedPerson).getVideo()));
                videoview.setVideoURI(Uri.parse(userModels.get(VideoAdapter.selectedPerson).getVideo()));
                uri = Uri.parse(userModels.get(VideoAdapter.selectedPerson).getVideo());
            } catch (Exception exception) {

                Log.e("Exception", "videoview====" + exception.toString());
            }

        }
        videoview.start();

        /*videoview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoview.start();
            }
        });*/

        videoview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });


        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                showcam1();
                showcam1();
            }
        }, 200);


        endcallvideo.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                    finish();

            }
        });

        mutevideocall.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (FinalVideoCallScreenActivity.aux % 2 == 0) {
                    stopcam();
                    surfaceView.setVisibility(View.INVISIBLE);
                    camevv.setVisibility(View.INVISIBLE);
                    FinalVideoCallScreenActivity.access$308();
                    mutevideocall.setImageDrawable(getResources().getDrawable(R.drawable.ic_vidcall_video_off));
                    return;
                }
                FinalVideoCallScreenActivity.access$308();
                surfaceView.setVisibility(View.VISIBLE);
                camevv.setVisibility(View.VISIBLE);
                if (!previewing) {
                    CameraInfo cameraInfo = new CameraInfo();
                    int cameraCount = Camera.getNumberOfCameras();
                    for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                        Camera.getCameraInfo(camIdx, cameraInfo);
                        if (cameraInfo.facing == 1) {
                            try {
                                camera = Camera.open(camIdx);
                                cameraPos = camIdx;
                            } catch (RuntimeException e) {
                            }
                        }
                    }
                    if (camera != null) {
                        try {
                            camera.setPreviewDisplay(surfaceHolder);
                            camera.startPreview();
                            previewing = true;
                            camera.setDisplayOrientation(90);
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                }
                mutevideocall.setImageDrawable(getResources().getDrawable(R.drawable.ic_vidcall_video_on));
            }
        });
        mutevideomic.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (FinalVideoCallScreenActivity.aux2 % 2 == 0) {
                    FinalVideoCallScreenActivity.access$508();
                    mutevideomic.setImageDrawable(getResources().getDrawable(R.drawable.ic_vidcall_mike_off));
                    return;
                }
                FinalVideoCallScreenActivity.access$508();
                mutevideomic.setImageDrawable(getResources().getDrawable(R.drawable.ic_vidcall_mike_on));
            }
        });
    }


    private void showcam1() {
        if (FinalVideoCallScreenActivity.aux % 2 == 0) {
            stopcam();
            surfaceView.setVisibility(View.INVISIBLE);
            camevv.setVisibility(View.INVISIBLE);
            FinalVideoCallScreenActivity.access$308();
            mutevideocall.setImageDrawable(getResources().getDrawable(R.drawable.ic_vidcall_video_off));
            return;
        }
        FinalVideoCallScreenActivity.access$308();
        surfaceView.setVisibility(View.VISIBLE);
        camevv.setVisibility(View.VISIBLE);
        if (!previewing) {
            CameraInfo cameraInfo = new CameraInfo();
            int cameraCount = Camera.getNumberOfCameras();
            for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == 1) {
                    try {
                        camera = Camera.open(camIdx);
                        cameraPos = camIdx;
                    } catch (RuntimeException e) {
                    }
                }
            }
            if (camera != null) {
                try {
                    camera.setPreviewDisplay(surfaceHolder);
                    try {
                        camera.startPreview();

                    } catch (Exception e) {
                        Log.e(TAG, "showcam1: " + e.getMessage());
                    }
                    previewing = true;
                    camera.setDisplayOrientation(90);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
        mutevideocall.setImageDrawable(getResources().getDrawable(R.drawable.ic_vidcall_video_on));
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
        if (videoview.isPlaying()) {
            videoview.pause();
        }
        stopcam1();
        stopcam();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        //startActivity(new Intent(BTSFinal_video.this, MainActivity.class));
    }

    public void switchCamera() {
        camera.stopPreview();
        camera.release();

        if (cameraPos == 0) {
            camera = Camera.open(1);
            cameraPos = 1;
        } else {
            camera = Camera.open(0);
            cameraPos = 0;
        }

        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
                camera.setDisplayOrientation(90);
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

}
