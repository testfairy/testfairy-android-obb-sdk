package com.testfairy.obb.sdk.license;

import static com.testfairy.obb.sdk.google.downloader.impl.DownloaderService.DOWNLOAD_REQUIRED;
import static com.testfairy.obb.sdk.google.downloader.impl.DownloaderService.LVL_CHECK_REQUIRED;
import static com.testfairy.obb.sdk.google.downloader.impl.DownloaderService.NO_DOWNLOAD_REQUIRED;
import static com.testfairy.obb.sdk.google.downloader.impl.DownloaderService.STATUS_SUCCESS;
import static com.testfairy.obb.sdk.Config.LOG_TAG;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;

import com.testfairy.obb.sdk.google.downloader.Constants;
import com.testfairy.obb.sdk.google.downloader.IDownloaderClient;
import com.testfairy.obb.sdk.google.downloader.impl.DownloadInfo;
import com.testfairy.obb.sdk.google.downloader.impl.DownloadNotification;
import com.testfairy.obb.sdk.google.downloader.impl.DownloaderService;
import com.testfairy.obb.sdk.google.downloader.impl.DownloadsDB;
import com.testfairy.obb.sdk.google.licensing.AESObfuscator;
import com.testfairy.obb.sdk.google.licensing.APKExpansionPolicy;
import com.testfairy.obb.sdk.Config;
import com.testfairy.obb.sdk.Policy;
import com.testfairy.obb.sdk.http.GetObbRequirementsRequestTask;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class TFLVL implements Runnable, GetObbRequirementsRequestTask.OnResult {

	private static final Set<Integer> registeredServices = new HashSet<>();
	private static DownloadNotification persistentNotification;

	private final Context context;
	private final DownloaderService downloaderService;
	private final String appToken;

	public TFLVL(Context context, DownloaderService downloaderService, String appToken) throws NoSuchFieldException, IllegalAccessException {
		this.context = context;
		this.downloaderService = downloaderService;
		this.appToken = appToken;

		registerService(downloaderService);
	}

	public static void enforceLVL(Context context, Policy policy) {
		DownloadsDB db = DownloadsDB.getDB(context);
		db.updateMetadata(-1, 0);

		if (policy == Policy.ALWAYS_DOWNLOAD_LATEST) {
			deleteOldObbs(context);
		}
	}

	public static void registerService(DownloaderService downloaderService) {
		try {
			if (persistentNotification != null) {
				setNotificationReflection(downloaderService, persistentNotification);
			}

			TFObbNotification.createNotificationChannel(downloaderService.getApplicationContext(), downloaderService);
			setPendingIntentReflection(downloaderService, getPendingIntentReflection(downloaderService));
			setCustomNotificationReflection(downloaderService.getApplicationContext(), downloaderService);
			registeredServices.add(downloaderService.hashCode());
			persistentNotification = getNotificationReflection(downloaderService);
		} catch (Throwable t) {
			Log.e(Config.LOG_TAG, "Error setting up OBB with TestFairy, falling back to Google Play", t);
		}
	}

	public static void updateLVL(Context context, DownloaderService downloaderService, String appToken) {
		Context c = context.getApplicationContext();
		Handler h = new Handler(c.getMainLooper());
		try {
			h.post(new TFLVL(c, downloaderService, appToken));
		} catch (Throwable t) {
			Log.e(Config.LOG_TAG, "Error setting up OBB with TestFairy, falling back to Google Play", t);
			downloaderService.updateLVL(context);
		}
	}

	private static void setPendingIntentReflection(DownloaderService downloaderService, PendingIntent intent) throws NoSuchFieldException, IllegalAccessException {
		Field mPendingIntent = DownloaderService.class.getDeclaredField("mPendingIntent");
		mPendingIntent.setAccessible(true);
		mPendingIntent.set(downloaderService, intent);
	}

	private static PendingIntent getPendingIntentReflection(DownloaderService downloaderService) throws NoSuchFieldException, IllegalAccessException {
		Field mPendingIntent = DownloaderService.class.getDeclaredField("mPendingIntent");
		mPendingIntent.setAccessible(true);
		return (PendingIntent) mPendingIntent.get(downloaderService);
	}

	private static void setServiceRunningReflection(DownloaderService downloaderService, boolean isRunning) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		Method setServiceRunning = DownloaderService.class.getDeclaredMethod("setServiceRunning", boolean.class);
		setServiceRunning.setAccessible(true);
		setServiceRunning.invoke(downloaderService, isRunning);
	}

	private static DownloadNotification getNotificationReflection(DownloaderService downloaderService) throws NoSuchFieldException, IllegalAccessException {
		Field downloadNotification = DownloaderService.class.getDeclaredField("mNotification");
		downloadNotification.setAccessible(true);

		return (DownloadNotification) downloadNotification.get(downloaderService);
	}

	private static void setNotificationReflection(DownloaderService downloaderService, DownloadNotification downloadNotification) throws NoSuchFieldException, IllegalAccessException {
		Field downloadNotificationField = DownloaderService.class.getDeclaredField("mNotification");
		downloadNotificationField.setAccessible(true);
		downloadNotificationField.set(downloaderService, downloadNotification);
	}

	private static DownloadInfo getDownloadInfoByFileNameReflection(DownloadsDB db, String filename) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
		Method setServiceRunning = DownloadsDB.class.getDeclaredMethod("getDownloadInfoByFileName", String.class);
		setServiceRunning.setAccessible(true);
		return (DownloadInfo) setServiceRunning.invoke(db, filename);
	}

	private static void setCustomNotificationReflection(Context context, DownloaderService downloaderService) throws NoSuchFieldException, IllegalAccessException {
		Log.d(LOG_TAG, "setCustomNotificationReflection");

		TFObbNotification TFObbNotification = new TFObbNotification();

		DownloadNotification downloadNotification = getNotificationReflection(downloaderService);
		Field mCustomNotification = DownloadNotification.class.getDeclaredField("mCustomNotification");
		mCustomNotification.setAccessible(true);
		mCustomNotification.set(downloadNotification, TFObbNotification);

		Notification notification = TFObbNotification.updateNotification(context);

		Field mNotification = DownloadNotification.class.getDeclaredField("mNotification");
		mNotification.setAccessible(true);
		mNotification.set(downloadNotification, notification);

		Field mCurrentNotification = DownloadNotification.class.getDeclaredField("mCurrentNotification");
		mCurrentNotification.setAccessible(true);
		mCurrentNotification.set(downloadNotification, notification);
	}

	private static void deleteOldObbs(Context context) {
		File root = Environment.getExternalStorageDirectory();
		String saveFilePath = root.toString() + Constants.EXP_PATH + context.getPackageName();
		File file = new File(saveFilePath);
		if (file.exists() && file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					try {
						f.delete();
					} catch (SecurityException securityException) {
						Log.d(LOG_TAG, "Cannot clear OBB cache", securityException);
					}
				}
			}
		}
	}

	@Override
	public void run() {
		GetObbRequirementsRequestTask getObbRequirementsRequestTask = new GetObbRequirementsRequestTask(context, appToken, this);
		getObbRequirementsRequestTask.execute();
	}

	@Override
	public void onResult(TFApkExpansionPolicy.OBBData[] result) {
		if (!registeredServices.contains(downloaderService.hashCode())) {
			Log.d(LOG_TAG, "skipping downloadObbsIfNecessary " + downloaderService.hashCode());
			return;
		}

		registeredServices.remove(downloaderService.hashCode());

		Context c = context.getApplicationContext();
		Handler h = new Handler(c.getMainLooper());
		h.post(new Runnable() {
			@Override
			public void run() {
				downloadObbsIfNecessary(result);
			}
		});
	}

	@Override
	public void onError() {
		Log.w(LOG_TAG, "TestFairy cannot download obbs, falling back to Google Play");

		Context c = context.getApplicationContext();
		Handler h = new Handler(c.getMainLooper());
		h.post(new Runnable() {
			@Override
			public void run() {
				downloaderService.updateLVL(context);
			}
		});
	}

	private void downloadObbsIfNecessary(TFApkExpansionPolicy.OBBData[] obbData) {
		try {
			Log.d(LOG_TAG, "downloadObbsIfNecessary " + downloaderService.hashCode());

			setServiceRunningReflection(downloaderService, true);
			getNotificationReflection(downloaderService).onDownloadStateChanged(IDownloaderClient.STATE_FETCHING_URL);

			String deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

			final APKExpansionPolicy aep = new TFApkExpansionPolicy(context,
					new AESObfuscator(downloaderService.getSALT(), context.getPackageName(), deviceId),
					obbData);

			// reset our policy back to the start of the world to force a
			// re-check
			aep.resetPolicy();

			// let's try and get the OBB file from LVL first
			// Construct the LicenseChecker with a Policy.
			try {
				int count = aep.getExpansionURLCount();
				DownloadsDB db = DownloadsDB.getDB(context);
				int status = 0;
				if (count != 0) {
					for (int i = 0; i < count; i++) {
						String currentFileName = aep
								.getExpansionFileName(i);
						if (null != currentFileName) {
							DownloadInfo di = new DownloadInfo(i,
									currentFileName, context.getPackageName());

							long fileSize = aep.getExpansionFileSize(i);
							if (downloaderService.handleFileUpdated(db, i, currentFileName,
									fileSize)) {
								status |= -1;
								di.resetDownload();
								di.mUri = aep.getExpansionURL(i);
								di.mTotalBytes = fileSize;
								di.mStatus = status;
								db.updateDownload(di);
							} else {
								// we need to read the download
								// information
								// from
								// the database
//										DownloadInfo dbdi = db.getDownloadInfoByFileName(di.mFileName);
								DownloadInfo dbdi = getDownloadInfoByFileNameReflection(db, di.mFileName);

								if (null == dbdi) {
									// the file exists already and is
									// the
									// correct size
									// was delivered by Market or
									// through
									// another mechanism
									Log.d(LOG_TAG, "file " + di.mFileName
											+ " found. Not downloading.");
									di.mStatus = STATUS_SUCCESS;
									di.mTotalBytes = fileSize;
									di.mCurrentBytes = fileSize;
									di.mUri = aep.getExpansionURL(i);
									db.updateDownload(di);
								} else if (dbdi.mStatus != STATUS_SUCCESS) {
									// we just update the URL
									dbdi.mUri = aep.getExpansionURL(i);

									db.updateDownload(dbdi);
									status |= -1;
								}
							}
						}
					}
				}
				// first: do we need to do an LVL update?
				// we begin by getting our APK version from the package
				// manager
				PackageInfo pi;
				try {
					pi = context.getPackageManager().getPackageInfo(
							context.getPackageName(), 0);
					db.updateMetadata(pi.versionCode, status);
					Class<?> serviceClass = downloaderService.getClass();
					switch (DownloaderService.startDownloadServiceIfRequired(context, getPendingIntentReflection(downloaderService), serviceClass)) {
						case NO_DOWNLOAD_REQUIRED:
							getNotificationReflection(downloaderService)
									.onDownloadStateChanged(IDownloaderClient.STATE_COMPLETED);
							break;
						case LVL_CHECK_REQUIRED:
							// DANGER WILL ROBINSON!
							Log.e(LOG_TAG, "In LVL checking loop!");
							getNotificationReflection(downloaderService)
									.onDownloadStateChanged(IDownloaderClient.STATE_FAILED_UNLICENSED);
							throw new RuntimeException(
									"Error with LVL checking and database integrity");
						case DOWNLOAD_REQUIRED:
							// do nothing. the download will notify the
							// application
							// when things are done
							break;
					}
				} catch (PackageManager.NameNotFoundException e1) {
					e1.printStackTrace();
					throw new RuntimeException(
							"Error with getting information from package name");
				}
			} catch (Throwable t) {
				throw new RuntimeException(t);
			} finally {
				try {
					setServiceRunningReflection(downloaderService, false);
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
};