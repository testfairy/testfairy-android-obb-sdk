package com.testfairy.obb.sdk;

import android.content.Context;

import com.google.android.vending.expansion.downloader.impl.DownloaderService;
import com.testfairy.obb.sdk.license.TestFairyLVL;
import com.testfairy.obb.sdk.license.TestFairyObbNotification;

public class TestFairyObb {

	public static void setServerEndpoint(String endpoint) {
		Config.setServerEndpoint(endpoint);
	}

	public static void setNotificationSettings(NotificationSettings notificationSettings) {
		TestFairyObbNotification.setSettings(notificationSettings);
	}

	public static void enforceLVL(Context context, Policy policy) {
		TestFairyLVL.enforceLVL(context, policy);
	}

	public static void updateLVL(Context context, DownloaderService downloaderService, String appToken) {
		TestFairyLVL.updateLVL(context, downloaderService, appToken);
	}

	public static void registerService(DownloaderService downloaderService) {
		TestFairyLVL.registerService(downloaderService);
	}
}
