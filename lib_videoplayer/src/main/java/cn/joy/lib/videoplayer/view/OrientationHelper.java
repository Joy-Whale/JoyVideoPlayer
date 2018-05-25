package cn.joy.lib.videoplayer.view;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.OrientationEventListener;

import java.util.concurrent.TimeUnit;

import cn.joy.lib.videoplayer.utils.SimpleObserver;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Author: Joy
 * Date:   2018/5/23
 */

class OrientationHelper {

	private static final String TAG = "oh";

	static final int SCREEN_ORIENTATION_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
	static final int SCREEN_ORIENTATION_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
	static final int SCREEN_ORIENTATION_REVERSE_LANDSCAPE = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
	static final int SCREEN_ORIENTATION_REVERSE_PORTRAIT = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;

	// 自动旋转
	static final int OPERATION_TYPE_AUTO = -1;
	// 用户旋转
	static final int OPERATION_TYPE_USER = 0;
	// 用户旋转为横屏
	static final int OPERATION_TYPE_USER_LAND = 0x1;
	// 用户旋转为竖屏
	static final int OPERATION_TYPE_USER_PORTRAIT = 0x10;

	private Context context;
	private OrientationEventListener mOrientationEventListener;
	private boolean enable = false;
	// 当前屏幕的方向
	private int mScreenOrientation = SCREEN_ORIENTATION_PORTRAIT;
	// 旋转操作方式
	private int mOperationType = OPERATION_TYPE_AUTO;

	private Disposable mOperationDisposable;

	OrientationHelper(@NonNull Context context) {
		this.context = context;
		mOrientationEventListener = new OrientationEventListener(context) {
			@Override
			public void onOrientationChanged(int orientation) {
				OrientationHelper.this.onOrientationChanged(orientation);
			}
		};
		setEnable(true);
	}

	private void onOrientationChanged(int orientation) {
		if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
			return;
		// 系统是否启用自动旋转
		boolean autoRotateOn = (Settings.System.getInt(getContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
		// 系统没有开启自动旋转
		if (!autoRotateOn) {
			return;
		}
		int newOrientation = convert2Orientation(orientation);
		// 如果当前屏幕方向与新的方向相同,不做任何操作
		if (newOrientation == mScreenOrientation)
			return;
		mScreenOrientation = newOrientation;
		Log.e(TAG, "onOrientationChanged-> " + newOrientation);
		// 如果当前为用户操作，则不做旋转
		if (mOperationType >= OPERATION_TYPE_USER) {
			mOperationType = OPERATION_TYPE_AUTO;
		} else {
			mOperationType = OPERATION_TYPE_AUTO;
			// 延迟旋转屏幕
			changedOrientationDelay(mScreenOrientation);
		}
	}

	/**
	 * 延迟执行屏幕旋转操作，防止一段时间内执行多次不同操作
	 * @param newOrientation 新的值
	 */
	private void changedOrientationDelay(int newOrientation) {
		// 取消之前的操作
		if (mOperationDisposable != null) {
			mOperationDisposable.dispose();
			mOperationDisposable = null;
		}
		Observable.timer(100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(new SimpleObserver<Long>() {
			@Override
			public void onSubscribe(Disposable d) {
				mOperationDisposable = d;
			}

			@Override
			public void onNext(Long aLong) {
				// 接收到操作执行命令后执行屏幕旋转操作
				changeActivityOrientation(newOrientation);
			}
		});
	}

	/**
	 * 用户操作去旋转屏幕
	 * 某些时刻会发生该操作，比如用户点击了全屏或退出全屏按钮时
	 * @param land 是否切换为横屏模式，当前只支持横竖屏切换
	 */
	void changeOrientationByUser(boolean land) {
		mOperationType = OPERATION_TYPE_USER;
		// 切换为横屏
		if (land) {
			changeActivityOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			mScreenOrientation = SCREEN_ORIENTATION_LANDSCAPE;
			mOperationType |= OPERATION_TYPE_USER_LAND;
		}
		// 切换为竖屏
		else {
			changeActivityOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			mScreenOrientation = SCREEN_ORIENTATION_PORTRAIT;
			mOperationType |= OPERATION_TYPE_USER_PORTRAIT;
		}
	}

	/**
	 * 改变当前player所在的Activity的Orientation
	 * @param newOrientation 新的Orientation
	 */
	private void changeActivityOrientation(int newOrientation) {
		if (getContext() instanceof Activity) {
			((Activity) getContext()).setRequestedOrientation(newOrientation);
		}
	}

	/**
	 * 设置是否启动该功能
	 * @param enable 是否启用
	 */
	void setEnable(boolean enable) {
		this.enable = enable;
		if (mOrientationEventListener != null) {
			if (enable) {
				mOrientationEventListener.enable();
			} else {
				mOrientationEventListener.disable();
			}
		}
	}

	boolean isEnable() {
		return mOrientationEventListener != null && enable;
	}

	/**
	 * 设置屏幕方向
	 */
	void setOrientation(int orientation) {
		this.mScreenOrientation = orientation;
	}

	int getOrientation() {
		return mScreenOrientation;
	}

	void setOperationType(int operationType) {
		this.mOperationType = operationType;
	}

	/**
	 * 根据当前旋转角度获取屏幕方向
	 * @param rotation 旋转角度
	 * @return 屏幕方向
	 */
	private int convert2Orientation(int rotation) {
		int orientation;
		// 竖屏
		if (((rotation >= 0) && (rotation <= 45)) || (rotation > 315)) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		} else if ((rotation > 45) && (rotation <= 135)) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
		} else if ((rotation > 135) && (rotation <= 225)) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
		} else if ((rotation > 225) && (rotation <= 315)) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		} else {
			orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		}
		return orientation;
	}

	private Context getContext() {
		return context;
	}
}
