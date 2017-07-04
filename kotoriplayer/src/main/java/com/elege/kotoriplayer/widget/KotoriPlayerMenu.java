package com.elege.kotoriplayer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.elege.kotoriplayer.R;
import com.elege.kotoriplayer.util.ScaleUtil;
import com.elege.kotoriplayer.widget.impl.PlayerMenuImpl;

/**
 * 播放器菜单控件
 * Created by admin on 2016/11/23.
 */
public class KotoriPlayerMenu extends RelativeLayout implements PlayerMenuImpl {

    private View player_controler_btn;
    private TextView player_current_position;
    private SeekBar player_seekbar;
    private TextView player_duration;
    private Button player_fullscreen_btn;

    public KotoriPlayerMenu(Context context) {
        this(context, null);
    }

    public KotoriPlayerMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KotoriPlayerMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        View view = LayoutInflater.from(context).inflate(R.layout.kotoriplayer_controller_layout, null);
        player_controler_btn = view.findViewById(R.id.player_controler_btn);
        player_current_position = (TextView) view.findViewById(R.id.player_current_position);
        player_seekbar = (SeekBar) view.findViewById(R.id.player_seekbar);
        player_duration = (TextView) view.findViewById(R.id.player_duration);
        player_fullscreen_btn = (Button) view.findViewById(R.id.player_fullscreen_btn);

        int height = ScaleUtil.dip2px(context, 46);
        addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
    }

    @Override
    public View getPlayerMenuView() {
        return this;
    }

    @Override
    public View getPauseButton() {
        return player_controler_btn;
    }

    @Override
    public View getFullScreenButton() {
        return player_fullscreen_btn;
    }

    @Override
    public TextView getCurrentPositionTextView() {
        return player_current_position;
    }

    @Override
    public TextView getDurationTextView() {
        return player_duration;
    }

    @Override
    public SeekBar getSeekBar() {
        return player_seekbar;
    }
}
