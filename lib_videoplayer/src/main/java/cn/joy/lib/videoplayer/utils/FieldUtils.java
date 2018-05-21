package cn.joy.lib.videoplayer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.ViewGroup;
import android.view.Window;

/**
 * Author: Joy
 * Date:   2018/5/16
 */

public class FieldUtils {

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
}
