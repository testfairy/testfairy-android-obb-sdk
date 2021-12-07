package com.testfairy.obb.sdk.license;

import android.content.Context;

import com.testfairy.obb.sdk.google.licensing.APKExpansionPolicy;
import com.testfairy.obb.sdk.google.licensing.Obfuscator;

public class TFApkExpansionPolicy extends APKExpansionPolicy {

	public static class OBBData {
		public final String url;
		public final String filename;
		public final long filesize;
		public final String type;

		public OBBData(String url, String filename, long filesize, String type) {
			this.url = url;
			this.filename = filename;
			this.filesize = filesize;
			this.type = type;
		}
	}

	public TFApkExpansionPolicy(Context context, Obfuscator obfuscator, OBBData[] obbs) {
		super(context, obfuscator);

		for (OBBData obb : obbs) {
			if (obb != null) {
				if (obb.type.equals("main")) {
					setExpansionURL(APKExpansionPolicy.MAIN_FILE_URL_INDEX, obb.url);
					setExpansionFileName(APKExpansionPolicy.MAIN_FILE_URL_INDEX, obb.filename);
					setExpansionFileSize(APKExpansionPolicy.MAIN_FILE_URL_INDEX, obb.filesize);
				} else {
					setExpansionURL(APKExpansionPolicy.PATCH_FILE_URL_INDEX, obb.url);
					setExpansionFileName(APKExpansionPolicy.PATCH_FILE_URL_INDEX, obb.filename);
					setExpansionFileSize(APKExpansionPolicy.PATCH_FILE_URL_INDEX, obb.filesize);
				}
			}
		}
	}

	@Override
	public boolean allowAccess() {
		return true;
	}
}
