package com.wen.reverse;
//5.JavascriptInterface建構式取得CallActivity,讓Ｊｓ玩CallActivity的方法
public class JavascriptInterface{
    CallActivity callActivity;

    public JavascriptInterface(CallActivity callActivity) {
        this.callActivity = callActivity;
    }

    @android.webkit.JavascriptInterface
    public void onPeerConnected(){
        this.callActivity.onPeerConnected();
    }
}
