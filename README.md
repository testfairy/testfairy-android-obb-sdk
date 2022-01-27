# TestFairy Android OBB SDK

This repo contains all the necessary code to distribute OBB files with TestFairy platform.

## Instalation

1. Follow [the official Android documentation](https://developer.android.com/google/play/expansion-files) to prepare your app for OBB downloads.

2. Upload your OBB files to TestFairy platform.

```shell
curl https://app.testfairy.com/api/upload         \
  -F api_key=<YOUR_API_KEY>                       \ 
  -F main=@main.1.com.example.app.obb             \
  -F patch=@patch.1.com.example.app.obb           # patch is optional, ignore if you don't have any
```

The formatting for OBB files names must follow the following pattern.

**Main OBB file**
```
main.VERSION_CODE.PACKAGE_NAME.obb
```

**Patch OBB file**
```
patch.VERSION_CODE.PACKAGE_NAME.obb
```

TestFairy enforces the same [file size limits](https://developer.android.com/google/play/expansion-files#Overview) as Google Play.

If you upload a new OBB file for an already uploaded version code, the old one is discarded from all builds.

3. Clone this repo.

4. Copy the **library** directory into your own project. Rename it to a name of your choice. (i.e `testfairy-obb`)

5. Add the following line to **settings.gradle** in your app project.

```gradle
include ':testfairy-obb' // Change the name if necessary
```

6. Add the following dependency to your app's **build.gradle** file.

```gradle
dependencies {
    ...

    api project(path: ':testfairy-obb') // Change the name if necessary
}
```

7. Prepare your app so that it asks runtime permissions for following items.

```java
Manifest.permission.WRITE_EXTERNAL_STORAGE
Manifest.permission.READ_EXTERNAL_STORAGE
Manifest.permission.REQUEST_INSTALL_PACKAGES
```

[See example](./app/src/main/java/com/testfairy/obb/app/MainActivity.java).

8. Prepare your app so that it allows access to the OBB folders when app is not installed from Google Play. Make sure you only do this when you distribute your app with TestFairy. In production, users don't have to change this setting to be able to download the original OBB files from Google Play.

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
    startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", getPackageName()))), REQUEST_PERMISSION_CODE);
} 
```

[See complete example](./app/src/main/java/com/testfairy/obb/app/MainActivity.java)

9. Call the SDK in the happy path.

```java
TestFairyObb.prepareObb(this, "YOUR_APP_TOKEN", Policy.DOWNLOAD_ONLY_IF_REQUIRED, new TestFairyObb.Callback() {
    @Override
    public void onObbReady() {
        startApp();
    }
});
```

## Download Policies

#### DOWNLOAD_ONLY_IF_REQUIRED

This is the same policy as Google Play expansion files. If the OBB file is found in cache and versions codes app and obb conform, the download will be skipped automatically.

#### ALWAYS_DOWNLOAD_LATEST

This policy enforces downloading of the latest available OBB from TestFairy regardless of what is found in OBB cache.