package com.testfairy.obb.sdk.expansion;

import android.content.Context;
import android.util.Log;

import com.testfairy.obb.sdk.Config;
import com.testfairy.obb.sdk.google.downloader.impl.DownloaderService;
import com.testfairy.obb.sdk.license.TFLVL;

public class TFDownloaderService extends DownloaderService {

	public static final String BASE64_PUBLIC_KEY = "IGNORED";
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
		return TFDownloaderServiceBroadcastReceiver.class.getName();
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(Config.LOG_TAG, "Registering service " + hashCode());
		TFLVL.registerService(this);
	}

	@Override
	public void updateLVL(Context context) {
		TFLVL.updateLVL(context, this, Config.getAppToken());
	}
}

