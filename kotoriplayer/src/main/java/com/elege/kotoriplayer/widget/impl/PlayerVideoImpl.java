package com.elege.kotoriplayer.widget.impl;

/**
 * 播放器
 * Created by admin on 2016/11/25.
 */
public interface PlayerVideoImpl {

    /**
     * 获取视频的总长度
     *
     * @return
     */
    long getDuration();

    /**
     * 获取视频当前的播放进度
     *
     * @return
     */
    long getCurrentPosition();

    /**
     * 开始滑动
     */
    void startSliding(boolean isProgress);

    /**
     * 正在滑动改变进度
     *
     * @param progress 此时滑动到的进度位置
     */
    void onChangingProgress(int progress);

    /**
     * 单纯点击了videoview
     */
    void onClick();

    /**
     * 改变视频播放进度
     *
     * @param position
     */
    void changeVideoPosition(long position);

    /**
     * 停止在播放器上的滑动
     */
    void stopSliding();

    /**
     * 双击
     */
    void onDoubleClick();

}
