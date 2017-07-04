package com.elege.kotoriplayer.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.elege.kotoriplayer.R;
import com.elege.kotoriplayer.util.VideoUtil;
import com.elege.kotoriplayer.widget.impl.OnPlayerClickListener;
import com.elege.kotoriplayer.widget.impl.PlayerBrightnessImpl;
import com.elege.kotoriplayer.widget.impl.PlayerFullScreenImpl;
import com.elege.kotoriplayer.widget.impl.PlayerMenuImpl;
import com.elege.kotoriplayer.widget.impl.PlayerProgressImpl;
import com.elege.kotoriplayer.widget.impl.PlayerVideoImpl;
import com.elege.kotoriplayer.widget.impl.PlayerVolumeImpl;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.widget.IjkVideoView;

import static com.elege.kotoriplayer.util.TimeFormatUtil.generateTime;

/**
 * 视频播放器
 * 1、菜单栏
 * Created by admin on 2016/9/27.
 */
public class KotoriPlayer extends RelativeLayout implements View.OnClickListener {

    private static final String TAG = "KotoriPlayer";
    private static final int MAX_ERROR_COUNT = 5;

    private static final int STATUS_ERROR = -1;
    private static final int STATUS_IDLE = 0;
    private static final int STATUS_LOADING = 1;
    private static final int STATUS_PLAYING = 2;
    private static final int STATUS_PAUSE = 3;
    private static final int STATUS_COMPLETED = 4;

    private boolean playerSupport = false;
    private int menuTimeout = 1000 * 5;

    private View mediaBufferingIndicator;       //缓冲时的提示View

    private IjkVideoView videoView;
    private SlideControlView slide_control_view;
    private View pause_state_view;

    private PlayerMenuImpl playerMenu;
    private View controller_layout;
    private View player_controler_btn;
    private TextView player_current_position;
    private SeekBar player_seekbar;
    private TextView player_duration;
    private View player_fullscreen_btn;

    private ProgressBar player_progressbar;
    private View error_msg_layout;
    private TextView error_msg;
    private Button retry_btn;

    private PlayerProgressImpl player_progress_impl;
    private PlayerBrightnessImpl player_brightness_impl;
    private PlayerVolumeImpl player_volume_impl;

    private String url;
    private int lastPosition;

    private boolean isLoop = false;
    private int state;
    private boolean playing = false;

    private boolean isDragging; //是否正在拖动进度(菜单栏的进度条)

    private int controller_layout_height = 0;
    private ObjectAnimator animator;

    private boolean isShowControllMenu = true;          //是否显示控制菜单
    private boolean isShowProgress = true;              //是否显示底部进度条
    private boolean isShowPauseBtn = true;              //是否显示暂停按钮
    private boolean isShowFullScreenBtn = true;         //是否显示全屏按钮
    private boolean autoRetry = false;                  //在错误时自动重试
    private int timeToRetry = 3;                 //几秒后重试
    private boolean isBuffering = false;
    private int errorCount = 0;
    private boolean isError = false;
    private long duration = 0;                  //视频总长度

    private OnPreparedListener preparedListener;
    private OnErrorListener errorListener;
    private OnInfoListener infoListener;
    private OnCompleteListener completeListener;

    private OnPlayerClickListener clickListener;
    private PlayerFullScreenImpl fullscreenImpl;

    private ViewGroup parentView;               //全屏前的父View
    private ViewGroup.LayoutParams layoutParams;
    private boolean isFullScreen = false;       //是否全屏

    private boolean isSliding = false;          //是否正在滑动
    private boolean isSlidingProgress = false;  //是否正在滑动改变进度

    public KotoriPlayer(Context context) {
        super(context);
        initViews(context);
    }

    public KotoriPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initViews(context);
    }

    public KotoriPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initViews(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public KotoriPlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initViews(context);
    }

    /**
     * 切换菜单栏（当显示时隐藏，隐藏时重新显示）
     */
    private void toggelMenuShow() {
        if (isMenuShow()) {
            hide(0);
        } else {
            show(menuTimeout);
        }
    }

    private void initViews(Context context) {
        try {
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            playerSupport = true;
        } catch (Throwable e) {
            Log.e("KotoriPlayer", "loadLibraries error", e);
        }

        View view = LayoutInflater.from(context).inflate(R.layout.kotoriplayer_layout, this);

        videoView = (IjkVideoView) view.findViewById(R.id.videoView);
        slide_control_view = (SlideControlView) view.findViewById(R.id.slide_control_view);
        pause_state_view = view.findViewById(R.id.pause_state_view);

        player_progressbar = (ProgressBar) view.findViewById(R.id.player_progressbar);
        error_msg_layout = view.findViewById(R.id.error_msg_layout);
        error_msg = (TextView) view.findViewById(R.id.error_msg);
        retry_btn = (Button) view.findViewById(R.id.retry_btn);

        initVideoView();
        setPlayerMenu(new KotoriPlayerMenu(context));

        if (!playerSupport) {
            Toast.makeText(context, "播放器不支持此设备！", Toast.LENGTH_LONG).show();
        }
    }

    private void initVideoView() {
        slide_control_view.setVideoImpl(new PlayerVideoImpl() {
            @Override
            public long getDuration() {
                return duration;
            }

            @Override
            public long getCurrentPosition() {
                return KotoriPlayer.this.getCurrentPosition();
            }

            @Override
            public void startSliding(boolean isProgress) {
                isSliding = true;
                isSlidingProgress = isProgress;
                if (isProgress) {
                    show(menuTimeout);
                }
                if (mediaBufferingIndicator != null) {
                    mediaBufferingIndicator.setVisibility(View.GONE);
                }
            }

            @Override
            public void onChangingProgress(int progress) {
                player_seekbar.setProgress(progress);
                hide(menuTimeout);
            }

            @Override
            public void onClick() {
                toggelMenuShow();
                if (clickListener != null) {
                    clickListener.onClick();
                }
            }

            @Override
            public void changeVideoPosition(long position) {
                videoView.seekTo((int) position);
                //再次开启更新进度条的消息
                sendUpdateProgressMsg(0);
            }

            @Override
            public void stopSliding() {
                isSliding = false;
                isSlidingProgress = false;
            }

            @Override
            public void onDoubleClick() {
                if (clickListener != null) {
                    clickListener.onDoubleClick();
                }
            }
        });
        setPlayerProgressView(new FloatProgressView(getContext()));
        FloatBrightVolumeView brightVolumeView = new FloatBrightVolumeView(getContext());
        setPlayerVolumeView(brightVolumeView);
        setPlayerBrightnessView(brightVolumeView);

//        videoView.setMediaController(new AndroidMediaController(getContext(), false));

        videoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                duration = videoView.getDuration();
                mp.setLooping(isLoop);
                if (mediaBufferingIndicator != null)
                    mediaBufferingIndicator.setVisibility(View.GONE);
                if (preparedListener != null) {
                    preparedListener.onPrepared();
                }
            }
        });

        videoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                switch (what) {
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_START:   //loading
                        updateViewByState(STATUS_LOADING);
                        isBuffering = true;
                        break;
                    case IMediaPlayer.MEDIA_INFO_BUFFERING_END:     //playing
                        updateViewByState(STATUS_PLAYING);
                        isBuffering = false;
                        break;
                    case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                        // 显示 下载速度
                        // Toast.makeText(activity,"download rate:" +
                        // extra,Toast.LENGTH_SHORT).show();
                        break;
                    case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START: //playing
                        isBuffering = false;
                        updateViewByState(STATUS_PLAYING);
                        break;
                }

                if (infoListener != null) {
                    infoListener.onInfo(what, extra);
                }
                return false;
            }
        });

        videoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                if (mediaBufferingIndicator != null)
                    mediaBufferingIndicator.setVisibility(View.GONE);
                isBuffering = false;
                updateViewByState(STATUS_ERROR);
                if (errorListener != null) {
                    errorListener.onError(what, extra);
                }
                return false;
            }
        });

        videoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                updateViewByState(STATUS_COMPLETED);
                isBuffering = false;
                if (completeListener != null) {
                    completeListener.onComplete();
                }
            }
        });

        pause_state_view.setOnClickListener(this);
        retry_btn.setOnClickListener(this);
    }

    private void initMenuView() {
        if (controller_layout != null) {
            removeView(controller_layout);
            if (playerMenu == null) {
                controller_layout = null;
                player_controler_btn = null;
                player_current_position = null;
                player_seekbar = null;
                player_duration = null;
                player_fullscreen_btn = null;
            }
        }
        controller_layout = playerMenu.getPlayerMenuView();
        player_controler_btn = playerMenu.getPauseButton();
        player_current_position = playerMenu.getCurrentPositionTextView();
        player_seekbar = playerMenu.getSeekBar();
        player_seekbar.setMax(1000);
        player_duration = playerMenu.getDurationTextView();
        player_fullscreen_btn = playerMenu.getFullScreenButton();

        if (controller_layout != null) {
            controller_layout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    controller_layout_height = controller_layout.getMeasuredHeight();
                    if (controller_layout_height > 0) {
                        controller_layout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        if (!isShowControllMenu) {
                            controller_layout.setVisibility(View.GONE);
                        } else {
                            player_progressbar.setVisibility(View.GONE);
                            hide(menuTimeout);
                        }
                    }
                }
            });

            controller_layout.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    hide(menuTimeout);
                    return true;
                }
            });
        }

        if (player_controler_btn != null) {
            player_controler_btn.setOnClickListener(controllBtnListener);
        }

        if (player_fullscreen_btn != null) {
            player_fullscreen_btn.setOnClickListener(fullscreenBtnListener);
        }

        if (player_seekbar != null) {
            player_seekbar.setOnSeekBarChangeListener(mSeekListener);
        }

        ViewGroup parentView = (ViewGroup) controller_layout.getParent();
        if (parentView != null) {
            parentView.removeView(controller_layout);
        }
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        addView(controller_layout, params);
    }

    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            Log.i("SeekBar", "onProgressChanged----fromUser:" + fromUser + "    progress:" + progress);
            if (!fromUser && !isSlidingProgress) {
                return;
            }
            changeProgress(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            Log.i("SeekBar", "onStartTrackingTouch----progress:" + seekBar.getProgress());
            isDragging = true;
            show(60 * 1000 * 10);
            handler.removeMessages(MSG_SET_PROGRESS);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.i("SeekBar", "onStopTrackingTouch----progress:" + seekBar.getProgress());
            videoView.seekTo(VideoUtil.getPositionByProgress(duration, seekBar.getProgress()));
            show(menuTimeout);
            isDragging = false;
            sendUpdateProgressMsg(0);
        }
    };

    /**
     * 改变视频播放进度
     *
     * @param progress
     */
    private void changeProgress(int progress) {
        int newPosition = VideoUtil.getPositionByProgress(duration, progress);
        if (!canAutoChangeProgress()) {
            if (player_current_position != null)
                player_current_position.setText(generateTime(newPosition));
            player_progressbar.setProgress(progress);
        } else {
            videoView.seekTo(newPosition);
            sendUpdateProgressMsg(0);
        }
    }

    private final OnClickListener controllBtnListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            hide(menuTimeout);
            boolean selected = player_controler_btn.isSelected();
            if (selected) {
                pauseVideo();
            } else {
                playVideo();
            }
        }
    };

    private final OnClickListener fullscreenBtnListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (fullscreenImpl == null || !fullscreenImpl.onPlayerFullScreen()) {
                toggleFullScreen();
                player_fullscreen_btn.setSelected(isFullScreen);
            }
        }
    };

    private static final int MSG_HIDE_MENU = 0;     //隐藏菜单栏
    private static final int MSG_SET_PROGRESS = 1;  //更新视频进度的相关View
    private static final int MSG_RETRY_TIME = 2;    //视频播放失败后的重试
    private static final int MSG_HIDE_ERROR = 3;    //隐藏播放失败信息

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE_MENU:
                    changeMenuVisible(false);
                    break;
                case MSG_SET_PROGRESS:
                    setProgress();
                    Log.e(TAG, "isPlaying:" + videoView.isPlaying());
                    if (canAutoChangeProgress()) {
                        sendUpdateProgressMsg(1000);
                    }
                    break;
                case MSG_RETRY_TIME:
                    int time = (Integer) msg.obj;
                    if (errorCount > MAX_ERROR_COUNT) {
                        error_msg.setText(R.string.retry_toomuch_time);
                        retry_btn.setVisibility(View.VISIBLE);
                        retry_btn.setText(R.string.retry_btn_again);
                    } else {
                        if (time == 0) {
                            error_msg.setText(getContext().getString(R.string.retry_immediate_time));
                            retry_btn.setVisibility(View.GONE);
                            handler.sendMessageDelayed(handler.obtainMessage(MSG_RETRY_TIME, -1), 500);
                        } else if (time < 0) {
                            retryPlay();
                        } else {
                            error_msg.setText(getContext().getString(R.string.retry_delay_time, time + ""));
                            retry_btn.setVisibility(View.VISIBLE);
                            retry_btn.setText(R.string.retry_btn_immediate);
                            time--;
                            if (time == 0) {
                                time = -1;
                            }
                            handler.sendMessageDelayed(handler.obtainMessage(MSG_RETRY_TIME, time), 1000);
                        }
                    }
                    break;
                case MSG_HIDE_ERROR:
                    error_msg_layout.setVisibility(View.GONE);
                    break;
            }
        }
    };

    /**
     * 设置视频播放器缓冲时的loading View
     *
     * @param mediaBufferingIndicator
     * @return
     */
    public KotoriPlayer setMediaBufferingIndicator(View mediaBufferingIndicator) {
        if (this.mediaBufferingIndicator != null) {
            removeView(this.mediaBufferingIndicator);
        }
        this.mediaBufferingIndicator = mediaBufferingIndicator;
        if (mediaBufferingIndicator != null) {
            ViewParent parent = mediaBufferingIndicator.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(mediaBufferingIndicator);
            }
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            addView(mediaBufferingIndicator, params);
            mediaBufferingIndicator.setVisibility(View.GONE);
        }
//        videoView.setMediaBufferingIndicator(mediaBufferingIndicator);
        return this;
    }

    /**
     * 设置视频播放器的菜单栏
     *
     * @param playerMenu
     * @return
     */
    public KotoriPlayer setPlayerMenu(PlayerMenuImpl playerMenu) {
        this.playerMenu = playerMenu;
        initMenuView();
        return this;
    }

    /**
     * 设置播放器滑动改变进度条时显示的提示信息View
     *
     * @param player_progress_impl
     * @return
     */
    public KotoriPlayer setPlayerProgressView(PlayerProgressImpl player_progress_impl) {
        this.player_progress_impl = player_progress_impl;
        if (player_progress_impl != null) {
            slide_control_view.setCanSlideProgress(true);
            addChildView(player_progress_impl.getProgressView());
        } else {
            slide_control_view.setCanSlideProgress(false);
        }
        slide_control_view.setProgressImpl(player_progress_impl);
        return this;
    }

    /**
     * 设置播放器滑动改变音量时显示的提示信息View
     *
     * @param player_volume_impl
     * @return
     */
    public KotoriPlayer setPlayerVolumeView(PlayerVolumeImpl player_volume_impl) {
        this.player_volume_impl = player_volume_impl;
        if (player_volume_impl != null) {
            slide_control_view.setCanSlideVolume(true);
            addChildView(player_volume_impl.getVolumeView());
        } else {
            slide_control_view.setCanSlideVolume(false);
        }
        slide_control_view.setVolumeImpl(player_volume_impl);
        return this;
    }

    /**
     * 设置播放器滑动改变亮度时显示的提示信息View
     *
     * @param player_brightness_impl
     * @return
     */
    public KotoriPlayer setPlayerBrightnessView(PlayerBrightnessImpl player_brightness_impl) {
        this.player_brightness_impl = player_brightness_impl;
        if (player_brightness_impl != null) {
            slide_control_view.setCanSlideBrightness(true);
            addChildView(player_brightness_impl.getBrightnessView());
        } else {
            slide_control_view.setCanSlideBrightness(false);
        }
        slide_control_view.setBrightnessImpl(player_brightness_impl);
        return this;
    }

    private final void addChildView(View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null && parent != this) {
            parent.removeView(view);
        }
        if (parent == null || parent != this) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
            addView(view, params);
        }
        view.setVisibility(View.GONE);
    }

    public KotoriPlayer play(String url) {
        return play(url, 0);
    }

    /**
     * @param url             开始播放(可播放指定位置)
     * @param currentPosition 指定位置的大小(0-1000)
     * @see （一般用于记录上次播放的位置或者切换视频源）
     */
    public KotoriPlayer play(String url, int currentPosition) {
        if (url != null && !url.equals(this.url)) {  //是新地址，重置错误次数
            duration = 0;
            errorCount = 0;
        }
        this.url = url;
        lastPosition = currentPosition;

        if (player_controler_btn != null)
            player_controler_btn.setEnabled(true);

        videoView.setVideoPath(url);
        seekTo(lastPosition, true);
        playVideo();

        return this;
    }

    /**
     * 暂停
     *
     * @return
     */
    public KotoriPlayer pauseVideo() {
        playing = false;
        videoView.pause();
        updateViewByState(STATUS_PAUSE);
        sendUpdateProgressMsg(0);

        if (player_controler_btn != null)
            player_controler_btn.setSelected(false);

        if (mediaBufferingIndicator != null) {
            mediaBufferingIndicator.setVisibility(View.GONE);
        }
        return this;
    }

    /**
     * 播放
     *
     * @return
     */
    public KotoriPlayer playVideo() {
        playing = true;
        isError = false;
        videoView.start();
        updateViewByState(STATUS_PLAYING);
        sendUpdateProgressMsg(0);

        if (player_controler_btn != null)
            player_controler_btn.setSelected(true);

        if (errorCount == 0) {
            show(menuTimeout);
        }
        if (isBuffering || duration <= 0) {
            updateViewByState(STATUS_LOADING);
        }
        return this;
    }

    /**
     * 释放资源
     */
    public void release() {
        videoView.stopPlayback();
        videoView.release(true);
    }

    /**
     * 获取视频的播放地址
     *
     * @return
     */
    public String getUrl() {
        return url;
    }

    /**
     * 切换全屏
     *
     * @return
     */
    public KotoriPlayer toggleFullScreen() {
        Activity activity = null;
        if (getContext() instanceof Activity) {
            activity = (Activity) getContext();
        } else {
            throw new RuntimeException("这个方法只允许在Activity中调用");
        }
        if (isFullScreen) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
            contentView.removeView(this);
            parentView.addView(this, layoutParams);
            isFullScreen = false;
        } else {
            if (parentView == null) {
                parentView = (ViewGroup) getParent();
                layoutParams = getLayoutParams();
            }
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            parentView.removeView(this);
            ViewGroup contentView = (ViewGroup) activity.findViewById(android.R.id.content);
            contentView.addView(this, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            isFullScreen = true;
        }
        return this;
    }

    /**
     * 播放失败后重试播放
     */
    private void retryPlay() {
        handler.removeMessages(MSG_RETRY_TIME);
        handler.removeMessages(MSG_HIDE_ERROR);
        handler.sendEmptyMessageDelayed(MSG_HIDE_ERROR, 200);
        play(url, lastPosition);
    }

    /**
     * seekTo position
     *
     * @param msec millisecond
     */
    public KotoriPlayer seekTo(int msec, boolean showControlPanle) {
        videoView.seekTo(msec);
        if (showControlPanle) {
            show(menuTimeout);
        }
        return this;
    }

    /**
     * 获取当前播放进度
     *
     * @return
     */
    public int getCurrentPosition() {
        return videoView.getCurrentPosition();
    }

    /**
     * 获取视频总长度
     *
     * @return
     */
    public int getDuration() {
        return videoView.getDuration();
    }

    /**
     * 能否显示底部控制菜单
     *
     * @param canShow
     * @return
     */
    public KotoriPlayer setCanShowControllMenu(boolean canShow) {
        if (canShow && !this.isShowControllMenu) {   //原来没显示的，马上显示
            show(menuTimeout);
        } else if (!canShow) {
            hide(0);
        }
        this.isShowControllMenu = canShow;
        return this;
    }

    /**
     * 能否显示底部进度条
     *
     * @param canShow
     * @return
     */
    public KotoriPlayer setCanShowProgress(boolean canShow) {
        this.isShowProgress = canShow;
        if (!isMenuShow() && canShow) {
            player_progressbar.setVisibility(View.VISIBLE);
        } else if (!canShow) {
            player_progressbar.setVisibility(View.GONE);
        }
        return this;
    }

    /**
     * 设置是否显示全屏按钮
     *
     * @param canShow
     * @return
     */
    public KotoriPlayer setCanShowFullScreenBtn(boolean canShow) {
        this.isShowFullScreenBtn = canShow;
        if (player_fullscreen_btn != null)
            if (canShow) {
                player_fullscreen_btn.setVisibility(View.VISIBLE);
            } else {
                player_fullscreen_btn.setVisibility(View.GONE);
            }
        return this;
    }

    public boolean isShowFullScreenBtn() {
        return isShowFullScreenBtn;
    }

    /**
     * 设置是否显示菜单栏的暂停按钮
     *
     * @param canShow
     * @return
     */
    public KotoriPlayer setCanShowPauseBtn(boolean canShow) {
        this.isShowPauseBtn = canShow;
        if (player_controler_btn != null)
            if (canShow) {
                player_controler_btn.setVisibility(View.VISIBLE);
            } else {
                player_controler_btn.setVisibility(View.GONE);
            }
        return this;
    }

    public boolean isShowPauseBtn() {
        return isShowPauseBtn;
    }

    /**
     * 设置底部控制菜单的显示超时时间
     *
     * @param timeout
     * @return
     */
    public KotoriPlayer setControllerMenuTimeout(int timeout) {
        menuTimeout = timeout;
        hide(menuTimeout);
        return this;
    }

    /**
     * 设置超时重试等候时间
     *
     * @param seconds 秒
     * @return
     */
    public KotoriPlayer setTimeToRetry(int seconds) {
        timeToRetry = seconds;
        return this;
    }

    /**
     * 设置当播放出错时是否自动马上重试
     *
     * @param autoRetry
     * @return
     */
    public KotoriPlayer setAutoRetry(boolean autoRetry) {
        this.autoRetry = autoRetry;
        return this;
    }

    /**
     * 显示底部菜单栏
     *
     * @param timeout
     */
    private void show(int timeout) {
        if (!isShowControllMenu) {
            return;
        }
        changeMenuVisible(true);
        hide(timeout);
    }

    /**
     * timeout秒后隐藏菜单栏
     *
     * @param timeout
     */
    private void hide(int timeout) {
        if (!isShowControllMenu) {
            return;
        }
        handler.removeMessages(MSG_HIDE_MENU);
        handler.sendEmptyMessageDelayed(MSG_HIDE_MENU, timeout);
    }

    /**
     * 底部菜单是否显示
     *
     * @return
     */
    private boolean isMenuShow() {
        if (controller_layout == null) {
            return false;
        }
        return controller_layout.getVisibility() == View.VISIBLE;
    }

    /**
     * 改变底部菜单栏的显示状态
     *
     * @param show true：显示    false：隐藏
     */
    private void changeMenuVisible(boolean show) {
        if (controller_layout == null) {
            return;
        }
        if (controller_layout_height <= 0 || !isShowControllMenu || (isError && show)) {
            return;
        }
        if ((show && isMenuShow()) || (!show && !isMenuShow())) {
            return;
        }
        if (animator != null) {
            animator.cancel();
        }
        float translationY = controller_layout.getTranslationY();
        if (show) {
            animator = ObjectAnimator.ofFloat(controller_layout, "translationY", translationY, 0f).setDuration((int) (translationY * 1.5));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    controller_layout.setVisibility(View.VISIBLE);
                    player_progressbar.setVisibility(View.GONE);
                }
            });
            animator.start();
        } else {
            animator = ObjectAnimator.ofFloat(controller_layout, "translationY", translationY, controller_layout_height)
                    .setDuration((int) (Math.abs(translationY - controller_layout_height) * 1.5));
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    controller_layout.setVisibility(View.GONE);
                    if (isShowProgress) {
                        player_progressbar.setVisibility(View.VISIBLE);
                    }
                }
            });
            animator.start();
        }
    }

    /**
     * 能否根据视频播放进度自动更新进度条界面
     *
     * @return
     */
    private boolean canAutoChangeProgress() {
        return !isDragging && !isSlidingProgress;
    }

    /**
     * 更新视频进度的相关显示View（当前时间、进度条）
     */
    private void setProgress() {
        if (!canAutoChangeProgress()) {
            return;
        }
        int currentPosition = videoView.getCurrentPosition();
        int bufferedPersentage = videoView.getBufferPercentage();
        int currentPersentage = (int) (currentPosition * 1000f / duration);

        if (duration > 0) {
            lastPosition = currentPosition;
        }

        if (player_current_position != null)
            player_current_position.setText(generateTime(currentPosition));
        if (player_duration != null)
            player_duration.setText(generateTime(duration));
        if (player_seekbar != null) {
            player_seekbar.setProgress(currentPersentage);
            player_seekbar.setSecondaryProgress(bufferedPersentage * 10);
        }
        player_progressbar.setProgress(currentPersentage);
        player_progressbar.setSecondaryProgress(bufferedPersentage * 10);
    }

    /**
     * 设置视频循环播放，请在调用play之前调用
     *
     * @param looping
     * @return
     */
    public KotoriPlayer setLooping(boolean looping) {
        isLoop = looping;

        return this;
    }

    /**
     * 通知每隔一秒刷新进度条
     *
     * @param delay
     */
    private void sendUpdateProgressMsg(int delay) {
        handler.removeMessages(MSG_SET_PROGRESS);
        handler.sendEmptyMessageDelayed(MSG_SET_PROGRESS, delay);
    }

    public KotoriPlayer setPreparedListener(OnPreparedListener preparedListener) {
        this.preparedListener = preparedListener;
        return this;
    }

    public KotoriPlayer setErrorListener(OnErrorListener errorListener) {
        this.errorListener = errorListener;
        return this;
    }

    public KotoriPlayer setInfoListener(OnInfoListener infoListener) {
        this.infoListener = infoListener;
        return this;
    }

    public KotoriPlayer setCompleteListener(OnCompleteListener completeListener) {
        this.completeListener = completeListener;
        return this;
    }

    /**
     * 设置播放器的点击监听器
     *
     * @param listener
     * @return
     */
    public KotoriPlayer setOnPlayerClickListener(OnPlayerClickListener listener) {
        this.clickListener = listener;
        return this;
    }

    /**
     * 设置对视频全屏事件的处理
     *
     * @param listener
     * @return
     */
    public KotoriPlayer setPlayerFullScreenImpl(PlayerFullScreenImpl listener) {
        this.fullscreenImpl = listener;
        return this;
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnErrorListener {
        void onError(int what, int extra);
    }

    public interface OnInfoListener {
        void onInfo(int what, int extra);
    }

    public interface OnCompleteListener {
        void onComplete();
    }

    /**
     * 根据视频状态更新界面
     *
     * @param state
     */
    private void updateViewByState(int state) {
        if (state == STATUS_PLAYING && playing) {
            sendUpdateProgressMsg(0);
            pause_state_view.setVisibility(View.GONE);
            if (mediaBufferingIndicator != null) {
                mediaBufferingIndicator.setVisibility(View.GONE);
            }
        } else if (state == STATUS_LOADING) {
            if (playing) {
                error_msg_layout.setVisibility(View.GONE);
                pause_state_view.setVisibility(View.GONE);
                if (mediaBufferingIndicator != null && !isSliding) {
                    mediaBufferingIndicator.setVisibility(View.VISIBLE);
                }
            }

        } else if (state == STATUS_COMPLETED) {
            videoView.seekTo(0);
            errorCount = 0;
            pauseVideo();
        } else if (state == STATUS_ERROR) {
            isError = true;
            errorCount++;
            hide(0);
            if (player_controler_btn != null) {
                player_controler_btn.setSelected(false);
                player_controler_btn.setEnabled(false);
            }
            if (autoRetry || timeToRetry <= 0) {
                error_msg_layout.setVisibility(View.VISIBLE);
                handler.removeMessages(MSG_HIDE_ERROR);
                handler.sendMessage(handler.obtainMessage(MSG_RETRY_TIME, 0));
            } else {
                error_msg_layout.setVisibility(View.VISIBLE);
                handler.removeMessages(MSG_HIDE_ERROR);
                handler.sendMessage(handler.obtainMessage(MSG_RETRY_TIME, timeToRetry));
            }
        } else if (state == STATUS_PAUSE) {
            pause_state_view.setVisibility(View.VISIBLE);

        }
        this.state = state;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.pause_state_view) {
            playVideo();
        } else if (id == R.id.retry_btn) {
            if (errorCount >= MAX_ERROR_COUNT) {
                errorCount = 0;
            }
            retryPlay();
        }
    }

}
