package cn.joy.lib.videoplayer.view;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Map;

import cn.joy.lib.videoplayer.FieldUtils;
import cn.joy.lib.videoplayer.IVideoPlayer;
import cn.joy.lib.videoplayer.R;
import cn.joy.lib.videoplayer.SimpleVideoPlayerListener;

/**
 * Author: Joy
 * Date:   2018/5/15
 */

public abstract class VideoPlayerView extends FrameLayout implements IVideoPlayer, IVideoPlayerView {

	private View mLayoutVideo;
	private View mLayoutController;
	private View mLayoutLoading;
	private View mBtnPlay;
	private View mBtnBack;
	private View mBtnFullScreen;
	private TextView mTextDuration;
	private TextView mTextCurrentPosition;
	private SeekBar mSeekBar;

	private IVideoPlayer mVideoPlayer;

	// 播放状态下自动隐藏控件的时间，默认2000毫秒
	private int mDurationToAutoHideWidgetOnPlaying = 2000;

	private ControlHandler mHandler;

	public VideoPlayerView(@NonNull Context context) {
		this(context, null);
	}

	public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mHandler = new ControlHandler(this);
		// 加载父布局
		LayoutInflater.from(context).inflate(getLayoutResId(), this, true);
		onInitWidget();
	}

	protected abstract int getLayoutResId();

	/**
	 * 初始化控件
	 */
	protected void onInitWidget() {
		mBtnBack = findViewById(R.id.btnBack);
		mBtnPlay = findViewById(R.id.btnPlay);
		mLayoutLoading = findViewById(R.id.layoutLoading);
		mLayoutVideo = findViewById(R.id.layoutVideo);
		mBtnFullScreen = findViewById(R.id.btnFullScreen);
		mTextDuration = (TextView) findViewById(R.id.textDurationTime);
		mTextCurrentPosition = (TextView) findViewById(R.id.textCurrentTime);
		mSeekBar = (SeekBar) findViewById(R.id.playingSeekBar);
		mLayoutController = findViewById(R.id.layoutController);
		mLayoutVideo = findViewById(R.id.layoutVideo);

		onIdle();

		// 播放按钮事件
		if (mBtnPlay != null)
			mBtnPlay.setOnClickListener(v -> {
				if (v.isSelected()) {
					pause();
				} else {
					start();
				}
				v.setSelected(!v.isSelected());
			});

		// 全屏
		if (mBtnFullScreen != null) {
			mBtnFullScreen.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {

				}
			});
		}

		if (mSeekBar != null) {
			mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						mVideoPlayer.seekTo(progress * getDuration() / 100);
					}
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					requestShowGeneralWidget();
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					mVideoPlayer.seekTo(seekBar.getProgress() * getDuration() / 100);
					requestHideGeneralWidgetDelay();
				}
			});
		}
	}

	@Override
	public void setDataSource(String dataSource, Map<String, String> headers) {
		mVideoPlayer.setDataSource(dataSource, headers);
	}


	@Override
	public void setDataSource(Uri uri, Map<String, String> headers) {
		mVideoPlayer.setDataSource(uri, headers);
	}

	@Override
	public void setListener(VideoPlayerListener listener) {
		mVideoPlayer.setListener(listener);
	}


	@Override
	public void setVolume(int volume) {
		mVideoPlayer.setVolume(volume);
	}

	@Override
	public void setSpeed(float speed) {
		mVideoPlayer.setSpeed(speed);
	}

	@Override
	public void attachToVideoView(View videoView) {

	}

	@Override
	public void prepare() {
		mVideoPlayer.prepare();
	}

	@Override
	public void start() {
		mVideoPlayer.start();
	}

	@Override
	public void pause() {
		mVideoPlayer.pause();
	}

	@Override
	public void restart() {
		mVideoPlayer.restart();
	}

	@Override
	public void stop() {
		mVideoPlayer.stop();
	}

	@Override
	public void release() {
		mVideoPlayer.release();
	}

	@Override
	public void reset() {
		mVideoPlayer.reset();
	}

	@Override
	public int getState() {
		return mVideoPlayer.getState();
	}

	@Override
	public long getDuration() {
		return mVideoPlayer.getDuration();
	}

	@Override
	public long getCurrentPosition() {
		return mVideoPlayer.getCurrentPosition();
	}

	@Override
	public int getBufferingPercentage() {
		return mVideoPlayer.getBufferingPercentage();
	}

	@Override
	public int getProgress() {
		return mVideoPlayer.getProgress();
	}

	@Override
	public void seekTo(long position) {
		mVideoPlayer.seekTo(position);
	}

	@Override
	public boolean isPlayable() {
		return mVideoPlayer.isPlayable();
	}

	@Override
	public boolean isPlaying() {
		return mVideoPlayer.isPlaying();
	}

	@Override
	public void onIdle() {
		changeWidgetVisibility(mTextDuration, false);
		changeWidgetVisibility(mTextCurrentPosition, false);
		changeWidgetVisibility(mLayoutLoading, false);
		changeWidgetVisibility(mSeekBar, false);
		changeWidgetVisibility(mBtnBack, false);
		changeWidgetVisibility(mBtnFullScreen, false);
		changeWidgetVisibility(mBtnPlay, true);
	}

	@Override
	public void onPrepared() {
		changeWidgetVisibility(mTextDuration, true);
		changeWidgetVisibility(mTextCurrentPosition, true);
		changeWidgetVisibility(mLayoutLoading, true);
		changeWidgetVisibility(mSeekBar, true);
		changeWidgetVisibility(mBtnBack, true);
		changeWidgetVisibility(mBtnFullScreen, true);
		changeWidgetVisibility(mBtnPlay, true);
		// 设置时长
		if (mTextDuration != null)
			mTextDuration.setText(FieldUtils.parseDuration(getDuration()));
	}

	@Override
	public void onStart() {
		requestHideGeneralWidgetDelay();
	}

	@Override
	public void onPause() {

	}

	@Override
	public void onProgress(long duration, long currentPosition, int percentage) {
		if (mSeekBar != null)
			mSeekBar.setProgress(percentage);

		// 设置时长
		if (mTextDuration != null)
			mTextDuration.setText(FieldUtils.parseDuration(duration));
		if (mTextCurrentPosition != null)
			mTextCurrentPosition.setText(FieldUtils.parseDuration(currentPosition));
	}

	@Override
	public void onBufferingUpdate(int progress) {
		mSeekBar.setSecondaryProgress(progress);
	}

	@Override
	public void onPlaybackCompleted() {
		// 设置播放按钮状态变为可播放状态
		if (mBtnPlay != null) {
			mBtnPlay.setSelected(false);
		}
	}

	@Override
	public boolean onError(int errCode) {
		// 设置播放按钮状态变为可播放状态
		if (mBtnPlay != null) {
			mBtnPlay.setSelected(false);
		}
		return false;
	}

	@Override
	public boolean onMobileNetworkRequestPlaying() {
		return false;
	}

	@Override
	public void onVideoSizeChanged() {

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// 播放中,先显示控件，然后2s后再关闭控件
		if (isPlayable()) {
			requestShowGeneralWidget();
			requestHideGeneralWidgetDelay();
		}
		return super.onTouchEvent(event);
	}

	/**
	 * 显示普通控件
	 */
	protected void showGeneralWidget() {
		changeWidgetVisibility(mLayoutController, true);
	}

	/**
	 * 隐藏普通控件
	 */
	protected void hideGeneralWidget() {
		changeWidgetVisibility(mLayoutController, false);
	}

	private void requestShowGeneralWidget() {
		mHandler.showGeneralWidget();
	}

	private void requestHideGeneralWidget() {
		mHandler.hideGeneralWidget();
	}

	private void requestHideGeneralWidgetDelay() {
		mHandler.hideGeneralWidgetDelay(mDurationToAutoHideWidgetOnPlaying);
	}

	protected void changeWidgetVisibility(View view, boolean visible) {
		if (view != null) {
			view.setVisibility(visible ? VISIBLE : GONE);
		}
	}

	/**
	 * 设置播放控制器
	 * @param videoPlayer 播放控制器
	 */
	public void setVideoPlayer(IVideoPlayer videoPlayer) {
		this.mVideoPlayer = videoPlayer;
		mVideoPlayer.setListener(mVideoPlayerListener);
		mVideoPlayer.attachToVideoView(mLayoutVideo);
	}

	public IVideoPlayer getVideoPlayer() {
		return mVideoPlayer;
	}

	public int getAutoHideWidgetOnPlayingDuration() {
		return mDurationToAutoHideWidgetOnPlaying;
	}

	public void setAutoHideWidgetOnPlayingDuration(int duration) {
		this.mDurationToAutoHideWidgetOnPlaying = duration;
	}

	// 播放控制器监听
	private SimpleVideoPlayerListener mVideoPlayerListener = new SimpleVideoPlayerListener() {

		@Override
		public void onStart() {
			VideoPlayerView.this.onStart();
		}

		@Override
		public void onPause() {
			VideoPlayerView.this.onPause();
		}

		@Override
		public void onProgress(long duration, long currentPosition, int percentage) {
			VideoPlayerView.this.onProgress(duration, currentPosition, percentage);
		}

		@Override
		public void onBufferingUpdate(int progress) {
			VideoPlayerView.this.onBufferingUpdate(progress);
		}

		@Override
		public void onPrepared() {
			VideoPlayerView.this.onPrepared();
		}

		@Override
		public void onPlaybackCompleted() {
			VideoPlayerView.this.onPlaybackCompleted();
		}
	};

	private static class ControlHandler extends Handler {

		// 播放状态下延迟去关闭widget
		private static final int MSG_HIDE_GENERAL_WIDGET = 0x1;
		private static final int MSG_SHOW_GENERAL_WIDGET = 0x2;

		private WeakReference<VideoPlayerView> mView;

		ControlHandler(VideoPlayerView view) {
			this.mView = new WeakReference<VideoPlayerView>(view);
		}

		@Override
		public void handleMessage(Message msg) {
			VideoPlayerView target = mView.get();
			if (target == null)
				return;
			switch (msg.what) {
				case MSG_HIDE_GENERAL_WIDGET:
					target.hideGeneralWidget();
					break;
				case MSG_SHOW_GENERAL_WIDGET:
					target.showGeneralWidget();
					break;
			}
		}

		/**
		 * 隐藏普通控件
		 */
		public void hideGeneralWidget() {
			hideGeneralWidgetDelay(0);
		}

		/**
		 * 延时隐藏普通控件
		 * @param delay 延时时间
		 */
		public void hideGeneralWidgetDelay(long delay) {
			removeMessages(MSG_HIDE_GENERAL_WIDGET);
			sendEmptyMessageDelayed(MSG_HIDE_GENERAL_WIDGET, delay);
		}

		/**
		 * 显示普通控件
		 */
		public void showGeneralWidget() {
			removeMessages(MSG_HIDE_GENERAL_WIDGET);
			sendEmptyMessage(MSG_SHOW_GENERAL_WIDGET);
		}
	}
}
