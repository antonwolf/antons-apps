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
 * THE SOFTWARE
 */
package de.antonwolf.agendawidget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RemoteViews;

public final class TwoLinesStyle extends Style {
	private int spaceLeft;
	private final List<Event> events;

	public TwoLinesStyle(WidgetInfo info, int widgetId, Context c) {
		super(info, widgetId, c);
		spaceLeft = Integer.parseInt(info.lines);
		events = new ArrayList<Event>(spaceLeft);
	}

	@Override
	public RemoteViews render() {
		RemoteViews widget = new RemoteViews(packageName,
				R.layout.widget_classic);
		widget.removeAllViews(R.id.widget);
		widget.setOnClickPendingIntent(R.id.widget, onClick);

		// TODO: Font size
		
		for (Event event : events) {
			final RemoteViews view;
			if (event.allDay) {
				view = new RemoteViews(packageName, R.layout.event_all_day);
				view.setTextViewText(R.id.text,
						formatEventText(event, false, info));
			} else {
				view = new RemoteViews(packageName, R.layout.event_two_lines);
				
				final SpannableStringBuilder firstLine = new SpannableStringBuilder();
				final int timeStartPos = firstLine.length();
				formatTime(firstLine, event, info);
				firstLine.append(' ');
				if (event.location != null) {
					firstLine.append(SEPARATOR_COMMA);
					firstLine.append(event.location);
				}
				firstLine.setSpan(new ForegroundColorSpan(DATETIME_COLOR),
						timeStartPos, firstLine.length(),
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				view.setTextViewText(R.id.first_line, firstLine);

				final SpannableStringBuilder secondLine = new SpannableStringBuilder();
				secondLine.append(event.title);
				secondLine.setSpan(new ForegroundColorSpan(0xffffffff), 0,
						secondLine.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				view.setTextViewText(R.id.second_line, secondLine);
			}
			
			int alarmFlag = event.hasAlarm ? View.VISIBLE : View.GONE;
			view.setViewVisibility(R.id.alarm, alarmFlag);
			
			Bitmap a = Bitmap.createBitmap(new int[] { event.color }, 1, 1,
					Config.ARGB_8888);
			view.setImageViewBitmap(R.id.color, a);
			widget.addView(R.id.widget, view);
		}

		final int opacityIndex = Integer.parseInt(info.opacity) / 20;
		final int background = BACKGROUNDS[opacityIndex];
		widget.setInt(R.id.widget, "setBackgroundResource", background);

		return widget;
	}

	@Override
	public void addEvent(Event e) {
		if (!events.contains(e)) {
			if (e.allDay && spaceLeft >= 1) {
				spaceLeft -= 1;
				events.add(e);
			} else if (spaceLeft >= 2) {
				spaceLeft -= 2;
				events.add(e);
			}
		}
	}

	@Override
	public boolean isFull() {
		return spaceLeft <= 0;
	}
}
