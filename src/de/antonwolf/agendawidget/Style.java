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

import java.util.Formatter;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.widget.RemoteViews;

public abstract class Style {
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

	protected final static int[] BACKGROUNDS = new int[] {
			R.drawable.background_0, R.drawable.background_20,
			R.drawable.background_40, R.drawable.background_60,
			R.drawable.background_80, R.drawable.background_100 };

	protected final static String COLOR_DOT = "■\t";
	protected final static String COLOR_HIDDEN = "\t";
	protected final static String SEPARATOR_COMMA = ", ";

	protected final static long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;
	protected final static int DATETIME_COLOR = 0xb8ffffff;

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
	}

	public abstract void addEvent(Event e);

	public abstract boolean isFull();

	public abstract RemoteViews render();

	protected void appendHour(final Formatter formatter,
			final SpannableStringBuilder builder, final long time,
			final WidgetInfo info) {
		if (info.twentyfourHours)
			formatter.format("%1$tk:%1$tM", time);
		else {
			formatter.format("%1$tl:%1$tM", time);
			final int start = builder.length();
			formatter.format("%1$tp", time);
			final int end = builder.length();
			builder.setSpan(new RelativeSizeSpan(0.5f), start, end, 0);
		}
	}

	protected void appendDay(final Formatter formatter,
			final SpannableStringBuilder builder, final long time,
			final Time day, final WidgetInfo info) {
		final boolean tomorrowYesterday = info.tomorrowYesterday;
		final long specialStart = tomorrowYesterday ? yesterdayStart
				: todayStart;
		final long specialEnd = tomorrowYesterday ? dayAfterTomorrowStart
				: tomorrowStart;
		final boolean weekday = info.weekday;
		final long weekEnd = weekday ? oneWeekFromNow : tomorrowStart;

		if (specialStart <= time && time < specialEnd) {
			final int from = builder.length();
			if (time < todayStart)
				builder.append(formatYesterday);
			else if (time < tomorrowStart)
				builder.append(formatToday);
			else
				builder.append(formatTomorrow);

			final RelativeSizeSpan smaller = new RelativeSizeSpan(0.5f);
			builder.setSpan(smaller, from, builder.length(), 0);
		} else if (todayStart <= time && time < weekEnd) // this week?
			builder.append(formatWeekdays[day.weekDay]);
		else if (yearStart <= time && time < yearEnd) // this year?
			formatter.format(info.dateFormat.shortFormat, time);
		else
			// not this year
			formatter.format(info.dateFormat.longFormat, time);
	}

	protected CharSequence formatEventText(final Event event,
			final boolean showColor, final WidgetInfo info) {
		if (event == null)
			return "";

		final SpannableStringBuilder builder = new SpannableStringBuilder();

		if (showColor)
			appendDot(event, builder);

		final int timeStartPos = builder.length();
		formatTime(builder, event, info);
		builder.append(' ');
		final int timeEndPos = builder.length();
		builder.setSpan(new ForegroundColorSpan(DATETIME_COLOR), timeStartPos,
				timeEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		builder.append(event.title);
		final int titleEndPos = builder.length();
		builder.setSpan(new ForegroundColorSpan(0xffffffff), timeEndPos,
				titleEndPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		if (event.location != null) {
			builder.append(SEPARATOR_COMMA);
			builder.append(event.location);
			builder.setSpan(new ForegroundColorSpan(DATETIME_COLOR),
					titleEndPos, builder.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		final float size = Integer.parseInt(info.size) / 100f;
		builder.setSpan(new RelativeSizeSpan(size), 0, builder.length(), 0);

		return builder;
	}

	protected void appendDot(final Event event,
			final SpannableStringBuilder builder) {
		if (event.isBirthday)
			builder.append(COLOR_HIDDEN);
		else {
			builder.append(COLOR_DOT);
			builder.setSpan(new ForegroundColorSpan(event.color), 0, 1,
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}
	}

	protected void formatTime(final SpannableStringBuilder builder,
			final Event event, final WidgetInfo info) {
		final Formatter formatter = new Formatter(builder);

		final boolean isStartToday = (todayStart <= event.startMillis && event.startMillis <= tomorrowStart);
		final boolean isEndToday = (todayStart <= event.endMillis && event.endMillis <= tomorrowStart);
		final boolean showStartDay = !isStartToday || !isEndToday
				|| event.allDay;

		// all-Day events
		if (event.allDay) {
			if (showStartDay)
				appendDay(formatter, builder, event.startMillis,
						event.startTime, info);

			if (event.startDay != event.endDay) {
				builder.append('-');
				appendDay(formatter, builder, event.endMillis, event.endTime,
						info);
			}
			return;
		}

		// events with no duration
		if (!info.endTime || event.startMillis == event.endMillis) {
			if (showStartDay) {
				appendDay(formatter, builder, event.startMillis,
						event.startTime, info);
				builder.append(' ');
			}
			appendHour(formatter, builder, event.startMillis, info);
			return;
		}

		// events with duration
		if (showStartDay) {
			appendDay(formatter, builder, event.startMillis, event.startTime,
					info);
			builder.append(' ');
		}
		appendHour(formatter, builder, event.startMillis, info);
		builder.append('-');

		if (event.endMillis - event.startMillis > DAY_IN_MILLIS) {
			appendDay(formatter, builder, event.endMillis, event.endTime, info);
			builder.append(' ');
		}
		appendHour(formatter, builder, event.endMillis, info);
	}
}
