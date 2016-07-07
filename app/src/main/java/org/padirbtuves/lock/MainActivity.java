package org.padirbtuves.lock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

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

import ioio.lib.*;
import ioio.lib.api.IOIO;
import ioio.lib.api.IOIO.VersionType;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

/**
 * This is the main activity of the HelloIOIO example application.
 *
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link IOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends IOIOActivity {

	private static final String SERVER_HOST = "mano.padirbtuves.lt";

	private Looper looper;

	private long lastOpen;

	private SharedPreferences preferences;
	
	private TextView idCodeText;
	private TextView validText;
	private ImageView lockView;
	
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
		
		idCodeText = (TextView) findViewById(R.id.idCode);
		validText = (TextView) findViewById(R.id.valid);
		lockView = (ImageView) findViewById(R.id.lockView);

	}

	@Override
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

	private AuthResult fallbackAuth(String id) {
		return new AuthResult(id, loadValid(id));
	}

	private AuthResult isValid(String id) {
		String jsonString;
		try {
			String requestUrl = String.format("http://%1$s/auth/nfc?id=%2$s;v=%3$s", SERVER_HOST, id, BuildConfig.VERSION_NAME);
			jsonString = getJSON(requestUrl);
		} catch (IOException e1) {
			return fallbackAuth(id);
		}

		try {
			JSONObject json = new JSONObject(jsonString);
			boolean valid = json.getBoolean("valid");
			String tillDateString = json.getString("till");

			Date result;
			try {
				result = df.parse(tillDateString);

			} catch (ParseException e) {
				e.printStackTrace();
				return fallbackAuth(id);
			}

			saveValid(id, result);
			return new AuthResult(id, result);
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
					AuthResult authResult = fallbackAuth(stringId);
					if (authResult.valid) {
						showAuthInfo(authResult);
						MainActivity.this.looper.open();
						
						// update value 
						isValid(stringId);
					} else {
						authResult = isValid(stringId);
						showAuthInfo(authResult);
						
						if (authResult.valid) {
							MainActivity.this.looper.open();
						}
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

	public void showAuthInfo(final AuthResult authResult) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				idCodeText.setText(authResult.tagId);
				validText.setText(authResult.toString());
			}

		});
	}
	public void showLockInfo(final boolean unlocked) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (unlocked) {
					lockView.setImageResource(R.drawable.lock_unlocked);
				} else {
					lockView.setImageResource(R.drawable.lock_locked);
				}
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