package cn.joy.lib.videoplayer.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.FloatRange;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import cn.joy.lib.videoplayer.R;

/**
 * Author: Joy
 * Date:   2018/5/30
 */

public class VolumeBrightnessDialog extends AlertDialog {

	private static final int TYPE_VOLUME = 1;
	private static final int TYPE_BRIGHTNESS = 2;

	private TextView mTextPercent;
	private ImageView mImageIcon;

	private int type = TYPE_VOLUME;
	private float mPercent;

	public VolumeBrightnessDialog(Context context) {
		super(context, R.style.JoyVideo_Dialog_Loading);
	}

	@Override
	public void show() {
		super.show();
		View parent = LayoutInflater.from(getContext()).inflate(R.layout.dialog_volume_brighness_seek_progress, null);
		setContentView(parent);
		mTextPercent = (TextView) parent.findViewById(R.id.textPercent);
		mImageIcon = (ImageView) parent.findViewById(R.id.imageIcon);
	}

	public VolumeBrightnessDialog brightness() {
		this.type = TYPE_BRIGHTNESS;
		return this;
	}

	public VolumeBrightnessDialog volume() {
		this.type = TYPE_VOLUME;
		return this;
	}

	@SuppressLint("SetTextI18n")
	public void updatePercent(@FloatRange(from = 0f, to = 1f) float percent) {
		this.mPercent = percent;
		if (mTextPercent == null || mImageIcon == null)
			return;
		mTextPercent.setText(((int) (mPercent * 100)) + "%");
		switch (type) {
			case TYPE_BRIGHTNESS:
				mImageIcon.setImageResource(R.drawable.ic_video_brightness);
				break;
			case TYPE_VOLUME:
				if (mPercent == 0)
					mImageIcon.setImageResource(R.drawable.ic_video_volume_off);
				else if (mPercent < 0.34f) {
					mImageIcon.setImageResource(R.drawable.ic_video_volume_1);
				} else if (mPercent < 0.66f)
					mImageIcon.setImageResource(R.drawable.ic_video_volume_2);
				else
					mImageIcon.setImageResource(R.drawable.ic_video_volume_3);
				break;
		}
	}
}
