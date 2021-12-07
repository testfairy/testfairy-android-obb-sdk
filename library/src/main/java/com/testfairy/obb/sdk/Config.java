package com.testfairy.obb.sdk;

public class Config {
	public static final String LOG_TAG = "TESTFAIRYSDK";

	private static String APP_SERVER = "https://app.testfairy.com/services/";
	private static String APP_TOKEN = "";

	public static void setServerEndpoint(String newEndpoint) {
		String appServerEndpoint = newEndpoint;

		if (appServerEndpoint.contains("http://")) {
			appServerEndpoint = appServerEndpoint.replace("http", "https");

		} else if (!appServerEndpoint.contains("https")) {
			appServerEndpoint = "https://" + appServerEndpoint;
		}

		if (!appServerEndpoint.endsWith("/")) {
			appServerEndpoint = appServerEndpoint + "/";
		}

		if (!appServerEndpoint.contains("services")) {
			appServerEndpoint = appServerEndpoint + "services/";
		}

		APP_SERVER = appServerEndpoint;
	}

	public static String getAppServerEndpoint() {
		return APP_SERVER;
	}

	public static void setAppToken(String appToken) {
		APP_TOKEN = appToken;
	}

	public static String getAppToken() {
		return APP_TOKEN;
	}
}
