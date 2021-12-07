package com.testfairy.obb.app.expansion;

import android.content.Context;
import android.util.Log;

import com.google.android.vending.expansion.downloader.impl.DownloaderService;
import com.testfairy.obb.sdk.TestFairyObb;

public class MyDownloaderService extends DownloaderService {

	// You must use the public key belonging to your publisher account
	public static final String BASE64_PUBLIC_KEY = "YourLVLKey";
	// You should also modify this salt
	public static final byte[] SALT = new byte[] { 1, 42, -12, -1, 54, 98,
			-100, -12, 43, 2, -8, -4, 9, 5, -106, -107, -33, 45, -1, 84
	};

	@Override
	public String getPublicKey() {
		return BASE64_PUBLIC_KEY;
	}

	@Override
	public byte[] getSALT() {
		return SALT;
	}

	@Override
	public String getAlarmReceiverClassName() {
		return MyDownloaderServiceBroadcastReceiver.class.getName();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d("MyDownloaderService", "Registering service " + hashCode());
		TestFairyObb.registerService(this);
	}

	@Override
	public void updateLVL(Context context) {
		TestFairyObb.updateLVL(context, this, "SDK-XXXX");
	}
}

