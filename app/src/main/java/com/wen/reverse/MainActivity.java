package com.wen.reverse;
/*
* https://www.youtube.com/watch?v=fzdFe6V9W5k
* https://github.com/heyletscode/android-video-call-app
* 1.加入fireBase
* 2.加入Js
* 3.權限
*     <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    *
    * A.取得權限
    * B.輸入匡輸入userName -> callActivtiy
    *
    *
    * 4.
    *
* */
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {
    private Button loginBtn;
    private EditText usernameEdit;
    private String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };
    private int permissionRequestCode = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //A.取得權限
        if (!permissionGranted()) {
            askPermissions();
        }

        FirebaseApp.initializeApp(this);
        initView();
    }

    private void initView() {
        loginBtn = findViewById(R.id.loginBtn);
        usernameEdit = findViewById(R.id.usernameEdit);

        loginBtn.setOnClickListener( v ->{
            String userName = usernameEdit.getText().toString();
            Intent intent = new Intent(MainActivity.this,CallActivity.class);
            intent.putExtra("userName",userName);
            startActivity(intent);
        });
    }

    //詢問權限
    private void askPermissions(){
        ActivityCompat.requestPermissions(MainActivity.this, permissions, permissionRequestCode);
    }

    public boolean permissionGranted() {
        for(String permission : permissions){
            if (ActivityCompat.checkSelfPermission(this, permission )!= PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}