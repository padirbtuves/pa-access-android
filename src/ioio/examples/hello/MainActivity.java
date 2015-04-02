package ioio.examples.hello;

import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main activity of the HelloIOIO example application.
 *
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity {

	private Looper looper;

	private long lastOpen;

	private SharedPreferences preferences;
	
	// 2015-08-30T21:00:00.000Z
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
			Locale.ENGLISH);
	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		preferences = getPreferences(MODE_PRIVATE);
	}

	/**
	 * A method to create our IOIO thread.
	 *
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	// @Override
	protected IOIOLooper createIOIOLooper() {
		this.looper = new Looper(this);
		return this.looper;
	}

	public void showVersions(IOIO ioio, String title) {
		toast(String.format("%s\n" + "IOIOLib: %s\n"
				+ "Application firmware: %s\n" + "Bootloader firmware: %s\n"
				+ "Hardware: %s", title,
				ioio.getImplVersion(VersionType.IOIOLIB_VER),
				ioio.getImplVersion(VersionType.APP_FIRMWARE_VER),
				ioio.getImplVersion(VersionType.BOOTLOADER_VER),
				ioio.getImplVersion(VersionType.HARDWARE_VER)));
	}

	public void toast(final String message) {
		final Context context = this;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(context, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	public void enableUi(final boolean enable) {
		// This is slightly trickier than expected to support a multi-IOIO
		// use-case.

	}

	public String getJSON(String address) throws IOException {
		URL url = new URL(address);
		InputStream is = url.openStream();

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder result = new StringBuilder();
		String text;
		while ((text = reader.readLine()) != null) {
			result.append(text);
		}
		return result.toString();
	}

	private void saveValid(String id, Date till) {
		preferences.edit().putString(id, df.format(till)).apply();
	}

	private Date loadValid(String id) {
		String result = preferences.getString(id, null);
		if (result == null) {
			return null;
		}

		try {
			return df.parse(result);
		} catch (ParseException e) {
			return null;
		}
	}

	private boolean fallbackAuth(String id) {
		Date validDate = loadValid(id);
		if (validDate != null) {
			return new Date().before(validDate);
		} else {
			return false;
		}
	}

	private boolean isValid(String id) {
		String jsonString;
		try {
			jsonString = getJSON("http://paaccess-vincnetas.rhcloud.com/auth/nfc?id="
					+ id);
		} catch (IOException e1) {
			return fallbackAuth(id);
		}

		try {
			JSONObject json = new JSONObject(jsonString);
			boolean valid = json.getBoolean("valid");
			String tillDateString = json.getString("till");

			try {
				Date result = df.parse(tillDateString);
				saveValid(id, result);
			} catch (ParseException e) {
				e.printStackTrace();
			}

			return valid;
		} catch (JSONException e) {
			return fallbackAuth(id);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		final Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if (tag != null) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					String stringId = toDecimalValue(tag.getId());
					boolean valid = isValid(stringId);
					if (valid) {
						MainActivity.this.looper.open();
					}
				}
			}).start();
		}
	}

	private static String toDecimalValue(byte[] a) {
		StringBuilder builder = new StringBuilder("0x");

		for (int i = a.length; i > 0; i--) {
			builder.append(Integer.toHexString(a[i - 1] & 0xFF));
		}

		return Long.decode(builder.toString()).toString();
	}

	public void showInfo(final long lastOpen) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				TextView text = (TextView) findViewById(R.id.title);
				text.setText("Time " + lastOpen + " "
						+ System.currentTimeMillis() + " " + getLastOpen());
			}

		});
	}

	public long getLastOpen() {
		return lastOpen;
	}

	public void setLastOpen(long lastOpen) {
		this.lastOpen = lastOpen;
	}

}