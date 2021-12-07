package com.testfairy.obb.sdk.http;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.testfairy.obb.sdk.Config;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public abstract class HttpJsonGetRequestTask extends AsyncTask<String, Void, JSONObject> {

	private URL url;

	protected final void prepare(String url, Map<String, String> queryParams) {
		try {
			Uri.Builder builder = Uri.parse(url).buildUpon();

			for (Map.Entry<String, String> e : queryParams.entrySet()) {
				builder = builder.appendQueryParameter(e.getKey(), e.getValue());
			}

			this.url = new URL(builder.build().toString());
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected final JSONObject doInBackground(String... params) {
		if (url == null) {
			throw new RuntimeException(new MalformedURLException("url cannot be null"));
		}

		try {
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();

			InputStream input = connection.getInputStream();
			StringBuilder textBuilder = new StringBuilder();

			try (Reader reader = new BufferedReader(new InputStreamReader(input, Charset.forName("UTF-8")))) {
				int c = 0;
				while ((c = reader.read()) != -1) {
					textBuilder.append((char) c);
				}
			}

			if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
				throw new IOException("Server responded with " + connection.getResponseCode());
			}

			return new JSONObject(textBuilder.toString());
		} catch (Throwable e) {
			Log.e(Config.LOG_TAG, e.getMessage(), e);
		}

		return null;
	}

	@Override
	protected abstract void onPostExecute(JSONObject result);
}