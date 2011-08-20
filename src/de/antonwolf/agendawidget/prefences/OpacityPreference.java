package de.antonwolf.agendawidget.prefences;

import java.text.NumberFormat;

import de.antonwolf.agendawidget.R;
import de.antonwolf.agendawidget.WidgetInfo;
import android.content.Context;
import android.preference.DialogPreference;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class OpacityPreference extends DialogPreference implements
		OnSeekBarChangeListener {
	private TextView text;
	private SeekBar bar;
	private static float step = 1f / (16f - 1f);

	public OpacityPreference(Context context, WidgetInfo info) {
		super(context, null);
		setDialogLayoutResource(R.layout.preference_opacity);
		setTitle(R.string.preference_opacity);
		setDialogTitle(R.string.preference_opacity);
		setKey(info.opacityKey);
		setDefaultValue(info.opacityDefault);
	}

	private void setLabelText(double value) {
		text.setText(getContext().getResources().getString(
				R.string.preference_opacity_dialog,
				NumberFormat.getPercentInstance().format(value)));
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		float value = getPersistedFloat(0.5f);
		text = (TextView) view.findViewById(R.id.value);
		setLabelText(value);
		bar = (SeekBar) view.findViewById(R.id.bar);
		bar.setMax((int) (1 / step));
		bar.setProgress((int) (value / step));
		bar.setOnSeekBarChangeListener(this);
		setDialogTitle("bind");
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		// TODO Auto-generated method stub
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			persistFloat(bar.getProgress() * step);
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		setLabelText(progress * step);
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
	}
}
