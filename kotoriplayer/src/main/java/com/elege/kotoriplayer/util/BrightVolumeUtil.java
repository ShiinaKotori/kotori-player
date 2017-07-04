package com.elege.kotoriplayer.util;

import android.content.Context;
import android.media.AudioManager;
import android.provider.Settings;

/**
 * 音量和亮度的工具类
 * Created by admin on 2016/11/24.
 */
public class BrightVolumeUtil {

    /**
     * 获得系统亮度
     *
     * @return
     */
    public static int getSystemBrightness(Context context) {
        int systemBrightness = 0;
        try {
            systemBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return systemBrightness;
    }

    /**
     * 设置系统亮度
     *
     * @param context
     * @param brightness
     */
    public static void setSystemBrightness(Context context, int brightness) {
        Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
    }

    /**
     * 获取最大音量
     *
     * @param context
     * @return
     */
    public static int getMusicMaxVolume(Context context) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    /**
     * 获取当前音量
     */
    public static int getMusicCurrentVolume(Context context) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
    }

    public static void setMusicVolume(Context context, int volume) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
    }

}
