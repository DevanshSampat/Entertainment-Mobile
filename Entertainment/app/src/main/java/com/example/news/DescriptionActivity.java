package com.example.news;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.transition.Fade;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.w3c.dom.ls.LSException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DescriptionActivity extends AppCompatActivity {

    private int position;
    private int hour;
    private int minute;
    private long time = 0;
    private boolean downloaded = false;
    private static final int PERMISSION_STORAGE_CODE = 1000;
    private boolean begin = false;
    private long view_count = 0;
    private long downloadId=0L;
    private boolean downloading = false;
    private boolean dark;
    private boolean longClick = false;
    private String downloadSize;
    private String developer_email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        findViewById(R.id.views).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout).startAnimation(AnimationUtils.loadAnimation(this,R.anim.appear_up));
        findViewById(R.id.top_layout).startAnimation(AnimationUtils.loadAnimation(this,R.anim.appear_down));
        FirebaseDatabase.getInstance().getReference("developer_email").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                developer_email = snapshot.getValue().toString();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        findViewById(R.id.image).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(Objects.requireNonNull(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail()).equals(developer_email)){
                    Intent intent = new Intent(DescriptionActivity.this,FirebaseNotificationActivity.class);
                    intent.putExtra("title",getIntent().getStringExtra("name"));
                    intent.putExtra("text","Now streaming on Entertainment");
                    intent.putExtra("image",getIntent().getStringExtra("image"));
                    intent.putExtra("data","search:"+getIntent().getStringExtra("name")+"\nopen:true");
                    startActivity(intent);
                }
                return false;
            }
        });
        if(!Settings.System.canWrite(getApplicationContext()))
        {
            Toast.makeText(getApplicationContext(),"Permission required to adjust volume and brightness in video player",Toast.LENGTH_LONG).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+BuildConfig.APPLICATION_ID));
                    startActivity(intent);
                }
            },1500);
        }
        if(!getIntent().hasExtra("description")) findViewById(R.id.description).setVisibility(View.GONE);
        loadData();
        findViewById(R.id.play_from_beginning).setVisibility(View.GONE);
        downloaded = false;
        if (new File(getApplicationContext().getExternalFilesDir(null),
                getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4").exists()) {
            downloaded = true;
            TextView textView = findViewById(R.id.text_download);
            textView.setText("Delete");
            ((ImageView)findViewById(R.id.download_image)).setImageResource(R.drawable.ic_baseline_delete_24);
        }
        final DocumentReference documentReference = FirebaseFirestore.getInstance().collection("Video Data").document(getIntent().getStringExtra("name"));
        documentReference.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if(findViewById(R.id.description).getVisibility()!=View.VISIBLE) return;
                try {
                    view_count = Long.parseLong(documentSnapshot.getString("Views"));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                String str;
                if(view_count>=10000000)
                {
                    str = String.valueOf(view_count/10000000);
                    view_count = view_count%10000000;
                    view_count = view_count/1000000;
                    if(view_count>0) str = str + "." + view_count;
                    str = str + " Crore";
                }
                else if(view_count>=100000)
                {
                    str = String.valueOf(view_count/100000);
                    view_count = view_count%100000;
                    view_count = view_count/10000;
                    if(view_count>0) str = str + "." + view_count;
                    str = str + " Lakh";
                }
                else str = String.valueOf(view_count);
                TextView textView = findViewById(R.id.views);
                textView.setText(str+" Views");
                textView.setVisibility(View.VISIBLE);
            }
        });
        if(getIntent().getStringExtra("movie_db")==null) findViewById(R.id.watchlist).setVisibility(View.GONE);
        else{
            FirebaseDatabase.getInstance().getReference(getIntent().getStringExtra("movie_db")+"info").addValueEventListener(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try{
                        ((TextView)findViewById(R.id.detailed_description)).setText(snapshot.getValue().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
        FirebaseFirestore.getInstance().collection("Users").document(UserName.getUsername(getApplicationContext())).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                try{
                    ArrayList<String> watchList = (ArrayList<String>) documentSnapshot.get("Watchlist");
                    if(watchList.contains(getIntent().getStringExtra("movie_db"))) {
                        ((TextView) findViewById(R.id.text_watchlist)).setText("Added to List");
                        ((ImageView)findViewById(R.id.list_image)).setImageResource(R.drawable.ic_baseline_check_24);
                    }
                } catch (Exception e) {

                }

            }
        });
        findViewById(R.id.watchlist).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if(((TextView)findViewById(R.id.text_watchlist)).getText().equals("Add to List")) {
                    new Sync().addToWatchList(getApplicationContext(), getIntent().getStringExtra("movie_db"));
                    ((TextView)findViewById(R.id.text_watchlist)).setText("Added to List");
                    ((ImageView)findViewById(R.id.list_image)).setImageResource(R.drawable.ic_baseline_check_24);
                }
                else{
                    new Sync().removeFromWatchList(getApplicationContext(), getIntent().getStringExtra("movie_db"));
                    ((TextView)findViewById(R.id.text_watchlist)).setText("Add to List");
                    ((ImageView)findViewById(R.id.list_image)).setImageResource(R.drawable.ic_baseline_add_to_list_24);
                }
            }
        });
        findViewById(R.id.play_from_beginning).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                begin = true;
                Intent intent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
                intent.putExtra("name", getIntent().getStringExtra("name"));
                if(getIntent().hasExtra("link")) intent.putExtra("link", getIntent().getStringExtra("link"));
                intent.putExtra("begin", true);
                intent.putExtra("image",getIntent().getStringExtra("image"));
                intent.putExtra("description",getIntent().getStringExtra("description"));
                intent.putExtra("movie_db",getIntent().getStringExtra("movie_db"));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                if(findViewById(R.id.description).getVisibility()==View.VISIBLE) intent.putExtra("online",true);
                printHistory(getIntent().getStringExtra("name"));
                if(getIntent().hasExtra("add_to_quick_picks")) new Sync().addToQuickPicks(getApplicationContext(),
                        getIntent().getStringExtra("add_to_quick_picks"));
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
            }
        });
        if (getIntent().hasExtra("dark")||new Theme(this).isInDarkMode()) {
            findViewById(R.id.linear_layout).setBackgroundColor(Color.parseColor("#000000"));
            TextView textView = findViewById(R.id.title);
            textView.setTextColor(Color.WHITE);
            textView = findViewById(R.id.description);
            textView.setTextColor(Color.parseColor("#CCCCCC"));
            textView = findViewById(R.id.views);
            textView.setTextColor(Color.parseColor("#CCCCCC"));
            textView = findViewById(R.id.detailed_description);
            textView.setTextColor(Color.parseColor("#CCCCCC"));
            dark=true;
        }
        findViewById(R.id.image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.watch).callOnClick();
            }
        });
        findViewById(R.id.watch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                begin = false;
                if(longClick){
                    longClick = false;
                    return;
                }
                Intent intent = new Intent(getApplicationContext(), VideoPlayerActivity.class);
                intent.putExtra("image",getIntent().getStringExtra("image"));
                intent.putExtra("description",getIntent().getStringExtra("description"));
                intent.putExtra("name", getIntent().getStringExtra("name"));
                intent.putExtra("movie_db",getIntent().getStringExtra("movie_db"));
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                if(getIntent().hasExtra("link")) intent.putExtra("link", getIntent().getStringExtra("link"));
                if(findViewById(R.id.description).getVisibility()==View.VISIBLE) intent.putExtra("online",true);
                printHistory(getIntent().getStringExtra("name"));
                if(getIntent().hasExtra("add_to_quick_picks")) new Sync().addToQuickPicks(getApplicationContext(),
                        getIntent().getStringExtra("add_to_quick_picks"));
                startActivity(intent);
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URLConnection connection = new URL(getIntent().getStringExtra("link")).openConnection();
                    connection.connect();
                    double size = connection.getContentLengthLong();
                    String string = "";
                    String index = "MB";
                    size = size/(1024*1024);
                    if(size>=1024){
                        size = size/1024;
                        index = "GB";
                    }
                    string = String.valueOf(size);
                    try{
                        string = string.substring(0,string.indexOf('.')+3);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    String finalString = string;
                    String finalIndex = index;
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            downloadSize = finalString+" "+finalIndex;
                            if(!getIntent().hasExtra("link")||new File(getExternalFilesDir(null),
                                    getIntent().getStringExtra("name").replace(':','_')
                                            .replace(' ','_')+".mp4").exists()) return;
                            ((TextView)findViewById(R.id.text_download)).setText(finalString+" "+finalIndex);
                            ((ImageView)findViewById(R.id.download_image)).setImageResource(R.drawable.ic_baseline_arrow_download_24);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        findViewById(R.id.download).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!getIntent().hasExtra("link")&&!downloaded)
                {
                    Toast.makeText(getApplicationContext(),"Download link not found",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!new File(getApplicationContext().getExternalFilesDir(null),
                        getIntent().getStringExtra("name").replace(':','_').replace(' ','_')+".mp4").exists()) download();
                else showDeleteDialog();
            }
        });
        findViewById(R.id.watch).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                longClick = true;
                if(!isMovieDownloading()) return false;
                Intent intent = new Intent(getApplicationContext(),PauseOrResumeMovieDownloadReceiver.class);
                intent.putExtra("name",getIntent().getStringExtra("name"));
                if(DownloadFileData.getText().equals("Long Press to Resume")) intent.putExtra("status","resume");
                else intent.putExtra("status","pause");
                sendBroadcast(intent);
                return false;
            }
        });
        try {
            if (getIntent().getStringExtra("image").substring(0, 4).equals("http")) {
                File file = new File(getExternalFilesDir(null), ".screenshot_of_" + getIntent().getStringExtra("name")
                        .replace(' ', '_').replace(':', '_') + ".jpeg");
                if (file.exists()) {
                    ((ImageView)findViewById(R.id.image)).setImageBitmap(BitmapFactory.decodeFile(file.getPath()));
                } else
                    Picasso.with(this).load(Uri.parse(getIntent().getStringExtra("image"))).into((ImageView) findViewById(R.id.image));
            }
            else
            {
                ImageView imageView = findViewById(R.id.image);
                Bitmap bitmap = BitmapFactory.decodeFile(getIntent().getStringExtra("image"));
                imageView.setImageBitmap(bitmap);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        TextView textView = findViewById(R.id.title);
        textView.setText(getIntent().getStringExtra("name"));
        textView = findViewById(R.id.description);
        if(getIntent().hasExtra("description")) textView.setText(getIntent().getStringExtra("description"));
        try {
            FileInputStream fis = null;
            fis = getApplicationContext().openFileInput(getIntent().getStringExtra("name").replace(' ', '+') + ".txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            try {
                String str = br.readLine();
                if(str.contains("\t")) str=str.substring(0,str.indexOf('\t'));
                position = Integer.parseInt(str);
                position = position / 1000;
                hour = position / 3600;
                minute = (position / 60) % 60;
                StringBuilder stringBuilder = new StringBuilder();
                if (hour > 0) stringBuilder.append(hour + "hr ");
                if (minute > 0) stringBuilder.append(minute + "min");
                findViewById(R.id.play_from_beginning).setVisibility(View.VISIBLE);
                if (hour == 0 && minute == 0) {
                    stringBuilder.append("Play");
                    findViewById(R.id.play_from_beginning).setVisibility(View.GONE);
                }
                TextView play_text = findViewById(R.id.play_text);
                play_text.setText(stringBuilder.toString());
                if (position == 0) play_text.setText("Play");
            } catch (Exception exception) {
                //Nothing
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        downloaded = false;
        if (new File(getApplicationContext().getExternalFilesDir(null),
                getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4").exists()) {
            downloaded = true;
            textView = findViewById(R.id.play_text);
            textView.setText(textView.getText().toString() + " Offline");
        }
    }

    private void showDeleteDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.delete_dialog);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setDimAmount(0.80f);
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = 100;
        lp.verticalMargin = 5;
        lp.horizontalMargin = 5;
        dialog.getWindow().setAttributes(lp);
        dialog.setCancelable(true);
        TextView textView = dialog.findViewById(R.id.text_warning);
        textView.setText("Do you want to delete "+getIntent().getStringExtra("name")+"?");
        if(getIntent().hasExtra("dark")||new Theme(this).isInDarkMode()) {
            textView = dialog.findViewById(R.id.text_warning);
            CardView cardView = dialog.findViewById(R.id.delete_card);
            textView.setTextColor(Color.parseColor("#CCCCCC"));
            cardView.setCardBackgroundColor(Color.parseColor("#222222"));
            textView = dialog.findViewById(R.id.text_keep);
            cardView = dialog.findViewById(R.id.keep);
            textView.setTextColor(Color.parseColor("#CCCCCC"));
            cardView.setCardBackgroundColor(Color.parseColor("#393939"));
            cardView = dialog.findViewById(R.id.card);
            cardView.setCardBackgroundColor(Color.parseColor("#393939"));
        }
        dialog.findViewById(R.id.keep).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.delete_movie).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                delete();
                dialog.dismiss();
            }
        });
        dialog.show();
        dialog.getWindow().setDimAmount(0.80f);
    }

    private void download() {
        try {
            FileInputStream fileInputStream = getApplicationContext().openFileInput("download_id_for_"+getIntent().getStringExtra("name")
                .replace(':','_').replace(' ','_')+".txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fileInputStream));
            String str = br.readLine();
            try {
                downloadId=Long.parseLong(str);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(new File(getApplicationContext().getFilesDir(),"isDownloading.txt").exists()&&downloadId==0)
        {
            String title = "";
            try {
                FileInputStream fis = getApplicationContext().openFileInput("isDownloading.txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                title=br.readLine();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Toast.makeText(getApplicationContext(),"Wait for "+title+" to complete download",Toast.LENGTH_SHORT).show();
            new DownloadNotification().enqueueDownload(getApplicationContext(),getIntent().getStringExtra("name"),getIntent().getStringExtra("link"));
            return;
        }
        if(downloadId!=0L&&downloadId!=-1&&!new File(getApplicationContext().getExternalFilesDir(null),
                getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4").exists())
        {
            new File(getApplicationContext().getFilesDir(),"download_id_for_"+
                    getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".txt").delete();
            downloadId=-1;
            return;
        }
        final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (!new File(getApplicationContext().getExternalFilesDir(null),
                getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4").exists()
                || downloadId>0  ) {
            if(new File(getApplicationContext().getExternalFilesDir(null),
                    getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4").exists()) {
                TextView textView = findViewById(R.id.text_download);
                textView.setText("Delete");
                ((ImageView)findViewById(R.id.download_image)).setImageResource(R.drawable.ic_baseline_delete_24);
            }
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(getIntent().getStringExtra("link")));
            request.setTitle(getIntent().getStringExtra("name"));
            request.setDescription("Entertainment");
            request.allowScanningByMediaScanner();
            request.setDestinationInExternalFilesDir(getApplicationContext(), "",
                    getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4");
            request.setVisibleInDownloadsUi(false);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            if(downloadId==0L) {
                downloadId = Objects.requireNonNull(manager).enqueue(request);
                if(!new File(getApplicationContext().getFilesDir(),"isDownloading.txt").exists())
                {
                    try {
                        new File(getApplicationContext().getFilesDir(),"isDownloading.txt").createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    FileOutputStream fos = getApplicationContext().openFileOutput("isDownloading.txt",MODE_PRIVATE);
                    fos.write(getIntent().getStringExtra("name").getBytes());
                    fos.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DownloadFileData.setText("Fetching Link");
                DownloadFileData.setPercent(0);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getDownloadProgress();
                    }
                },1000);
                TextView textView = findViewById(R.id.text_download);
                textView.setText("Delete");
                ((ImageView)findViewById(R.id.download_image)).setImageResource(R.drawable.ic_baseline_delete_24);
                File file = new File(getApplicationContext().getFilesDir(),"download_id_for_"+getIntent().getStringExtra("name")
                        .replace(':','_').replace(' ','_')+".txt");
                if(!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    FileOutputStream fileOutputStream = getApplicationContext().openFileOutput("download_id_for_"+getIntent().getStringExtra("name")
                            .replace(':','_').replace(' ','_')+".txt",MODE_PRIVATE);
                    fileOutputStream.write((downloadId+"\n").getBytes());
                    fileOutputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        new DownloadNotification().generateDownloadNotification(getApplicationContext(),this,dark);
    }

    private void delete() {
        TextView textView = findViewById(R.id.text_download);
        textView.setText("Save");
        ((ImageView)findViewById(R.id.download_image)).setImageResource(R.drawable.ic_baseline_arrow_download_24);
        if(downloadSize!=null) textView.setText(downloadSize);
        new File(getApplicationContext().getFilesDir(),"download_id_for_"+getIntent().getStringExtra("name")
                .replace(':','_').replace(' ','_')+".txt").delete();
        if(downloadId>0)
        {
            try {
                FileInputStream fis = getApplicationContext().openFileInput("isDownloading.txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                if(br.readLine().equals(getIntent().getStringExtra("name")))
                {
                    new File(getApplicationContext().getFilesDir(),"isDownloading.txt").delete();
                    new DownloadNotification().goToNextTask(DescriptionActivity.this,dark);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            DownloadManager manager = (DownloadManager)  getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            manager.remove(downloadId);
            NotificationManagerCompat.from(getApplicationContext()).cancel(18);
            downloading=false;
            downloadId=0;
        }
        downloaded = false;
        Uri updateFileUri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider",
                new File(getApplicationContext().getExternalFilesDir(null),
                        getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4"));
        getContentResolver().delete(updateFileUri, null, null);
        updateFileUri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".provider",
                new File(getApplicationContext().getExternalFilesDir(null),
                        getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".jpg"));
        getContentResolver().delete(updateFileUri, null, null);
        Toast.makeText(getApplicationContext(), "Deleted " + getIntent().getStringExtra("name"), Toast.LENGTH_SHORT).show();
        onResume();
    }
    @SuppressLint("SetTextI18n")
    private void getDownloadProgress()
    {
        if(!new File(getApplicationContext().getFilesDir(),"isDownloading.txt").exists()) {
            ((TextView)findViewById(R.id.play_text)).setText("Play");
            loadData();
            return;
        }
        String str = DownloadFileData.getPercent()+"%";
        if(DownloadFileData.getText().equals("Long Press to Resume")) str = str + "\nPaused";
        ((TextView)findViewById(R.id.play_text)).setText(str);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getDownloadProgress();
            }
        },1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(new File(getApplicationContext().getFilesDir(),"download_id_for_"
                +getIntent().getStringExtra("name").replace(':','_').replace(' ','_')+".txt").exists())
        {
            try {
                FileInputStream fis = getApplicationContext().openFileInput("download_id_for_"
                        +getIntent().getStringExtra("name").replace(':','_').replace(' ','_')+".txt");
                BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                try {
                    downloadId=Long.parseLong(br.readLine());
                } catch (NumberFormatException e) {
                    downloadId=0L;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileInputStream fis = getApplicationContext().openFileInput("isDownloading.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            if(br.readLine().equals(getIntent().getStringExtra("name")))
            {
                try{
                    try {
                        fis = getApplicationContext().openFileInput("download_id_for_" + getIntent().getStringExtra("name")
                                .replace(':', '_').replace(' ', '_') + ".txt");
                    } catch (FileNotFoundException e) {
                        new File(getApplicationContext().getFilesDir(),"isDownloading.txt").delete();
                        Toast.makeText(getApplicationContext(),"An error occured",Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                    br = new BufferedReader(new InputStreamReader(fis));
                    downloadId = Long.parseLong(br.readLine());
                    DownloadManager manager = (DownloadManager) getApplicationContext().getSystemService(DOWNLOAD_SERVICE);
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    try{
                    Cursor cursor = manager.query(query);
                    cursor.moveToFirst();
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        new File(getApplicationContext().getFilesDir(), "isDownloading.txt").delete();
                        NotificationManagerCompat.from(getApplicationContext()).cancel(18);
                        Toast.makeText(getApplicationContext(), "Downloaded", Toast.LENGTH_SHORT).show();
                    }
                    else if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_FAILED) {
                        new File(getApplicationContext().getFilesDir(), "isDownloading.txt").delete();
                        NotificationManagerCompat.from(getApplicationContext()).cancel(18);
                    }
                    else {
                        getDownloadProgress();
                    }
                } catch (Exception exception) {
                        new File(getApplicationContext().getFilesDir(),"isDownloading.txt").delete();
                        Toast.makeText(getApplicationContext(),"An error occured",Toast.LENGTH_SHORT).show();
                    }

            } catch (NumberFormatException e) {
                    downloadId=0L;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Handler().postDelayed(new Runnable() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                loadData();
            }
        }, 1000);
        if(getIntent().hasExtra("no_ui_thread")||new File(getApplicationContext().getFilesDir(),"isDownloading.txt").exists())
        {
            return;
        }
        NotificationManagerCompat.from(getApplicationContext()).cancel(18);
        if(!downloading)
        {
            ProgressBar progressBar = findViewById(R.id.download_progress);
            progressBar.setProgress(100);
            TextView textView = findViewById(R.id.play_text);
            textView.setText("Play");
        }
    }

    private void loadData() {
        try {
            FileInputStream fis = null;
            fis = getApplicationContext().openFileInput(getIntent().getStringExtra("name").replace(' ', '+') + ".txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            try {
                String str = br.readLine();
                String temp = str;
                if(str.contains("\t")) str=str.substring(0,str.indexOf('\t'));
                position = Integer.parseInt(str);
                position = position / 1000;
                int time_watched = position;
                hour = position / 3600;
                minute = (position / 60) % 60;
                StringBuilder stringBuilder = new StringBuilder();
                if (hour > 0) stringBuilder.append(hour + "h");
                if (minute > 0) stringBuilder.append(minute + "m");
                findViewById(R.id.play_from_beginning).setVisibility(View.VISIBLE);
                if (hour == 0 && minute == 0) {
                    stringBuilder.append("Play");
                    findViewById(R.id.play_from_beginning).setVisibility(View.GONE);
                }
                TextView play_text = findViewById(R.id.play_text);
                play_text.setText(stringBuilder.toString());
                if (position == 0) play_text.setText("Play");
                if (new File(getApplicationContext().getExternalFilesDir(null),
                        getIntent().getStringExtra("name").replace(' ', '_').replace(':', '_') + ".mp4").exists()) {
                    play_text.setText(play_text.getText().toString() + "\nOffline");
                }
                if(temp.contains("\t"))
                {
                    temp = temp.substring(temp.indexOf('\t')+1);
                    if(temp.contains("\t")) temp = temp.substring(0,temp.indexOf('\t'));
                    int total_time = (Integer.parseInt(temp))/1000;
                    ProgressBar progressBar = findViewById(R.id.progress_of_movie);
                    progressBar.setMax(total_time);
                    progressBar.setProgress(time_watched);
                }
                try {
                    if (getIntent().getStringExtra("image").substring(0, 4).equals("http")) {
                        File file = new File(getExternalFilesDir(null), ".screenshot_of_" + getIntent().getStringExtra("name")
                                .replace(' ', '_').replace(':', '_') + ".jpeg");
                        if (file.exists()) {
                            ((ImageView)findViewById(R.id.image)).setImageBitmap(BitmapFactory.decodeFile(file.getPath()));
                        } else
                            Picasso.with(DescriptionActivity.this).load(Uri.parse(getIntent().getStringExtra("image"))).into((ImageView) findViewById(R.id.image));
                    }
                    else
                    {
                        ImageView imageView = findViewById(R.id.image);
                        Bitmap bitmap = BitmapFactory.decodeFile(getIntent().getStringExtra("image"));
                        imageView.setImageBitmap(bitmap);
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception exception) {
                //Nothing
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void printHistory(String str) {
        try {
            printDate();
            FileOutputStream fos = openFileOutput("History.txt", MODE_APPEND);
            Calendar calendar = Calendar.getInstance();
            if (calendar.get(Calendar.HOUR_OF_DAY) < 10) fos.write("0".getBytes());
            fos.write((calendar.get(Calendar.HOUR_OF_DAY) + ":").getBytes());
            if (calendar.get(Calendar.MINUTE) < 10) fos.write("0".getBytes());
            fos.write((calendar.get(Calendar.MINUTE) + "\t\t" + str + "\n").getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printDate() {
        File temp_history = new File(this.getFilesDir(), "TempHistory.txt");
        try {
            File file = new File(this.getFilesDir(), "Date.txt");
            if (!file.exists()) {
                file.createNewFile();
                try {
                    FileOutputStream fileOutputStream = openFileOutput("Date.txt", MODE_PRIVATE);
                    fileOutputStream.write("00000000".getBytes());
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            FileInputStream fis = null;
            fis = this.openFileInput("Date.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String str;
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            Calendar calendar = Calendar.getInstance();
            if (!sb.toString().substring(0, 4).equals(String.valueOf(calendar.get(Calendar.YEAR)))) {
                try {
                    FileOutputStream fileOutputStream = openFileOutput("History.txt", MODE_PRIVATE);
                    fileOutputStream.write(("History of " + String.valueOf(calendar.get(Calendar.YEAR))).getBytes());
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!sb.toString().equals(calendar.get(Calendar.YEAR) + "" + (calendar.get(Calendar.MONTH) + 1) + "" + calendar.get(Calendar.DAY_OF_MONTH))) {
                try {
                    FileOutputStream fos = openFileOutput("Date.txt", MODE_PRIVATE);
                    fos.write((calendar.get(Calendar.YEAR) + "" + (calendar.get(Calendar.MONTH) + 1) + "" + calendar.get(Calendar.DAY_OF_MONTH)).getBytes());
                    fos.close();
                    File file_history = new File(this.getFilesDir(), "History.txt");
                    fos = openFileOutput("History.txt", MODE_APPEND);
                    fos.write(("\n" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + (1 + calendar.get(Calendar.MONTH)) + "\n" + "\n").getBytes());
                    fos = openFileOutput("TempHistory.txt", MODE_APPEND);
                    fos.write(("\n" + calendar.get(Calendar.DAY_OF_MONTH) + "/" + (1 + calendar.get(Calendar.MONTH)) + "\n" + "\n").getBytes());
                    try {
                        FileOutputStream fileOutputStream = openFileOutput("Backup.txt", MODE_PRIVATE);
                        fileOutputStream.write("true".getBytes());
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private boolean isMovieDownloading()
    {
        try {
            FileInputStream fis = openFileInput("isDownloading.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            if(br.readLine().equals(getIntent().getStringExtra("name"))) return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        findViewById(R.id.top_layout).startAnimation(AnimationUtils.loadAnimation(this,R.anim.disappear_up));
        findViewById(R.id.layout).startAnimation(AnimationUtils.loadAnimation(this,R.anim.disappear_down));
        findViewById(R.id.top_layout).setVisibility(View.INVISIBLE);
        findViewById(R.id.layout).setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                DescriptionActivity.super.onBackPressed();
                overridePendingTransition(R.anim.fade_in,R.anim.fade_out);
            }
        },230);
    }
}