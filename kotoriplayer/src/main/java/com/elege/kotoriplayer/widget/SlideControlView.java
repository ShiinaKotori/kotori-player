package com.elege.kotoriplayer.widget;


import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import com.elege.kotoriplayer.util.VideoUtil;
import com.elege.kotoriplayer.widget.impl.PlayerBrightnessImpl;
import com.elege.kotoriplayer.widget.impl.PlayerProgressImpl;
import com.elege.kotoriplayer.widget.impl.PlayerVideoImpl;
import com.elege.kotoriplayer.widget.impl.PlayerVolumeImpl;

import static android.R.attr.duration;

/**
 * 对播放器上的事件进行统一处理的View
 * Created by admin on 2016/11/24.
 */
public class SlideControlView extends FrameLayout {

    private static final int SLIDING_CHANGE_PROGRESS = 1;       //滑动改变进度
    private static final int SLIDING_CHANGE_BRIGHT = 2;          //滑动改变亮度
    private static final int SLIDING_CHANGE_VOLUME = 3;         //滑动改变音量
    private static final int SLIDING_NONE = -1;                 //没有在滑动

    private static final int SLIDING_CHANGE_PROGRESS_INTERVAL = 12; //滑动多少距离时间改变一秒
    private static final int SLIDING_CHANGE_BRIGHT_INTERVAL = 1;    //滑动多少距离亮度改变2
    private static final int SLIDING_CHANGE_VOLUME_INTERVAL = 30;   //滑动多少距离音量改变1

    private static final int CLICK_TIME_INTERVAL = 150;             //判断为点击的按下和弹起的间隔时间
    private static final int DOUBLE_CLICK_MAX_INTERVAL = 150;       //能判断为双击的最大间隔时间

    private int mTouchSlop;
    private boolean isVideoControll = false;
    private float downX;
    private float downY;
    private float lastY;
    private int tmpProgress;
    private int sliding_state = SLIDING_NONE;

    private int deltaSeconds;

    private PlayerVolumeImpl volumeImpl;
    private PlayerBrightnessImpl brightnessImpl;
    private PlayerProgressImpl progressImpl;
    private PlayerVideoImpl videoImpl;

    private boolean canSlideProgress = true;
    private boolean canSlideBrightness = true;
    private boolean canSlideVolume = true;

    private long downTime;
    private long upTime;
    private boolean hasOneClick = false;        //已经点击过一次了

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            hasOneClick = false;
            if (videoImpl != null) {
                videoImpl.onClick();
            }
        }
    };

    public SlideControlView(Context context) {
        this(context, null);
    }

    public SlideControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setBrightnessImpl(PlayerBrightnessImpl brightnessImpl) {
        this.brightnessImpl = brightnessImpl;
    }

    public void setVolumeImpl(PlayerVolumeImpl volumeImpl) {
        this.volumeImpl = volumeImpl;
    }

    public void setProgressImpl(PlayerProgressImpl progressImpl) {
        this.progressImpl = progressImpl;
    }

    public void setVideoImpl(PlayerVideoImpl videoImpl) {
        this.videoImpl = videoImpl;
    }

    public void setCanSlideVolume(boolean canSlideVolume) {
        this.canSlideVolume = canSlideVolume;
    }

    public void setCanSlideProgress(boolean canSlideProgress) {
        this.canSlideProgress = canSlideProgress;
    }

    public void setCanSlideBrightness(boolean canSlideBrightness) {
        this.canSlideBrightness = canSlideBrightness;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isVideoControll = false;
                downX = x;
                downY = y;
                break;
            case MotionEvent.ACTION_MOVE:

                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:

                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return isVideoControll;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = System.currentTimeMillis();
                handler.removeMessages(0);
                if (downTime - upTime > DOUBLE_CLICK_MAX_INTERVAL) {   //超过间隔时间，不能作为双击判断，清除上次点击标记
                    if (hasOneClick) {
                        handler.sendEmptyMessage(0);
                    }
                    hasOneClick = false;
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                handleMove(x, y);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                handleUp();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void handleMove(float x, float y) {
        float deltaX = x - downX;
        float deltaY = y - downY;
        float lastDeltaY = y - lastY;
        int width = getWidth();
        switch (sliding_state) {
            case SLIDING_NONE:
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > mTouchSlop && canSlideProgress) {
                    if (duration > 0) {
                        sliding_state = SLIDING_CHANGE_PROGRESS;
                        if (videoImpl != null) {
                            tmpProgress = VideoUtil.getProgressByPosition(videoImpl.getDuration(), videoImpl.getCurrentPosition());
                        }
                        if (progressImpl != null && videoImpl != null) {
                            progressImpl.startChangeProgress(videoImpl.getCurrentPosition(), videoImpl.getDuration());
                        }
                        if (videoImpl != null) {
                            videoImpl.startSliding(true);
                        }
                    }
                } else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > mTouchSlop
                        && ((downX <= width / 2) && canSlideBrightness) || (downX > width / 2 && canSlideVolume)) {
                    lastY = y;
                    if (videoImpl != null) {
                        videoImpl.startSliding(false);
                    }
                    if (downX <= width / 2) {
                        sliding_state = SLIDING_CHANGE_BRIGHT;
                        if (brightnessImpl != null) {
                            brightnessImpl.startChangeBrightness();
                        }
                    } else {
                        sliding_state = SLIDING_CHANGE_VOLUME;
                        if (volumeImpl != null) {
                            volumeImpl.startChangeVolume();
                        }
                    }
                }
                break;
            case SLIDING_CHANGE_PROGRESS:
                deltaSeconds = (int) deltaX / SLIDING_CHANGE_PROGRESS_INTERVAL;
                long duration = 0;
                if (videoImpl != null) {
                    duration = videoImpl.getDuration();
                }
                int tmpPosition = VideoUtil.getPositionByProgress(duration, tmpProgress);
                if (deltaSeconds < -tmpPosition / 1000) {
                    deltaSeconds = -tmpPosition / 1000;
                } else if (deltaSeconds > (duration - tmpPosition) / 1000) {
                    deltaSeconds = (int) (duration - tmpPosition) / 1000;
                }
                if (progressImpl != null) {
                    progressImpl.changeProgress(deltaSeconds);
                }
                if (videoImpl != null) {
                    videoImpl.onChangingProgress(tmpProgress + VideoUtil.getProgressByPosition(videoImpl.getDuration(), deltaSeconds * 1000));
                }
                break;
            case SLIDING_CHANGE_BRIGHT:
                if (Math.abs(lastDeltaY) >= SLIDING_CHANGE_BRIGHT_INTERVAL) {
                    lastY = y;
                    if (brightnessImpl != null) {
                        brightnessImpl.changeBrightness(lastDeltaY < 0 ? 3 : -3);
                    }
                }
                break;
            case SLIDING_CHANGE_VOLUME:
                if (Math.abs(lastDeltaY) >= SLIDING_CHANGE_VOLUME_INTERVAL) {
                    lastY = y;
                    if (volumeImpl != null) {
                        volumeImpl.changeVolume(lastDeltaY < 0 ? 1 : -1);
                    }
                }
                break;
        }
    }

    private void handleUp() {
        if (sliding_state == SLIDING_NONE) {
            upTime = System.currentTimeMillis();
            if (hasOneClick && upTime - downTime <= CLICK_TIME_INTERVAL) {   //已经点击过一次了
                hasOneClick = false;
                if (videoImpl != null) {
                    videoImpl.onDoubleClick();
                }
            } else {
                hasOneClick = true;
                handler.sendEmptyMessageDelayed(0, CLICK_TIME_INTERVAL);
            }
        }
        int state = sliding_state;
        if (state != SLIDING_NONE && videoImpl != null) {
            videoImpl.stopSliding();
        }
        sliding_state = SLIDING_NONE;
        switch (state) {
            case SLIDING_CHANGE_PROGRESS:
                if (progressImpl != null) {
                    progressImpl.stopChangeProgress();
                }
                if (videoImpl != null) {
                    int toPosition = VideoUtil.getPositionByProgress(videoImpl.getDuration(), tmpProgress) + deltaSeconds * 1000;
                    videoImpl.changeVideoPosition(toPosition);
                }
                break;
            case SLIDING_CHANGE_BRIGHT:
                if (brightnessImpl != null) {
                    brightnessImpl.stopChangeBrightness();
                }
                break;
            case SLIDING_CHANGE_VOLUME:
                if (volumeImpl != null) {
                    volumeImpl.stopChangeVolume();
                }
                break;
        }
    }

}
