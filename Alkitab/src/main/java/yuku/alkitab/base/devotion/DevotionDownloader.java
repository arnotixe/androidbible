package yuku.alkitab.base.devotion;

import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import yuku.alkitab.base.App;
import yuku.alkitab.base.S;
import yuku.alkitab.base.ac.DevotionActivity;
import yuku.alkitab.debug.R;

import java.io.IOException;
import java.util.LinkedList;

public class DevotionDownloader extends Thread {
	private static final String TAG = DevotionDownloader.class.getSimpleName();
	
	public interface DownloadStatusListener {
		void onDownloadStatus(String s);
	}
	
	private Context context_;
	private DownloadStatusListener listener_;
	private LinkedList<DevotionArticle> queue_ = new LinkedList<>();
	private boolean idle_;

	public DevotionDownloader(Context context, DownloadStatusListener listener) {
		context_ = context;
		listener_ = listener;
	}
	
	public void setListener(DownloadStatusListener listener) {
		this.listener_ = listener;
	}
	
	public synchronized boolean add(DevotionArticle article, boolean prioritize) {
		if (queue_.contains(article)) return false;
		
		if (prioritize) {
			queue_.addFirst(article);
		} else {
			queue_.add(article);
		}
		
		return true;
	}
	
	private synchronized DevotionArticle dequeue() {
		while (true) {
			if (queue_.size() == 0) {
				return null;
			}
			
			DevotionArticle article = queue_.getFirst();
			queue_.removeFirst();
			
			if (article.getReadyToUse()) {
				continue;
			}
			
			return article;
		}
	}
	
	public void interruptWhenIdle() {
		if (idle_) {
			this.interrupt();
		}
	}
	
	@Override
	public void run() {
		while (true) {
			DevotionArticle article = dequeue();
			
			if (article == null) {
				try {
					idle_ = true;
					Thread.sleep(60000);
				} catch (InterruptedException e) {
					Log.d(TAG, "Downloader is awaken from sleep"); //$NON-NLS-1$
				} finally {
					idle_ = false;
				}
			} else {
				final DevotionActivity.DevotionKind kind = article.getKind();
				final String url = "https://alkitab-host.appspot.com/devotion/get?name=" + kind.name + "&date=" + article.getDate() + "&app_versionCode=" + App.getVersionCode() + "&app_versionName=" + Uri.encode(App.getVersionName());

				Log.d(TAG, "Downloader starts downloading name=" + kind.name + " date=" + article.getDate());
				listener_.onDownloadStatus(context_.getString(R.string.mengunduh_namaumum_tgl_tgl, kind.title, article.getDate()));

				try {
					final String output = App.downloadString(url);

					// success!
					listener_.onDownloadStatus(context_.getString(R.string.berhasil_mengunduh_namaumum_tgl_tgl, kind.title, article.getDate()));

					article.fillIn(output);
					if (output.startsWith("NG")) { //$NON-NLS-1$
						listener_.onDownloadStatus(context_.getString(R.string.kesalahan_dalam_mengunduh_namaumum_tgl_tgl_output, kind.title, article.getDate(), output));
					}

					// let's now store it to db
					S.getDb().storeArticleToDevotions(article);
				} catch (IOException e) {
					Log.w(TAG, "@@run", e); //$NON-NLS-1$

					listener_.onDownloadStatus(context_.getString(R.string.gagal_mengunduh_namaumum_tgl_tgl, kind.title, article.getDate()));
					Log.d(TAG, "Downloader failed to download"); //$NON-NLS-1$
				}
			}
			
			SystemClock.sleep(1000);
		}
	}
}
