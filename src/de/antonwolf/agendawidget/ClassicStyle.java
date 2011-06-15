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

import java.util.Iterator;

import android.content.Context;
import android.view.View;
import android.widget.RemoteViews;

public final class ClassicStyle extends Style {

	public ClassicStyle(WidgetInfo info, int widgetId, Context c) {
		super(info, widgetId, c);
	}

	public RemoteViews render() {
		RemoteViews widget = new RemoteViews(packageName,
				R.layout.widget_classic);
		widget.removeAllViews(R.id.widget);
		widget.setOnClickPendingIntent(R.id.widget, onClick);

		final boolean calendarColor = info.calendarColor;

		Iterator<Event> bdayIterator = birthdayEvents.iterator();
		while (bdayIterator.hasNext()) {
			final RemoteViews view = new RemoteViews(packageName,
					R.layout.birthdays);
			view.setTextViewText(R.id.birthday1_text,
					formatEventText(bdayIterator.next(), calendarColor, info));
			if (bdayIterator.hasNext())
				view.setTextViewText(R.id.birthday2_text,
						formatEventText(bdayIterator.next(), false, info));
			else
				view.setTextViewText(R.id.birthday2_text, "");
			widget.addView(R.id.widget, view);
		}

		for (Event event : agendaEvents) {
			final RemoteViews view = new RemoteViews(packageName,
					R.layout.event);
			view.setTextViewText(R.id.event_text,
					formatEventText(event, calendarColor, info));
			int alarmFlag = event.hasAlarm ? View.VISIBLE : View.GONE;
			view.setViewVisibility(R.id.event_alarm, alarmFlag);
			widget.addView(R.id.widget, view);
		}

		final int opacityIndex = Integer.parseInt(info.opacity) / 20;
		final int background = BACKGROUNDS[opacityIndex];
		widget.setInt(R.id.widget, "setBackgroundResource", background);

		return widget;
	}
}
