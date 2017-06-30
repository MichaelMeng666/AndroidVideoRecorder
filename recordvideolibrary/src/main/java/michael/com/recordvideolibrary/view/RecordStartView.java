package michael.com.recordvideolibrary.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import michael.com.recordvideolibrary.R;

public class RecordStartView extends View {
    public final int RING_WHAT = 101;
    private Runnable mLongPressRunnable;
    private Paint mRingProgressPaint;
    private Paint mCenterPaint;
    private Paint mRingPaint;
    //圆环颜色
    public int mRingColor;
    // 圆环进度的颜色
    public int mRingProgressColor;
    //中间颜色
    public int mCenterColor;
    //圆环的宽度
    public int mRingWidth;
    //控件宽高
    private int mWidth;
    private int mHeight;
    //中间X坐标
    private int centerX;
    //中间Y坐标
    private int centerY;
    //进度
    private float progress;
    //中间方法比例
    private float centerScale = 0.8f;
    //半径
    private int radius;
    //最大时间
    private int mRingMax;
    //最小时间
    private int mRingMin;
    //时间间隔
    private long timeSpan = 100;
    //开始时间
    private long startTime;
    //是否录制中
    private boolean isRecording = false;

    public RecordStartView(Context context) {
        this(context, null);
    }

    public RecordStartView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordStartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RecordStartView);
        mRingColor = typedArray.getColor(R.styleable.RecordStartView_mRingColor, Color.parseColor("#66FFFFFF"));
        mRingProgressColor = typedArray.getColor(R.styleable.RecordStartView_mRingProgressColor, Color.WHITE);
        mCenterColor = typedArray.getColor(R.styleable.RecordStartView_mCenterColor, Color.WHITE);
        mRingWidth = typedArray.getDimensionPixelOffset(R.styleable.RecordStartView_mRingWidth, 14);
        mRingMax = typedArray.getInt(R.styleable.RecordStartView_mRingMax, 21000);
        mRingMin = typedArray.getInt(R.styleable.RecordStartView_mRingMin, 3000);
        typedArray.recycle();

        mCenterPaint = new Paint();
        mCenterPaint.setStyle(Paint.Style.FILL);
        mCenterPaint.setAntiAlias(true);
        mCenterPaint.setColor(mCenterColor);


        mRingPaint = new Paint();
        mRingPaint.setColor(mRingColor);
        mRingPaint.setStyle(Paint.Style.STROKE);
        mRingPaint.setAntiAlias(true);
        mRingPaint.setStrokeWidth(mRingWidth);

        mRingProgressPaint = new Paint();
        mRingProgressPaint.setColor(mRingProgressColor);
        mRingProgressPaint.setStyle(Paint.Style.STROKE);
        mRingProgressPaint.setAntiAlias(true);
        mRingProgressPaint.setStrokeWidth(mRingWidth);


        /**
         * 自己实现长按事件
         */
        mLongPressRunnable = new Runnable() {

            @Override
            public void run() {
                startRecord();
            }
        };
    }

    /**
     * 开始录制
     */
    private void startRecord() {
        if (mOnRecordButtonListener != null) {
            isRecording = true;
            mOnRecordButtonListener.onStartRecord();
        }
        startTime = System.currentTimeMillis();
        mHandler.sendEmptyMessageDelayed(RING_WHAT, timeSpan);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width, height;
        if (widthMode == MeasureSpec.AT_MOST) {
            width = dp2px(100);
        } else {
            width = widthSize;
        }
        if (heightMode == MeasureSpec.AT_MOST) {
            height = dp2px(100);
        } else {
            height = heightSize;
        }
        setMeasuredDimension(Math.min(width, height), Math.min(width, height));
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = getWidth();
        mHeight = getHeight();
        //获取中心点的位置
        centerX = getWidth() / 2;
        centerY = getHeight() / 2;
        radius = (int) (centerX - mRingWidth / 2) - 10;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                removeCallbacks(mLongPressRunnable);
                progress = 0;
                mHandler.removeMessages(RING_WHAT);
                long endTime = System.currentTimeMillis();
                if (mOnRecordButtonListener != null) {
                    if (endTime - startTime < mRingMin) {
                        mOnRecordButtonListener.onRecordTooShort();
                    } else {
                        mOnRecordButtonListener.onStopRecord();
                    }
                }
                isRecording = false;
                resetView();
                break;
        }
        return true;
    }

    private void resetView() {
        progress = 0;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCenter(canvas);
        drawRing(canvas);
//        drawRingProgress(canvas);//可以绘制录制按钮进度
    }

    /**
     * 绘制中间的圆
     *
     * @param canvas
     */
    private void drawCenter(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, centerScale * radius, mCenterPaint);
    }

    /**
     * 绘制圆环
     *
     * @param canvas
     */
    private void drawRing(Canvas canvas) {
        canvas.drawCircle(mWidth / 2, mHeight / 2, radius, mRingPaint);
    }

    /**
     * 绘制圆环进度
     *
     * @param canvas
     */
    private void drawRingProgress(Canvas canvas) {
        RectF rectF = new RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        canvas.drawArc(rectF, -90, 360 * (1.0f * progress / mRingMax), false, mRingProgressPaint);
    }


    /**
     * dp转px
     *
     * @param dp
     * @return
     */
    public int dp2px(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }


    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (radius < (centerX - mRingWidth / 2)) {
                radius++;
            }
            switch (msg.what) {
                case RING_WHAT:
                    if (progress < mRingMax) {
                        progress = System.currentTimeMillis() - startTime;
                        postInvalidate();
                        mHandler.sendEmptyMessageDelayed(RING_WHAT, timeSpan);
                    }
                    break;
            }
        }
    };

    /**
     * 设置最大时间
     *
     * @param maxTime
     */
    public void setMaxTime(int maxTime) {
        this.mRingMax = maxTime;
    }

    /**
     * 设置最小时间
     *
     * @param minTime
     */
    public void setMinTime(int minTime) {
        this.mRingMin = minTime;
    }

    OnRecordButtonListener mOnRecordButtonListener;

    public interface OnRecordButtonListener {
        void onStartRecord();

        void onStopRecord();

        void onRecordTooShort();
    }

    public void setOnRecordButtonListener(OnRecordButtonListener mOnRecordButtonListener) {
        this.mOnRecordButtonListener = mOnRecordButtonListener;
    }
}