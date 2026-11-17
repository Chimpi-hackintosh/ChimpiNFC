package com.chimpihackintosh.chimpinfc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

/** Public, read-only version check: no files, device data or credentials are sent. */
final class SupportNotice {
    private static final String APP = "ChimpiNFC";
    private static final String TABLE = "https://chimpi-hackintosh.github.io/ChimpiBlender-Online/support.html";
    private static final String POLICY = "https://raw.githubusercontent.com/Chimpi-hackintosh/ChimpiBlender-Online/main/support-policy.json";
    private SupportNotice() {}

    static String read(InputStream source) throws Exception {
        try (InputStream input = source; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int count;
            while ((count=input.read(buffer)) != -1) {
                if (output.size()+count>262144) throw new java.io.IOException("Oversized policy");
                output.write(buffer,0,count);
            }
            return output.toString("UTF-8");
        }
    }
    static boolean official(String raw) {
        try {
            URL url = new URL(raw);
            return "https".equals(url.getProtocol()) && "github.com".equals(url.getHost()) &&
                url.getUserInfo()==null && url.getPort()==-1 && url.getQuery()==null && url.getRef()==null &&
                (url.getPath().equals("/Chimpi-hackintosh/"+APP) || url.getPath().startsWith("/Chimpi-hackintosh/"+APP+"/"));
        } catch (Exception error) { return false; }
    }
    static JSONObject entry(JSONObject policy) throws Exception {
        if (policy.optInt("schema")!=1) throw new IllegalArgumentException("Unknown policy");
        JSONObject value=policy.getJSONObject("apps").getJSONObject(APP);
        if (!APP.equals(value.optString("repo")) || SupportVersions.parse(value.optString("minimum"))==null || !official(value.optString("url")))
            throw new IllegalArgumentException("Invalid entry");
        return value;
    }
    static String text(JSONObject all, String lang, String key) throws Exception {
        JSONObject translated=all.optJSONObject(lang);
        return translated!=null && translated.has(key) ? translated.getString(key) : all.getJSONObject("en").getString(key);
    }
    static String format(String value, String current, JSONObject entry) throws Exception {
        return value.replace("{app}",entry.getString("name")).replace("{current}",current)
            .replace("{minimum}",entry.getString("minimum")).replace("{latest}",entry.optString("tag",entry.getString("minimum")));
    }
    static void open(Activity activity, String url) {
        try { activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); }
        catch (android.content.ActivityNotFoundException ignored) { /* No browser installed. */ }
    }
    static void check(Activity activity) {
        new Thread(() -> {
            try {
                final String current=activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).versionName;
                JSONObject bundled=new JSONObject(read(activity.getAssets().open("support-policy.json")));
                JSONObject selected=entry(bundled);
                JSONObject messages=new JSONObject(read(activity.getAssets().open("support-messages.json")));
                HttpsURLConnection connection=null;
                try {
                    connection=(HttpsURLConnection)new URL(POLICY).openConnection();
                    connection.setConnectTimeout(4000); connection.setReadTimeout(4000);
                    connection.setInstanceFollowRedirects(false); connection.setRequestProperty("User-Agent","Chimpi-support-check/1");
                    if (connection.getResponseCode()==200) {
                        JSONObject candidate=entry(new JSONObject(read(connection.getInputStream())));
                        if (!SupportVersions.older(candidate.getString("minimum"),selected.getString("minimum"))) selected=candidate;
                    }
                } catch (Exception ignored) { /* Bundled policy works offline. */ }
                finally { if (connection!=null) connection.disconnect(); }
                if (!SupportVersions.older(current,selected.getString("minimum"))) return;
                final JSONObject value=selected;
                final String lang=activity.getResources().getConfiguration().getLocales().get(0).getLanguage();
                final String phase=SupportVersions.ended(System.currentTimeMillis()) ? "after" : "before";
                final String title=text(messages,lang,phase);
                final String body=format(text(messages,lang,"body_"+phase),current,value)+"\n\n"+
                    text(messages,lang,"risk")+"\n\n"+format(text(messages,lang,"versions"),current,value);
                final String update=text(messages,lang,"update"), later=text(messages,lang,"later"), table=text(messages,lang,"table");
                final String url=value.getString("url");
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    new AlertDialog.Builder(activity).setTitle(title).setMessage(body)
                        .setPositiveButton(update,(dialog,which)->open(activity,url))
                        .setNegativeButton(later,null).setNeutralButton(table,(dialog,which)->open(activity,TABLE)).show();
                });
            } catch (Exception ignored) { /* A failed optional check must not break startup. */ }
        },"Chimpi-support").start();
    }
}
