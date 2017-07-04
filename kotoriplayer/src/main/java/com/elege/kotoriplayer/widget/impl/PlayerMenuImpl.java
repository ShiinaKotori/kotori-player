package com.elege.kotoriplayer.widget.impl;

import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * 播放器菜单栏
 * Created by admin on 2016/11/23.
 */
public interface PlayerMenuImpl {

    /**
     * 提供整个菜单栏
     * @return
     */
    View getPlayerMenuView();

    /**
     * 提供暂停按钮
     * @return
     */
    View getPauseButton();

    /**
     * 提供全屏按钮
     * @return
     */
    View getFullScreenButton();

    /**
     * 提供显示播放进度的TextView
     * @return
     */
    TextView getCurrentPositionTextView();

    /**
     * 提供显示视频总长度的TextView
     * @return
     */
    TextView getDurationTextView();

    /**
     * 提供用于显示和改变视频进度的SeekBar
     * @return
     */
    SeekBar getSeekBar();

}
