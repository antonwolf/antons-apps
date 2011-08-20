package de.antonwolf.agendawidget.prefences;

import de.antonwolf.agendawidget.R;
import de.antonwolf.agendawidget.WidgetInfo;
import android.content.Context;
import android.preference.DialogPreference;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class FontSizePreference extends DialogPreference implements
		OnSeekBarChangeListener {
	private TextView text;
	private SeekBar bar;
	private final float defaultValue;

	public FontSizePreference(Context context, WidgetInfo info) {
		super(context, null);

		setDialogLayoutResource(R.layout.preference_font_size);
		setTitle(R.string.preference_font_size);
		setDialogTitle(R.string.preference_font_size);
		setKey(info.fontSizeKey);
		defaultValue = info.fontSizeDefault;
		setDefaultValue(defaultValue);
		final int percent = (int) (100 * info.fontSize);
		setSummary(getContext().getResources().getString(
				R.string.preference_font_size_summary, percent));
	}

	private float progressToValue(int progress) {
		return 0.5f + (progress / 20f);
	}

	private int valueToProgress(float value) {
		return (int) ((value - 0.5f) * 20f);
	}

	private void displayProgress(float value) {
		final int valuePercent = (int) (value * 100);
		final String textString = getContext().getResources().getString(
				R.string.preference_font_size_summary, valuePercent);
		SpannableStringBuilder builder = new SpannableStringBuilder(textString);
		builder.setSpan(new RelativeSizeSpan(value), 0, builder.length(), 0);
		text.setText(builder);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);

		float value = getPersistedFloat(defaultValue);
		text = (TextView) view.findViewById(R.id.value);
		bar = (SeekBar) view.findViewById(R.id.bar);
		bar.setMax(20);
		bar.setProgress(valueToProgress(value));
		bar.setOnSeekBarChangeListener(this);
		displayProgress(value);
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			final float value = progressToValue(bar.getProgress());
			persistFloat(value);
			final int percent = (int) (100 * value);
			setSummary(getContext().getResources().getString(
					R.string.preference_font_size_summary, percent));
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		displayProgress(progressToValue(progress));
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
	}

}
