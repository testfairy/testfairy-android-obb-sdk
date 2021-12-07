package com.testfairy.obb.app.expansion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;

public class MyDownloaderServiceBroadcastReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			DownloaderClientMarshaller.startDownloadServiceIfRequired(context,
					intent, MyDownloaderService.class);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e("OBB", "Exception: ", e);
		}
	}
}
