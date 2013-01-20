package za.dams.paracrm;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

public class CrmImageLoader {
    private static final String LOG_TAG = "CrmImageLoader";
    private static final int RES_LOADING = R.drawable.ic_explorer_fileicon ;
    private static final int RES_ERROR = R.drawable.crm_missing ;
    
    private final Context mContext ;
    private final Bitmap mBitmapLoading ;
    private final Bitmap mBitmapError ;

    public static class CrmUrl {
    	public String syncVuid ;
    	public boolean thumbnail ;
    	
		public boolean equals( Object o ) {
			CrmUrl cu = (CrmUrl)o ;
			if( !this.syncVuid.equals(cu.syncVuid) ) {
				return false ;
			}
			if( this.thumbnail != cu.thumbnail ) {
				return false ;
			}
			return true ;
		}
		public int hashCode() {
			int result = 17 ;
			
			result = 31 * result + syncVuid.hashCode() ;
			result = 31 * result + (thumbnail?1:0) ;
			
			return result ;
		}
    	public String toString() {
    		String retStr = "syncVuid="+syncVuid+" thumbnail="+(thumbnail?"yes":"no") ;
    		return retStr ;
    	}
    }
    public interface Callback {
    	public void onImageLoaded( ImageView imageView ) ;
    }
    
    
    
    public CrmImageLoader( Context c ) {
    	mContext = c ;
    	mBitmapLoading = ((BitmapDrawable)c.getResources().getDrawable(RES_LOADING)).getBitmap() ;
    	mBitmapError = ((BitmapDrawable)c.getResources().getDrawable(RES_ERROR)).getBitmap() ;
    }
    
    
    /**
     * Download the specified image from the Internet and binds it to the provided ImageView. The
     * binding is immediate if the image is found in the cache and will be done asynchronously
     * otherwise. A null bitmap will be associated to the ImageView if an error occurs.
     *
     * @param url The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void download(CrmUrl crmUrl, ImageView imageView) {
        resetPurgeTimer();
        Bitmap bitmap = getBitmapFromCache(crmUrl);

        if (bitmap == null) {
            if (cancelPotentialDownload(crmUrl, imageView)) {
            	BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
            	DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
            	imageView.setImageDrawable(downloadedDrawable);
            	imageView.setMinimumHeight(156);
            	task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,crmUrl);
            }
        } else {
            cancelPotentialDownload(crmUrl, imageView);
            imageView.setImageBitmap(bitmap);
        }
    }

    /*
     * Same as download but the image is always downloaded and the cache is not used.
     * Kept private at the moment as its interest is not clear.
       private void forceDownload(String url, ImageView view) {
          forceDownload(url, view, null);
       }
     */


    /**
     * Returns true if the current download has been canceled or if there was no download in
     * progress on this image view.
     * Returns false if the download in progress deals with the same url. The download is not
     * stopped in that case.
     */
    private static boolean cancelPotentialDownload(CrmUrl crmUrl, ImageView imageView) {
        BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

        if (bitmapDownloaderTask != null) {
            CrmUrl bitmapCrmUrl = bitmapDownloaderTask.crmUrl;
            if ((bitmapCrmUrl == null) || (!bitmapCrmUrl.equals(crmUrl))) {
                bitmapDownloaderTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active download task (if any) associated with this imageView.
     * null if there is no such task.
     */
    private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof DownloadedDrawable) {
                DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
                return downloadedDrawable.getBitmapDownloaderTask();
            }
        }
        return null;
    }

    Bitmap downloadBitmap(CrmUrl crmUrl) {
        final int IO_BUFFER_SIZE = 4 * 1024;

        // AndroidHttpClient is not allowed to be used from the main thread
        final HttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpPost postRequest = new HttpPost(mContext.getString(R.string.server_url));

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("_domain", "paramount"));
            nameValuePairs.add(new BasicNameValuePair("_moduleName", "paracrm"));
            nameValuePairs.add(new BasicNameValuePair("_action", "android_imgPull"));
            nameValuePairs.add(new BasicNameValuePair("sync_vuid", crmUrl.syncVuid));
            nameValuePairs.add(new BasicNameValuePair("thumbnail", crmUrl.thumbnail?"O":"N"));
			postRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        	
            HttpResponse response = client.execute(postRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                Log.w(LOG_TAG, "Error " + statusCode +
                        " while retrieving bitmap from " + crmUrl.toString());
                return mBitmapError;
            }

            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent();
                    
                    /*
    				BufferedReader reader = new BufferedReader(
    						new InputStreamReader(inputStream));
    				String line;
    				Log.w(LOG_TAG,"Decoding ") ;
    				while ((line = reader.readLine()) != null) {
    					Log.w("TAGSSJK",line) ;
    				}
    				*/
    				
                    // return BitmapFactory.decodeStream(inputStream);
                    // Bug on slow connections, fixed in future release.
                    Bitmap decodedBitmap =  BitmapFactory.decodeStream(new FlushedInputStream(inputStream));
                    if( decodedBitmap==null ) {
                        Log.w(LOG_TAG, "Error " + statusCode +
                                " while decoding bitmap from " + crmUrl.toString());
                    	return mBitmapError ;
                    }
                    return decodedBitmap ;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    entity.consumeContent();
                }
            }
        } catch (IOException e) {
        	postRequest.abort();
            Log.w(LOG_TAG, "I/O error while retrieving bitmap from " + crmUrl.toString(), e);
        } catch (IllegalStateException e) {
        	postRequest.abort();
            Log.w(LOG_TAG, "Incorrect URL: " + crmUrl.toString());
        } catch (Exception e) {
        	postRequest.abort();
            Log.w(LOG_TAG, "Error while retrieving bitmap from " + crmUrl.toString(), e);
        } finally {
            if ((client instanceof AndroidHttpClient)) {
                ((AndroidHttpClient) client).close();
            }
        }
        return mBitmapError;
    }

    /*
     * An InputStream that skips the exact number of bytes provided, unless it reaches EOF.
     */
    static class FlushedInputStream extends FilterInputStream {
        public FlushedInputStream(InputStream inputStream) {
            super(inputStream);
        }

        @Override
        public long skip(long n) throws IOException {
            long totalBytesSkipped = 0L;
            while (totalBytesSkipped < n) {
                long bytesSkipped = in.skip(n - totalBytesSkipped);
                if (bytesSkipped == 0L) {
                    int b = read();
                    if (b < 0) {
                        break;  // we reached EOF
                    } else {
                        bytesSkipped = 1; // we read one byte
                    }
                }
                totalBytesSkipped += bytesSkipped;
            }
            return totalBytesSkipped;
        }
    }

    /**
     * The actual AsyncTask that will asynchronously download the image.
     */
    class BitmapDownloaderTask extends AsyncTask<CrmUrl, Void, Bitmap> {
        private CrmUrl crmUrl;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapDownloaderTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        /**
         * Actual download method.
         */
        @Override
        protected Bitmap doInBackground(CrmUrl... params) {
            crmUrl = params[0];
            return downloadBitmap(crmUrl);
        }

        /**
         * Once the image is downloaded, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            addBitmapToCache(crmUrl, bitmap);

            if (imageViewReference != null) {
                ImageView imageView = imageViewReference.get();
                BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
                // Change bitmap only if this process is still associated with it
                // Or if we don't use any bitmap to task association (NO_DOWNLOADED_DRAWABLE mode)
                if( this == bitmapDownloaderTask ) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }


    /**
     * A fake Drawable that will be attached to the imageView while the download is in progress.
     *
     * <p>Contains a reference to the actual download task, so that a download task can be stopped
     * if a new binding is required, and makes sure that only the last started download process can
     * bind its result, independently of the download finish order.</p>
     */
    private class DownloadedDrawable extends BitmapDrawable {
        private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

        public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
            super(mContext.getResources(),mBitmapLoading);
            bitmapDownloaderTaskReference =
                new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
        }

        public BitmapDownloaderTask getBitmapDownloaderTask() {
            return bitmapDownloaderTaskReference.get();
        }
    }

    
    /*
     * Cache-related fields and methods.
     * 
     * We use a hard and a soft cache. A soft reference cache is too aggressively cleared by the
     * Garbage Collector.
     */
    
    private static final int HARD_CACHE_CAPACITY = 10;
    private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds

    // Hard cache, with a fixed maximum capacity and a life duration
    private final HashMap<CrmUrl, Bitmap> sHardBitmapCache =
        new LinkedHashMap<CrmUrl, Bitmap>(HARD_CACHE_CAPACITY / 2, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(LinkedHashMap.Entry<CrmUrl, Bitmap> eldest) {
            if (size() > HARD_CACHE_CAPACITY) {
                // Entries push-out of hard reference cache are transferred to soft reference cache
                sSoftBitmapCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
                return true;
            } else
                return false;
        }
    };

    // Soft cache for bitmaps kicked out of hard cache
    private final static ConcurrentHashMap<CrmUrl, SoftReference<Bitmap>> sSoftBitmapCache =
        new ConcurrentHashMap<CrmUrl, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);

    private final Handler purgeHandler = new Handler();

    private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };

    /**
     * Adds this bitmap to the cache.
     * @param bitmap The newly downloaded bitmap.
     */
    private void addBitmapToCache(CrmUrl crmUrl, Bitmap bitmap) {
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                sHardBitmapCache.put(crmUrl, bitmap);
            }
        }
    }

    /**
     * @param url The URL of the image that will be retrieved from the cache.
     * @return The cached bitmap or null if it was not found.
     */
    private Bitmap getBitmapFromCache(CrmUrl crmUrl) {
        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final Bitmap bitmap = sHardBitmapCache.get(crmUrl);
            if (bitmap != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(crmUrl);
                sHardBitmapCache.put(crmUrl, bitmap);
                return bitmap;
            }
        }

        // Then try the soft reference cache
        SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(crmUrl);
        if (bitmapReference != null) {
            final Bitmap bitmap = bitmapReference.get();
            if (bitmap != null) {
                // Bitmap found in soft cache
                return bitmap;
            } else {
                // Soft reference has been Garbage Collected
                sSoftBitmapCache.remove(crmUrl);
            }
        }

        return null;
    }
 
    /**
     * Clears the image cache used internally to improve performance. Note that for memory
     * efficiency reasons, the cache will automatically be cleared after a certain inactivity delay.
     */
    public void clearCache() {
        sHardBitmapCache.clear();
        sSoftBitmapCache.clear();
    }

    /**
     * Allow a new delay before the automatic cache clear is done.
     */
    private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }
}
