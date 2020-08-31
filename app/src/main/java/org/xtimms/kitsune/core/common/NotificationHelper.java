package org.xtimms.kitsune.core.common;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import org.xtimms.kitsune.R;
import org.xtimms.kitsune.utils.ThemeUtils;

public final class NotificationHelper {

	private int mId;
	private final Resources mResources;
	private final NotificationManager mManager;
	private final NotificationCompat.Builder mBuilder;

	public NotificationHelper(Context context, String channelId, @StringRes int channelName) {
		this(context, 0, channelId, channelName);
		nextId();
	}

	public NotificationHelper(Context context, int id, String channelId, @StringRes int channelName) {
		mId = id;
		mResources = context.getResources();
		mBuilder = new NotificationCompat.Builder(context, channelId);
		mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createChannel(context, channelId, channelName);
		}
		mBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);
		mBuilder.setColor(ThemeUtils.getThemeAttrColor(context, R.attr.colorAccent));
	}

	public Notification get() {
		return mBuilder.build();
	}

	public int getId() {
		return mId;
	}

	public void setId(int id) {
		mId = id;
	}

	public void update() {
		mManager.notify(mId, get());
	}

	public void setProgress(int progress, int max) {
		mBuilder.setProgress(max, progress, false);
	}

	public void setIndeterminate() {
		mBuilder.setProgress(0, 0, true);
	}

	public void removeProgress() {
		mBuilder.setProgress(0, 0, false);
	}

	public void setTitle(String title) {
		mBuilder.setContentTitle(title);
	}

	public void setTitle(@StringRes int title) {
		mBuilder.setContentTitle(mResources.getString(title));
	}

	public void setText(String text) {
		mBuilder.setContentText(text);
	}

	public void setText(@StringRes int text) {
		mBuilder.setContentText(mResources.getString(text));
	}

	public void setIcon(@DrawableRes int icon) {
		mBuilder.setSmallIcon(icon);
	}

	public void setOngoing() {
		mBuilder.setOngoing(true);
		mBuilder.setAutoCancel(false);
	}

	public void setAutoCancel() {
		mBuilder.setOngoing(false);
		mBuilder.setAutoCancel(true);
	}

	public void nextId() {
		mId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
	}

	public void setImage(@Nullable Bitmap bitmap) {
		if (bitmap == null) {
			mBuilder.setLargeIcon(null);
		} else {
			final int width = mResources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
			final int height = mResources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height);
			final Bitmap thumb = ThumbnailUtils.extractThumbnail(bitmap, width, height);
			mBuilder.setLargeIcon(thumb);
		}
	}

	public void setSubText(@StringRes int subText) {
		mBuilder.setSubText(mResources.getString(subText));
	}

	@SuppressLint("RestrictedApi")
	public void clearActions() {
		mBuilder.mActions.clear();
	}

	public void addCancelAction(PendingIntent pendingIntent) {
		mBuilder.addAction(R.drawable.sym_cancel, mResources.getString(android.R.string.cancel), pendingIntent);
	}

	public void dismiss() {
		mManager.cancel(mId);
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	private void createChannel(Context context, String channelId, @StringRes int channelName) {
		final NotificationChannel channel = new NotificationChannel(
				channelId,
				context.getString(channelName),
				NotificationManager.IMPORTANCE_LOW
		);
		mManager.createNotificationChannel(channel);
	}
}
