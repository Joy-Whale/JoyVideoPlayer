package cn.joy.lib.videoplayer;

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
}
