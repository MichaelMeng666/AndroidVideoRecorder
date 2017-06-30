package michael.com.recordvideolibrary.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.List;

import michael.com.recordvideolibrary.R;
import michael.com.recordvideolibrary.activity.RecorderActivity;
import michael.com.recordvideolibrary.helper.CameraHelper;
import michael.com.recordvideolibrary.utils.SystemVersionUtil;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback {

    private static final String TAG = "CameraPreview";
    // 用于判断双击事件的两次按下事件的间隔
    private static final long DOUBLE_CLICK_INTERVAL = 200;
    private int previewWidth;
    private int previewHeight;
    private int OUTPUT_WIDTH;
    private int OUTPUT_HEIGHT;
    private Camera mCamera;
    private long mLastTouchDownTime;
    private ZoomRunnable mZoomRunnable;
    // 0 后置摄像头 1 前置摄像头
    private int cameraState = 0;
    // 0 闪光灯关闭 1 闪光灯开启
    private int lightState = 0;
    private Camera.Size mVideoSize;
    private Context context;
    private int focusAreaSize;
    private Matrix matrix;
    private RecorderActivity activity;
    private int currentCameraId;
    private OnFocusEventListener onFocusEventListener;

    public CameraPreview(Context context) {
        super(context);
        this.context = context;
        this.activity = (RecorderActivity) context;
        init(activity);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.activity = (RecorderActivity) context;
        init(activity);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CameraPreview);
        Drawable focusDrawable = typedArray.getDrawable(R.styleable.CameraPreview_cpv_focusDrawable);
        typedArray.recycle();
        this.context = context;
        this.activity = (RecorderActivity) context;
        init(activity);
    }

    private void init(Activity activity) {
        int cameraId = CameraHelper.getDefaultCameraID();
        if (!CameraHelper.isCameraFacingBack(cameraId)) {
            Toast.makeText(activity, "找不到后置相机，3秒后退出！", Toast.LENGTH_SHORT).show();
            activity.finish();
            return;
        }
        // Create an instance of Camera
        mCamera = CameraHelper.getCameraInstance(cameraId);
        if (null == mCamera) {
            Toast.makeText(activity, "打开相机失败！", Toast.LENGTH_SHORT).show();
            activity.finish();
            return;
        }
        setupCamera(cameraId, mCamera);
        getHolder().addCallback(this);
        focusAreaSize = getResources().getDimensionPixelSize(R.dimen.camera_focus_area_size);
        matrix = new Matrix();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.getDefaultSize(previewWidth, widthMeasureSpec);
        int height = View.getDefaultSize(previewHeight, widthMeasureSpec);
        if (previewWidth > 0 && previewHeight > 0) {
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
            float specAspectRatio = (float) widthSpecSize / (float) heightSpecSize;
            float displayAspectRatio = previewWidth < previewHeight ? (float) previewWidth / (float) previewHeight : (float) previewHeight / (float) previewWidth;
            boolean shouldBeWider = displayAspectRatio > specAspectRatio;
            if (shouldBeWider) {
                // not high enough, fix height
                height = heightSpecSize;
                width = (int) (height * displayAspectRatio);
            } else {
                // not wide enough, fix width
                width = widthSpecSize;
                height = (int) (width / displayAspectRatio);
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mCamera.getParameters().isZoomSupported() && event.getDownTime() - mLastTouchDownTime <= DOUBLE_CLICK_INTERVAL) {
//                    zoomPreview();//双击放大，拉近距离，但是在录制过程中双击会一直放大，无法缩小
                }
                mLastTouchDownTime = event.getDownTime();
                focusOnTouch(event.getX(), event.getY());
                break;
        }
        return super.onTouchEvent(event);
    }

    private void focusOnTouch(float x, float y) {
        mCamera.cancelAutoFocus();
        CameraHelper.setCameraFocusMode(Camera.Parameters.FOCUS_MODE_AUTO, mCamera);
        mCamera.autoFocus(this);
        if (onFocusEventListener != null) {
            onFocusEventListener.onFocusStart(x, y);
        }
    }


    @Override
    public void onAutoFocus(boolean success, Camera camera) {

    }

    /**
     * 放大预览视图
     */
    private void zoomPreview() {
        Camera.Parameters parameters = mCamera.getParameters();
        int currentZoom = parameters.getZoom();
        int maxZoom = (int) (parameters.getMaxZoom() / 2f + 0.5);
        int destZoom = 0 == currentZoom ? maxZoom : 0;
        if (parameters.isSmoothZoomSupported()) {
            mCamera.stopSmoothZoom();
            mCamera.startSmoothZoom(destZoom);
        } else {
            Handler handler = getHandler();
            if (null == handler)
                return;
            handler.removeCallbacks(mZoomRunnable);
            handler.post(mZoomRunnable = new ZoomRunnable(destZoom, currentZoom, mCamera));
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        Log.d(TAG, "surfaceDestroyed");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            focusOnTouch(CameraPreview.this.getWidth() / 2f, CameraPreview.this.getHeight() / 2f);
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 放大预览视图任务
     *
     * @author Martin
     */
    private static class ZoomRunnable implements Runnable {

        int destZoom, currentZoom;
        WeakReference<Camera> cameraWeakRef;

        public ZoomRunnable(int destZoom, int currentZoom, Camera camera) {
            this.destZoom = destZoom;
            this.currentZoom = currentZoom;
            cameraWeakRef = new WeakReference<>(camera);
        }

        @Override
        public void run() {
            Camera camera = cameraWeakRef.get();
            if (null == camera)
                return;

            boolean zoomUp = destZoom > currentZoom;
            for (int i = currentZoom; zoomUp ? i <= destZoom : i >= destZoom; i = (zoomUp ? ++i : --i)) {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setZoom(i);
                camera.setParameters(parameters);
            }
        }
    }

    public void changeToBack() {
        getHolder().removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();//停掉原来摄像头的预览
        mCamera.release();//释放资源
        mCamera = null;//取消原来摄像头
        mCamera = CameraHelper.getCameraInstance(CameraHelper.getDefaultCameraID());//打开当前选中的摄像头
        currentCameraId = CameraHelper.getDefaultCameraID();
        setupCamera(currentCameraId, mCamera);
        getHolder().addCallback(this);
        try {
            mCamera.setPreviewDisplay(getHolder());//通过surfaceview显示取景画面
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();//开始预览
        cameraState = 0;
    }

    public void changeToFront() {
        getHolder().removeCallback(this);
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();//停掉原来摄像头的预览
        mCamera.release();//释放资源
        mCamera = null;//取消原来摄像头
        mCamera = CameraHelper.getCameraInstance(CameraHelper.getFrontCameraID());//打开当前选中的摄像头
        currentCameraId = CameraHelper.getFrontCameraID();
        setupCamera(currentCameraId, mCamera);
        getHolder().addCallback(this);
        try {
            mCamera.setPreviewDisplay(getHolder());//通过surfaceview显示取景画面
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();//开始预览
        cameraState = 1;
    }

    /**
     * 开关闪光灯，先保留这个方法备用
     */
    public void toggleFlashMode() {
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters != null) {
            try {
                final String mode = parameters.getFlashMode();
                if (TextUtils.isEmpty(mode) || Camera.Parameters.FLASH_MODE_OFF.equals(mode)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    lightState = 1;
                } else {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    lightState = 0;
                }
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                Log.e(" ", "toggleFlashMode", e);
            }
        }
    }

    public int getCameraState() {
        return cameraState;
    }

    public int getLightState() {
        return lightState;
    }

    public Camera getmCamera() {
        return mCamera;
    }

    public int getCurrentCameraId() {
        return currentCameraId;
    }

    public void setCurrentCameraId(int currentCameraId) {
        this.currentCameraId = currentCameraId;
    }

    /**
     * 设置相机参数
     */
    public void setupCamera(int cameraId, Camera mCamera) {
        // 设置相机方向
        CameraHelper.setCameraDisplayOrientation(activity, cameraId, mCamera);
        // 设置相机参数
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        List<String> whiteBalance = parameters.getSupportedWhiteBalance();
        if (whiteBalance.contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

        Camera.Size optimalSize;
        optimalSize = CameraHelper.getOptimalPreviewSize(activity, parameters.getSupportedPreviewSizes());
        previewWidth = optimalSize.width;
        previewHeight = optimalSize.height;

//        mVideoSize = CameraHelper.getCameraPreviewSizeForVideo(cameraId, mCamera);
//        parameters.setPreviewSize(mVideoSize.width, mVideoSize.height);
        parameters.setPreviewSize(previewWidth, previewHeight); //获得摄像区域的大小
        mCamera.setParameters(parameters);
        if (!focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mCamera.cancelAutoFocus();
        }
    }

    /**
     * 手动对焦方法，目前有点问题，先保留
     *
     * @param camera
     * @param cb
     * @param focusAreas
     * @param mFocusAreas
     * @return
     */
    public boolean manualFocus(Camera camera, Camera.AutoFocusCallback cb, List<Camera.Area> focusAreas, List<Camera.Area> mFocusAreas) {
        //判断系统是否是4.0以上的版本
        if (camera != null && focusAreas != null && SystemVersionUtil.hasICS()) {
            try {
                camera.cancelAutoFocus();
                Camera.Parameters parameters = camera.getParameters();
                if (parameters != null) {
                    // getMaxNumFocusAreas检测设备是否支持
                    if (parameters.getMaxNumFocusAreas() > 0) {
                        parameters.setFocusAreas(focusAreas);
                    }
                    // getMaxNumMeteringAreas检测设备是否支持
                    if (parameters.getMaxNumMeteringAreas() > 0)
                        parameters.setMeteringAreas(mFocusAreas);
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
                    camera.setParameters(parameters);
                    camera.autoFocus(cb);
                    return true;
                }
            } catch (Exception e) {
                if (e != null)
                    Log.e(" ", "autoFocus", e);
            }
        }
        return false;
    }

    /**
     * Convert touch position x:y to {@link Camera.Area} position -1000:-1000 to 1000:1000.
     * <p>
     * Rotate, scale and translate touch rectangle using matrix configured in
     * {@link SurfaceHolder.Callback#surfaceChanged(SurfaceHolder, int, int, int)}
     */
    private Rect calculateTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int left = clamp((int) x - areaSize / 2, 0, getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, getHeight() - areaSize);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        matrix.mapRect(rectF);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    // preview size 排序
    public class PreviewOrder implements Comparator<Camera.Size> {

        @Override
        public int compare(Camera.Size lhs, Camera.Size rhs) {
            return rhs.height * rhs.width - lhs.height * lhs.width;
        }
    }

    public void setOnFocusEventListener(OnFocusEventListener onFocusEventListener) {
        this.onFocusEventListener = onFocusEventListener;
    }

    public interface OnFocusEventListener {
        void onFocusStart(float x, float y);
    }
}