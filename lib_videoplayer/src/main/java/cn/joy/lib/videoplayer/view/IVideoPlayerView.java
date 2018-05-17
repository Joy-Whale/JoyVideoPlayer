package cn.joy.lib.videoplayer.view;

/**
 * Author: Joy
 * Date:   2018/5/17
 */

public interface IVideoPlayerView {

	/**
	 * 初始化状态
	 */
	void onIdle();

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
}
