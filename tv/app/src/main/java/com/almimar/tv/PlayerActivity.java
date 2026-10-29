package com.almimar.tv;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

public class PlayerActivity extends Activity {
    private PlayerView playerView;
    private ExoPlayer player;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        playerView=new PlayerView(this);
        playerView.setUseController(true);playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS);
        setContentView(playerView);
    }
    private static boolean safe(String x){return x!=null&&!x.isEmpty()&&x.length()<500&&!x.contains("\r")&&!x.contains("\n");}
    @Override protected void onStart(){
        super.onStart();
        String url=getIntent().getStringExtra("url");
        if(url==null||!(url.startsWith("https://")||url.startsWith("http://"))){finish();return;}
        String type=getIntent().getStringExtra("type");
        String agent=getIntent().getStringExtra("agent");
        String referrer=getIntent().getStringExtra("referrer");
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true).setUserAgent(safe(agent)?agent:"AlmimarTV/1.3 (Android TV)");
        if(safe(referrer)&&(referrer.startsWith("https://")||referrer.startsWith("http://"))){
            Map<String,String> headers=new HashMap<>();headers.put("Referer",referrer);
            http.setDefaultRequestProperties(headers);
        }
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(
            new DefaultMediaSourceFactory(new DefaultDataSource.Factory(this,http))).build();
        playerView.setPlayer(player);
        MediaItem.Builder item=new MediaItem.Builder().setUri(url);
        String lower=url.toLowerCase(Locale.ROOT);
        if("hls".equals(type)||lower.matches(".*\\.m3u8(?:\\?.*)?$"))item.setMimeType(MimeTypes.APPLICATION_M3U8);
        if("dash".equals(type)||lower.matches(".*\\.mpd(?:\\?.*)?$"))item.setMimeType(MimeTypes.APPLICATION_MPD);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException e){
                Toast.makeText(PlayerActivity.this,"فشل البث: افحص رابط القناة أو صلاحية الاشتراك",Toast.LENGTH_LONG).show();
            }
        });
        player.setMediaItem(item.build());player.prepare();player.play();
    }
    @Override protected void onStop(){if(player!=null){playerView.setPlayer(null);player.release();player=null;}super.onStop();}
}
