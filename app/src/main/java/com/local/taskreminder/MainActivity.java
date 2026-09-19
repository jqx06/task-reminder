package com.local.taskreminder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
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
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        checkForUpdates();
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    private void checkForUpdates() {
        SharedPreferences preferences = getSharedPreferences("updates", MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - preferences.getLong("last_check", 0) < CHECK_INTERVAL_MS) return;
        preferences.edit().putLong("last_check", now).apply();
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(UPDATE_URL).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "TaskReminder-Android");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                    JSONObject release = new JSONObject(body.toString());
                    String tag = release.getString("tag_name").replaceFirst("^[vV]", "");
                    String page = release.getString("html_url");
                    String current = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    if (isNewer(tag, current)) runOnUiThread(() -> showUpdate(tag, page));
                }
            } catch (Exception ignored) {
                // Update checks must never interrupt the offline task list.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }

    private void showUpdate(String version, String page) {
        new AlertDialog.Builder(this)
                .setTitle("发现新版本 " + version)
                .setMessage("前往 GitHub 下载更新？任务数据仍保存在本机。")
                .setNegativeButton("稍后", null)
                .setPositiveButton("查看更新", (dialog, which) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page))))
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