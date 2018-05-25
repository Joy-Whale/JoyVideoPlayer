package cn.joy.lib.videoplayer;

import android.net.Uri;
import android.support.annotation.IntRange;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

/**
 * Author: Joy
 * Date:   2018/5/14
 */

public interface IVideoPlayer {

	// 加载出错
	int ERROR_LOAD_FAILED = 1001;

	/** 当前状态值 */
	// 正在准备
	int STATE_IDLE = 0;
	// 准备中
	int STATE_PREPARING = 1;
	// 准备完成
	int STATE_PREPARED = 2;
	// 正在播放
	int STATE_PLAYING = 3;
	// 暂停中
	int STATE_PAUSE = 4;
	// 播放完成
	int STATE_PLAYBACK_COMPLETED = 5;
	// 播放出错
	int STATE_ERROR = -1;

	interface VideoPlayerListener {
		/**
		 * 正在加载
		 */
		void onPreparing();

		/**
		 * 加载完成
		 */
		void onPrepared();

		/**
		 * 开始播放
		 */
		void onStart();

		/**
		 * 停止播放
		 */
		void onStop();

		/**
		 * 暂停播放
		 */
		void onPause();

		/**
		 * 播放进度改变时
		 * @param duration        总进度
		 * @param currentPosition 当前进度
		 * @param percentage      当前进度百分比
		 */
		void onProgress(long duration, long currentPosition, @IntRange(from = 0, to = 100) int percentage);

		/**
		 * 预加载进度
		 * @param progress 当前进度
		 */
		void onBufferingUpdate(int progress);

		/**
		 * 状态改变时
		 */
		void onStatusChanged(int status);

		/**
		 *
		 */
		void onResume();

		/**
		 * 播放结束
		 */
		void onPlaybackCompleted();

		/**
		 * 销毁后
		 */
		void onDestroy();

		/**
		 * 拖动进度成功
		 */
		void onSeekTo(long progress);

		/**
		 * 播放出错
		 * @param error error
		 */
		void onError(int error);
	}

	/**
	 * 设置播放源
	 * @param path    path
	 * @param headers header
	 */
	void setDataSource(String path, Map<String, String> headers);

	/**
	 * 设置播放源
	 * @param uri     uri
	 * @param headers header
	 */
	void setDataSource(Uri uri, Map<String, String> headers);

	/**
	 * 设置监听
	 * @param listener listener
	 */
	void setListener(VideoPlayerListener listener);

	/**
	 * 设置音量
	 * @param volume 音量值
	 */
	void setVolume(int volume);

	/**
	 * 设置播放速度
	 * @param speed 播放速度
	 */
	void setSpeed(float speed);

	/**
	 * 设置是否循环
	 * @param loop 循环
	 */
	void setLooping(boolean loop);

	/**
	 * 加载VideoView
	 * @param videoView videoView
	 */
	void attachToVideoView(ViewGroup videoView);

	/**
	 * 预加载
	 */
	void prepare();

	/**
	 * 开始播放
	 */
	void start();


	/**
	 * 暂停播放
	 */
	void pause();

	/**
	 * 继续播放
	 */
	void restart();

	/**
	 * 停止播放
	 */
	void stop();

	/**
	 * 释放资源
	 */
	void release();

	/**
	 * 重新加载
	 */
	void reset();

	/**
	 * 获取当前的状态值
	 * @return 当前状态值 {STATE_IDLE, STATE_PREPARING, STATE_PREPARED}
	 */
	int getState();

	/**
	 * 获取时长，单位:秒
	 * @return 时长
	 */
	long getDuration();

	/**
	 * 获取当前播放进度
	 * @return 当前播放进度
	 */
	long getCurrentPosition();

	/**
	 * 获取缓存进度百分比
	 * @return 播放进度
	 */
	@IntRange(from = 0, to = 100)
	int getBufferingPercentage();

	/**
	 * 获取播放进度百分比
	 * @return 播放进度
	 */
	@IntRange(from = 0, to = 100)
	int getProgress();

	/**
	 * 拖动到。。。
	 * @param position 进度
	 */
	void seekTo(long position);

	/**
	 * 当前是否循环播放
	 */
	boolean isLooping();

	/**
	 * 是否正在播放
	 */
	boolean isPlaying();

	/**
	 * 是否为可播放状态
	 */
	boolean isPlayable();
}
