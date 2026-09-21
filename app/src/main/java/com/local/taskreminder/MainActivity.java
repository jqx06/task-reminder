package com.local.taskreminder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/jqx06/task-reminder/main/update.json";
    private static final long CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000;
    private static final int FILE_CHOOSER_REQUEST = 10;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        webView.addJavascriptInterface(new UpdateBridge(), "AndroidUpdates");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                });
                startActivityForResult(Intent.createChooser(intent, "选择课表"), FILE_CHOOSER_REQUEST);
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        checkForUpdates(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private final class UpdateBridge {
        @JavascriptInterface
        public void check() {
            checkForUpdates(true);
        }
    }

    private void checkForUpdates(boolean force) {
        SharedPreferences preferences = getSharedPreferences("updates", MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!force && now - preferences.getLong("last_check", 0) < CHECK_INTERVAL_MS) return;
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String url = force ? UPDATE_URL + "?t=" + System.currentTimeMillis() : UPDATE_URL;
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                connection.setRequestProperty("User-Agent", "TaskReminder-Android");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                    JSONObject update = new JSONObject(body.toString());
                    String version = update.getString("version");
                    String downloadUrl = update.getString("download_url");
                    String notes = update.optString("notes", "");
                    String current = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    preferences.edit().putLong("last_check", System.currentTimeMillis()).apply();
                    if (isNewer(version, current)) {
                        runOnUiThread(() -> showUpdate(version, downloadUrl, notes));
                    } else if (force) {
                        runOnUiThread(() -> showStatus("已是最新版本", "当前版本 " + current));
                    }
                }
            } catch (Exception ignored) {
                if (force) runOnUiThread(() -> showStatus("检查失败", "请检查网络后重试。"));
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void showUpdate(String version, String downloadUrl, String notes) {
        String message = notes.isEmpty()
                ? "下载更新后确认安装，任务数据会保留。"
                : notes + "\n\n下载更新后确认安装，任务数据会保留。";
        new AlertDialog.Builder(this)
                .setTitle("发现新版本 " + version)
                .setMessage(message)
                .setNegativeButton("稍后", null)
                .setPositiveButton("下载更新", (dialog, which) ->
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))))
                .show();
    }

    private void showStatus(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show();
    }

    static boolean isNewer(String latest, String current) {
        String[] a = latest.split("\\.");
        String[] b = current.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int left = i < a.length ? number(a[i]) : 0;
            int right = i < b.length ? number(b[i]) : 0;
            if (left != right) return left > right;
        }
        return false;
    }

    private static int number(String part) {
        try { return Integer.parseInt(part.replaceAll("[^0-9].*$", "")); }
        catch (NumberFormatException ignored) { return 0; }
    }
}
