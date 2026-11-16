package com.Thanks.myapp; // <-- Apna package name yahan check karein

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.service.wallpaper.WallpaperService;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER);

        Button btn = new Button(this);
        btn.setText("Select Video (Max 17 Sec)");
        layout.addView(btn);
        setContentView(layout);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(intent, 101);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();

                // 15 second check
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, uri);
                long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                retriever.release();

                if (duration > 17500) {
                    Toast.makeText(this, "15 sec se chhota video chunein!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Video ko app ke folder me save karein
                InputStream in = getContentResolver().openInputStream(uri);
                FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "wallpaper.mp4"));
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close();
                out.close();

                // Live Wallpaper screen kholein
                Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        new ComponentName(this, VideoService.class));
                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- LIVE WALLPAPER ENGINE (ISI FILE KE ANDAR) ---
    public static class VideoService extends WallpaperService {
        @Override
        public Engine onCreateEngine() {
            return new Engine() {
                private MediaPlayer mp;

                @Override
                public void onSurfaceCreated(SurfaceHolder holder) {
                    super.onSurfaceCreated(holder);
                    File file = new File(getFilesDir(), "wallpaper.mp4");
                    if (file.exists()) {
                        try {
                            mp = new MediaPlayer();
                            mp.setDataSource(file.getAbsolutePath());
                            mp.setSurface(holder.getSurface());
                            mp.setLooping(true);
                            mp.setVolume(0f, 0f);
                            mp.prepare();
                            mp.start();
                        } catch (Exception e) {}
                    }
                }

                @Override
                public void onVisibilityChanged(boolean visible) {
                    if (mp != null) {
                        if (visible) mp.start();
                        else mp.pause();
                    }
                }

                @Override
                public void onSurfaceDestroyed(SurfaceHolder holder) {
                    super.onSurfaceDestroyed(holder);
                    if (mp != null) {
                        mp.release();
                        mp = null;
                    }
                }
            };
        }
    }
}
