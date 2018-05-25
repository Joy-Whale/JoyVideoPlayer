package cn.joy.lib.videoplayer.ijk;

import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.joy.lib.videoplayer.FileMediaDataSource;
import cn.joy.lib.videoplayer.IVideoPlayer;
import cn.joy.lib.videoplayer.utils.SimpleObserver;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import tv.danmaku.ijk.media.exo.IjkExoMediaPlayer;
import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
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

	private Context mAppContext;
	private ViewGroup mVideoView;
	private IJKSettings mSettings;

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

	// 进度更新操作器
	private Disposable mProgressDisposable;

	public IJKVideoPlayer(Context context) {
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
			stopUpdateProgress();
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
			if (mListener != null) {
				mListener.onError(what);
			}
			return false;
		}
	};

	private IMediaPlayer.OnTimedTextListener mOnTimeTextListener = new IMediaPlayer.OnTimedTextListener() {
		@Override
		public void onTimedText(IMediaPlayer mp, IjkTimedText text) {
			Log.d("jv", "onTimedText " + text);
		}
	};

	private IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
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

	/**
	 * 开始播放
	 */
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
			} else {
				mMediaPlayer.setDataSource(mAppContext, mUri, mDataHeaders);
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

	/**
	 * 创建一个播放器
	 */
	private IMediaPlayer createPlayer() {
		final IJKSettings settings = getSettings();
		IMediaPlayer player;
		switch (settings.getPlayerType()) {
			case IJKSettings.PLAYER_ANDROID:
				player = new AndroidMediaPlayer();
				break;
			case IJKSettings.PLAYER_IJK_EXO:
				player = new IjkExoMediaPlayer(mAppContext);
				break;
			default:
				IjkMediaPlayer ijk = new IjkMediaPlayer();
				if (settings.isUsingMediaCodec()) {
					ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
					ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", settings.isUsingMediaCodecAutoRotate() ? "1" : "0");
					ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", settings.isUsingMediaCodecHandleResolutionChange() ? "1" : "0");
				} else {
					ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
				}
				ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", settings.isUsingOpenSLES() ? "1" : "0");
				String pixelFormat = settings.getPixelFormat();
				if (TextUtils.isEmpty(pixelFormat)) {
					ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
				} else {
					ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
				}
				ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
				ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);
				ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);
				ijk.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
				player = ijk;
				break;
		}
		player.setLogEnabled(false);
		return player;
	}

	/**
	 * 创建一个渲染器
	 */
	private void createRenderView() {
		if (mVideoView == null || mVideoView.getContext() == null)
			return;
		final Context context = mVideoView.getContext();
		final IJKSettings settings = getSettings();
		IRenderView renderView;
		switch (settings.getRenderViewType()) {
			case IJKSettings.RENDER_VIEW_SURFACE:
				renderView = new SurfaceRenderView(context);
				break;
			default:
				renderView = new TextureRenderView(context);
				break;
		}
		renderView.setAspectRatio(settings.getRenderAspectRatio());
		if (mVideoWidth > 0 && mVideoHeight > 0)
			renderView.setVideoSize(mVideoWidth, mVideoHeight);
		if (mVideoSarNum > 0 && mVideoSarDen > 0)
			renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

		setRenderView(renderView);
	}

	/**
	 * 设置渲染View
	 * @param renderView 渲染View
	 */
	private void setRenderView(IRenderView renderView) {
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
	public void setLooping(boolean loop) {
		mMediaPlayer.setLooping(loop);
	}

	@Override
	public void attachToVideoView(ViewGroup videoView) {
		// 如果当前的视频Layout不为空,需要先清除掉当前视频Layout中添加的RenderView,然后重新添加RenderView
		if (mVideoView != null) {
			mVideoView.removeAllViews();
			releaseWithoutStop();
		}
		this.mVideoView = videoView;
		if (videoView == null)
			return;
		// 设置一个默认的渲染器
		createRenderView();
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
				startUpdateProgress();
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
			if (mListener != null) {
				mListener.onPause();
			}
		}
		stopUpdateProgress();
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
		stopUpdateProgress();
		if (mListener != null) {
			mListener.onStop();
		}
	}

	@Override
	public void release() {
		mMediaPlayer.reset();
		mMediaPlayer.release();
		mMediaPlayer = null;
		mCurrState = STATE_IDLE;
		stopUpdateProgress();
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

	@Override
	public boolean isLooping() {
		return mMediaPlayer.isLooping();
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

	/**
	 * 开始更新进度
	 */
	private void startUpdateProgress() {
		if (mProgressDisposable != null) {
			mProgressDisposable.dispose();
		}
		Observable.interval(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new SimpleObserver<Long>() {

			@Override
			public void onSubscribe(Disposable d) {
				mProgressDisposable = d;
			}

			@Override
			public void onNext(Long o) {
				super.onNext(o);
				updateProgress();
			}
		});
	}

	/**
	 * 停止更新进度
	 */
	private void stopUpdateProgress() {
		if (mProgressDisposable != null) {
			mProgressDisposable.dispose();
		}
		mProgressDisposable = null;
	}

	protected IMediaPlayer getMediaPlayer() {
		return mMediaPlayer;
	}

	public IJKSettings getSettings() {
		return mSettings == null ? mSettings = IJKSettings.getDefault() : mSettings;
	}

	public void setSettings(IJKSettings mSettings) {
		this.mSettings = mSettings;
	}
}
