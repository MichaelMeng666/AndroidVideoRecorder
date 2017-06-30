package michael.com.recordvideolibrary.utils;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

/**
 * 描述:  图片相关工具类
 */
public class ImageUtils {

    /**
     * 保存图片
     *
     * @param bm bitmap
     * @return filepath
     */
    public static String saveBitmap2File(Bitmap bm, File thumbFile) {
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(thumbFile));
            bm.compress(CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            try {
                if (bos != null) {
                    bos.flush();
                    bos.close();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return thumbFile.getAbsolutePath();
    }

    /**
     * 获取视频的缩略图
     * 先通过ThumbnailUtils来创建一个视频的缩略图，然后再利用ThumbnailUtils来生成指定大小的缩略图。
     * 如果想要的缩略图的宽和高都小于MICRO_KIND，则类型要使用MICRO_KIND作为kind的值，这样会节省内存。
     *
     * @param videoPath 视频的路径
     * @param width     指定输出视频缩略图的宽度
     * @param height    指定输出视频缩略图的高度度
     * @param kind      参照MediaStore.Images.Thumbnails类中的常量MINI_KIND和MICRO_KIND。
     *                  其中，MINI_KIND: 512 x 384，MICRO_KIND: 96 x 96
     * @return 指定大小的视频缩略图
     */
    public static Bitmap getVideoThumbnail(String videoPath, int width, int height, int kind) {
        Bitmap bitmap = null;
        // 获取视频的缩略图
//        bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, kind);//随机抓取视频一帧作为缩略图
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (!TextUtils.isEmpty(videoPath)) {
            retriever.setDataSource(videoPath);
            bitmap = retriever.getFrameAtTime(1000);//按照视频指定时间获取缩略图
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        }
        retriever.release();
        return bitmap;
    }
}