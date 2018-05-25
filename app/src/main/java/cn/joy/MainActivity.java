package cn.joy;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.joy.lib.videoplayer.ijk.IJKVideoPlayer;
import cn.joy.lib.videoplayer.view.GeneralVideoPlayerView;

/**
 * Author: Joy
 * Date:   2018/5/15
 */

public class MainActivity extends FragmentActivity {

	@BindView(R.id.player)
	GeneralVideoPlayerView mPlayer;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);

		mPlayer.setVideoPlayer(new IJKVideoPlayer(this));
		mPlayer.setDataSource("http://qukufile2.qianqian.com/data2/video/591488159/423925a97b5e5aad402d5b7aa85be198/591488159.mp4", null);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		//		switch (newConfig.orientation) {
		//			case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
		//				mPlayer.toggleFullScreen(false);
		//				break;
		//			case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
		//				mPlayer.toggleFullScreen(true);
		//				break;
		//		}
	}
}
