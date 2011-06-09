package de.antonwolf.agendawidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

final class UpdateContext {
	private static final String TAG = "AgendaWidget";
	
	public final AppWidgetManager appWidgetManager;
	public final AppWidgetProviderInfo appWidgetProviderInfo;
	public final int widgetId;
	public final WidgetInfo info;
	
	private UpdateContext(AppWidgetManager appWidgetManager, int widgetId,
			AppWidgetProviderInfo appWidgetProviderInfo, WidgetInfo info) {
		this.appWidgetManager = appWidgetManager;
		this.widgetId = widgetId;
		this.appWidgetProviderInfo = appWidgetProviderInfo;
		this.info = info;
	}

	public static UpdateContext create(Context context, Intent intent) {
		final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		final int widgetId = Integer.parseInt(intent.getData().getHost());
		final AppWidgetProviderInfo appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(widgetId);
		if (null == appWidgetProviderInfo) {
			Log.d(TAG, "Invalid widget ID!");
			return null;
		}
		final WidgetInfo info = new WidgetInfo(widgetId, context);
		return new UpdateContext(appWidgetManager, widgetId, appWidgetProviderInfo, info);
	}
}
