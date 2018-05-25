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
			// 只有 可播放|播放|暂停 状态下支持自动旋转
			mDefault.setAutoRotateFlag(FLAG_AUTO_ROTATE_PREPARED | FLAG_AUTO_ROTATE_PLAYING | FLAG_AUTO_ROTATE_PAUSE);
		}
		return mDefault;
	}

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

	// 自动旋转
	private boolean mAutoRotate = false;
	// 自动旋转flag
	private int mAutoRotateFlag;

	public boolean isAutoRotateEnabled() {
		return mAutoRotate;
	}

	public void setAutoRotateEnable(boolean mAutoRotate) {
		this.mAutoRotate = mAutoRotate;
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

	/**
	 * 是否支持该状态下的自动旋转
	 * @param flag flag
	 */
	public boolean canAutoRotate(int flag) {
		return mAutoRotate && (mAutoRotateFlag & flag) == flag;
	}
}
