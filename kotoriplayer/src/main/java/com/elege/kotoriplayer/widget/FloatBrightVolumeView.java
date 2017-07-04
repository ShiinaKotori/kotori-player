package com.elege.kotoriplayer.widget;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.elege.kotoriplayer.R;
import com.elege.kotoriplayer.util.BrightVolumeUtil;
import com.elege.kotoriplayer.util.ScaleUtil;
import com.elege.kotoriplayer.widget.impl.PlayerBrightnessImpl;
import com.elege.kotoriplayer.widget.impl.PlayerVolumeImpl;

import static android.R.attr.max;

/**
 * 显示音量和亮度的控件
 * Created by admin on 2016/11/24.
 */
public class FloatBrightVolumeView extends RelativeLayout implements PlayerBrightnessImpl, PlayerVolumeImpl {

    private TextView desc_text;
    private LinearLayout change_progress_layout;
    private int leftMargin;
    private int topMargin;
    private ImageView max_image;
    private ProgressBar value_progress;
    private ImageView min_image;

    private int maxVolume;
    private int curVolume;
    private int curBright;

    public FloatBrightVolumeView(Context context) {
        this(context, null);
    }

    public FloatBrightVolumeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatBrightVolumeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        maxVolume = BrightVolumeUtil.getMusicMaxVolume(context);

        desc_text = new TextView(context);
        desc_text.setTextColor(Color.WHITE);
        int paddingTop = ScaleUtil.dip2px(context, 12);
        int paddingLeft = ScaleUtil.dip2px(context, 22);
        desc_text.setPadding(paddingLeft, paddingTop, paddingLeft, paddingTop);
        desc_text.setBackgroundColor(Color.parseColor("#99000000"));
        RelativeLayout.LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        addView(desc_text, params);

        change_progress_layout = new LinearLayout(context);
        change_progress_layout.setOrientation(LinearLayout.VERTICAL);
        change_progress_layout.setGravity(Gravity.CENTER);
        change_progress_layout.setPadding(paddingTop, paddingLeft, paddingTop, paddingLeft);
        change_progress_layout.setBackgroundColor(Color.parseColor("#99000000"));

        int imageSize = ScaleUtil.dip2px(context, 30);
        max_image = new ImageView(context);
        change_progress_layout.addView(max_image, new LinearLayout.LayoutParams(imageSize, imageSize));

        value_progress = (ProgressBar) LayoutInflater.from(context).inflate(R.layout.value_progress_layout, null);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(ScaleUtil.dip2px(context, 6), 0);
        progressParams.weight = 1;
        int _topMargin = ScaleUtil.dip2px(context, 4);
        progressParams.topMargin = _topMargin;
        progressParams.bottomMargin = _topMargin;
        change_progress_layout.addView(value_progress, progressParams);

        min_image = new ImageView(context);
        change_progress_layout.addView(min_image, new LinearLayout.LayoutParams(imageSize, imageSize));

        leftMargin = ScaleUtil.dip2px(context, 40);
        topMargin = ScaleUtil.dip2px(context, 40);
        addView(change_progress_layout);
    }

    private RelativeLayout.LayoutParams getParams(boolean left) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        params.topMargin = topMargin;
        params.bottomMargin = topMargin;
        if (left) {
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
            params.leftMargin = leftMargin;
        } else {
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            params.rightMargin = leftMargin;
        }
        return params;
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        super.setLayoutParams(params);
    }

    @Override
    public View getBrightnessView() {
        return this;
    }

    @Override
    public View getVolumeView() {
        return this;
    }

    @Override
    public void startChangeBrightness() {
        min_image.setImageResource(R.drawable.ic_bright_off_white);
        max_image.setImageResource(R.drawable.ic_bright_up_white);
        change_progress_layout.setLayoutParams(getParams(false));

        value_progress.setMax(255); //亮度最大255
        curBright = BrightVolumeUtil.getSystemBrightness(getContext());
        setCurrentBrightness();

        setVisibility(View.VISIBLE);
    }

    @Override
    public void changeBrightness(int deltaBrightness) {
        curBright += deltaBrightness;
        if (curBright < 0) {
            curBright = 0;
        } else if (curBright > 255) {
            curBright = 255;
        }
        setCurrentBrightness();
        BrightVolumeUtil.setSystemBrightness(getContext(), curBright);
    }

    @Override
    public void stopChangeBrightness() {
        setVisibility(View.GONE);
    }

    @Override
    public void startChangeVolume() {
        min_image.setImageResource(R.drawable.ic_volume_off_white);
        max_image.setImageResource(R.drawable.ic_volume_up_white);
        change_progress_layout.setLayoutParams(getParams(true));

        curVolume = BrightVolumeUtil.getMusicCurrentVolume(getContext());
        value_progress.setMax(maxVolume);
        setCurrentVolume();

        setVisibility(View.VISIBLE);
    }

    @Override
    public void changeVolume(int deltaVolume) {
        curVolume += deltaVolume;
        if (curVolume < 0) {
            curVolume = 0;
        } else if (curVolume > maxVolume) {
            curVolume = maxVolume;
        }
        setCurrentVolume();
        BrightVolumeUtil.setMusicVolume(getContext(), curVolume);
    }

    @Override
    public void stopChangeVolume() {
        setVisibility(View.GONE);
    }

    private void setCurrentVolume() {
        value_progress.setProgress(curVolume);
        if (curVolume == 0) {
            desc_text.setText("静音");
        } else if (curVolume == maxVolume) {
            desc_text.setText("最大音量");
        } else {
            int voluemP = (int) (curVolume * 100f / maxVolume);
            desc_text.setText("当前音量：" + voluemP + "%");
        }
    }

    private void setCurrentBrightness() {
        value_progress.setProgress(curBright);
        if (curBright == 0) {
            desc_text.setText("最低亮度");
        } else if (curBright == 255) {
            desc_text.setText("最大亮度");
        } else {
            int brightP = (int) (curBright * 100f / 255);
            desc_text.setText("当前亮度：" + brightP + "%");
        }
    }

}
