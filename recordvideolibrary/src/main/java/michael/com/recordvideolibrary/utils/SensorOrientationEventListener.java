package michael.com.recordvideolibrary.utils;

import android.content.Context;
import android.view.OrientationEventListener;

public class SensorOrientationEventListener extends OrientationEventListener {

    private int mOrientation = -1;
    private OrientationChangeListener orientationEventListener;

    public SensorOrientationEventListener(Context context) {
        super(context);
    }

    public SensorOrientationEventListener(Context context, int rate) {
        super(context, rate);
    }

    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
            return;
        }
        //过滤角度，保证只返回四个方向
        int newOrientation = ((orientation + 45) / 90 * 90) % 360;
        if (newOrientation != mOrientation) {
            mOrientation = newOrientation;
            //返回的mOrientation就是手机方向，为0°、90°、180°和270°中的一个
            orientationEventListener.currentOrientation(mOrientation);
        }
    }

    public void setOrientationEventListener(OrientationChangeListener orientationEventListener) {
        this.orientationEventListener = orientationEventListener;
    }

    public interface OrientationChangeListener {
        void currentOrientation(int orientation);
    }
}