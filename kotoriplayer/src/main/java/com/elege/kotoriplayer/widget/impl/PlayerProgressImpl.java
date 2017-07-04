package com.elege.kotoriplayer.widget.impl;

import android.view.View;

/**
 * 滑动改变进度
 * Created by admin on 2016/11/25.
 */
public interface PlayerProgressImpl {

    /**
     * 返回显示视频进度信息的View
     * @return
     */
    View getProgressView();

    /**
     * 开始滑动改变视频进度
     * @param currentPosition
     * @param duration
     */
    void startChangeProgress(long currentPosition, long duration);

    /**
     * 改变视频进度
     * @param deltaSeconds  改变进度的秒数偏差值
     */
    void changeProgress(long deltaSeconds);

    /**
     * 停止改变视频进度
     */
    void stopChangeProgress();

}
