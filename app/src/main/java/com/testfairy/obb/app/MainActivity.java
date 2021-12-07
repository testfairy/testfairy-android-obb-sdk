package com.testfairy.obb.app;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Messenger;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;
import com.testfairy.obb.app.expansion.MyDownloaderService;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements IDownloaderClient {

	private int REQUEST_PERMISSION_CODE = 9876;

	private IStub downloaderClientStub;
	private IDownloaderService remoteDownloaderService;
	private boolean started = false;

	@Override
	public void onServiceConnected(Messenger m) {
		Log.d("MainActivity", "onServiceConnected");

		if (remoteDownloaderService != null) {
			downloaderClientStub.disconnect(this);
		}

		remoteDownloaderService = DownloaderServiceMarshaller.CreateProxy(m);
		remoteDownloaderService.onClientUpdated(downloaderClientStub.getMessenger());
	}

	@Override
	public void onDownloadStateChanged(int newState) {
		Log.d("MainActivity", "onDownloadStateChanged: " + newState);

		if (newState == IDownloaderClient.STATE_COMPLETED) {
			startApp();
		}
	}

	@Override
	public void onDownloadProgress(DownloadProgressInfo progress) {
		Log.d("MainActivity", "onDownloadProgress: " + progress.mOverallProgress + "/" + progress.mOverallTotal);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		REQUEST_PERMISSION_CODE += Process.myPid();

		checkPermissionsForObb();
	}

	private synchronized void checkPermissionsForObb() {
		boolean permissionGranted =
				ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
						ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

		if (!permissionGranted) {
			ActivityCompat.requestPermissions(this, new String[] {
							Manifest.permission.WRITE_EXTERNAL_STORAGE,
							Manifest.permission.READ_EXTERNAL_STORAGE,
							Manifest.permission.REQUEST_INSTALL_PACKAGES },
					REQUEST_PERMISSION_CODE);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
			startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", getPackageName()))), REQUEST_PERMISSION_CODE);
		} else {
			downloadObb();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == REQUEST_PERMISSION_CODE) {
			// Checking whether user granted the permission or not.
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				checkPermissionsForObb();
			} else {
				Toast.makeText(this, "Cannot download obb, external storage permission denied!", Toast.LENGTH_LONG).show();
			}
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_PERMISSION_CODE && resultCode == Activity.RESULT_OK) {
			if (getPackageManager().canRequestPackageInstalls()) {
				checkPermissionsForObb();
			} else {
				Toast.makeText(this, "Cannot download obb, install package permission denied!", Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	protected void onResume() {
		if (downloaderClientStub != null) {
			downloaderClientStub.connect(this);
		}

		super.onResume();
	}

	@Override
	protected void onStop() {
		if (downloaderClientStub != null) {
			downloaderClientStub.disconnect(this);
		}

		super.onStop();
	}

	private synchronized void downloadObb() {
		createObbStore();

		// Check if expansion files are available before going any further
		if (!expansionFilesDelivered()) {
			// Build an Intent to start this activity from the Notification
			Intent notifierIntent = new Intent(this, MainActivity.class);
			notifierIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
					Intent.FLAG_ACTIVITY_CLEAR_TOP);

			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
					notifierIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			// Start the download service (if required)
			int startResult = 0;
			try {
				startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(this,
						pendingIntent, MyDownloaderService.class);
			} catch (PackageManager.NameNotFoundException e) {
				throw new RuntimeException("Cannot download obb", e);
			}

			// If download has started, initialize this activity to show
			// download progress
			if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
				// This is where you do set up to display the download
				// progress (next step)

				Log.d("MainActivity", "OBB not found. Downloading...");

				// Instantiate a member instance of IStub
				downloaderClientStub = DownloaderClientMarshaller.CreateStub(this,
						MyDownloaderService.class);

				return;
			}
		}

		Log.d("MainActivity", "OBB found. Starting app...");
		startApp(); // Expansion files are available, start the app
	}

	private void createObbStore() {
		String saveFilePath = Helpers.getSaveFilePath(this);
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
			String fileName = Helpers.getExpansionAPKFileName(this, xf.isBase,
					xf.fileVersion);
			if (!Helpers.doesFileExist(this, fileName, xf.fileSize, false))
				return false;
		}
		return true;
	}

	private synchronized void startApp() {
		if (!started) {
			started = true;
			Log.d("MainActivity", "App started!");
			Toast.makeText(this, "App started!", Toast.LENGTH_LONG).show();
		}
	}
}