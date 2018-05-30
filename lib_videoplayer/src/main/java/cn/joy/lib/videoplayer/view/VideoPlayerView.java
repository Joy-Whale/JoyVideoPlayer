package cn.joy.lib.videoplayer.view;

import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.util.Map;

import cn.joy.lib.videoplayer.IVideoPlayer;
import cn.joy.lib.videoplayer.R;
import cn.joy.lib.videoplayer.SimpleVideoPlayerListener;
import cn.joy.lib.videoplayer.dialog.VolumeBrightnessDialog;
import cn.joy.lib.videoplayer.utils.CommonUtils;

/**
 * Author: Joy
 * Date:   2018/5/15
 * 最基础的播放器实现类
 * 包含基础的控件(播放|暂停按钮、进度条、返回按钮、标题按钮、锁屏按钮、全屏按钮、当前进度时间、进度总时间)
 * 及功能(全屏、自动旋转、锁屏、声音控制、亮度调节、播放|暂停|重置|出错、检测是否为移动网络)
 */

public abstract class VideoPlayerView extends FrameLayout implements IVideoPlayer, IVideoPlayerView {

	private static final String TAG = "VP";
	private static final int ID_FULL_SCREEN = R.id.videoFullScreen;

	private ViewGroup mLayoutController;
	private ViewGroup mLayoutVideo;
	private View mLayoutLoading;
	private View mLayoutError;
	private View mBtnPlay;
	private View mBtnBack;
	private View mBtnLock;
	private View mBtnFullScreen;
	private TextView mTextTitle;
	private TextView mTextDuration;
	private TextView mTextCurrentPosition;
	private SeekBar mSeekBar;

	private IVideoPlayer mVideoPlayer;
	private VideoPlayViewSettings mSettings;

	// 当前播放器是否为横屏模式
	private boolean mIsLandMode = false;
	// 当前横屏是否已启用
	private boolean mIsLandEnabled = false;
	// 当前状态是否为锁屏状态
	private boolean mIsLock = false;
	// 是否正在改变音量或亮度
	private boolean mIsVolumeSeeking = false;
	private boolean mIsBrightnessSeeking = false;
	private float mVolumeDownPercent;
	// 当前屏幕方向
	private int mOrientation = Configuration.ORIENTATION_PORTRAIT;

	private ControlHandler mHandler;

	private OrientationHelper mOrientationHelper;

	private GestureDetectorCompat mDetector;

	private AudioManager mAudioManager;

	private VolumeBrightnessDialog mVolumeBrightnessDialog;

	public VideoPlayerView(@NonNull Context context) {
		this(context, null);
	}

	public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mDetector = new GestureDetectorCompat(getContext(), mGestureListener);
		mHandler = new ControlHandler(this);
		mOrientationHelper = new OrientationHelper(context);
		mOrientationHelper.setEnable(false);
		mAudioManager = (AudioManager) context.getApplicationContext().getSystemService(Service.AUDIO_SERVICE);
		// 加载父布局
		LayoutInflater.from(context).inflate(getLayoutResId(), this, true);
		onInitWidget();
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		//		mOrientationHelper.setEnable(isAutoRotateEnabled());
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mOrientationHelper.setEnable(false);
	}

	/**
	 * 屏幕发生改变时
	 * @param newConfig 新的config
	 */
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// 横屏不执行任何操作
		if (isLandMode())
			return;
		if (mOrientation == newConfig.orientation)
			return;
		mOrientation = newConfig.orientation;
		switch (mOrientation) {
			// 竖屏
			case Configuration.ORIENTATION_PORTRAIT:
				toggleFullScreen(false);
				break;
			// 横屏
			case Configuration.ORIENTATION_LANDSCAPE:
				toggleFullScreen(true);
				break;
		}
	}

	/**
	 * 多媒体音量监测
	 */
	protected AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
				case AudioManager.AUDIOFOCUS_GAIN:
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					break;
			}
		}
	};

	private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

		// 单击
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			return VideoPlayerView.this.onSingleTapConfirm(e);
		}

		// 双击
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			Log.d(TAG, "onDoubleTap");
			return VideoPlayerView.this.onDoubleTap(e);
		}
	};

	private float mDownX, mDownY;
	private float mLastX, mLastY;
	private boolean mSeeking = false;

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		float x = event.getX();
		float y = event.getY();
		Log.d(TAG, "onTouchEvent:" + event.getAction() + " x:" + event.getX() + " y:" + event.getY());

		mDetector.onTouchEvent(event);

		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDownX = mLastX = x;
				mDownY = mLastY = y;
				return true;
			case MotionEvent.ACTION_MOVE:
				float deltaX = x - mLastX;
				float deltaY = y - mLastY;
				Log.d(TAG, "onTouchEvent move : " + Math.abs(x - mDownX));
				// 垂直状态下滑动大于50
				if (Math.abs(y - mDownY) > getSettings().getVolumeBrightnessSeekBias()) {
					if (!mSeeking) {
						mSeeking = true;
						onTouchSeekingStart(mDownX, mDownY);
					}
					onTouchSeeking(deltaX, deltaY);
				}
				mLastX = x;
				mLastY = y;
				return true;
			case MotionEvent.ACTION_UP:
				if (mSeeking) {
					onTouchSeekEnd();
				}
				mSeeking = false;
				break;
		}
		// 播放中,先显示控件，然后2s后再关闭控件
		if (isPlayable()) {
			requestShowGeneralWidget();
			requestHideGeneralWidgetDelay();
		}
		return false;
	}

	/**
	 * 触摸移动事件开始
	 * @param downX 按下时的x坐标
	 * @param downY 按下时的y坐标
	 */
	protected void onTouchSeekingStart(float downX, float downY) {
		// 如果当前不是全屏状态需要判断全屏状态下是否支持拖动改变音量和亮度的功能
		// 如果当前为横屏则不支持
		// 如果当前不能播放也不支持
		if (!getSettings().isVolumeBrightnessSeekEnabled() || getSettings().isVolumeBrightnessSeekFullScreen() && !isLandMode() || mIsLock || !isPlayable()) {
			return;
		}
		if (downX < getWidth() / 2) {
			mIsBrightnessSeeking = !getSettings().isVolumeBrightnessSeekReversed();
			mIsVolumeSeeking = !mIsBrightnessSeeking;
			Log.d(TAG, "onTouchSeekingStartBrightness");
		} else {
			mIsBrightnessSeeking = getSettings().isVolumeBrightnessSeekReversed();
			mIsVolumeSeeking = !mIsBrightnessSeeking;
			Log.d(TAG, "onTouchSeekingStartVolume");
		}
		if (mIsVolumeSeeking) {
			mVolumeDownPercent = getCurrentAudioVolumePercent();
		}
	}

	/**
	 * 触摸移动事件
	 * @param deltaX x轴移动总距离
	 * @param deltaY y轴移动总距离
	 */
	protected void onTouchSeeking(float deltaX, float deltaY) {
		final int currHeight = getHeight();
		// 滑动百分比
		final float percent = -deltaY / (currHeight * 0.8f);
		// 亮度调节
		if (mIsBrightnessSeeking) {
			float curr = getCurrentWindowBrightnessPercent() + percent;
			curr = setWindowBrightnessPercent(curr);
			onSeekingBrightness(curr);
		}
		// 声音调节
		if (mIsVolumeSeeking) {
			mVolumeDownPercent += percent;
			if (mVolumeDownPercent > 1) {
				mVolumeDownPercent = 1;
			} else if (mVolumeDownPercent < 0) {
				mVolumeDownPercent = 0;
			}
			setAudioVolumePercent(mVolumeDownPercent);
			onSeekingVolume(mVolumeDownPercent);
		}
	}

	/**
	 * 触摸移动事件停止
	 */
	protected void onTouchSeekEnd() {
		if (mIsBrightnessSeeking) {
			mIsBrightnessSeeking = false;
			onSeekBrightnessEnd();
		}
		if (mIsVolumeSeeking) {
			mIsVolumeSeeking = false;
			onSeekVolumeEnd();
		}
	}

	protected void onSeekingVolume(float percentChanged) {
		Log.e(TAG, "onSeekingVolume " + percentChanged);
		if (mVolumeBrightnessDialog == null)
			mVolumeBrightnessDialog = new VolumeBrightnessDialog(getContext());
		if (!mVolumeBrightnessDialog.isShowing())
			mVolumeBrightnessDialog.show();
		mVolumeBrightnessDialog.volume().updatePercent(percentChanged);
	}

	protected void onSeekingBrightness(float percentChanged) {
		Log.e(TAG, "onSeekingBrightness " + percentChanged);
		if (mVolumeBrightnessDialog == null)
			mVolumeBrightnessDialog = new VolumeBrightnessDialog(getContext());
		if (!mVolumeBrightnessDialog.isShowing())
			mVolumeBrightnessDialog.show();
		mVolumeBrightnessDialog.brightness().updatePercent(percentChanged);
	}

	/**
	 * 拖动修改音量结束
	 */
	protected void onSeekVolumeEnd() {
		if (mVolumeBrightnessDialog != null)
			mVolumeBrightnessDialog.dismiss();
	}

	/**
	 * 拖动修改亮度结束
	 */
	protected void onSeekBrightnessEnd() {
		if (mVolumeBrightnessDialog != null)
			mVolumeBrightnessDialog.dismiss();
	}

	/**
	 * 单击
	 * @param e event
	 */
	protected boolean onSingleTapConfirm(MotionEvent e) {
		Log.d(TAG, "onSingleTapConfirm");
		return false;
	}

	/**
	 * 双击
	 * @param e event
	 */
	protected boolean onDoubleTap(MotionEvent e) {
		Log.d(TAG, "onDoubleTap");
		return false;
	}

	/**
	 * 播放按钮点击
	 */
	protected void onClickPlayButton() {
		if (isPlaying()) {
			pause();
		} else if (!isPlayable()) {
			if (!getSettings().isWifiNetworkPlayable() || CommonUtils.isWifiConnected(getContext()) || !CommonUtils.isWifiConnected(getContext()) && onNotWifiNetworkClickPlayButton())
				start();
		} else {
			start();
		}
	}

	/**
	 * 非wifi状态下点击播放按钮
	 * @return 是否可以直接播放
	 */
	protected boolean onNotWifiNetworkClickPlayButton() {
		Dialog dialog = new AlertDialog.Builder(getContext()) //
				.setTitle(R.string.video_wifi_network_title) //
				.setPositiveButton(R.string.video_wifi_network_position, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						start();
						dialog.dismiss();
					}
				}) //
				.setNegativeButton(R.string.video_wifi_network_negative, null) //
				.create();
		dialog.show();
		return false;
	}

	private float getCurrentAudioVolumePercent() {
		float value = (float) getCurrentAudioVolume() / getAudioVolumeMax();
		if (value < 0) {
			value = 0;
		} else if (value > 1) {
			value = 1;
		}
		return value;
	}

	private float setAudioVolumePercent(float percent) {
		if (percent < 0) {
			percent = 0;
		} else if (percent > 1) {
			percent = 1;
		}
		setAudioVolume((int) (getAudioVolumeMax() * percent));
		return percent;
	}

	/**
	 * 获取当前系统的音量
	 */
	private int getCurrentAudioVolume() {
		return mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	private int getAudioVolumeMax() {
		return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}

	private void setAudioVolume(int volume) {
		mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
	}

	private float getCurrentWindowBrightnessPercent() {
		return ((Activity) getContext()).getWindow().getAttributes().screenBrightness;
	}

	private float setWindowBrightnessPercent(float percent) {
		if (percent > 1) {
			percent = 1f;
		} else if (percent < 0) {
			percent = 0f;
		}
		WindowManager.LayoutParams params = ((Activity) getContext()).getWindow().getAttributes();
		params.screenBrightness = percent;
		((Activity) getContext()).getWindow().setAttributes(params);
		return percent;
	}

	/**
	 * 获取控件布局
	 * 可以在此自定义布局
	 */
	protected abstract int getLayoutResId();

	/**
	 * 初始化控件
	 */
	protected void onInitWidget() {
		mBtnBack = findViewById(R.id.btnBack);
		mBtnPlay = findViewById(R.id.btnPlay);
		mBtnLock = findViewById(R.id.btnLock);
		mBtnFullScreen = findViewById(R.id.btnFullScreen);
		mTextTitle = (TextView) findViewById(R.id.textTitle);
		mTextDuration = (TextView) findViewById(R.id.textDurationTime);
		mTextCurrentPosition = (TextView) findViewById(R.id.textCurrentTime);
		mSeekBar = (SeekBar) findViewById(R.id.playingSeekBar);
		mLayoutError = findViewById(R.id.layoutError);
		mLayoutLoading = findViewById(R.id.layoutLoading);
		mLayoutVideo = (ViewGroup) findViewById(R.id.layoutVideo);
		mLayoutController = (ViewGroup) findViewById(R.id.layoutController);

		onIdle();

		// 播放按钮事件
		changeWidgetClickListener(mBtnPlay, mOnClickListener);

		// 锁屏
		changeWidgetClickListener(mBtnLock, mOnClickListener);

		// 全屏
		changeWidgetClickListener(mBtnFullScreen, mOnClickListener);

		// 退出按钮
		changeWidgetClickListener(mBtnBack, mOnClickListener);

		// SeekBar
		if (mSeekBar != null)
			mSeekBar.setOnSeekBarChangeListener(mSeekBarListener);
	}

	private OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			// 播放按钮
			if (v.getId() == R.id.btnPlay) {
				onClickPlayButton();
			}
			// 全屏按钮
			else if (v.getId() == R.id.btnFullScreen) {
				if (isPlayable()) {
					mOrientationHelper.changeOrientationByUser(!isLandMode());
				}
			}
			// 锁屏按钮
			else if (v.getId() == R.id.btnLock) {
				toggleLockStatus();
			}
			// 返回按钮
			else if (v.getId() == R.id.btnBack) {
				// 如果当前为全屏状态，返回到正常状态
				if (isLandMode()) {
					mOrientationHelper.changeOrientationByUser(false);
				}
			}
			requestShowGeneralWidget();
		}
	};

	private SeekBar.OnSeekBarChangeListener mSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			//			if (fromUser) {
			//				mVideoPlayer.seekTo(progress * getDuration() / 100);
			//			}
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
	};

	/**
	 * 设置当前的标题
	 * @param text 标题
	 */
	public void setTitle(CharSequence text) {
		changeWidgetText(mTextTitle, text);
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
	public void setLooping(boolean loop) {
		mVideoPlayer.setLooping(loop);
	}

	@Override
	public void attachToVideoView(ViewGroup videoView) {
		mVideoPlayer.attachToVideoView(mLayoutVideo);
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
	public boolean isLooping() {
		return mVideoPlayer.isLooping();
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
		onChangeUiToIdle();
		updateOrientationHelper();
	}

	@Override
	public void onPreparing() {
		onChangeUiToPreparing();
		updateOrientationHelper();
	}

	@Override
	public void onPrepared() {
		onChangeUiToPrepared();
		updateOrientationHelper();
	}

	@Override
	public void onStart() {
		// 注册音频监听
		mAudioManager.requestAudioFocus(mAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
		onChangeUiToPlaying();
		requestHideGeneralWidgetDelay();
		updateOrientationHelper();
	}

	@Override
	public void onPause() {
		// 注册音频监听
		mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
		onChangeUiToPause();
		updateOrientationHelper();
	}

	@Override
	public void onProgress(long duration, long currentPosition, int percentage) {
		if (mSeekBar != null)
			mSeekBar.setProgress(percentage);

		// 设置时长
		if (mTextDuration != null)
			mTextDuration.setText(CommonUtils.parseDuration(duration));
		if (mTextCurrentPosition != null)
			mTextCurrentPosition.setText(CommonUtils.parseDuration(currentPosition));
	}

	@Override
	public void onBufferingUpdate(int progress) {
		mSeekBar.setSecondaryProgress(progress);
	}

	@Override
	public void onPlaybackCompleted() {
		// 取消音频监听
		mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
		onChangeUiToComplete();
		updateOrientationHelper();
	}

	@Override
	public boolean onError(int errCode) {
		// 取消音频监听
		mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
		// 设置播放按钮状态变为可播放状态
		if (mBtnPlay != null) {
			mBtnPlay.setSelected(false);
		}
		updateOrientationHelper();
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
	public void onVolumeChanged(int newVolume, boolean fromUser) {

	}

	@Override
	public void onBrightnessChanged(int newBrightness, boolean fromUser) {

	}

	public void toggleLockStatus() {
		mIsLock = !mIsLock;
		onLockStatusChanged(mIsLock);
	}

	/**
	 * 设置锁屏状态
	 * @param enable 是否启用
	 */
	public void setLockEnable(boolean enable) {
		this.mIsLock = enable;
		onLockStatusChanged(mIsLock);
	}

	/**
	 * 锁屏状态改变时
	 * @param lock 是否锁屏
	 */
	protected void onLockStatusChanged(boolean lock) {
		// 切换旋转感应器
		mOrientationHelper.setEnable(!lock);
		if (lock) {
			onChangeUiToLockScreen();
		} else {
			onChangeUiToUnLockScreen();
		}
	}

	/**
	 * 切换UI状态为初始状态
	 */
	protected void onChangeUiToIdle() {
		changeWidgetVisibility(mTextDuration, INVISIBLE);
		changeWidgetVisibility(mTextCurrentPosition, INVISIBLE);
		changeWidgetVisibility(mLayoutLoading, INVISIBLE);
		changeWidgetVisibility(mSeekBar, INVISIBLE);
		changeWidgetVisibility(mBtnBack, INVISIBLE);
		changeWidgetVisibility(mBtnFullScreen, INVISIBLE);
		changeWidgetVisibility(mBtnLock, INVISIBLE);
		changeWidgetVisibility(mBtnPlay, VISIBLE);
	}

	/**
	 * 切换UI状态为准备中
	 * 显示LoadingView,隐藏播放按钮
	 */
	protected void onChangeUiToPreparing() {
		onChangeUiToIdle();
		changeWidgetVisibility(mLayoutError, GONE);
		changeWidgetVisibility(mLayoutLoading, VISIBLE);
		changeWidgetVisibility(mBtnPlay, INVISIBLE);
	}

	/**
	 * 切换UI状态为准备完成
	 */
	protected void onChangeUiToPrepared() {
		changeWidgetVisibility(mLayoutError, GONE);
		changeWidgetVisibility(mLayoutLoading, GONE);
		changeWidgetVisibility(mTextDuration, VISIBLE);
		changeWidgetVisibility(mTextCurrentPosition, VISIBLE);
		changeWidgetVisibility(mLayoutLoading, VISIBLE);
		changeWidgetVisibility(mSeekBar, VISIBLE);
		changeWidgetVisibility(mBtnBack, VISIBLE);
		changeWidgetVisibility(mBtnLock, isLandMode() ? VISIBLE : INVISIBLE);
		changeWidgetVisibility(mBtnFullScreen, VISIBLE);
		changeWidgetVisibility(mBtnPlay, VISIBLE);
		changeWidgetSelected(mBtnPlay, false);
		// 设置时长
		changeWidgetText(mTextDuration, CommonUtils.parseDuration(getDuration()));
	}

	/**
	 * 切换UI状态为播放状态
	 */
	protected void onChangeUiToPlaying() {
		onChangeUiToPrepared();
		changeWidgetSelected(mBtnPlay, true);
	}

	/**
	 * 切换UI状态为暂停状态
	 */
	protected void onChangeUiToPause() {
		onChangeUiToPrepared();
		changeWidgetSelected(mBtnPlay, false);
	}

	/**
	 * 切换UI状态为播放完成状态
	 */
	protected void onChangeUiToComplete() {
		onChangeUiToPrepared();
		// 设置播放按钮状态变为可播放状态
		changeWidgetSelected(mBtnPlay, false);
	}

	/**
	 * 切换UI状态为锁屏状态
	 */
	protected void onChangeUiToLockScreen() {
		changeWidgetVisibility(mLayoutLoading, INVISIBLE);
		changeWidgetVisibility(mTextDuration, INVISIBLE);
		changeWidgetVisibility(mTextCurrentPosition, INVISIBLE);
		changeWidgetVisibility(mLayoutLoading, INVISIBLE);
		changeWidgetVisibility(mSeekBar, INVISIBLE);
		changeWidgetVisibility(mBtnBack, INVISIBLE);
		changeWidgetVisibility(mBtnFullScreen, INVISIBLE);
		changeWidgetVisibility(mBtnPlay, INVISIBLE);
		changeWidgetVisibility(mBtnLock, VISIBLE);
		changeWidgetSelected(mBtnLock, true);
	}

	/**
	 * 切换UI状态为不锁屏状态
	 */
	protected void onChangeUiToUnLockScreen() {
		updateUiWithPlayer();
		changeWidgetSelected(mBtnLock, false);
	}

	/**
	 * 切换UI状态为播放错误状态
	 */
	protected void onChangeUiToError() {
		changeWidgetVisibility(mLayoutError, VISIBLE);
	}

	/**
	 * 根据VideoPlay的状态来更新UI
	 */
	private void updateUiWithPlayer() {
		final IVideoPlayer player = getVideoPlayer();
		// 如果当前未设置播放器，则需要将播放器的界面初始化
		if (player == null) {
			onIdle();
			return;
		}
		if (player.isPlayable()) {
			onPrepared();
		}
		// 更新播放按钮状态
		changeWidgetSelected(mBtnPlay, player.isPlaying());
		// 更新进度条
		changeSeekBarProgress(getProgress(), getBufferingPercentage());
		// 更新返回按钮
		changeWidgetVisibility(mBtnBack, isLandMode() ? VISIBLE : GONE);
		// 更新锁屏按钮
		changeWidgetVisibility(mBtnLock, isLandMode() ? VISIBLE : INVISIBLE);
		changeWidgetSelected(mBtnLock, mIsLock);
		// 更新全屏按钮状态
		changeWidgetSelected(mBtnFullScreen, isLandMode());
		// 更新时长
		changeWidgetText(mTextDuration, CommonUtils.parseDuration(getDuration()));
		// 更新当前进度
		changeWidgetText(mTextCurrentPosition, CommonUtils.parseDuration(getCurrentPosition()));
	}

	/**
	 * 显示普通控件
	 */
	protected void showGeneralWidget() {
		changeWidgetVisibility(mLayoutController, VISIBLE);
	}

	/**
	 * 隐藏普通控件
	 */
	protected void hideGeneralWidget() {
		changeWidgetVisibility(mLayoutController, INVISIBLE);
	}

	private void requestShowGeneralWidget() {
		mHandler.showGeneralWidget();
	}

	private void requestHideGeneralWidget() {
		mHandler.hideGeneralWidget();
	}

	private void requestHideGeneralWidgetDelay() {
		mHandler.hideGeneralWidgetDelay(getAutoHideWidgetDuration());
	}

	protected void changeWidgetVisibility(View view, int visible) {
		if (view != null && view.getVisibility() != visible) {
			view.setVisibility(visible);
		}
	}

	protected void changeWidgetSelected(View view, boolean selected) {
		if (view != null && view.isSelected() != selected) {
			view.setSelected(selected);
		}
	}

	protected void changeWidgetText(TextView view, CharSequence text) {
		if (view != null) {
			view.setText(text);
		}
	}

	protected void changeSeekBarProgress(int progress, int bufferingProgress) {
		if (mSeekBar != null) {
			mSeekBar.setProgress(progress);
			mSeekBar.setSecondaryProgress(bufferingProgress);
		}
	}

	protected void changeWidgetClickListener(View view, OnClickListener listener) {
		if (view != null) {
			view.setOnClickListener(listener);
		}
	}

	/**
	 * 切换全屏/正常状态
	 * 切换的逻辑为，如果当前为普通模式，需要在ParentViewGroup（即Activity的ContentLayout层）中添加一个新的全屏播放器，
	 * 然后将VideoPlay切换PlayerView为该播放器；退出全屏时，需将全屏播放器从ParentViewGroup中移除，并将VideoPlay的PlayerView
	 * 切换回来。
	 * @param fullScreen 是否为全屏
	 */
	protected void toggleFullScreen(boolean fullScreen) {
		// 如果当前屏幕为横屏模式的播放器，则不做任何操作，该操作都由normal状态的播放器来处理
		if (isLandMode())
			return;
		mIsLandEnabled = fullScreen;
		final ViewGroup parent = getParentViewGroup();
		// 获取全屏控件
		View oldV = parent.findViewById(ID_FULL_SCREEN);
		// 在OnConfigurationChanged中不能执行删除操作,否则会报空指针,将该操作放在parent的事件队列中执行
		if (oldV != null) {
			parent.post(new Runnable() {
				@Override
				public void run() {
					parent.removeView(oldV);
				}
			});
		}
		// 删除当前VideoLayout控件中的所有view,包括RenderView
		if (mLayoutVideo.getChildCount() > 0) {
			mLayoutVideo.removeAllViews();
		}
		// todo 不需要暂停播放，直接转换屏幕
		//	// 暂停播放
		//	mVideoPlayer.pause();
		// 切换全屏
		if (fullScreen) {
			// 隐藏状态栏
			CommonUtils.hideSupportActionBar(getContext(), true, true);
			// 关闭当前的旋转感应器
			mOrientationHelper.setEnable(false);
			try {
				Constructor<VideoPlayerView> constructor = (Constructor<VideoPlayerView>) getClass().getConstructor(Context.class);
				VideoPlayerView playerView = constructor.newInstance(getContext());
				playerView.setId(ID_FULL_SCREEN);
				DisplayMetrics dm = CommonUtils.getDisplayMetrics(getContext());
				int w = dm.widthPixels;
				int h = dm.heightPixels;
				ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(h, w);
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
				parent.addView(playerView, lp);
				playerView.setLandMode(true);
				playerView.setVideoPlayer(mVideoPlayer);
				playerView.requestHideGeneralWidgetDelay();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// 显示状态栏
			CommonUtils.showSupportActionBar(getContext(), true, true);
			setVideoPlayer(mVideoPlayer);
			requestShowGeneralWidget();
		}
	}

	/**
	 * 根据当前播放状态更新当前的旋转感应器状态
	 */
	private void updateOrientationHelper() {
		// 如果当前未开启自动旋转
		if (!getSettings().isAutoRotateEnabled()) {
			mOrientationHelper.setEnable(false);
			return;
		}
		// 如果当前为锁屏模式
		if (mIsLock) {
			mOrientationHelper.setEnable(false);
			return;
		}
		if (mVideoPlayer == null) {
			mOrientationHelper.setEnable(false);
			return;
		}
		switch (mVideoPlayer.getState()) {
			case IVideoPlayer.STATE_IDLE:
				mOrientationHelper.setEnable(false);
				break;
			case IVideoPlayer.STATE_PREPARING:
				mOrientationHelper.setEnable(getSettings().canAutoRotate(VideoPlayViewSettings.FLAG_AUTO_ROTATE_PREPARING));
				break;
			case IVideoPlayer.STATE_PREPARED:
				mOrientationHelper.setEnable(getSettings().canAutoRotate(VideoPlayViewSettings.FLAG_AUTO_ROTATE_PREPARED));
				break;
			case IVideoPlayer.STATE_PLAYING:
				mOrientationHelper.setEnable(getSettings().canAutoRotate(VideoPlayViewSettings.FLAG_AUTO_ROTATE_PLAYING));
				break;
			case IVideoPlayer.STATE_PLAYBACK_COMPLETED:
				mOrientationHelper.setEnable(getSettings().canAutoRotate(VideoPlayViewSettings.FLAG_AUTO_ROTATE_COMPLETE));
				break;
			case IVideoPlayer.STATE_ERROR:
				mOrientationHelper.setEnable(getSettings().canAutoRotate(VideoPlayViewSettings.FLAG_AUTO_ROTATE_ERROR));
				break;
			default:
				mOrientationHelper.setEnable(getSettings().isAutoRotateEnabled());
				break;
		}
		Log.d("VP", "当前播放器状态 " + mVideoPlayer.getState() + " 当前自动旋转状态 " + mOrientationHelper.isEnable());
	}

	/**
	 * 设置播放控制器
	 * @param videoPlayer 播放控制器
	 */
	public void setVideoPlayer(IVideoPlayer videoPlayer) {
		this.mVideoPlayer = videoPlayer;
		if (mVideoPlayer != null) {
			mVideoPlayer.setListener(mVideoPlayerListener);
			mVideoPlayer.attachToVideoView(mLayoutVideo);
		}
		// 在播放控制器切换时，需要根据新播放器的状态更新当前的UI
		updateUiWithPlayer();
		// 更新旋转感应器状态
		updateOrientationHelper();
	}

	public IVideoPlayer getVideoPlayer() {
		return mVideoPlayer;
	}

	public int getAutoHideWidgetDuration() {
		return getSettings().getAutoHideWidgetDuration();
	}

	public void setAutoHideWidgetDuration(int duration) {
		getSettings().setAutoHideWidgetDuration(duration);
	}

	protected ViewGroup getParentViewGroup() {
		return CommonUtils.getWindowParentView(getContext());
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
		public void onPreparing() {
			VideoPlayerView.this.onPreparing();
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

	/**
	 * 当前是否为横屏模式
	 */
	public boolean isLandMode() {
		return mIsLandMode;
	}

	/**
	 * 设置当前是否为横屏模式
	 * @param isLandMode 是否为横屏模式
	 */
	public void setLandMode(boolean isLandMode) {
		this.mIsLandMode = isLandMode;
		// 当横屏模式切换后，需要告诉OrientationHelper当前为手动切换，并且赋值当前的屏幕模式给OrientationHelper;
		// mOrientationHelper.setOrientation(isLandMode ? OrientationHelper.SCREEN_ORIENTATION_LANDSCAPE : OrientationHelper.SCREEN_ORIENTATION_PORTRAIT);
		// mOrientationHelper.setOperationType((isLandMode ? OrientationHelper.OPERATION_TYPE_USER_LAND : OrientationHelper.SCREEN_ORIENTATION_PORTRAIT));
		// 横屏状态下不启用旋转感应，防止冲突
		//	if (mIsLandMode) {
		//		mOrientationHelper.setEnable(false);
		//	}
	}

	/**
	 * 是否自动旋转
	 */
	public boolean isAutoRotateEnabled() {
		return getSettings().isAutoRotateEnabled();
	}

	/**
	 * 设置是否支持自动旋转
	 */
	public void setAutoRotateEnable(boolean mIsAutoRotate) {
		getSettings().setAutoRotateEnable(mIsAutoRotate);
		// 如果当前为竖屏且横屏模式没有启用，则立即开启旋转感应器
		if (!mIsLandMode && !mIsLandEnabled) {
			mOrientationHelper.setEnable(mIsAutoRotate);
		}
	}

	public VideoPlayViewSettings getSettings() {
		return mSettings == null ? mSettings = VideoPlayViewSettings.getDefault() : mSettings;
	}

	public void setSettings(VideoPlayViewSettings mSettings) {
		this.mSettings = mSettings;
	}

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
