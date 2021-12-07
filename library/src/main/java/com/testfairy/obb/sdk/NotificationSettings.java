package com.testfairy.obb.sdk;

public class NotificationSettings {
	public final boolean dismissOnComplete;
	public final String downloadText;
	public final String completionText;

	public NotificationSettings(boolean dismissOnComplete, String downloadText, String completionText) {
		this.dismissOnComplete = dismissOnComplete;
		this.downloadText = downloadText != null ? downloadText : "Downloading";
		this.completionText = completionText != null ? completionText : "Download complete!";
	}

	public NotificationSettings() {
		this(false, "Downloading", "Download complete!");
	}
}
