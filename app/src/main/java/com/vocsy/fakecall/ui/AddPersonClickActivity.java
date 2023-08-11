package com.vocsy.fakecall.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import com.bumptech.glide.Glide;
import com.vocsy.fakecall.Globals;
import com.vocsy.fakecall.ImageUtils;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.UserModel;
import com.vocsy.fakecall.newFakeCall.MainActivity;
import com.vocsy.fakecall.newFakeCall.UserDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;


public class AddPersonClickActivity extends AppCompatActivity {

    ImageView imagePerson;
    EditText personname;
    EditText personnumber;
    EditText personEmail;
    ImageView selectVideo;
    String imgfilePath;
    Uri videouri;
    String videoPath = "";
    String audioPath = "";
    ImageView selectaudioBT;
    TextView audiopathTV;
    TextView videopathTV;
    private int PROFILE_IMAGE_REQ_CODE = 101;
    ImageView backbuttondialog;
    UserDatabase database;
    List<UserModel> userModels;
    int userposition = 0;
    FrameLayout frameLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_custom_settings);

        frameLayout = findViewById(R.id.fl_adplaceholder);

        database = new UserDatabase(getApplicationContext());
        userModels = new ArrayList<>();
        opendialog();

    }

    public void opendialog() {

        Rect displayRectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

        backbuttondialog = (ImageView) findViewById(R.id.back);
        imagePerson = (ImageView) findViewById(R.id.imagePerson);
        personname = (EditText) findViewById(R.id.personname);
        personnumber = (EditText) findViewById(R.id.personNumber);
        personEmail = (EditText) findViewById(R.id.personEmail);
        selectVideo = (ImageView) findViewById(R.id.selectVideoBT);
        selectaudioBT = (ImageView) findViewById(R.id.selectaudioBT);
        videopathTV = (TextView) findViewById(R.id.videoPathTV);
        audiopathTV = (TextView) findViewById(R.id.audiopathTV);


        if (getIntent().getStringExtra("setMode") != null) {

            userModels = database.retriveData();
            userposition = getIntent().getIntExtra("userId", 0);

            try {
                if (userModels.get(userposition).getType().equals("Asset")) {
                    imagePerson.setImageBitmap(getBitmapFromAsset("person/" + userModels.get(userposition).getPhoto()));
                } else {
                    imagePerson.setImageBitmap(getBitmap(userModels.get(userposition).getPhoto()));
                }
            } catch (Exception e) {
                Log.e("exceptoin", "" + e.getMessage());
            }

            personname.setText(userModels.get(userposition).getName());
            personnumber.setText(userModels.get(userposition).getPhonenumber());
            personEmail.setText(userModels.get(userposition).getEmail());
            imgfilePath = userModels.get(userposition).getPhoto();
            videoPath = userModels.get(userposition).getVideo();
            audioPath = userModels.get(userposition).getAudio();

            if (userModels.get(userposition).getAvb().matches("video") || userModels.get(userposition).getAvb().matches("both")) {
                int cut = videoPath.lastIndexOf('/');
                int videoDot = videoPath.lastIndexOf('.');
                if (cut != -1) {
                    videopathTV.setVisibility(View.VISIBLE);
                    videopathTV.setText(videoPath.substring(cut + 1, videoDot));
                    selectVideo.setImageResource(R.drawable.ic_add_video_check);
                    ImageView iv = (ImageView) findViewById(R.id.imagePreview);
                    iv.setVisibility(View.VISIBLE);
                    Glide.with(getApplicationContext()).load(Uri.fromFile(new File(videoPath))).into(iv);
                }
            }
            if (userModels.get(userposition).getAvb().matches("audio") || userModels.get(userposition).getAvb().matches("both")) {
                int cut1 = audioPath.lastIndexOf('/');
                int audioDot = audioPath.lastIndexOf('.');
                if (cut1 != -1) {
                    selectaudioBT.setImageResource(R.drawable.ic_add_audio_check);
                    audiopathTV.setVisibility(View.VISIBLE);
                    audiopathTV.setText(audioPath.substring(cut1 + 1, audioDot));
                }
            }


            Log.e("TAG", "opendialog: " + userModels.get(userposition).getVideo());

        }
        TransferImage("1.jpg");
        TransferImage("2.jpg");

        selectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("video/*");
                startActivityForResult(pickIntent, 1);


            }

        });
        imagePerson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                ImagePicker.with(AddPersonClickActivity.this)
//                        .setFolderMode(true)
//                        .setFolderTitle("Album")
//                        .setRootDirectoryName(getString(R.string.app_name))
//                        .setDirectoryName("do not delete")
//                        .setMultipleMode(false)
//                        .setShowNumberIndicator(true)
//                        .setMaxSize(1)
//                        .setRequestCode(PROFILE_IMAGE_REQ_CODE)
//                        .start();

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction("android.intent.action.PICK");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PROFILE_IMAGE_REQ_CODE);
            }
        });
        selectaudioBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent videoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(Intent.createChooser(videoIntent, "Select Audio"), 2);
            }
        });


        backbuttondialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        ImageView buttonOk = findViewById(R.id.done);
        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (personname.getText().toString().isEmpty()) {
                    Toast.makeText(AddPersonClickActivity.this, "Please enter caller name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (personnumber.getText().toString().isEmpty()) {
                    Toast.makeText(AddPersonClickActivity.this, "Please enter Contact Number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (personEmail.getText().toString().isEmpty()) {
                    Toast.makeText(AddPersonClickActivity.this, "Please enter Email Address", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (imgfilePath == null) {
                    Toast.makeText(AddPersonClickActivity.this, "Please Select calling person image", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (videoPath.isEmpty() && audioPath.isEmpty()) {
                    Toast.makeText(AddPersonClickActivity.this, "Please select record a Video or Audio", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (getIntent().getStringExtra("setMode") != null) {
                    if (userposition < 6) {
                        // default persons
                        database.updateUserDetails(String.valueOf(userModels.get(userposition).getId()), personname.getText().toString(), personnumber.getText().toString(), imgfilePath, videoPath, "Asset", personEmail.getText().toString(), audioPath, "both");
                    } else {
                        database.updateUserDetails(String.valueOf(userModels.get(userposition).getId()), personname.getText().toString(), personnumber.getText().toString(), imgfilePath, videoPath, "FILE", personEmail.getText().toString(), audioPath, "both");
                    }

                } else {
                    // user add persons
                    if (!audioPath.isEmpty() && videoPath.isEmpty()) {
                        database.insertUSER(personname.getText().toString(), personnumber.getText().toString(), imgfilePath, videoPath, "FILE", personEmail.getText().toString(), audioPath, "0", "audio");
                    } else if (!videoPath.isEmpty() && audioPath.isEmpty()) {
                        database.insertUSER(personname.getText().toString(), personnumber.getText().toString(), imgfilePath, videoPath, "FILE", personEmail.getText().toString(), audioPath, "0", "video");
                    } else {
                        database.insertUSER(personname.getText().toString(), personnumber.getText().toString(), imgfilePath, videoPath, "FILE", personEmail.getText().toString(), audioPath, "0", "both");
                    }

                   /* persons.add(new VideoPersonModel(personname.getText().toString(), personnumber.getText().toString(), imgfilePath, null, videoPath, "FILE", personEmail.getText().toString()));
                    DataProccessor.getInstance(AddPersonClickActivity.this).setarraylistVideo(AddPersonClickActivity.this, "VideoPerson", persons);*/

                }
                ContactsBookFragment.mVoiceAdapter.notifyDataSetChanged();

                onBackPressed();
            }
        });

    }

    public void TransferImage(String name) {


        String path = Environment.getExternalStorageDirectory() + "/" + getString(R.string.app_name) + "/";


        File file1 = new File(path);

        if (!file1.exists()) {


            file1.mkdirs();

        }


        File file = new File(path + name);
        if (!file.exists()) {


            copyFile(this, path, name);
        }
    }

    private void copyFile(Context context, String path, String name) {
        AssetManager assetManager = context.getAssets();
        try {
            InputStream in = assetManager.open("background/" + name + "");
            OutputStream out = new FileOutputStream(path + name);
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }

    public Bitmap getBitmapFromAsset(String path) {

        AssetManager am = getApplicationContext().getAssets();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PROFILE_IMAGE_REQ_CODE) {

            if (resultCode == RESULT_OK) {


                Bitmap mBitmap = null;
                try {
                    mBitmap = Globals.getBitmapFromUri(this, data.getData(), 512, 512);
//                    imgfilePath = ImagePicker.getImages(data).get(0).getPath();
                    imgfilePath = ImageUtils.getRealPathFromURI(data.getData(), this);

                    //Bitmap bitmap = getBitmap(imgfilePath);

                    imagePerson.setImageBitmap(mBitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        }


        if (requestCode == 1 && null != data) {


            if (resultCode == RESULT_OK) {

                videouri = data.getData();

                videoPath = getAudioPath(videouri);

                Log.e("TAG", "onActivityResult: " + videoPath);

                int cut = videoPath.lastIndexOf('/');
                int videoDot = videoPath.lastIndexOf('.');
                if (cut != -1) {

                    videopathTV.setVisibility(View.VISIBLE);
                    selectVideo.setImageResource(R.drawable.ic_add_video_check);
                    videopathTV.setText(videoPath.substring(cut + 1, videoDot));
                }
                // videoPath = videouri.toString();

                ImageView iv = (ImageView) findViewById(R.id.imagePreview);
                iv.setVisibility(View.VISIBLE);
                Glide.with(getApplicationContext()).load(videouri).into(iv);

            }
        }
        if (requestCode == 2 && null != data) {
            if (resultCode == RESULT_OK) {

                Uri uri = data.getData();

                audioPath = getAudioPath(uri);

                int cut = audioPath.lastIndexOf('/');
                int videoDot = audioPath.lastIndexOf('.');
                if (cut != -1) {
                    audiopathTV.setVisibility(View.VISIBLE);
                    selectaudioBT.setImageResource(R.drawable.ic_add_audio_check);
                    audiopathTV.setText(audioPath.substring(cut + 1, videoDot));
                }
            }
        }
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

    private String getAudioPath(Uri uri) {
        String[] data = {MediaStore.Audio.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), uri, data, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public void onBackPressed() {
        hideKeyboard(AddPersonClickActivity.this);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("position", "2");
        startActivity(intent);
        finish();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}