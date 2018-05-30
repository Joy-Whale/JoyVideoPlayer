package cn.joy.lib.videoplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ContextThemeWrapper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

/**
 * Author: Joy
 * Date:   2018/5/16
 */

public class CommonUtils {

	public static boolean isEmpty(CharSequence s) {
		return s == null || TextUtils.isEmpty(s.toString().replace(" ", "").replace("\n", ""));
	}

	public static String parseDuration(long time) {
		if (time == 0)
			return "00:00";
		int minute = (int) (time / 60000);
		int second = (int) (time % 60000 / 1000);
		return (minute < 10 ? "0" + minute : minute) + ":" + (second < 10 ? "0" + second : second);
	}

	/**
	 * Get activity from context object
	 * @param context something
	 * @return object of Activity or null if it is not Activity
	 */
	public static Activity scanForActivity(Context context) {
		if (context == null)
			return null;

		if (context instanceof Activity) {
			return (Activity) context;
		} else if (context instanceof ContextWrapper) {
			return scanForActivity(((ContextWrapper) context).getBaseContext());
		}

		return null;
	}

	public static ViewGroup getWindowParentView(Context context) {
		return (ViewGroup) scanForActivity(context).findViewById(Window.ID_ANDROID_CONTENT);
	}

	public static void hideSupportActionBar(Context context, boolean actionBar, boolean statusBar) {
		if (actionBar) {
			AppCompatActivity appCompatActivity = getAppCompActivity(context);
			if (appCompatActivity != null) {
				ActionBar ab = appCompatActivity.getSupportActionBar();
				if (ab != null) {
					ab.setShowHideAnimationEnabled(false);
					ab.hide();
				}
			}
		}
		if (statusBar) {
			if (context instanceof FragmentActivity) {
				FragmentActivity fragmentActivity = (FragmentActivity) context;
				fragmentActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			} else {
				getAppCompActivity(context).getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
		}
	}

	public static void showSupportActionBar(Context context, boolean actionBar, boolean statusBar) {
		if (actionBar) {
			AppCompatActivity appCompatActivity = getAppCompActivity(context);
			if (appCompatActivity != null) {
				ActionBar ab = appCompatActivity.getSupportActionBar();
				if (ab != null) {
					ab.setShowHideAnimationEnabled(false);
					ab.show();
				}
			}
		}

		if (statusBar) {
			if (context instanceof FragmentActivity) {
				FragmentActivity fragmentActivity = (FragmentActivity) context;
				fragmentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			} else {
				getAppCompActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
		}
	}

	/**
	 * Get AppCompatActivity from context
	 * @param context context
	 * @return AppCompatActivity if it's not null
	 */
	public static AppCompatActivity getAppCompActivity(Context context) {
		if (context == null)
			return null;
		if (context instanceof AppCompatActivity) {
			return (AppCompatActivity) context;
		} else if (context instanceof ContextThemeWrapper) {
			return getAppCompActivity(((ContextThemeWrapper) context).getBaseContext());
		}
		return null;
	}

	public static DisplayMetrics getDisplayMetrics(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
		return dm;
	}

	/**
	 * 获取屏幕宽度
	 * @param context context
	 */
	public static int getScreenWidth(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
		return dm.widthPixels;
	}

	/**
	 * 获取屏幕高度
	 * @param context context
	 */
	public static int getScreenHeight(Context context) {
		DisplayMetrics dm = new DisplayMetrics();
		((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(dm);
		return dm.heightPixels;
	}

	/**
	 * 检测是否为wifi状态
	 */
	public static boolean isWifiConnected(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifiNetworkInfo.isConnected();
	}
}
