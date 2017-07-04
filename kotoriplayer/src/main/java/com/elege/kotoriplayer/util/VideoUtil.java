package com.elege.kotoriplayer.util;

import static android.R.attr.duration;

/**
 * 将视频播放时间和进度进行转换的工具类
 * Created by admin on 2016/11/25.
 */
public class VideoUtil {

    /**
     * 把视频当前播放的时长转为进度（总进度1000）
     *
     * @param duration
     * @param position
     * @return
     */
    public static int getProgressByPosition(long duration, long position) {
        int progress = (int) (position * 1f / duration * 1000);

        return progress;
    }

    /**
     * 把进度转为视频当前播放时长（总长度1000）
     *
     * @param duration
     * @param progress
     * @return
     */
    public static int getPositionByProgress(long duration, long progress) {
        int position = (int) ((duration * progress * 1.0) / 1000);

        return position;
    }

}
