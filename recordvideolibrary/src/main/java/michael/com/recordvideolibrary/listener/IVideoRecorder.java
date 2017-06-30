package michael.com.recordvideolibrary.listener;

import michael.com.recordvideolibrary.module.VideoObject;

public interface IVideoRecorder {
    public VideoObject startRecord();

    public void stopRecord();

    void recordTooShort();
}