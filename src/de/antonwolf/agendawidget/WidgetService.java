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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;

public final class WidgetService extends IntentService {
	private static final String TAG = "AgendaWidget";
	private static final String THEAD_NAME = "WidgetServiceThead";

	private static Pattern[] birthdayPatterns;

	private final static String CURSOR_FORMAT = "content://com.android.calendar/instances/when/%1$s/%2$s";
	private final static long SEARCH_DURATION = 2 * DateUtils.YEAR_IN_MILLIS;
	private final static String CURSOR_SORT = "begin ASC, end DESC, title ASC";
	private final static String[] CURSOR_PROJECTION = new String[] { "title",
			"color", "eventLocation", "allDay", "startDay", "endDay", "end",
			"hasAlarm", "calendar_id", "begin" };
	public final static int COL_TITLE = 0;
	public final static int COL_COLOR = 1;
	public final static int COL_LOCATION = 2;
	public final static int COL_ALL_DAY = 3;
	public final static int COL_START_DAY = 4;
	public final static int COL_END_DAY = 5;
	public final static int COL_END_MILLIS = 6;
	public final static int COL_HAS_ALARM = 7;
	public final static int COL_CALENDAR = 8;
	public final static int COL_START_MILLIS = 9;

	private static long todayStart;
	private static int today;

	private final static Pattern IS_EMPTY_PATTERN = Pattern.compile("^\\s*$");

	public WidgetService() {
		super(THEAD_NAME);
	}

	@Override
	protected synchronized void onHandleIntent(final Intent intent) {
		Log.d(TAG, "Handling " + intent);

		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(this);
		final int widgetId = Integer.parseInt(intent.getData().getHost());
		final AppWidgetProviderInfo appWidgetProviderInfo = appWidgetManager
				.getAppWidgetInfo(widgetId);
		if (null == appWidgetProviderInfo) {
			Log.d(TAG, "Invalid widget ID!");
			return;
		}
		final WidgetInfo info = new WidgetInfo(widgetId, this);

		final Style style = new Style(info, widgetId, this);

		Cursor cursor = null;
		final Time now = new Time();
		now.setToNow();
		today = Time.getJulianDay(System.currentTimeMillis(), now.gmtoff);
		now.second = 0;
		now.minute = 0;
		now.hour = 0;
		todayStart = now.normalize(false);
		now.monthDay++;
		long nextUpdate = now.normalize(false);

		try {
			final long start = todayStart - 1000 * 60 * 60 * 24;
			final long end = start + SEARCH_DURATION;
			final String uriString = String.format(CURSOR_FORMAT, start, end);
			cursor = getContentResolver().query(Uri.parse(uriString),
					CURSOR_PROJECTION, null, null, CURSOR_SORT);

			while (true) {
				if (style.isFull())
					break; // widget is full

				Event event = null;
				while (event == null && !cursor.isAfterLast())
					event = readEvent(cursor, info);
				if (event == null)
					break; // no further events

				style.addEvent(event);
				if (!event.allDay && event.endMillis < nextUpdate)
					nextUpdate = event.endMillis;
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}

		appWidgetManager.updateAppWidget(widgetId, style.render());

		// schedule next update
		PendingIntent pending = PendingIntent.getService(this, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pending);
		alarmManager.set(AlarmManager.RTC, nextUpdate + 1000, pending);
	}

	private Event readEvent(final Cursor cursor, final WidgetInfo info) {
		if (!cursor.moveToNext())
			return null; // no next item
		if (!info.calendars.get(cursor.getInt(COL_CALENDAR)).enabled)
			return null; // Calendar is disabled

		final Event event = new Event();

		if (1 == cursor.getInt(COL_ALL_DAY))
			event.allDay = true;

		event.startDay = cursor.getInt(COL_START_DAY);
		event.startMillis = cursor.getLong(COL_START_MILLIS);
		event.endDay = cursor.getInt(COL_END_DAY);
		event.endMillis = cursor.getLong(COL_END_MILLIS);
		
		if ((event.allDay && event.endDay < today)
				|| (!event.allDay && event.endMillis <= System
						.currentTimeMillis()))
			return null; // Skip events in the past

		event.title = cursor.getString(COL_TITLE);
		if (event.title == null)
			event.title = "";

		if (event.allDay && !info.birthdays.equals(WidgetInfo.BIRTHDAY_NORMAL))
			for (Pattern pattern : getBirthdayPatterns()) {
				Matcher matcher = pattern.matcher(event.title);
				if (!matcher.find())
					continue;
				event.title = matcher.group(1);
				event.isBirthday = true;
				break;
			}

		// Skip birthday events if necessary
		if (event.isBirthday && info.birthdays.equals(WidgetInfo.BIRTHDAY_HIDE))
			return null;

		event.location = cursor.getString(COL_LOCATION);
		if (event.location != null
				&& IS_EMPTY_PATTERN.matcher(event.location).find())
			event.location = null;

		event.color = cursor.getInt(COL_COLOR);
		event.hasAlarm = cursor.getInt(COL_HAS_ALARM) == 1;
		return event;
	}

	private synchronized Pattern[] getBirthdayPatterns() {
		if (birthdayPatterns == null) {
			String[] strings = getResources().getStringArray(
					R.array.birthday_patterns);
			birthdayPatterns = new Pattern[strings.length];
			for (int i = 0; i < strings.length; i++) {
				birthdayPatterns[i] = Pattern.compile(strings[i]);
			}
		}
		return birthdayPatterns;
	}
}
