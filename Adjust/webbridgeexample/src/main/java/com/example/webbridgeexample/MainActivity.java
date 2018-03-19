package com.example.webbridgeexample;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.bridge.AdjustBridge;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView webView = (WebView) findViewById(R.id.webView1);
        AdjustBridge.setApplicationContext(getApplication());
        AdjustBridge.setWebView(webView);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(AdjustBridge.getDefaultInstance(), "AdjustBridge");

        try {
            webView.loadUrl("file:///android_asset/AdjustExample-WebView.html");

            Intent intent = getIntent();
            Uri data = intent.getData();

            //AdjustBridge.deeplinkReceived(data);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}