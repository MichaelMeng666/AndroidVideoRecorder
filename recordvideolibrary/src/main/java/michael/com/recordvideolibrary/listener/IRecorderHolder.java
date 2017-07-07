package michael.com.recordvideolibrary.listener;

public interface IRecorderHolder {
    void startRecord();

    void stopRecord(String videoPath, String thumbPath);

    void recordTooShort();
}