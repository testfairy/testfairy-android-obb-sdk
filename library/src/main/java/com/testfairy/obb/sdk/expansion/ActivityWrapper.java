package com.testfairy.obb.sdk.expansion;

import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Messenger;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.testfairy.obb.sdk.Config;
import com.testfairy.obb.sdk.Policy;
import com.testfairy.obb.sdk.TestFairyObb;
import com.testfairy.obb.sdk.google.downloader.DownloadProgressInfo;
import com.testfairy.obb.sdk.google.downloader.DownloaderClientMarshaller;
import com.testfairy.obb.sdk.google.downloader.DownloaderServiceMarshaller;
import com.testfairy.obb.sdk.google.downloader.Helpers;
import com.testfairy.obb.sdk.google.downloader.IDownloaderClient;
import com.testfairy.obb.sdk.google.downloader.IDownloaderService;
import com.testfairy.obb.sdk.google.downloader.IStub;
import com.testfairy.obb.sdk.license.TFLVL;

import java.io.File;
import java.io.IOException;

public class ActivityWrapper implements IDownloaderClient, Application.ActivityLifecycleCallbacks {

	private static boolean enforced = false;

	private final Activity wrappedActivity;
	private final Policy policy;
	private final TestFairyObb.Callback callback;

	private IStub downloaderClientStub;
	private IDownloaderService remoteDownloaderService;

	public ActivityWrapper(Activity wrappedActivity, Policy policy, TestFairyObb.Callback callback) {
		this.wrappedActivity = wrappedActivity;
		this.policy = policy;
		this.callback = callback;
	}

	@Override
	public void onServiceConnected(Messenger m) {
		Log.d(Config.LOG_TAG, "onServiceConnected");

		if (remoteDownloaderService != null) {
			downloaderClientStub.disconnect(wrappedActivity);
		}

		remoteDownloaderService = DownloaderServiceMarshaller.CreateProxy(m);
		remoteDownloaderService.onClientUpdated(downloaderClientStub.getMessenger());
	}

	@Override
	public void onDownloadStateChanged(int newState) {
		Log.d(Config.LOG_TAG, "onDownloadStateChanged: " + newState);

		if (newState == IDownloaderClient.STATE_COMPLETED) {
			startApp();
		}
	}

	@Override
	public void onDownloadProgress(DownloadProgressInfo progress) {
		Log.d(Config.LOG_TAG, "onDownloadProgress: " + progress.mOverallProgress + "/" + progress.mOverallTotal);
	}

	private static class XAPKFile {
		public final boolean isBase;
		public final long fileSize;
		public final int fileVersion;

		public XAPKFile(boolean isBase, long fileSize, int fileVersion) {
			this.isBase = isBase;
			this.fileSize = fileSize;
			this.fileVersion = fileVersion;
		}
	}

	private XAPKFile[] xAPKS = new XAPKFile[] {
		new XAPKFile(true, 265019, 1),
			new XAPKFile(false, 265019, 1)
	};

	public synchronized void prepareObb() {
		wrappedActivity.getApplication().registerActivityLifecycleCallbacks(this);

		if (!enforced) {
			TFLVL.enforceLVL(wrappedActivity.getApplication(), policy);
			enforced = true;
		}

		createObbStore();
		downloadObb();
	}

	private synchronized void downloadObb() {
		// Check if expansion files are available before going any further
		if (!expansionFilesDelivered()) {
			// Build an Intent to start this activity from the Notification
			Intent notifierIntent = new Intent(wrappedActivity, wrappedActivity.getClass());
			notifierIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
					Intent.FLAG_ACTIVITY_CLEAR_TOP);

			PendingIntent pendingIntent = PendingIntent.getActivity(wrappedActivity, 0,
					notifierIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

			// Start the download service (if required)
			int startResult = 0;
			try {
				startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(wrappedActivity,
						pendingIntent, TFDownloaderService.class);
			} catch (PackageManager.NameNotFoundException e) {
				throw new RuntimeException("Cannot download obb", e);
			}

			// If download has started, initialize this activity to show
			// download progress
			if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
				// This is where you do set up to display the download
				// progress (next step)

				Log.d(Config.LOG_TAG, "OBB not found. Downloading...");

				// Instantiate a member instance of IStub
				downloaderClientStub = DownloaderClientMarshaller.CreateStub(this,
						TFDownloaderService.class);

				return;
			}
		}

		Log.d(Config.LOG_TAG, "OBB found. Starting app...");

		startApp(); // Expansion files are available, start the app
	}

	private void createObbStore() {
		String saveFilePath = Helpers.getSaveFilePath(wrappedActivity);
		File file = new File(saveFilePath);
		if(!file.exists()) {
			boolean mkdir = file.mkdir();
			if (!mkdir) {
				throw new RuntimeException(new IOException("Cannot create obb storage"));
			}
		}
	}

	private boolean expansionFilesDelivered() {
		for (XAPKFile xf : xAPKS) {
			String fileName = Helpers.getExpansionAPKFileName(wrappedActivity, xf.isBase,
					xf.fileVersion);
			if (!Helpers.doesFileExist(wrappedActivity, fileName, xf.fileSize, false))
				return false;
		}
		return true;
	}

	private synchronized void startApp() {
		if (callback != null) {
			callback.onObbReady();
		}
	}

	@Override
	public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

	}

	@Override
	public void onActivityStarted(@NonNull Activity activity) {

	}

	@Override
	public void onActivityResumed(@NonNull Activity activity) {
		if (downloaderClientStub != null && activity == wrappedActivity) {
			downloaderClientStub.connect(activity);
		}
	}

	@Override
	public void onActivityPaused(@NonNull Activity activity) {

	}

	@Override
	public void onActivityStopped(@NonNull Activity activity) {
		if (downloaderClientStub != null && activity == wrappedActivity) {
			downloaderClientStub.disconnect(activity);
		}
	}

	@Override
	public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

	}

	@Override
	public void onActivityDestroyed(@NonNull Activity activity) {

	}
}