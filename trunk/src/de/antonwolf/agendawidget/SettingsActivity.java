package de.antonwolf.agendawidget;

import java.util.Map.Entry;

import de.antonwolf.agendawidget.WidgetInfo.CalendarPreferences;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity {
	public final static String EXTRA_WIDGET_ID = "widgetId";
	private static final String TAG = "AgendaWidget";
	private static final String[] BIRTHDAY_PREFERENCES = new String[] {
			WidgetInfo.BIRTHDAY_SPECIAL, WidgetInfo.BIRTHDAY_NORMAL,
			WidgetInfo.BIRTHDAY_HIDE };

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final int widgetId = getIntent().getIntExtra(EXTRA_WIDGET_ID, -1);
		Log.d(TAG, "SettingsActivity.onCreate(" + widgetId + ")");
		if (-1 == widgetId)
			return;
		final WidgetInfo info = new WidgetInfo(widgetId, this);

		final PreferenceScreen screen = getPreferenceManager()
				.createPreferenceScreen(this);
		setPreferenceScreen(screen);

		final PreferenceCategory display = new PreferenceCategory(this);
		display.setTitle(R.string.settings_display);
		screen.addPreference(display);

		final ListPreference lines = new ListPreference(this);
		lines.setTitle(R.string.settings_display_lines);
		lines.setKey(info.linesKey);
		lines.setEntries(R.array.settings_display_lines_entries);
		lines.setEntryValues(new String[] { "3", "4", "5", "6", "7", "8", "9",
				"10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
				"20", "21", "22", "23", "24", "25" });
		lines.setDefaultValue(info.linesDefault);
		final OnPreferenceChangeListener linesChanged = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference pref,
					final Object newValue) {
				pref.setSummary(getResources().getString(
						R.string.settings_display_lines_summary, newValue));
				return true;
			}
		};
		linesChanged.onPreferenceChange(lines, info.lines);
		lines.setOnPreferenceChangeListener(linesChanged);
		display.addPreference(lines);

		final ListPreference opacity = new ListPreference(this);
		opacity.setTitle(R.string.settings_display_opacity);
		opacity.setKey(info.opacityKey);
		opacity.setEntries(R.array.settings_display_opacity_entries);
		opacity.setEntryValues(new String[] { "0", "20", "40", "60", "80",
				"100" });
		opacity.setDefaultValue(info.opacityDefault);
		final OnPreferenceChangeListener opacityChanged = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference pref,
					final Object newValue) {
				pref.setSummary(getResources().getString(
						R.string.settings_display_opacity_summary, newValue));
				return true;
			}
		};
		opacityChanged.onPreferenceChange(opacity, info.opacity);
		opacity.setOnPreferenceChangeListener(opacityChanged);
		display.addPreference(opacity);

		final ListPreference birthdays = new ListPreference(this);
		birthdays.setTitle(R.string.settings_birthdays);
		birthdays.setKey(info.birthdaysKey);
		birthdays.setEntries(R.array.settings_birthdays_entries);
		birthdays.setEntryValues(BIRTHDAY_PREFERENCES);
		birthdays.setDefaultValue(info.birthdaysDefault);
		final OnPreferenceChangeListener birthdaysChanged = new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference pref,
					final Object newValue) {
				Log.d(TAG, newValue.toString());
				int i = 0;
				for (; i < BIRTHDAY_PREFERENCES.length; i++)
					if (BIRTHDAY_PREFERENCES[i].equals(newValue))
						break;
				String[] summaries = getResources().getStringArray(
						R.array.settings_birthdays_summaries);
				pref.setSummary(summaries[i]);
				return true;
			}
		};
		birthdaysChanged.onPreferenceChange(birthdays, info.birthdays);
		birthdays.setOnPreferenceChangeListener(birthdaysChanged);
		display.addPreference(birthdays);

		final CheckBoxPreference weekday = new CheckBoxPreference(this);
		weekday.setDefaultValue(info.weekday);
		weekday.setKey(info.weekdayKey);
		weekday.setTitle(R.string.settings_weekday);
		weekday.setSummaryOn(R.string.settings_weekday_yes);
		weekday.setSummaryOff(R.string.settings_weekday_no);
		display.addPreference(weekday);

		final CheckBoxPreference tomorrowYesterday = new CheckBoxPreference(
				this);
		tomorrowYesterday.setDefaultValue(info.tomorowYesterdayDefault);
		tomorrowYesterday.setKey(info.tomorowYesterdayKey);
		tomorrowYesterday.setTitle(R.string.settings_tommorow_yesterday);
		tomorrowYesterday
				.setSummaryOn(R.string.settings_tommorow_yesterday_yes);
		tomorrowYesterday
				.setSummaryOff(R.string.settings_tommorow_yesterday_no);
		display.addPreference(tomorrowYesterday);

		final CheckBoxPreference calendarColor = new CheckBoxPreference(this);
		calendarColor.setDefaultValue(info.calendarColorDefault);
		calendarColor.setKey(info.calendarColorKey);
		calendarColor.setTitle(R.string.settings_calendar_color);
		calendarColor.setSummaryOn(R.string.settings_calendar_color_show);
		calendarColor.setSummaryOff(R.string.settings_calendar_color_hide);
		display.addPreference(calendarColor);

		final PreferenceCategory calendars = new PreferenceCategory(this);
		calendars.setTitle(R.string.settings_calendars);
		screen.addPreference(calendars);

		for (final Entry<Integer, CalendarPreferences> cinfo : info.calendars
				.entrySet()) {
			final CheckBoxPreference calendar = new CheckBoxPreference(this);
			calendar.setDefaultValue(cinfo.getValue().enabledDefault);
			calendar.setKey(cinfo.getValue().key);

			final SpannableStringBuilder title = new SpannableStringBuilder(
					"■ ");
			title.setSpan(new ForegroundColorSpan(cinfo.getValue().color), 0,
					1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			title.append(cinfo.getValue().displayName);
			calendar.setTitle(title);

			calendar.setSummaryOn(getResources().getString(
					R.string.settings_calendars_show,
					cinfo.getValue().displayName));
			calendar.setSummaryOff(getResources().getString(
					R.string.settings_calendars_hide,
					cinfo.getValue().displayName));
			calendars.addPreference(calendar);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		int widgetId = getIntent().getIntExtra(EXTRA_WIDGET_ID, -1);
		Log.d(TAG, "SettingsActivity.onResume(" + widgetId + ")");
	}

	@Override
	protected void onPause() {
		super.onPause();

		int widgetId = getIntent().getIntExtra(EXTRA_WIDGET_ID, -1);
		Log.d(TAG, "SettingsActivity.onPause(" + widgetId + ")");
		if (-1 == widgetId)
			return;

		Intent intent = new Intent("update", Uri.parse("widget://" + widgetId),
				this, WidgetService.class);
		Log.d(TAG, "Sending " + intent);
		startService(intent);
	}
}