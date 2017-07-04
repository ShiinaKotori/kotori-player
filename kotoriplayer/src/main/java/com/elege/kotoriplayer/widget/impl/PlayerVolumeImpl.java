package com.elege.kotoriplayer.widget.impl;

import android.view.View;

/**
 * 滑动改变音量
 * Created by admin on 2016/11/25.
 */
public interface PlayerVolumeImpl {

    /**
     * 返回显示音量信息的View
     * @return
     */
    View getVolumeView();

    /**
     * 开始滑动改变音量
     */
    void startChangeVolume();

    /**
     * 改变音量
     * @param deltaVolume 改变的偏差值
     */
    void changeVolume(int deltaVolume);

    /**
     * 停止滑动改变音量
     */
    void stopChangeVolume();
}
