package com.elege.kotoriplayer.widget.impl;

import android.view.View;

/**
 * 滑动改变亮度
 * Created by admin on 2016/11/25.
 */
public interface PlayerBrightnessImpl {

    /**
     * 返回显示亮度信息的View
     *
     * @return
     */
    View getBrightnessView();

    /**
     * 开始滑动改变亮度
     */
    void startChangeBrightness();

    /**
     * 改变亮度
     *
     * @param deltaBrightness 改变的亮度偏差值
     */
    void changeBrightness(int deltaBrightness);

    /**
     * 停止改变亮度
     */
    void stopChangeBrightness();
}
