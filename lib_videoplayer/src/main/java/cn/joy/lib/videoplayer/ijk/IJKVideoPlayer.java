package cn.joy.lib.videoplayer.ijk;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Map;

import cn.joy.lib.videoplayer.FileMediaDataSource;
import cn.joy.lib.videoplayer.IVideoPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.IjkTimedText;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

/**
 * Author: Joy
 * Date:   2018/5/15
 */

public class IJKVideoPlayer implements IVideoPlayer {

	private static final String TAG = "IJKVideoPlayer";

	private static final int[] ALL_ASPECT_RATIO = {
			IRenderView.AR_ASPECT_FIT_PARENT, IRenderView.AR_ASPECT_FILL_PARENT, IRenderView.AR_ASPECT_WRAP_CONTENT,
			// IRenderView.AR_MATCH_PARENT,
			IRenderView.AR_16_9_FIT_PARENT, IRenderView.AR_4_3_FIT_PARENT
	};

	private Context mAppContext;
	private PlayHandler mHandler;
	private ViewGroup mVideoView;

	private Uri mUri;
	private Map<String, String> mDataHeaders;

	private IRenderView.ISurfaceHolder mSurfaceHolder = null;
	private IMediaPlayer mMediaPlayer;
	private IRenderView mRenderView;
	private VideoPlayerListener mListener;

	private int mCurrState = STATE_IDLE;
	private int mTargetState = STATE_IDLE;
	private int mCurrentBufferPercentage = 0;
	private int mSurfaceWidth, mSurfaceHeight;
	private int mSeekWhenPrepared = 0;
	private int mVideoWidth, mVideoHeight;
	private int mVideoSarNum, mVideoSarDen;
	private int mVideoRotationDegree;

	public IJKVideoPlayer(Context context) {
		mHandler = new PlayHandler(this);
		mAppContext = context.getApplicationContext();
	}

	private IMediaPlayer.OnPreparedListener mOnPreparedListener = new IMediaPlayer.OnPreparedListener() {
		@Override
		public void onPrepared(IMediaPlayer mp) {
			mCurrState = STATE_PREPARED;
			if (mListener != null) {
				mListener.onPrepared();
			}
			if (mTargetState == STATE_PLAYING) {
				start();
			}
			Log.d("jv", "onPrepared");
		}
	};

	private IMediaPlayer.OnInfoListener mOnInfoListener = new IMediaPlayer.OnInfoListener() {
		@Override
		public boolean onInfo(IMediaPlayer mp, int what, int extra) {
			Log.d("jv", "onInfo " + what + " " + extra);
			return false;
		}
	};

	private IMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new IMediaPlayer.OnSeekCompleteListener() {
		@Override
		public void onSeekComplete(IMediaPlayer mp) {
			if (mListener != null) {
				mListener.onSeekTo(mp.getCurrentPosition());
			}
		}
	};

	// 预加载进度更新
	private IMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
		@Override
		public void onBufferingUpdate(IMediaPlayer mp, int percent) {
			mCurrentBufferPercentage = percent;
			if (mListener != null) {
				mListener.onBufferingUpdate(percent);
			}
		}
	};

	/**
	 * 播放结束监听
	 */
	private IMediaPlayer.OnCompletionListener mOnCompletionListener = new IMediaPlayer.OnCompletionListener() {
		@Override
		public void onCompletion(IMediaPlayer mp) {
			Log.d("jv", "onCompletion ");
			mCurrState = STATE_PLAYBACK_COMPLETED;
			mTargetState = STATE_PLAYBACK_COMPLETED;
			// 关闭实时刷新
			mHandler.stop();
			// 关闭播放
			stop();
			if (mListener != null) {
				mListener.onPlaybackCompleted();
			}
		}
	};

	private IMediaPlayer.OnErrorListener mOnErrorListener = new IMediaPlayer.OnErrorListener() {
		@Override
		public boolean onError(IMediaPlayer mp, int what, int extra) {
			Log.d("jv", "onError " + what + " " + extra);
			mCurrState = STATE_ERROR;
			mTargetState = STATE_ERROR;
			return false;
		}
	};

	private IMediaPlayer.OnTimedTextListener mOnTimeTextListener = new IMediaPlayer.OnTimedTextListener() {
		@Override
		public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
			Log.d("jv", "onTimedText " + text);
		}
	};

	IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
		@Override
		public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
			if (holder.getRenderView() != mRenderView) {
				Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
				return;
			}

			mSurfaceWidth = w;
			mSurfaceHeight = h;
			boolean isValidState = (mTargetState == STATE_PLAYING);
			boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
			if (mMediaPlayer != null && isValidState && hasValidSize) {
				if (mSeekWhenPrepared != 0) {
					seekTo(mSeekWhenPrepared);
				}
				start();
			}
		}

		@Override
		public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
			if (holder.getRenderView() != mRenderView) {
				Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
				return;
			}

			mSurfaceHolder = holder;
			if (mMediaPlayer != null)
				bindSurfaceHolder(mMediaPlayer, holder);
			else if (mTargetState == STATE_PLAYING)
				openVideo();
		}

		@Override
		public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
			if (holder.getRenderView() != mRenderView) {
				Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
				return;
			}

			// after we return from this we can't use the surface any more
			mSurfaceHolder = null;
			// REMOVED: if (mMediaController != null) mMediaController.hide();
			// REMOVED: release(true);
			releaseWithoutStop();
		}
	};

	// REMOVED: mSHCallback
	private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
		if (mp == null)
			return;

		if (holder == null) {
			mp.setDisplay(null);
			return;
		}

		holder.bindToMediaPlayer(mp);
	}

	private void openVideo() {
		if (mUri == null) {
			throw new IllegalStateException("DataSource should be set before start play!");
		}
		if (mSurfaceHolder == null)
			return;
		mCurrState = STATE_PREPARING;
		if (mListener != null) {
			mListener.onPreparing();
		}
		if (mMediaPlayer != null) {
			mMediaPlayer.stop();
			mMediaPlayer.setDisplay(null);
			mMediaPlayer.release();
		}
		mMediaPlayer = createPlayer();
		// 使用多媒体音量
		mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		// 保持屏幕常亮
		mMediaPlayer.setScreenOnWhilePlaying(true);
		// 开启log
		mMediaPlayer.setLogEnabled(true);
		mMediaPlayer.setOnPreparedListener(mOnPreparedListener);
		mMediaPlayer.setOnInfoListener(mOnInfoListener);
		mMediaPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener);
		mMediaPlayer.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
		mMediaPlayer.setOnCompletionListener(mOnCompletionListener);
		mMediaPlayer.setOnErrorListener(mOnErrorListener);
		mMediaPlayer.setOnTimedTextListener(mOnTimeTextListener);
		// 设置播放信息
		try {
			String scheme = mUri.getScheme();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (TextUtils.isEmpty(scheme) || scheme.equalsIgnoreCase("file"))) {
				IMediaDataSource dataSource = new FileMediaDataSource(new File(mUri.toString()));
				mMediaPlayer.setDataSource(dataSource);
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mMediaPlayer.setDataSource(mAppContext, mUri, mDataHeaders);
			} else {
				mMediaPlayer.setDataSource(mUri.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
			if (mListener != null) {
				mListener.onError(ERROR_LOAD_FAILED);
			}
		}
		bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
		mMediaPlayer.prepareAsync();
		//		if (mSurfaceView != null) {
		//			mMediaPlayer.setDisplay(mSurfaceView.getHolder());
		//		}
	}


	private IMediaPlayer createPlayer() {
		return new IjkMediaPlayer();
	}

	/**
	 * 开启硬编码
	 */
	public void openCodec() {
		//		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
		//		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
		//		mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
	}

	/**
	 * 更新当前进度
	 */
	private void updateProgress() {
		if (!isPlayable() || mListener == null)
			return;
		long duration = getDuration();
		long currentPosition = getCurrentPosition();
		int progress = getProgress();
		mListener.onProgress(duration, currentPosition, progress);
		Log.d(TAG, "onProgress(duration:" + duration + ",position:" + currentPosition + ",progress:" + progress);
	}

	public void setRenderView(IRenderView renderView) {
		if (mRenderView != null) {
			if (mMediaPlayer != null)
				mMediaPlayer.setDisplay(null);

			View renderUIView = mRenderView.getView();
			mRenderView.removeRenderCallback(mSHCallback);
			mRenderView = null;
			removeView(renderUIView);
		}

		if (renderView == null)
			return;

		mRenderView = renderView;
		renderView.setAspectRatio(ALL_ASPECT_RATIO[0]);
		if (mVideoWidth > 0 && mVideoHeight > 0)
			renderView.setVideoSize(mVideoWidth, mVideoHeight);
		if (mVideoSarNum > 0 && mVideoSarDen > 0)
			renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
		View renderUIView = mRenderView.getView();
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		renderUIView.setLayoutParams(lp);
		addView(renderUIView);

		mRenderView.addRenderCallback(mSHCallback);
		mRenderView.setVideoRotation(mVideoRotationDegree);
	}

	private void addView(View view) {
		if (mVideoView != null)
			mVideoView.addView(view);
	}

	private void removeView(View view) {
		if (mVideoView != null) {
			mVideoView.removeView(view);
		}
	}

	@Override
	public void setDataSource(String path, Map<String, String> headers) {
		this.mUri = Uri.parse(path);
		this.mDataHeaders = headers;
	}

	@Override
	public void setDataSource(Uri uri, Map<String, String> headers) {
		this.mUri = uri;
		this.mDataHeaders = headers;
	}

	@Override
	public void setListener(VideoPlayerListener listener) {
		this.mListener = listener;
	}

	@Override
	public void setVolume(int volume) {
		// 设置音量。左声道，右声道
		mMediaPlayer.setVolume(volume, volume);
	}

	@Override
	public void setSpeed(float speed) {
		if (mMediaPlayer instanceof IjkMediaPlayer) {
			((IjkMediaPlayer) mMediaPlayer).setSpeed(speed);
		}
	}

	@Override
	public void attachToVideoView(ViewGroup videoView) {
		this.mVideoView = videoView;
		if (videoView == null)
			return;
		setRenderView(new TextureRenderView(videoView.getContext()));
	}

	@Override
	public void prepare() {

	}

	/**
	 * 播放逻辑，如果当前状态为初始化状态或播放结束状态，则重新初始化，若当前为暂停状态，则继续播放
	 */
	@Override
	public void start() {
		switch (mCurrState) {
			case STATE_IDLE:
			case STATE_PLAYBACK_COMPLETED:
			case STATE_ERROR:
				openVideo();
				break;
			case STATE_PREPARED:
			case STATE_PAUSE:
				mMediaPlayer.start();
				mCurrState = STATE_PLAYING;
				mHandler.start();
				if (mListener != null) {
					mListener.onStart();
				}
				break;
		}
		mTargetState = STATE_PLAYING;
	}

	/**
	 * 只有在播放状态才能暂停该任务
	 */
	@Override
	public void pause() {
		if (isPlaying()) {
			mMediaPlayer.pause();
			mCurrState = STATE_PAUSE;
			mHandler.stop();
			if (mListener != null) {
				mListener.onPause();
			}
		}
		mTargetState = STATE_PAUSE;
	}

	@Override
	public void restart() {
		mMediaPlayer.start();
		if (mListener != null) {
			mListener.onStart();
		}
		mCurrState = STATE_PLAYING;
	}

	@Override
	public void stop() {
		mMediaPlayer.stop();
		mMediaPlayer.release();
		mCurrState = STATE_IDLE;
		mHandler.stop();
		if (mListener != null) {
			mListener.onStop();
		}
	}

	@Override
	public void release() {
		mMediaPlayer.reset();
		mMediaPlayer.release();
		mMediaPlayer = null;
		mHandler.stop();
		mCurrState = STATE_IDLE;
		if (mListener != null) {
			mListener.onDestroy();
		}
	}

	@Override
	public void reset() {
		mMediaPlayer.reset();
	}

	@Override
	public int getState() {
		return mCurrState;
	}

	public void releaseWithoutStop() {
		if (mMediaPlayer != null)
			mMediaPlayer.setDisplay(null);
	}

	@Override
	public long getDuration() {
		if (isPlayable())
			return mMediaPlayer.getDuration();
		return -1;
	}

	@Override
	public long getCurrentPosition() {
		if (isPlayable())
			return mMediaPlayer.getCurrentPosition();
		return -1;
	}

	@Override
	public int getBufferingPercentage() {
		return mCurrentBufferPercentage;
	}

	@Override
	public int getProgress() {
		long duration = getDuration();
		return duration <= 0 ? 0 : (int) ((float) getCurrentPosition() / duration * 100);
	}

	@Override
	public void seekTo(long position) {
		if (isPlayable())
			mMediaPlayer.seekTo(position);
	}

	/**
	 * 是否正在播放
	 */
	@Override
	public boolean isPlaying() {
		return isPlayable() && mMediaPlayer.isPlaying();
	}

	/**
	 * 是否为可播放状态
	 */
	@Override
	public boolean isPlayable() {
		return mMediaPlayer != null && mCurrState != STATE_ERROR && mCurrState != STATE_IDLE && mCurrState != STATE_PREPARING;
	}

	protected IMediaPlayer getMediaPlayer() {
		return mMediaPlayer;
	}

	private static class PlayHandler extends Handler {

		static final int MSG_UPDATE_PROGRESS = 1;

		WeakReference<IJKVideoPlayer> mPlayer;

		PlayHandler(IJKVideoPlayer player) {
			mPlayer = new WeakReference<IJKVideoPlayer>(player);
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case MSG_UPDATE_PROGRESS:
					IJKVideoPlayer player = mPlayer.get();
					if (player == null) {
						stop();
						return;
					}
					player.updateProgress();
					sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 500);
					break;
			}
		}

		void start() {
			sendEmptyMessage(MSG_UPDATE_PROGRESS);
		}

		void stop() {
			removeMessages(MSG_UPDATE_PROGRESS);
		}
	}
}
