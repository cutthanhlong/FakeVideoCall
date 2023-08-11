package com.vocsy.fakecall.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.vocsy.fakecall.ChatMessage;
import com.vocsy.fakecall.ChatRecyclerAdapter;
import com.vocsy.fakecall.R;
import com.vocsy.fakecall.RequestJavaV2Task;
import com.vocsy.fakecall.UserModel;
import com.vocsy.fakecall.newFakeCall.UserDatabase;

import org.lunainc.chatbar.ViewChatBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ai.api.AIDataService;
import ai.api.AIListener;
import ai.api.AIServiceContext;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;

public class ChatActivity extends AppCompatActivity implements AIListener {
    RelativeLayout addBtn;

    Button ccall;
    private List<ChatMessage> chatList = new ArrayList();
    int cuntads = 0;
    EditText editText;
    Boolean flagFab = Boolean.valueOf(true);

    MediaPlayer mMediaPlayer = new MediaPlayer();
    String message = "";
    ChatRecyclerAdapter rcadapter;
    ImageView backBT;
    RecyclerView recyclerView;
    private ViewChatBar viewChatBar;
    public CountDownTimer yourCountDownTimer;

    private String uuid = UUID.randomUUID().toString();
    // Android client for older V1 --- recommend not to use this

    private AIDataService aiDataService;
    private AIServiceContext customAIServiceContext;

    // Java V2
    private SessionsClient sessionsClient;
    private SessionName session;
    int selectedPos = 0;
    TextView title;
    ImageView icontoolbar;
    List<UserModel> userModels;
    UserDatabase database;


    private void initV2Chatbot() {
        try {
            InputStream stream = getResources().openRawResource(R.raw.test_agent_credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            session = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView((int) R.layout.fake_chat_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        backBT = findViewById(R.id.backBT);

        backBT.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        database = new UserDatabase(this);
        userModels = database.retriveData();
        if (getIntent().getStringExtra("userId") != null) {
            selectedPos = Integer.parseInt(String.valueOf(getIntent().getStringExtra("userId")));
            Log.e("TAG", "onCreate: position " + selectedPos);
        }

        title = findViewById(R.id.title);
        icontoolbar = findViewById(R.id.icontoolbar);

        try {
            if (userModels.get(selectedPos).getType().equals("Asset")) {
                icontoolbar.setImageBitmap(getBitmapFromAsset("person/" + userModels.get(selectedPos).getPhoto()));
            } else {
                icontoolbar.setImageBitmap(getBitmap(userModels.get(selectedPos).getPhoto()));
            }
        } catch (Exception e) {
            Log.e("exceptoin", "" + e.getMessage());
        }
        title.setText(userModels.get(selectedPos).getName());


        ActivityCompat.requestPermissions(this, new String[]{"android.permission.RECORD_AUDIO"}, 1);
        this.recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        this.editText = (EditText) findViewById(R.id.editText);


        initV2Chatbot();
        this.recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        this.recyclerView.setLayoutManager(linearLayoutManager);
        this.rcadapter = new ChatRecyclerAdapter(this, this.chatList);
        this.chatList.add(new ChatMessage("Hello!.".replaceAll("\r\nr", "\n"), "bot"));
        this.viewChatBar = (ViewChatBar) findViewById(R.id.chatbar);
        this.viewChatBar.setMessageBoxHint("Enter Your Message");

        this.rcadapter.notifyDataSetChanged();
        this.recyclerView.smoothScrollToPosition(this.rcadapter.getItemCount() - 1);


        final TextView tv = (TextView) findViewById(R.id.subtitle);

        this.ccall = (Button) findViewById(R.id.ccall);
        ((Button) findViewById(R.id.ccallvid)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(ChatActivity.this, VideoCallActivity.class);
                intent.putExtra("DEFULT", "YES");
                startActivity(intent);
            }
        });
        this.ccall.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {

                Intent intent = new Intent(ChatActivity.this, AudioCallActivity.class);
                intent.putExtra("DEFULT", "YES");
                startActivity(intent);

            }
        });
        this.viewChatBar.setSendClickListener(new OnClickListener() {
            @SuppressLint({"StaticFieldLeak"})
            public void onClick(View view) {
                ChatActivity.this.message = ChatActivity.this.viewChatBar.getMessageText().toString().trim();
                if (!ChatActivity.this.isNetworkAvailable()) {
                    Toast.makeText(ChatActivity.this, "No NetworkInfo", Toast.LENGTH_SHORT).show();
                    Builder builder = new Builder(ChatActivity.this);
                    builder.setTitle((CharSequence) "No Internet Connection");
                    builder.setMessage((CharSequence) "You need to have Mobile Data or wifi to access this.");
                    builder.show();
                    builder.setPositiveButton((CharSequence) "Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ChatActivity.this.finish();
                        }
                    });
                } else if (ChatActivity.this.message.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "Enter your Message !!", Toast.LENGTH_SHORT).show();
                } else {
                    QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
                    new RequestJavaV2Task(ChatActivity.this, session, sessionsClient, queryInput).execute();
                    ChatActivity.this.chatList.add(new ChatMessage(ChatActivity.this.message, "user"));
                    ChatActivity.this.rcadapter.notifyDataSetChanged();
                    ChatActivity.this.recyclerView.smoothScrollToPosition(ChatActivity.this.rcadapter.getItemCount() - 1);

                    ChatActivity.this.viewChatBar.setClearMessage(Boolean.valueOf(true));
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        public void run() {
                            tv.setText("typing...");
                        }
                    }, 1000);

                    ChatActivity.this.viewChatBar.setClearMessage(Boolean.valueOf(true));
                }
                ChatActivity chatActivity = ChatActivity.this;
                chatActivity.cuntads++;
                if (ChatActivity.this.cuntads == 3) {

                    ChatActivity.this.cuntads = 0;
                }
            }
        });
        this.editText.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Bitmap img = BitmapFactory.decodeResource(ChatActivity.this.getResources(), R.drawable.ic_send_white_24dp);
                Bitmap img1 = BitmapFactory.decodeResource(ChatActivity.this.getResources(), R.drawable.ic_mic_white_24dp);
                if (s.toString().trim().length() != 0 && ChatActivity.this.flagFab.booleanValue()) {
                    ChatActivity.this.flagFab = Boolean.valueOf(false);
                } else if (s.toString().trim().length() == 0) {
                    ChatActivity.this.flagFab = Boolean.valueOf(true);
                }
            }

            public void afterTextChanged(Editable s) {
            }
        });
        this.recyclerView.setAdapter(this.rcadapter);
    }

    public void ImageViewAnimatedChange(Context c, final ImageView v, final Bitmap new_image) {
        Animation anim_out = AnimationUtils.loadAnimation(c, R.anim.zoom_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, R.anim.zoom_in);
        anim_out.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                v.setImageBitmap(new_image);
                anim_in.setAnimationListener(new AnimationListener() {
                    public void onAnimationStart(Animation animation) {
                    }

                    public void onAnimationRepeat(Animation animation) {
                    }

                    public void onAnimationEnd(Animation animation) {
                    }
                });
                v.startAnimation(anim_in);
            }
        });
        v.startAnimation(anim_out);
    }


    public void callbackV2(DetectIntentResponse response) {
        if (response != null) {
            // process aiResponse here
            String botReply = response.getQueryResult().getFulfillmentText();
            Log.e("callbackV2", "V2 Bot Reply: " + botReply);
            this.chatList.add(new ChatMessage(botReply, "bot"));
            this.rcadapter.notifyDataSetChanged();
            this.recyclerView.smoothScrollToPosition(this.rcadapter.getItemCount() - 1);
        } else {
            Log.e("callbackV2", "Bot Reply: Null");

        }
    }


    @Override
    public void onResult(AIResponse response) {
        Result result = response.getResult();

        Log.e("anu", result.getResolvedQuery());
        this.chatList.add(new ChatMessage(result.getResolvedQuery(), "user"));
        this.recyclerView.smoothScrollToPosition(this.rcadapter.getItemCount() - 1);
        this.chatList.add(new ChatMessage(result.getFulfillment().getSpeech().replaceAll("\r\nr", "\n"), "bot"));
        this.rcadapter.notifyDataSetChanged();
        this.recyclerView.smoothScrollToPosition(this.rcadapter.getItemCount() - 1);
    }


    private boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void onError(AIError error) {
    }

    public void onAudioLevel(float level) {
    }

    public void onListeningStarted() {
    }

    public void onListeningCanceled() {
    }

    public void onListeningFinished() {
    }


    public Bitmap getBitmapFromAsset(String path) {

        AssetManager am = getAssets();
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
}
