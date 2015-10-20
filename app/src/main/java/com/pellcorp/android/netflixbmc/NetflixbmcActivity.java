package com.pellcorp.android.netflixbmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.pellcorp.android.netflixbmc.jsonrpc.JsonClient;
import com.pellcorp.android.netflixbmc.jsonrpc.JsonClientImpl;
import com.pellcorp.android.netflixbmc.jsonrpc.JsonClientResponse;
import com.pellcorp.android.netflixbmc.jsonrpc.JsonClientUtils;
import com.pellcorp.android.netflixbmc.jsonrpc.MovieIdSender;

public class NetflixbmcActivity extends Activity {
	private final Logger logger = LoggerFactory.getLogger(getClass().getName());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        logger.info("Starting onCreate");

        String url = getPreference(R.string.pref_host_url);
		if (url == null) {
			Dialog dialog = createSettingsMissingDialog(getString(R.string.missing_connection_details));
			dialog.show();
		} else {
			try {
				//final Intent intent = getIntent();

				JsonClient jsonClient = new JsonClientImpl(url);
				
				SendToXbmc task = new SendToXbmc(jsonClient);
				JsonClientResponse result = task.execute(url).get();
				
				if (result.isSuccess()) {
					Toast.makeText(this, R.string.successful_submission, Toast.LENGTH_SHORT).show();
					finish();
				} else if (result.isError()) {
					Dialog dialog = createErrorDialog(result.getErrorMessage());
					dialog.show();
				}
			} catch (Exception e) {
				String stackTrace = JsonClientUtils.getStackTrace(e);
				Dialog dialog = createErrorDialog(stackTrace);
				dialog.show();
			}
		}
	}

    private String getPreference(int resId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String value = preferences.getString(getString(resId), null);
        if (value != null && value.length() == 0) {
            return null;
        } else {
            return value;
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.settings:
			startActivity(new Intent(this, PrefsActivity.class));
			return true;
		}
		return false;
	}

	private AlertDialog createErrorDialog(String message) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error Message");
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
		return builder.create();
	}

	private AlertDialog createSettingsMissingDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		
		builder.setPositiveButton(R.string.settings_label,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								startActivity(new Intent(
										NetflixbmcActivity.this,
										PrefsActivity.class));
							}
						});
		return builder.create();
	}
	
	private class SendToXbmc extends AsyncTask<String, Integer, JsonClientResponse> {
		private MovieIdSender sender;
		
		public SendToXbmc(JsonClient jsonClient) {
			sender = new MovieIdSender(jsonClient);
		}
		
		@Override
		protected JsonClientResponse doInBackground(String... params) {
			String movieId = params[0];
			return sender.sendMovie(movieId);
		}
	}
}