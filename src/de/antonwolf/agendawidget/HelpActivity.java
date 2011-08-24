package de.antonwolf.agendawidget;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

public class HelpActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.help);
	}

	public void onThoughtClick(View v) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.putExtra(Intent.EXTRA_EMAIL, new String[] { "support@antonwolf.de" });
		i.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources()
				.getString(R.string.app_name));
		i.setType("text/plain");
		startActivity(Intent.createChooser(i,
				getResources().getText(R.string.help_button_thought)));
	}

	public void onDonateClick(View v) {
		Intent i = new Intent(
				Intent.ACTION_VIEW,
				Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=9V9XVRRTAHCWJ"));
		startActivity(i);
	}

	public void onSuggestClick(View v) {
		Intent i = new Intent(Intent.ACTION_VIEW,
				Uri.parse("http://code.google.com/p/antons-apps/issues/list"));
		startActivity(i);
	}

	public void onRateClick(View v) {
		Intent i = new Intent(Intent.ACTION_VIEW,
				Uri.parse("market://search?q=pname:de.antonwolf.agendawidget"));
		startActivity(i);
	}
}
