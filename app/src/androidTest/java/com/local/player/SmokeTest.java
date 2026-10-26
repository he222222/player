package com.local.player;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.security.NetworkSecurityPolicy;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import java.util.concurrent.atomic.AtomicReference;

public final class SmokeTest extends Instrumentation {
    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        start();
    }

    @Override
    public void onStart() {
        Bundle result = new Bundle();
        MainActivity activity = null;
        int resultCode = Activity.RESULT_OK;
        try {
            Intent intent = new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity = (MainActivity) startActivitySync(intent);
            MainActivity launched = activity;
            AtomicReference<Throwable> failure = new AtomicReference<>();
            runOnMainSync(() -> {
                try {
                    WebView webView = launched.findViewById(R.id.web_view);
                    require(MainActivity.HOME_URL.equals(webView.getOriginalUrl()), "Wrong start URL");
                    require(webView.getSettings().getJavaScriptEnabled(), "JavaScript disabled");
                    require(webView.getSettings().getDomStorageEnabled(), "DOM storage disabled");
                    require(!webView.getSettings().getAllowFileAccess(), "File access enabled");
                    require(!webView.getSettings().getAllowContentAccess(), "Content access enabled");
                    NetworkSecurityPolicy policy = NetworkSecurityPolicy.getInstance();
                    require(policy.isCleartextTrafficPermitted("192.168.1.1"), "LAN HTTP blocked");
                    require(!policy.isCleartextTrafficPermitted("example.com"), "Unrelated HTTP enabled");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        verifyFullscreen(launched, webView);
                    }
                } catch (Throwable error) {
                    failure.set(error);
                }
            });
            if (failure.get() != null) {
                throw failure.get();
            }
            result.putString("stream", "PASS: launch URL, WebView settings, HTTP scope, fullscreen checks\n");
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + error + "\n");
            resultCode = Activity.RESULT_CANCELED;
        } finally {
            if (activity != null) {
                MainActivity launched = activity;
                runOnMainSync(launched::finish);
            }
        }
        finish(resultCode, result);
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.O)
    @SuppressWarnings("deprecation")
    private static void verifyFullscreen(MainActivity activity, WebView webView) {
        WebChromeClient client = webView.getWebChromeClient();
        View decor = activity.getWindow().getDecorView();
        int originalUi = decor.getSystemUiVisibility();
        int originalFlags = activity.getWindow().getAttributes().flags;
        View video = new View(activity);
        int[] exits = {0};
        client.onShowCustomView(video, () -> exits[0]++);
        require(video.getParent() != null, "Fullscreen view not attached");
        require(webView.getVisibility() == View.GONE, "Web page still visible in fullscreen");
        require((activity.getWindow().getAttributes().flags
                & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0, "Fullscreen flag missing");
        require((decor.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0,
                "Navigation bar not hidden");

        View duplicate = new View(activity);
        int[] rejected = {0};
        client.onShowCustomView(duplicate, () -> rejected[0]++);
        require(duplicate.getParent() == null && rejected[0] == 1,
                "Duplicate fullscreen request not rejected");
        require(video.getParent() != null && exits[0] == 0, "Active fullscreen dismissed");

        client.onHideCustomView();
        require(video.getParent() == null && exits[0] == 1, "Fullscreen not dismissed once");
        require(webView.getVisibility() == View.VISIBLE, "Web page not restored");
        require(decor.getSystemUiVisibility() == originalUi, "System UI not restored");
        require(activity.getWindow().getAttributes().flags == originalFlags, "Window flags changed");
        client.onHideCustomView();
        require(exits[0] == 1, "Duplicate exit callback");

        client.onShowCustomView(video, () -> exits[0]++);
        activity.onBackPressed();
        require(video.getParent() == null && exits[0] == 2, "Back did not exit fullscreen");
        require(!activity.isFinishing(), "Back closed the app during fullscreen");
        require(webView.getVisibility() == View.VISIBLE, "Back did not restore the web page");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
