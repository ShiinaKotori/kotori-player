package com.elege.kotoriplayer.widget.impl;

/**
 * 对视频全屏事件的拦截处理
 * Created by admin on 2016/11/29.
 */
public interface PlayerFullScreenImpl {

    /**
     * 对视频全屏的拦截处理
     *
     * @return 是否拦截，为true表示拦截，此时全屏处理由用户自己处理；为false表示不拦截，此时由播放器内部进行全屏处理
     */
    boolean onPlayerFullScreen();

}
