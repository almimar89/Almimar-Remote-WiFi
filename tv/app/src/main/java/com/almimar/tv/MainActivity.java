package com.almimar.tv;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int PICK=78, LIMIT=12*1024*1024;
    private WebView web;
    private final Set<String> official = new HashSet<>(Arrays.asList("tod.tv","bein.com","aljazeera.net","nilesat.org"));
    private static boolean validHttp(String url) {
        if(url==null||url.length()>8192)return false;
        try { Uri u=Uri.parse(url); return "https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()); }
        catch(Exception e){return false;}
    }
    private boolean official(String url) {
        if(!validHttp(url))return false;
        String host=Uri.parse(url).getHost(); if(host==null)return false;
        host=host.toLowerCase(Locale.ROOT);
        for(String domain:official)if(host.equals(domain)||host.endsWith("."+domain))return true;
        return false;
    }
    private void openOfficial(String url){
        if(!official(url))return;
        try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
        catch(Exception e){Toast.makeText(this,"لم أجد متصفحاً لفتح الموقع",Toast.LENGTH_LONG).show();}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        web=new WebView(this); web.setBackgroundColor(0xff070e1c);
        web.setFocusable(true);web.setFocusableInTouchMode(true);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);s.setJavaScriptCanOpenWindowsAutomatically(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){
                Uri u=r.getUrl();
                if("file".equalsIgnoreCase(u.getScheme()))return !u.toString().equals("file:///android_asset/index.html");
                if(official(u.toString()))openOfficial(u.toString());
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(),"AlmimarAndroid");
        setContentView(web);web.loadUrl("file:///android_asset/index.html");
    }
    private void js(String code){runOnUiThread(()->{if(web!=null)web.evaluateJavascript(code,null);});}
    private static String read(InputStream input) throws Exception {
        try(InputStream in=input;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] b=new byte[8192];int n;
            while((n=in.read(b))!=-1){
                if(out.size()+n>LIMIT)throw new Exception("القائمة أكبر من 12 ميغابايت");
                out.write(b,0,n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
    private void deliver(String text) {
        js("window.AlmimarTV.beginNativePlaylist()");
        for(int p=0;p<text.length();p+=20000){
            String piece=text.substring(p,Math.min(p+20000,text.length()));
            js("window.AlmimarTV.appendNativePlaylist("+JSONObject.quote(piece)+")");
        }
        js("window.AlmimarTV.finishNativePlaylist()");
    }
    private void fetch(String url){
        if(!validHttp(url)){js("window.AlmimarTV.onPlaylistError('رابط القائمة غير صالح')");return;}
        new Thread(()->{
            HttpURLConnection conn=null;
            try {
                conn=(HttpURLConnection)new URL(url).openConnection();
                conn.setConnectTimeout(15000);conn.setReadTimeout(20000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent","AlmimarTV/1.3 (Android TV)");
                if(conn.getResponseCode()!=200)throw new Exception("الخادم: "+conn.getResponseCode());
                deliver(read(conn.getInputStream()));
            }catch(Exception e){
                js("window.AlmimarTV.onPlaylistError("+JSONObject.quote(e.getMessage()==null?"فشل تحميل القنوات":e.getMessage())+")");
            }finally {if(conn!=null)conn.disconnect();}
        }).start();
    }
    public final class Bridge {
        @JavascriptInterface public void fetchPlaylist(String url){fetch(url);}
        @JavascriptInterface public void openOfficial(String url){runOnUiThread(()->MainActivity.this.openOfficial(url));}
        @JavascriptInterface public void pickPlaylist(){
            runOnUiThread(()->{
                try{
                    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
                    startActivityForResult(i,PICK);
                }catch(Exception e){Toast.makeText(MainActivity.this,"تعذر فتح الملفات",Toast.LENGTH_SHORT).show();}
            });
        }
        @JavascriptInterface public void play(String url,String name,String type,String agent,String referrer){
            if(!validHttp(url))return;
            runOnUiThread(()->{
                Intent i=new Intent(MainActivity.this,PlayerActivity.class);
                i.putExtra("url",url);i.putExtra("name",name);
                i.putExtra("type",type);i.putExtra("agent",agent);
                i.putExtra("referrer",referrer);startActivity(i);
            });
        }
    }
    @Override @SuppressWarnings("deprecation") protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req!=PICK||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        new Thread(()->{try{deliver(read(getContentResolver().openInputStream(uri)));}
        catch(Exception e){js("window.AlmimarTV.onPlaylistError("+JSONObject.quote(e.getMessage()==null?"تعذر فتح الملف":e.getMessage())+")");}}).start();
    }
    @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("AlmimarAndroid");web.destroy();web=null;}super.onDestroy();}
}
