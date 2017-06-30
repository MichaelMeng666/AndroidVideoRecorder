package michael.com.recordvideolibrary.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import michael.com.recordvideolibrary.R;
import michael.com.recordvideolibrary.helper.CameraHelper;
import michael.com.recordvideolibrary.listener.IVideoRecorder;
import michael.com.recordvideolibrary.module.VideoObject;
import michael.com.recordvideolibrary.ui.VideoRecorderBaseActivity;
import michael.com.recordvideolibrary.utils.FileUtils;
import michael.com.recordvideolibrary.utils.ImageUtils;
import michael.com.recordvideolibrary.view.CameraPreview;
import michael.com.recordvideolibrary.view.RecordProgressBar;
import michael.com.recordvideolibrary.view.RecordStartView;

public class RecorderActivity extends VideoRecorderBaseActivity implements IVideoRecorder {

    private static final String TAG = "RecorderActivity";
    private static int MAX_TIME = 21;//最大20秒，因为录制有延时
    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    private CameraPreview mPreview;
    private boolean isRecording = false;
    private RecordProgressBar record_pb;
    private TextView tv_max_record_hint;
    private RecordStartView button_start;
    private ImageView change_camera;
    private ImageView cancel_btn;
    private String recordPath;
    private String thumbPath;
    private Animation mFocusAnimation;
    private ImageView mFocusAnimationView;
    private boolean isFocusAnimationHasShow = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CameraHelper.checkCameraHardware(this)) {
            Toast.makeText(this, "找不到相机，3秒后退出！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!cameraIsCanUse()) {
            Toast.makeText(this, "申请权限失败，请到设置中修改权限！", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_preview);
        mPreview = (CameraPreview) findViewById(R.id.camera_preview);
        record_pb = (RecordProgressBar) findViewById(R.id.record_pb);
        tv_max_record_hint = (TextView) findViewById(R.id.tv_max_record_hint);
        button_start = (RecordStartView) findViewById(R.id.button_start);
        mFocusAnimationView = (ImageView) findViewById(R.id.iv_focus_view);
        change_camera = (ImageView) findViewById(R.id.change_camera);
        cancel_btn = (ImageView) findViewById(R.id.cancel_btn);

        mFocusAnimation = AnimationUtils.loadAnimation(this, R.anim.focus_animation);
        mFocusAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mFocusAnimationView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mPreview.setOnFocusEventListener(new CameraPreview.OnFocusEventListener() {
            @Override
            public void onFocusStart(float x, float y) {
                mFocusAnimation.cancel();
                mFocusAnimationView.clearAnimation();
                int left = (int) (x - mFocusAnimationView.getWidth() / 2.0f);
                int top = (int) (y - mFocusAnimationView.getHeight() / 2.0f);
                if (isFocusAnimationHasShow) {
                    RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mFocusAnimationView.getLayoutParams();
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                    layoutParams.setMargins(left, top, 0, 0);
                    mFocusAnimationView.setLayoutParams(layoutParams);
                }
                isFocusAnimationHasShow = true;
                mFocusAnimationView.setVisibility(View.VISIBLE);
                mFocusAnimationView.startAnimation(mFocusAnimation);
            }
        });

        record_pb.setRunningTime(MAX_TIME);
        record_pb.setOnFinishListener(new RecordProgressBar.OnFinishListener() {
            @Override
            public void onFinish() {
                stopRecord();
            }
        });

        change_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPreview.getCameraState() == 0) {
                    mPreview.changeToFront();
                } else {
                    mPreview.changeToBack();
                }
            }
        });

        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    finish();
                }
            }
        });

        button_start.setOnRecordButtonListener(new RecordStartView.OnRecordButtonListener() {
            @Override
            public void onStartRecord() {
                startRecord();
            }

            @Override
            public void onStopRecord() {
                stopRecord();
            }

            @Override
            public void onRecordTooShort() {
                recordTooShort();
            }
        });
    }


    @Override
    public VideoObject startRecord() {
        if (isRecording) {
            Toast.makeText(this, "正在录制中…", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (prepareVideoRecorder()) {
            tv_max_record_hint.setAlpha(1f);
            tv_max_record_hint.animate().alpha(0f).setDuration(1000).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    tv_max_record_hint.setVisibility(View.GONE);
                }
            });

            change_camera.setEnabled(false);
            record_pb.setVisibility(View.VISIBLE);
            record_pb.start();
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.start();
            isRecording = true;
        } else {
            // prepare didn't work, release the camera
            releaseMediaRecorder();
            // inform user
        }
        return null;
    }

    @Override
    public void stopRecord() {
        if (isRecording) {
            // stop recording and release camera
            try {
                tv_max_record_hint.setAlpha(0f);
                tv_max_record_hint.setVisibility(View.VISIBLE);
                tv_max_record_hint.animate().alpha(1f).setDuration(500).setListener(null);

                change_camera.setEnabled(true);
                record_pb.setVisibility(View.GONE);
                record_pb.stop();
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setPreviewDisplay(null);
                mMediaRecorder.stop();  // stop the recording
            } catch (Exception e) {
                // TODO 删除已经创建的视频文件
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            isRecording = false;
            generateThumb();
            afterRecorded();
        }
    }

    @Override
    public void recordTooShort() {
        if (isRecording) {
            try {
                tv_max_record_hint.setAlpha(0f);
                tv_max_record_hint.setVisibility(View.VISIBLE);
                tv_max_record_hint.animate().alpha(1f).setDuration(500).setListener(null);

                change_camera.setEnabled(true);
                record_pb.setVisibility(View.GONE);
                record_pb.stop();
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setPreviewDisplay(null);
                mMediaRecorder.stop();  // stop the recording
            } catch (Exception e) {
                // TODO 删除已经创建的视频文件
            }
            releaseMediaRecorder(); // release the MediaRecorder object
            isRecording = false;
            FileUtils.deleteFile(recordPath);
            FileUtils.deleteFile(thumbPath);
            Toast.makeText(RecorderActivity.this, "录制时间过短", Toast.LENGTH_SHORT).show();
        }
    }

    private void generateThumb() {
        Bitmap videoThumbBitmap = ImageUtils.getVideoThumbnail(recordPath, 270, 480, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
        File thumbFile = FileUtils.getOutputMediaFile(this, FileUtils.MEDIA_TYPE_IMAGE);
        if (thumbFile != null) {
            thumbPath = ImageUtils.saveBitmap2File(videoThumbBitmap, thumbFile);
        } else {
            Toast.makeText(this, "无法读取视频文件，请重启手机后重试。", Toast.LENGTH_SHORT).show();
        }
    }

    private void afterRecorded() {

    }

    @Override
    protected void onPause() {
        super.onPause();
        // 页面销毁时要停止录制
        stopRecord();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
        finish();
    }

    /**
     * 准备视频录制器
     *
     * @return
     */
    private boolean prepareVideoRecorder() {
        if (!FileUtils.isSDCardMounted()) {
            Toast.makeText(this, "SD卡不可用！", Toast.LENGTH_SHORT).show();
            return false;
        }

        File file = FileUtils.getOutputMediaFile(this, FileUtils.MEDIA_TYPE_VIDEO);
        if (null == file) {
            Toast.makeText(this, "创建存储文件失败！", Toast.LENGTH_SHORT).show();
            return false;
        }
        recordPath = file.getAbsolutePath();
        mMediaRecorder = new MediaRecorder();
        // 获取最新对象，因为当mPreview中切换相机之后对象指向会有问题
        mCamera = mPreview.getmCamera();
        // Step 1: Unlock and set camera to MediaRecorder
        // 解锁相机以让其他进程能够访问相机，4.0以后系统自动管理调用，但是实际使用中，不调用的话，MediaRecorder.start()报错闪退
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

//        mMediaRecorder.setAudioEncodingBitRate(44100);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        if (CamcorderProfile.hasProfile(mPreview.getCurrentCameraId(), CamcorderProfile.QUALITY_720P)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));//720P不能去掉，在某些机型上前置摄像头会崩溃
        } else if (CamcorderProfile.hasProfile(mPreview.getCurrentCameraId(), CamcorderProfile.QUALITY_HIGH)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        } else if (CamcorderProfile.hasProfile(mPreview.getCurrentCameraId(), CamcorderProfile.QUALITY_480P)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        } else if (CamcorderProfile.hasProfile(mPreview.getCurrentCameraId(), CamcorderProfile.QUALITY_QVGA)) {
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QVGA));
        }

        mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(recordPath);

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        mMediaRecorder.setMaxDuration(MAX_TIME * 1000);

        if (mPreview.getCameraState() == 0) {
            mMediaRecorder.setOrientationHint(90);
        } else {
            mMediaRecorder.setOrientationHint(270);
        }

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            // 锁相机，4.0以后系统自动管理调用，但若录制器prepare()方法失败，必须调用
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        mCamera = mPreview.getmCamera();
        if (mCamera != null) {
            // 虽然我之前并没有setPreviewCallback，但不加这句的话，
            // 后面要用到Camera时，调用Camera随便一个方法，都会报
            // Method called after release() error闪退，推测可能
            // Camera内存泄露无法真正释放，加上这句可以规避该问题
            mCamera.setPreviewCallback(null);
            // 释放前先停止预览
            mCamera.stopPreview();
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    /**
     * 返回true 表示可以使用  返回false表示不可以使用
     */
    public boolean cameraIsCanUse() {
        boolean isCanUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open();
            Camera.Parameters mParameters = mCamera.getParameters(); //针对魅族手机
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            isCanUse = false;
        }

        if (mCamera != null) {
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
                return isCanUse;
            }
        }
        return isCanUse;
    }

    @Override
    public void onBackPressed() {
        if (!isRecording) {
            super.onBackPressed();
        }
    }
}