package com.testfairy.obb.sdk.http;

import static com.testfairy.obb.sdk.Config.LOG_TAG;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.testfairy.obb.sdk.Config;
import com.testfairy.obb.sdk.license.TFApkExpansionPolicy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetObbRequirementsRequestTask extends HttpJsonGetRequestTask {

	public interface OnResult {
		void onResult(TFApkExpansionPolicy.OBBData[] result);
		void onError();
	}

	private final Map<String, String> queryParams = new HashMap<>();
	private final OnResult onResult;

	public GetObbRequirementsRequestTask(Context context, String appToken, OnResult onResult) {
		this.onResult = onResult;

		queryParams.put("method", "testfairy.session.getObbRequirements");
		queryParams.put("appToken", appToken);
		queryParams.put("platform", "0");
		queryParams.put("versionName", getVersionName(context));
		queryParams.put("versionCode", getVersionCode(context));
		queryParams.put("packageName", context.getApplicationContext().getPackageName());

		prepare(Config.getAppServerEndpoint(), queryParams);
	}

	@Override
	protected void onPostExecute(JSONObject result) {
		Log.d(LOG_TAG, "Received: " + result.toString());
/*
{
  "status": "ok",
  "data": [
    {
      "downloadUrl": "https://testfairy.s3.amazonaws.com/releases/....",
      "packageName": "com.testfairy.obb.app",
      "filename": "main.1.com.testfairy.obb.app.obb",
      "filesize": 265019,
      "versionCode": "1",
      "type": "main",
      "uploadedAt": "2021-11-29 02:40:55 PST"
    },
    {
      "downloadUrl": "https://testfairy.s3.amazonaws.com/releases/......",
      "packageName": "com.testfairy.obb.app",
      "filename": "patch.1.com.testfairy.obb.app.obb",
      "filesize": 2265019,
      "versionCode": "1",
      "type": "patch",
      "uploadedAt": "2021-11-29 02:40:55 PST"
    }
  ]
}
* */
		if (onResult != null) {
			if (result == null) {
				onResult.onError();
				return;
			}

			String status = result.optString("status", "fail");
			if (!status.equals("ok")) {
				onResult.onError();
				return;
			}

			JSONArray data = result.optJSONArray("data");

			if (data == null) {
				onResult.onError();;
				return;
			}

			List<TFApkExpansionPolicy.OBBData> obbList = new ArrayList<>(data.length());
			for (int i = 0; i < data.length(); i++) {
				JSONObject jsonObject = data.optJSONObject(i);
				if (jsonObject != null) {
					try {
						String downloadUrl = jsonObject.getString("downloadUrl");
						String packageName = jsonObject.getString("packageName");
						String filename = jsonObject.getString("filename");
						int filesize = jsonObject.getInt("filesize");
						String versionCode = jsonObject.getString("versionCode");
						String type = jsonObject.getString("type");
						String uploadedAt = jsonObject.getString("uploadedAt");

						obbList.add(new TFApkExpansionPolicy.OBBData(downloadUrl, filename, filesize, type));
					} catch (JSONException e) {
						Log.e(LOG_TAG, "Cannot parse json", e);
						onResult.onError();
						return;
					}
				}
			}

			TFApkExpansionPolicy.OBBData[] obbData = obbList.toArray(new TFApkExpansionPolicy.OBBData[obbList.size()]);
			onResult.onResult(obbData);
		}
	}

	private static String getVersionCode(Context context) {
		int versionCode = 0;

		try {
			PackageManager pm = context.getPackageManager();
			versionCode = pm.getPackageInfo(context.getApplicationContext().getPackageName(), 0).versionCode;
		} catch (Throwable t ) {
			// Ignore
		}

		return "" + versionCode;
	}

	private static String getVersionName(Context context) {
		String versionName = null;

		try {
			PackageManager pm = context.getPackageManager();
			versionName = pm.getPackageInfo(context.getApplicationContext().getPackageName(), 0).versionName;
		} catch (Throwable t ) {
			// Ignore
		}

		return versionName;
	}
}
