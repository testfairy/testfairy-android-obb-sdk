package com.testfairy.obb.sdk.expansion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.testfairy.obb.sdk.Config;
import com.testfairy.obb.sdk.google.downloader.DownloaderClientMarshaller;

public class TFDownloaderServiceBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			DownloaderClientMarshaller.startDownloadServiceIfRequired(context, intent, TFDownloaderService.class);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(Config.LOG_TAG, "Exception: ", e);
		}
	}
}
