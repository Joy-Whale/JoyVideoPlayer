package cn.joy.lib.videoplayer.view;

/**
 * Author: Joy
 * Date:   2018/5/24
 */

public class VideoPlayViewSettings {

	private static VideoPlayViewSettings mDefault;

	public static VideoPlayViewSettings getDefault() {
		if (mDefault == null) {
			mDefault = new VideoPlayViewSettings();
			mDefault.setAutoRotateEnable(true);
			// 默认只有 可播放|播放|暂停 状态下支持自动旋转
			mDefault.setAutoRotateFlag(FLAG_AUTO_ROTATE_PREPARED | FLAG_AUTO_ROTATE_PLAYING | FLAG_AUTO_ROTATE_PAUSE);
			// 默认只有横屏状态下可以滑动修改音量和亮度
			mDefault.setVolumeBrightnessSeekFlag(FLAG_VOLUME_BRIGHTNESS_SEEK_FULL_SCREEN);
			// 默认只有wifi状态下可以播放
			mDefault.setWifiNetworkPlayable(true);
		}
		return mDefault;
	}

	public static final int FLAG_AUTO_ROTATE_DISABLED = 0;
	// 自动旋转:准备中
	public static final int FLAG_AUTO_ROTATE_PREPARING = 0x1;
	// 自动旋转:准备完成
	public static final int FLAG_AUTO_ROTATE_PREPARED = 0x10;
	// 自动旋转:播放中
	public static final int FLAG_AUTO_ROTATE_PLAYING = 0x100;
	// 自动旋转:暂停中
	public static final int FLAG_AUTO_ROTATE_PAUSE = 0x1000;
	// 自动旋转:错误状态
	public static final int FLAG_AUTO_ROTATE_ERROR = 0x10000;
	// 自动旋转:播放结束
	public static final int FLAG_AUTO_ROTATE_COMPLETE = 0x100000;

	// 触摸滑动改变音量或亮度被禁止
	public static final int FLAG_VOLUME_BRIGHTNESS_SEEK_DISABLE = 0;
	// 触摸滑动改变音量或亮度只允许横屏模式，若不设置，所有模式都可以
	public static final int FLAG_VOLUME_BRIGHTNESS_SEEK_FULL_SCREEN = 0x1;
	// 触摸滑动改变音量或亮度方向置反，默认左边修改亮度，右边修改音量
	public static final int FLAG_VOLUME_BRIGHTNESS_SEEK_REVERSED = 0x10;

	// 自动旋转flag
	private int mAutoRotateFlag;
	// 音量和亮度调节
	private int mVolumeBrightnessSeekFlag;
	// 触摸拖动改变的偏差值, 即y轴的拖动距离大于该距离才会产生效果
	private int mSeekBias = 50;
	// 播放状态下自动隐藏控件的时间，默认2000毫秒
	private int mDurationToAutoHideWidgetOnPlaying = 2000;
	// 是否只有wifi状态下才可以直接播放
	private boolean mIsWifiNetworkPlayable;

	public void setAutoRotateEnable(boolean mAutoRotate) {
		if (mAutoRotate) {
			// 默认只有 可播放|播放|暂停 状态下支持自动旋转
			mAutoRotateFlag = FLAG_AUTO_ROTATE_PREPARED | FLAG_AUTO_ROTATE_PLAYING | FLAG_AUTO_ROTATE_PAUSE;
		} else {
			mAutoRotateFlag = FLAG_AUTO_ROTATE_DISABLED;
		}
	}

	public int getAutoRotateFlag() {
		return mAutoRotateFlag;
	}

	/**
	 * 设置自动旋转flag
	 */
	public void setAutoRotateFlag(int flag) {
		this.mAutoRotateFlag = flag;
	}

	/**
	 * 添加自动旋转flag
	 */
	public void addAutoRotateFlag(int flag) {
		mAutoRotateFlag |= flag;
	}

	public boolean isAutoRotateEnabled() {
		return mAutoRotateFlag > FLAG_AUTO_ROTATE_DISABLED;
	}

	/**
	 * 是否支持该状态下的自动旋转
	 * @param flag flag
	 */
	public boolean canAutoRotate(int flag) {
		return mAutoRotateFlag > FLAG_AUTO_ROTATE_DISABLED && (mAutoRotateFlag & flag) == flag;
	}

	/**
	 * 设置音量亮度调节flag
	 * @param flag see {@link VideoPlayViewSettings#FLAG_VOLUME_BRIGHTNESS_SEEK_DISABLE}、{@link VideoPlayViewSettings#FLAG_VOLUME_BRIGHTNESS_SEEK_FULL_SCREEN}
	 *             {@link VideoPlayViewSettings#FLAG_VOLUME_BRIGHTNESS_SEEK_REVERSED}
	 */
	public void setVolumeBrightnessSeekFlag(int flag) {
		this.mVolumeBrightnessSeekFlag = flag;
	}

	public void addVolumeBrightnessSeekFlag(int flag) {
		this.mVolumeBrightnessSeekFlag |= flag;
	}

	public int getVolumeBrightnessSeekFlag() {
		return mVolumeBrightnessSeekFlag;
	}

	/**
	 * 声音和亮度调节是否可用
	 */
	public boolean isVolumeBrightnessSeekEnabled() {
		return mVolumeBrightnessSeekFlag > FLAG_VOLUME_BRIGHTNESS_SEEK_DISABLE;
	}

	/**
	 * 是否置反声音和亮度调节,正常为左亮度|右声音
	 */
	public boolean isVolumeBrightnessSeekReversed() {
		return (mVolumeBrightnessSeekFlag & FLAG_VOLUME_BRIGHTNESS_SEEK_REVERSED) == FLAG_VOLUME_BRIGHTNESS_SEEK_REVERSED;
	}

	/**
	 * 是否只有在全屏模式下才能滑动调节亮度和声音
	 */
	public boolean isVolumeBrightnessSeekFullScreen() {
		return (mVolumeBrightnessSeekFlag & FLAG_VOLUME_BRIGHTNESS_SEEK_FULL_SCREEN) == FLAG_VOLUME_BRIGHTNESS_SEEK_FULL_SCREEN;
	}

	/**
	 * 触摸拖动改变的偏差值, 即y轴的拖动距离大于该距离才会产生效果
	 */
	public int getVolumeBrightnessSeekBias() {
		return mSeekBias;
	}

	public void setVolumeBrightnessSeekBias(int bias) {
		this.mSeekBias = bias;
	}

	public int getAutoHideWidgetDuration() {
		return mDurationToAutoHideWidgetOnPlaying;
	}

	public void setAutoHideWidgetDuration(int mDurationToAutoHideWidgetOnPlaying) {
		this.mDurationToAutoHideWidgetOnPlaying = mDurationToAutoHideWidgetOnPlaying;
	}

	public boolean isWifiNetworkPlayable() {
		return mIsWifiNetworkPlayable;
	}

	/**
	 * 设置只有wifi状态下才可以直接播放
	 */
	public void setWifiNetworkPlayable(boolean mIsWifiNetworkPlayable) {
		this.mIsWifiNetworkPlayable = mIsWifiNetworkPlayable;
	}
}
