package com.testfairy.obb.sdk.license;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.testfairy.obb.sdk.google.downloader.Constants;
import com.testfairy.obb.sdk.google.downloader.impl.DownloadNotification;
import com.testfairy.obb.sdk.google.downloader.impl.DownloaderService;
import com.testfairy.obb.sdk.NotificationSettings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TFObbNotification implements DownloadNotification.ICustomNotification {

	private static final int NOTIFICATION_ID = "DownloadNotification".hashCode();

	private static NotificationSettings settings = new NotificationSettings();

//	private CharSequence mTitle;
	private CharSequence mTicker;
	private int mIcon;
	private long mTotalKB = -1;
	private long mCurrentKB = -1;
	private long mTimeRemaining;
	private PendingIntent mPendingIntent;

	public static void setSettings(NotificationSettings notificationSettings) {
		settings = notificationSettings;
	}

	private void observeProgress(Context context, Notification notification) {
		final NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		Handler handler = new Handler(context.getMainLooper());
		handler.post(new Runnable() {
			@Override
			public void run() {
				int filteredCancelFlag = notification.flags & Notification.FLAG_AUTO_CANCEL;

				if (filteredCancelFlag > 0) {
					notificationManager.cancel(NOTIFICATION_ID);
					sendCompletionNotification(context);
				} else {
					handler.postDelayed(this, 1000);
				}
			}
		});
	}

	private void sendCompletionNotification(Context context) {
		final NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (!settings.dismissOnComplete) {
			Notification completionNotification = updateNotification(context, false, settings.completionText, "", false);
			notificationManager.notify(NOTIFICATION_ID, completionNotification);
		}
	}

	@Override
	public void setIcon(int icon) {
		mIcon = icon;
	}

	@Override
	public void setTitle(CharSequence title) {
//		mTitle = title;
	}

	@Override
	public void setTotalBytes(long totalBytes) {
		mTotalKB = totalBytes;
	}

	@Override
	public void setCurrentBytes(long currentBytes) {
		mCurrentKB = currentBytes;
	}

	@Override
	public Notification updateNotification(Context c) {
		String contentText = getDownloadProgressString(mCurrentKB, mTotalKB);
		String contentInfo = String.format("%1$s left", getTimeRemaining(mTimeRemaining));
		boolean ongoing = true;

		return updateNotification(c, true, contentText, contentInfo, ongoing);
	}

	private Notification updateNotification(Context c, boolean showProgress, String contentText, String contentInfo, boolean ongoing) {
		Notification.Builder builder = new Notification.Builder(c);
		builder.setContentTitle(ongoing ? settings.downloadText : settings.completionText);

		if (showProgress) {
			if (mTotalKB > 0 && -1 != mCurrentKB) {
				builder.setProgress((int) (mTotalKB >> 8), (int) (mCurrentKB >> 8), false);
			} else {
				builder.setProgress(0, 0, true);
			}
		}

		if (ongoing) {
			builder.setContentText(contentText);
			builder.setContentInfo(contentInfo);
		}

		if (ongoing && mIcon != 0) {
			builder.setSmallIcon(mIcon);
		} else if (ongoing) {
			int iconResource = android.R.drawable.stat_sys_download;
			builder.setSmallIcon(iconResource);
		} else {
			Integer appIconResourceId = getAppIconResourceId(c);
			if (appIconResourceId != null) {
				builder.setSmallIcon(appIconResourceId);
			}
		}

		builder.setOngoing(ongoing);
		builder.setTicker(mTicker);
		builder.setContentIntent(mPendingIntent);
		builder.setOnlyAlertOnce(true);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			builder.setChannelId(getNotificationChannelId(c));
		}

		Notification notification = builder.build();
		observeProgress(c, notification);

		return notification;
	}

	@Override
	public void setPendingIntent(PendingIntent contentIntent) {
		mPendingIntent = contentIntent;
	}

	@Override
	public void setTicker(CharSequence ticker) {
		mTicker = ticker;
	}

	@Override
	public void setTimeRemaining(long timeRemaining) {
		mTimeRemaining = timeRemaining;
	}

	public static void createNotificationChannel(Context context, DownloaderService downloaderService) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
			NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

			CharSequence name = "TestFairyObb";
			String description = "TestFairyObb notification channel";
			int importance = NotificationManager.IMPORTANCE_LOW;

			NotificationChannel channel = new NotificationChannel(getNotificationChannelId(context), name, importance);
			channel.setDescription(description);
			channel.setSound(null, null);

			notificationManager.createNotificationChannel(channel); // This is documented to be idempotent
		}
	}

	private static String getDownloadProgressString(long overallProgress, long overallTotal) {
		if (overallTotal == 0) {
			if (Constants.LOGVV) {
				Log.e(Constants.TAG, "Notification called when total is zero");
			}
			return "";
		}
		return String.format("%.2f",
				(float) overallProgress / (1024.0f * 1024.0f))
				+ "MB /" +
				String.format("%.2f", (float) overallTotal /
						(1024.0f * 1024.0f)) + "MB";
	}

	private static String getTimeRemaining(long durationInMilliseconds) {
		SimpleDateFormat sdf;
		if (durationInMilliseconds > 1000 * 60 * 60) {
			sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
		} else {
			sdf = new SimpleDateFormat("mm:ss", Locale.getDefault());
		}
		return sdf.format(new Date(durationInMilliseconds - TimeZone.getDefault().getRawOffset()));
	}

	private static String getNotificationChannelId(Context context) {
		return "TestFairyObb." + context.getPackageName();
	}

	private static Integer getAppIconResourceId(Context context) {
		try {
			String packageName = context.getPackageName(); //use getPackageName() in case you wish to use yours
			final PackageManager pm = context.getPackageManager();
			final ApplicationInfo applicationInfo;

			applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

			pm.getResourcesForApplication(applicationInfo); //Throws if icon actually not available

			return applicationInfo.icon;
		} catch (PackageManager.NameNotFoundException e) {
		}

		return null;
	}
}