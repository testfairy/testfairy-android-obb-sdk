package com.testfairy.obb.app;

import android.app.Application;
import android.util.Log;

import com.testfairy.obb.sdk.Policy;
import com.testfairy.obb.sdk.TestFairyObb;

public class MyApplication extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		Log.d("MyApplication", "Enforcing TestFairyLVL");
		TestFairyObb.enforceLVL(this, Policy.ALWAYS_DOWNLOAD_LATEST);
	}
}
