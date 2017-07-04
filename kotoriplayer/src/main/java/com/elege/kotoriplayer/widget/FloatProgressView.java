package com.elege.kotoriplayer.widget;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.elege.kotoriplayer.util.ScaleUtil;
import com.elege.kotoriplayer.util.TimeFormatUtil;
import com.elege.kotoriplayer.widget.impl.PlayerProgressImpl;

/**
 * 显示播放进度的控件
 * Created by admin on 2016/11/24.
 */
public class FloatProgressView extends LinearLayout implements PlayerProgressImpl {

    private TextView progress_text;
    private TextView delta_time_text;

    private long currentPosition;
    private long duration;

    public FloatProgressView(Context context) {
        this(context, null);
    }

    public FloatProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setBackgroundColor(Color.parseColor("#99000000"));
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER);
        int paddingTop = ScaleUtil.dip2px(context, 6);
        int paddingLeft = ScaleUtil.dip2px(context, 20);
        setPadding(paddingLeft, paddingTop, paddingLeft, paddingTop);

        progress_text = new TextView(context);
        delta_time_text = new TextView(context);

        addTextView(progress_text);
        addTextView(delta_time_text);
    }

    private void addTextView(TextView textView) {
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(20);
        int paddingTop = ScaleUtil.dip2px(getContext(), 4);
        textView.setPadding(0, paddingTop, 0, paddingTop);

        addView(textView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public View getProgressView() {
        return this;
    }

    @Override
    public void startChangeProgress(long currentPosition, long duration) {
        this.currentPosition = currentPosition;
        this.duration = duration;

        setTimeDesc(currentPosition);
        changeProgress(0);

        setVisibility(View.VISIBLE);
    }

    @Override
    public void changeProgress(long deltaSeconds) {
        if (deltaSeconds < -currentPosition / 1000) {
            deltaSeconds = (int) -currentPosition / 1000;
        }
        if (deltaSeconds > (duration - currentPosition) / 1000) {
            deltaSeconds = (int) (duration - currentPosition) / 1000;
        }
        setTimeDesc(currentPosition + deltaSeconds * 1000);
        String desc = deltaSeconds >= 0 ? "+" : "-";
        delta_time_text.setText(desc + Math.abs(deltaSeconds) + "秒");
    }

    @Override
    public void stopChangeProgress() {
        setVisibility(View.GONE);
    }

    private void setTimeDesc(long position) {
        if (position > duration) {
            position = duration;
        }
        if (position < 0) {
            position = 0;
        }
        String curDesc = TimeFormatUtil.generateTime(position);
        String durationDesc = TimeFormatUtil.generateTime(duration);
        progress_text.setText(curDesc + "/" + durationDesc);
    }

}
