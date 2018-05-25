package cn.joy.lib.videoplayer.ijk;

/**
 * Author: Joy
 * Date:   2018/5/24
 * IJKPlayer相关设置类
 */

public class IJKSettings {

	private static IJKSettings mDefault;

	public static IJKSettings getDefault() {
		if (mDefault == null) {
			mDefault = new IJKSettings();
			mDefault.setPlayerType(PLAYER_IJK);
			mDefault.setUsingMediaCodec(true);
			mDefault.setRenderViewType(RENDER_VIEW_TEXTURE);
			mDefault.setRenderAspectRatio(RENDER_ASPECT_RATIO_FIT_PARENT);
		}
		return mDefault;
	}

	public static final int PLAYER_AUTO = 0;
	public static final int PLAYER_ANDROID = 1;
	public static final int PLAYER_IJK = 2;
	public static final int PLAYER_IJK_EXO = 3;

	public static final int RENDER_VIEW_SURFACE = 1;
	public static final int RENDER_VIEW_TEXTURE = 2;

	public static final int RENDER_ASPECT_RATIO_FIT_PARENT = 0;
	public static final int RENDER_ASPECT_RATIO_FILL_PARENT = 0;
	public static final int RENDER_ASPECT_RATIO_16_9_FIT_PARENT = 0;
	public static final int RENDER_ASPECT_RATIO_4_3_FIT_PARENT = 0;

	// 是否开启多媒体解码
	private boolean isUsingMediaCodec = false;

	private boolean isUsingMediaCodecAutoRotate = false;
	//
	private boolean isUsingMediaCodecHandleResolutionChange = false;

	private boolean isUsingOpenSLES = false;

	private String mPixelFormat;
	// 播放器类型
	private int mPlayerType;
	// 渲染器类型
	private int mRenderViewType;
	// 渲染器显示比例
	private int mRenderAspectRatio;

	boolean isUsingMediaCodec() {
		return isUsingMediaCodec;
	}

	public void setUsingMediaCodec(boolean usingMediaCodec) {
		isUsingMediaCodec = usingMediaCodec;
	}

	boolean isUsingMediaCodecHandleResolutionChange() {
		return isUsingMediaCodecHandleResolutionChange;
	}

	public void setUsingMediaCodecHandleResolutionChange(boolean usingMediaCodecHandleResolutionChange) {
		isUsingMediaCodecHandleResolutionChange = usingMediaCodecHandleResolutionChange;
	}

	boolean isUsingOpenSLES() {
		return isUsingOpenSLES;
	}

	public void setUsingOpenSLES(boolean usingOpenSLES) {
		isUsingOpenSLES = usingOpenSLES;
	}

	String getPixelFormat() {
		return mPixelFormat;
	}

	public void setPixelFormat(String mPixelFormat) {
		this.mPixelFormat = mPixelFormat;
	}

	int getPlayerType() {
		return mPlayerType;
	}

	public void setPlayerType(int mPlayerType) {
		this.mPlayerType = mPlayerType;
	}

	int getRenderViewType() {
		return mRenderViewType;
	}

	public void setRenderViewType(int mRenderViewType) {
		this.mRenderViewType = mRenderViewType;
	}

	int getRenderAspectRatio() {
		return mRenderAspectRatio;
	}

	public void setRenderAspectRatio(int mRenderAspectRatio) {
		this.mRenderAspectRatio = mRenderAspectRatio;
	}

	public boolean isUsingMediaCodecAutoRotate() {
		return isUsingMediaCodecAutoRotate;
	}

	public void setUsingMediaCodecAutoRotate(boolean usingMediaCodecAutoRotate) {
		isUsingMediaCodecAutoRotate = usingMediaCodecAutoRotate;
	}
}
