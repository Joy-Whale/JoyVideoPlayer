package cn.joy.lib.videoplayer.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import java.util.Map;

import cn.joy.lib.videoplayer.R;

/**
 * Author: Joy
 * Date:   2018/5/15
 */

public class GeneralVideoPlayerView extends VideoPlayerView {

	public GeneralVideoPlayerView(@NonNull Context context) {
		super(context);
	}

	public GeneralVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
	}

	public GeneralVideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	protected int getLayoutResId() {
		return R.layout.widget_general_video_player;
	}


}
