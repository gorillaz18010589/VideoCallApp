package com.wen.reverse;
/*
*https://www.youtube.com/watch?v=5Wn6rpJGn5o 08:20
* */
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class CallActivity extends AppCompatActivity {
    private String TAG = "hank";
    private Button callBtn;
    private LinearLayout callControlLayout;
    private RelativeLayout callLayout;
    private RelativeLayout inputLayout;
    private ImageView toggleAudioBtn;
    private ImageView toggleVideoBtn;
    private ImageView acceptBtn;
    private ImageView rejectBtn;
    private TextView incomingCallTxt;
    private EditText friendNameEdit;
    private WebView webView;
    private String userName = "";
    private String friendUserName = "";
    private DatabaseReference fireBaseRef = FirebaseDatabase.getInstance().getReference("users");
    private boolean isPeerConnected = false;
    private boolean isAudio = true;
    private boolean isVideo = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        //1.取得使用者輸入userName參數
        userName = getIntent().getExtras().getString("userName");
        Log.v(TAG, "userName:" + userName);

        //2.init
        initView();
    }

    @SuppressLint("NewApi")
    private void initView() {
        callBtn = findViewById(R.id.callBtn);
        toggleAudioBtn = findViewById(R.id.toggleAudioBtn);
        toggleVideoBtn = findViewById(R.id.toggleVideoBtn);
        acceptBtn = findViewById(R.id.acceptBtn);
        rejectBtn = findViewById(R.id.rejectBtn);
        callLayout = findViewById(R.id.callLayout);
        callControlLayout = findViewById(R.id.callControlLayout);
        inputLayout = findViewById(R.id.inputLayout);
        incomingCallTxt = findViewById(R.id.incomingCallTxt);
        friendNameEdit = findViewById(R.id.friendNameEdit);
        webView = findViewById(R.id.webView);

        //11.打電話按下送出按鈕
        callBtn.setOnClickListener(v -> {
            friendUserName = friendNameEdit.getText().toString();
            sendCallRequest();
        });

        //9.切換Audio
        toggleAudioBtn.setOnClickListener(v -> {
            isAudio = !isAudio;
            callJavascriptFunction("javascript:toggleAudio("+ isAudio+ ")");
            toggleAudioBtn.setImageResource(isAudio ? R.drawable.ic_baseline_mic_24 : R.drawable.ic_baseline_mic_off_24);
        });

        //10.切換Viedo
        toggleVideoBtn.setOnClickListener(v -> {
            isVideo = !isVideo;
            callJavascriptFunction("javascript:toggleVideo("+ isVideo+ ")");
            toggleVideoBtn.setImageResource(isVideo ? R.drawable.ic_baseline_videocam_24 : R.drawable.ic_baseline_videocam_off_24);
        });

        //3.初始化webView
        setWebView();
    }

    /*
    * 9.撥出電話
    * */
    private void sendCallRequest() {
        //如果沒有連線直接跳警示Toast結束
        if(!isPeerConnected){
            Toast.makeText(this,"You 're not connected. Check yout internet.", Toast.LENGTH_SHORT).show();
            return;
        }

        fireBaseRef.child(friendUserName).child("incoming").setValue(userName);
        fireBaseRef.child(friendUserName).child("isAvailable").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.getValue().toString() == "true"){
                    listenForConnId();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    /*
    * 11.
    *
    */
    private void listenForConnId() {
        fireBaseRef.child(friendUserName).child("connid").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot == null) return;
                switchToControls();
                callJavascriptFunction("javascript:startCall("+snapshot.getValue()+")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setWebView() {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            //來自網頁的權限請求
            public void onPermissionRequest(PermissionRequest request) {
                super.onPermissionRequest(request);
                runOnUiThread(() -> {
                    request.grant(request.getResources());//調用此方法授予Origin訪問給定資源的權限
                    String[] permissionRequestRes = request.getResources();

                    for (String permissionRequestRe : permissionRequestRes) {
                        Log.v(TAG, "permissionRequestRe:" + permissionRequestRe);
                    }
                });
                Log.v(TAG, "setWebView() onPermissionRequest() request:" + request.getResources());
            }
        });

        //4.webView設定
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);//設定javaScript為true
        webSettings.setMediaPlaybackRequiresUserGesture(false);//手勢播放媒體設定為false
        webView.addJavascriptInterface(new JavascriptInterface(CallActivity.this), "Android");//寫一個JavascriptInterface,將CallActivity建構式傳遞,使用onPeerConnected(),讓Ｊs去使用

        //6.webView
        loadVideoCall();
    }


    //6.讀取寫好的html,當頁面載入完成呼叫Js方法init()
    private void loadVideoCall() {
//        String file = "file:///android_assets/call.html";
        String file = "file:///android_asset/call.html";
        webView.loadUrl(file);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                //7當頁面載入完成時,呼叫Ｊs -> init() 初始化initPeer
                initializePeer();
            }
        });
    }

    private String uniqueId = "";
    //7.initPeer
    private void initializePeer() {
        uniqueId = getUniqueId();

        //呼叫Js方法輸入uniqueId,創建peer物件
        callJavascriptFunction("javascript:init("+uniqueId+")");

        //監聽incoming,當有值變化時
        fireBaseRef.child(userName).child("incoming").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //8.當接到對方打來時,出現對方打電話UI
                onCallRequest((String)snapshot.getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //TODO
            }
        });
    }

    //8.當接到對方打來時,出現對方打電話UI
    private void onCallRequest(String caller) {
        if(caller == null) return;

        callLayout.setVisibility(View.VISIBLE);
        incomingCallTxt.setText(caller +"isCalling..");

        //按下接通時儲存對方connid,而且isAvailable,
        acceptBtn.setOnClickListener(v ->{
            fireBaseRef.child(userName).child("connId").setValue(uniqueId);
            fireBaseRef.child(userName).child("isAvailable").setValue(true);

            callLayout.setVisibility(View.VISIBLE);
            switchToControls();
        });

        //拒絕通話時,incoming值設定為空
        rejectBtn.setOnClickListener(v ->{
            fireBaseRef.child(userName).child("incoming").setValue(null);
            callLayout.setVisibility(View.GONE);
        });
    }

    /*
    * 關掉input,顯示callControlLayout
    * */
    private void switchToControls() {
        inputLayout.setVisibility(View.GONE);
        callControlLayout.setVisibility(View.VISIBLE);
    }

    /*
    * 取得唯一key
    * */
    private String getUniqueId(){
        return UUID.randomUUID().toString();
    }

    /*
     * 調用Js方法,使方法不會像loadUrl,刷新頁面,必須在執行緒里執行
     * @param  js方法
     * */
    @SuppressLint("NewApi")
    private void callJavascriptFunction(String functionString) {
        webView.post(() -> {
            webView.evaluateJavascript(functionString, null);//調用Js方法,使方法不會像loadUrl,刷新頁面,必須在執行緒里執行
        });
    }

    //
    public void onPeerConnected() {
        isPeerConnected = true;
    }

    //12.onBackPressed時關閉
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    //13.結束時清空值
    @Override
    protected void onDestroy() {
        fireBaseRef.child(userName).setValue(null);
        webView.loadUrl("about:blank");
        super.onDestroy();
    }
}