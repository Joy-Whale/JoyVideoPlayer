package cn.joy.lib.videoplayer.view;

/**
 * Author: Joy
 * Date:   2018/5/17
 */

public interface IVideoPlayerView {

	interface VideoPlayerViewListener {

		/**
		 * 初始化状态
		 */
		void onIdle(IVideoPlayerView playerView);

		/**
		 * 准备中
		 */
		void onPreparing(IVideoPlayerView playerView);

		/**
		 * 准备好状态
		 */
		void onPrepared(IVideoPlayerView playerView);

		/**
		 * 播放状态
		 */
		void onStart(IVideoPlayerView playerView);

		/**
		 * 暂停状态
		 */
		void onPause(IVideoPlayerView playerView);

		/**
		 * 播放进度中
		 * @param duration        总时长
		 * @param currentPosition 当前播放时长
		 * @param percentage      播放进度
		 */
		void onProgress(IVideoPlayerView playerView, long duration, long currentPosition, int percentage);

		/**
		 * 缓存进度发生改变
		 * @param playerView playerView
		 * @param progress   进度
		 */
		void onBufferingUpdate(IVideoPlayerView playerView, int progress);

		/**
		 * 播放结束状态
		 */
		void onPlaybackCompleted(IVideoPlayerView playerView);

		/**
		 * 播放出错状态
		 */
		boolean onError(IVideoPlayerView playerView, int errCode);

		/**
		 * 在移动网络下请求播放
		 */
		boolean onMobileNetworkRequestPlaying(IVideoPlayerView playerView);

		/**
		 * 视频尺寸发生变化
		 */
		void onVideoSizeChanged(IVideoPlayerView playerView);

		/**
		 * 当前音量发生改变
		 * @param newVolume 新的音量
		 * @param fromUser  是否为用户操作
		 */
		void onVolumeChanged(IVideoPlayerView playerView, int newVolume, boolean fromUser);

		/**
		 * 当前亮度发生改变
		 * @param newBrightness 新的亮度
		 * @param fromUser      是否为用户操作
		 */
		void onBrightnessChanged(IVideoPlayerView playerView, int newBrightness, boolean fromUser);

		/**
		 * 触摸去改变音量
		 * @param playerView 播放器
		 */
		void onTouchToSeekVolume(IVideoPlayerView playerView);

		/**
		 * 触摸去改变亮度
		 * @param playerView 播放器
		 */
		void onTouchToSeekBrightness(IVideoPlayerView playerView);

		/**
		 * 触摸去改变音量完成
		 * @param playerView 播放器
		 */
		void onTouchToSeekVolumeFinish(IVideoPlayerView playerView);

		/**
		 * 触摸去改变亮度完成
		 * @param playerView 播放器
		 */
		void onTouchToSeekBrightnessFinish(IVideoPlayerView playerView);
	}

	/**
	 * 初始化状态
	 */
	void onIdle();

	/**
	 * 准备中
	 */
	void onPreparing();

	/**
	 * 准备好状态
	 */
	void onPrepared();

	/**
	 * 播放状态
	 */
	void onStart();

	/**
	 * 暂停状态
	 */
	void onPause();

	/**
	 * 播放进度中
	 * @param duration        总时长
	 * @param currentPosition 当前播放时长
	 * @param percentage      播放进度
	 */
	void onProgress(long duration, long currentPosition, int percentage);

	void onBufferingUpdate(int progress);

	/**
	 * 播放结束状态
	 */
	void onPlaybackCompleted();

	/**
	 * 播放出错状态
	 */
	boolean onError(int errCode);

	/**
	 * 在移动网络下请求播放
	 */
	boolean onMobileNetworkRequestPlaying();

	/**
	 * 视频尺寸发生变化
	 */
	void onVideoSizeChanged();

	/**
	 * 当前音量发生改变
	 * @param newVolume 新的音量
	 * @param fromUser  是否为用户操作
	 */
	void onVolumeChanged(int newVolume, boolean fromUser);

	/**
	 * 当前亮度发生改变
	 * @param newBrightness 新的亮度
	 * @param fromUser      是否为用户操作
	 */
	void onBrightnessChanged(int newBrightness, boolean fromUser);
}
