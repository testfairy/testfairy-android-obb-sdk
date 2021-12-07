package com.testfairy.obb.sdk;

import android.app.Activity;

import com.testfairy.obb.sdk.expansion.ActivityWrapper;
import com.testfairy.obb.sdk.license.TFObbNotification;

public class TestFairyObb {

	public interface Callback {
		void onObbReady();
	}

	public static void setServerEndpoint(String endpoint) {
		Config.setServerEndpoint(endpoint);
	}

	public static void setNotificationSettings(NotificationSettings notificationSettings) {
		TFObbNotification.setSettings(notificationSettings);
	}

	public static void prepareObb(Activity activity, String appToken, Policy policy, Callback callback) {
		Config.setAppToken(appToken);

		ActivityWrapper activityWrapper = new ActivityWrapper(activity, policy, callback);
		activityWrapper.prepareObb();
	}
}
