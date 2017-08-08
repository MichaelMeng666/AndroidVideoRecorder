package michael.com.recordvideolibrary.activity;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.VideoView;

import michael.com.recordvideolibrary.R;
import michael.com.recordvideolibrary.ui.VideoRecorderBaseActivity;

public class VideoPlayActivity extends VideoRecorderBaseActivity implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    private VideoView videoView;
    private ImageView ivPlay;
    private String videoPath;
    private String thumbPath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_play);
        videoView = (VideoView) findViewById(R.id.vv_video);
        ivPlay = (ImageView) findViewById(R.id.iv_play);
        initData(savedInstanceState);
        setListener();
    }

    private void initData(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            videoPath = getIntent().getStringExtra("videoPath");
            thumbPath = getIntent().getStringExtra("thumbPath");
        } else {
            videoPath = savedInstanceState.getString("videoPath");
            thumbPath = savedInstanceState.getString("thumbPath");
        }
        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnCompletionListener(this);
    }

    private void setListener() {
        ivPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                videoView.start();
                ivPlay.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setPlaySchedule();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            int playSchedule = savedInstanceState.getInt("currentPlayPosition");
            videoView.stopPlayback();
            videoView.seekTo(playSchedule);
        }
    }

    private void setPlaySchedule() {
        if (!TextUtils.isEmpty(videoPath)) {
            videoView.setVideoPath(videoPath);
            videoView.start();
            ivPlay.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setLooping(false);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        ivPlay.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int currentPlayPosition = videoView.getCurrentPosition();
        outState.putInt("currentPlayPosition", currentPlayPosition);
        outState.putString("videoPath", videoPath);
        outState.putString("thumbPath", thumbPath);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null) {
            videoView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}