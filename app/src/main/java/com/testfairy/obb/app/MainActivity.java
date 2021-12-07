package com.testfairy.obb.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.testfairy.obb.sdk.Policy;
import com.testfairy.obb.sdk.TestFairyObb;

public class MainActivity extends AppCompatActivity {

	private int REQUEST_PERMISSION_CODE = 9876;

	private boolean started = false;

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

	private synchronized void downloadObb() {
		TestFairyObb.prepareObb(this, "SDK-XXXX", Policy.ALWAYS_DOWNLOAD_LATEST, new TestFairyObb.Callback() {
			@Override
			public void onObbReady() {
				startApp();
			}
		});
	}

	private synchronized void startApp() {
		if (!started) {
			started = true;
			Log.d("MainActivity", "App started!");
			Toast.makeText(this, "App started!", Toast.LENGTH_LONG).show();
		}
	}
}