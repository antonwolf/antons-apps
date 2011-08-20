/*
 * Copyright (C) 2011 by Anton Wolf
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *  
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.antonwolf.agendawidget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.RemoteViews;

public class Style {
	private final long yesterdayStart;
	private final long todayStart;
	private final long tomorrowStart;
	private final long dayAfterTomorrowStart;
	private final long oneWeekFromNow;
	private final long yearStart;
	private final long yearEnd;

	private final String formatYesterday;
	private final String formatToday;
	private final String formatTomorrow;
	private final String[] formatWeekdays;

	protected final String packageName;

	protected final PendingIntent onClick;

	protected final WidgetInfo info;

	protected final static String COLOR_DOT = "■\t";
	protected final static String COLOR_HIDDEN = "\t";
	protected final static String SEPARATOR_COMMA = ", ";

	protected final static long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
	protected final static ForegroundColorSpan DATETIME_COLOR_SPAN = new ForegroundColorSpan(
			0xffbbbbbb);
	protected final static ForegroundColorSpan FOREGROUND_COLOR_SPAN = new ForegroundColorSpan(
			0xffffffff);
	private boolean widgetFull = false;
	private final int maxLines;
	private final List<Event> birthdayEvents;
	private final List<Event> agendaEvents;

	public Style(final WidgetInfo info, final int widgetId, final Context c) {
		this.info = info;

		packageName = c.getPackageName();

		final Intent pickAction = new Intent("pick", Uri.parse("widget://"
				+ widgetId), c, PickActionActivity.class);
		pickAction.putExtra(PickActionActivity.EXTRA_WIDGET_ID, widgetId);
		onClick = PendingIntent.getActivity(c, 0, pickAction, 0);

		formatYesterday = c.getResources().getString(R.string.format_yesterday);
		formatTomorrow = c.getResources().getString(R.string.format_tomorrow);
		formatToday = c.getResources().getString(R.string.format_today);
		formatWeekdays = c.getResources().getStringArray(
				R.array.format_day_of_week);

		// compute time ranges
		final Time now = new Time();
		now.setToNow();
		final int julianDay = Time.getJulianDay(System.currentTimeMillis(),
				now.gmtoff);

		yearStart = now.setJulianDay(julianDay - now.yearDay);
		now.year++;
		yearEnd = now.toMillis(false);
		yesterdayStart = now.setJulianDay(julianDay - 1);
		todayStart = now.setJulianDay(julianDay);
		tomorrowStart = now.setJulianDay(julianDay + 1);
		dayAfterTomorrowStart = now.setJulianDay(julianDay + 2);
		oneWeekFromNow = now.setJulianDay(julianDay + 8);

		maxLines = Integer.parseInt(info.lines);
		birthdayEvents = new ArrayList<Event>(maxLines * 2);
		agendaEvents = new ArrayList<Event>(maxLines);
	}

	public RemoteViews render() {
		RemoteViews widget = new RemoteViews(packageName, R.layout.widget);
		widget.removeAllViews(R.id.widget);
		widget.setOnClickPendingIntent(R.id.widget, onClick);

		final int calendarColor = info.calendarColor ? View.VISIBLE : View.GONE;

		Iterator<Event> bdayIterator = birthdayEvents.iterator();
		while (bdayIterator.hasNext()) {
			final RemoteViews view = new RemoteViews(packageName,
					R.layout.birthdays);
			final Event left = bdayIterator.next();
			view.setTextViewText(R.id.left_time, formatTime(left));
			view.setTextViewText(R.id.left_title, formatTitle(left));

			if (bdayIterator.hasNext()) {
				final Event right = bdayIterator.next();
				view.setTextViewText(R.id.right_time, formatTime(right));
				view.setTextViewText(R.id.right_title, formatTitle(right));
			} else {
				view.setTextViewText(R.id.right_time, "");
				view.setTextViewText(R.id.right_title, "");
			}

			view.setViewVisibility(R.id.color, calendarColor);
			widget.addView(R.id.widget, view);
		}

		for (Event event : agendaEvents) {
			final RemoteViews view = new RemoteViews(packageName,
					R.layout.event);
			view.setTextViewText(R.id.time, formatTime(event));
			final CharSequence title = formatTitle(event);
			if (event.location != null) {
				final SpannableStringBuilder builder = new SpannableStringBuilder(
						title);
				final int from = builder.length();
				builder.append(SEPARATOR_COMMA);
				builder.append(event.location);
				builder.setSpan(DATETIME_COLOR_SPAN, from, builder.length(), 0);
				view.setTextViewText(R.id.text, builder);
			} else
				view.setTextViewText(R.id.text, title);

			int alarmFlag = event.hasAlarm ? View.VISIBLE : View.GONE;
			view.setViewVisibility(R.id.event_alarm, alarmFlag);
			view.setInt(R.id.color, "setColorFilter", event.color);
			view.setViewVisibility(R.id.color, calendarColor);
			widget.addView(R.id.widget, view);
		}

		final int opacityPercent = (int) (100 * info.opacity);
		widget.setInt(R.id.background, "setImageLevel", opacityPercent);

		return widget;
	}

	public void addEvent(Event e) {
		widgetFull = Math.ceil(birthdayEvents.size() / 2.0)
				+ agendaEvents.size() >= maxLines;
		if (e.isBirthday) {
			if (!birthdayEvents.contains(e))
				birthdayEvents.add(e);
		} else if (!widgetFull)
			agendaEvents.add(e);
	}

	public boolean isFull() {
		boolean evenBirthdayCount = birthdayEvents.size() % 2 == 0;
		return widgetFull && evenBirthdayCount;
	}

	private CharSequence formatTitle(Event event) {
		if (event.title == null) {
			final SpannableStringBuilder builder = new SpannableStringBuilder(
					event.title);
			builder.append('-');
			builder.setSpan(DATETIME_COLOR_SPAN, 0, builder.length(), 0);
			return builder;
		} else
			return event.title;
	}

	protected CharSequence formatTime(final Event event) {
		final SpannableStringBuilder builder = new SpannableStringBuilder();
		final boolean isStartToday = (todayStart <= event.startMillis && event.startMillis <= tomorrowStart);
		final boolean isEndToday = (todayStart <= event.endMillis && event.endMillis <= tomorrowStart);
		final boolean showStartDay = !isStartToday || !isEndToday
				|| event.allDay;
		if (showStartDay)
			builder.append(formatDay(event.startMillis, event.startDay));
		// all-Day events
		if (event.allDay) {
			if (event.startDay != event.endDay) {
				builder.append('-');
				builder.append(formatDay(event.endMillis, event.endDay));
			}
		}
		// events with no duration
		else if (!info.endTime || event.startMillis == event.endMillis) {
			if (showStartDay)
				builder.append(' ');
			builder.append(formatHour(event.startMillis, info));
		} else {
			// events with duration
			if (showStartDay)
				builder.append(' ');
			builder.append(formatHour(event.startMillis, info));
			builder.append('-');

			if (event.endMillis - event.startMillis > DAY_IN_MILLIS) {
				builder.append(formatDay(event.endMillis, event.endDay));
				builder.append(' ');
			}
			builder.append(formatHour(event.endMillis, info));
		}
		return builder;
	}

	protected CharSequence formatHour(final long time, final WidgetInfo info) {
		if (info.twentyfourHours)
			return String.format("%1$tk:%1$tM", time);

		SpannableStringBuilder builder = new SpannableStringBuilder();
		builder.append(String.format("%1$tl:%1$tM", time));
		final int start = builder.length();
		builder.append(String.format("%1$tp", time));
		final int end = builder.length();
		builder.setSpan(new RelativeSizeSpan(0.5f), start, end, 0);
		return builder;
	}

	private CharSequence formatDay(long time, int day) {
		final boolean tomorrowYesterday = info.tomorrowYesterday;
		final long specialStart = tomorrowYesterday ? yesterdayStart
				: todayStart;
		final long specialEnd = tomorrowYesterday ? dayAfterTomorrowStart
				: tomorrowStart;
		final boolean weekday = info.weekday;
		final long weekEnd = weekday ? oneWeekFromNow : tomorrowStart;

		if (specialStart <= time && time < specialEnd) {
			final String result;
			if (time < todayStart)
				result = formatYesterday;
			else if (time < tomorrowStart)
				result = formatToday;
			else
				result = formatTomorrow;

			final SpannableStringBuilder builder = new SpannableStringBuilder(
					result);
			builder.setSpan(new RelativeSizeSpan(0.75f), 0, builder.length(), 0);
			return builder;
		}
		if (todayStart <= time && time < weekEnd) // this week?
			return formatWeekdays[(day + 1) % 7];
		if (yearStart <= time && time < yearEnd) // this year?
			return String.format(info.dateFormat.shortFormat, time);
		return String.format(info.dateFormat.longFormat, time);
	}
}
