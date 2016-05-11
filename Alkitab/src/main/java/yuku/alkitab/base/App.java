package yuku.alkitab.base;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import yuku.afw.storage.Preferences;
import yuku.alkitab.base.model.VersionImpl;
import yuku.alkitab.base.sync.Gcm;
import yuku.alkitab.base.sync.Sync;
import yuku.alkitab.debug.R;
import yuku.alkitab.reminder.util.DevotionReminder;
import yuku.alkitabfeedback.FeedbackSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class App extends yuku.afw.App {

	// http://stackoverflow.com/questions/2002288/static-way-to-get-context-on-android
	// for use in AddonManager
	private static Context appctx;

    public static Context getAppContext() {
        return App.appctx;
    }

	public static final String TAG = App.class.getSimpleName();

	private static boolean initted = false;
	private static Tracker APP_TRACKER;

	enum OkHttpClientWrapper {
		INSTANCE;

		OkHttpClient httpClient = new OkHttpClient();
	}

	enum GsonWrapper {
		INSTANCE;

		Gson gson = new Gson();
	}

	public static String downloadString(String url) throws IOException {
		return OkHttpClientWrapper.INSTANCE.httpClient.newCall(new Request.Builder().url(url).build()).execute().body().string();
	}

	public static byte[] downloadBytes(String url) throws IOException {
		return OkHttpClientWrapper.INSTANCE.httpClient.newCall(new Request.Builder().url(url).build()).execute().body().bytes();
	}

	public static Call downloadCall(String url) {
		return OkHttpClientWrapper.INSTANCE.httpClient.newCall(new Request.Builder().url(url).build());
	}

	public static OkHttpClient getOkHttpClient() {
		return OkHttpClientWrapper.INSTANCE.httpClient;
	}

	public static void treecopy(File sourceLocation, File targetLocation) throws IOException {

		if (sourceLocation.isDirectory()) {
			if (!targetLocation.exists()) {
				targetLocation.mkdir();
			}

			String[] children = sourceLocation.list();
			for (int i = 0; i < sourceLocation.listFiles().length; i++) {

				treecopy(new File(sourceLocation, children[i]),
						new File(targetLocation, children[i]));
			}
		} else {

			InputStream in = new FileInputStream(sourceLocation);

			OutputStream out = new FileOutputStream(targetLocation);

			// Copy the bits from instream to outstream
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
		}

	}










	@Override public void onCreate() {
		super.onCreate();

		super.onCreate();
		App.appctx = getApplicationContext();

		staticInit();

		{ // Google Analytics V4
			// This can't be in staticInit because we need the Application instance.
			final GoogleAnalytics analytics = GoogleAnalytics.getInstance(context);
			final Tracker t = analytics.newTracker(context.getString(R.string.ga_trackingId));
			t.enableAutoActivityTracking(true);
			t.enableExceptionReporting(true);
			APP_TRACKER = t;
			analytics.enableAutoActivityReports(this);
		}

		// Move old files to new, app-internal directory
		//   external storage (old method)
		// OLD path 1: return new File(Environment.getExternalStorageDirectory(), "bible/fonts").getAbsolutePath();
		// OLD path 2: return new File(Environment.getExternalStorageDirectory(), "bible/yes").getAbsolutePath();
		//   app-internal storage (new location)
		// NEW path 1: return new File(App.getAppContext().getExternalFilesDir(null), "bible/fonts").getAbsolutePath();
		// NEW path 2: return new File(App.getAppContext().getExternalFilesDir(null), "bible/yes").getAbsolutePath();
		// 1 Check if app-internal bible exists. If not,
		// 2 Check if external storage bible/ exists. If so,
		// 3 move it to app-internal storage.
		/*File olddir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/bible");
		File newdir = new File(App.getAppContext().getExternalFilesDir(null).getAbsolutePath(), "bible");
		if  (!newdir.exists()) {
			// app-internal dir does NOT exist
			if (olddir.isDirectory()) {
				// old dir DOES exist. move data.
				// Caveat: could be someone had a folder named "bible" on sdcard and just installed qibi.
				// uups

				// olddir.renameTo(newdir);
				// HMM old dir is moved, but the app seemingly can't use the data from the new location grrr
			}
		}*/
	}

	public synchronized static void staticInit() {
		if (initted) return;
		initted = true;

		FeedbackSender fs = FeedbackSender.getInstance(context);
		fs.trySend();

		PreferenceManager.setDefaultValues(context, R.xml.settings_display, false);
		PreferenceManager.setDefaultValues(context, R.xml.settings_usage, false);
		PreferenceManager.setDefaultValues(context, R.xml.secret_settings, false);
		PreferenceManager.setDefaultValues(context, R.xml.sync_settings, false);

		updateConfigurationWithPreferencesLocale();

		// all activities need at least the activeVersion from S, so initialize it here.
		synchronized (S.class) {
			if (S.activeVersion == null) {
				S.activeVersion = VersionImpl.getInternalVersion();
			}
		}

		// also pre-calculate calculated preferences value here
		S.calculateAppliedValuesBasedOnPreferences();

		{ // GCM
			Gcm.renewGcmRegistrationIdIfNeeded(Sync::notifyNewGcmRegistrationId);
		}

		DevotionReminder.scheduleAlarm(context);
	}

	private static Locale getLocaleFromPreferences() {
		String lang = Preferences.getString(context.getString(R.string.pref_language_key), context.getString(R.string.pref_language_default));
		if (lang == null || "DEFAULT".equals(lang)) { //$NON-NLS-1$
			lang = Locale.getDefault().getLanguage();
		}

		switch (lang) {
			case "zh-CN":
				return Locale.SIMPLIFIED_CHINESE;
			case "zh-TW":
				return Locale.TRADITIONAL_CHINESE;
			default:
				return new Locale(lang);
		}
	}

	@Override public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		Log.d(TAG, "@@onConfigurationChanged: config changed to: " + newConfig); //$NON-NLS-1$
		updateConfigurationWithPreferencesLocale();
	}

	public static void updateConfigurationWithPreferencesLocale() {
		final Configuration config = context.getResources().getConfiguration();
		final Locale locale = getLocaleFromPreferences();
		if (!U.equals(config.locale.getLanguage(), locale.getLanguage()) || !U.equals(config.locale.getCountry(), locale.getCountry())) {
			Log.d(TAG, "@@updateConfigurationWithPreferencesLocale: locale will be updated to: " + locale); //$NON-NLS-1$

			config.locale = locale;
			context.getResources().updateConfiguration(config, null);
		}
	}

	public static LocalBroadcastManager getLbm() {
		return LocalBroadcastManager.getInstance(context);
	}

	public static Gson getDefaultGson() {
		return GsonWrapper.INSTANCE.gson;
	}

	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	public synchronized static Tracker getTracker() {
		return APP_TRACKER;
	}
}
