package com.aviary.android.feather.utils;

import java.util.HashMap;
import java.util.LinkedHashMap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.support.v4.util.LruCache;

public class SimpleBitmapCache {

	private static final int DELAY_BEFORE_PURGE = 30 * 1000;
	private static final int HARD_CACHE_CAPACITY = 4;
	private static final int SIZE_OF_LRU_CACHE = 4 * 1024 * 1024;      // 4MB
	private final Handler purgeHandler = new Handler();

	private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>( HARD_CACHE_CAPACITY / 2, 0.75f, true ) {

		private static final long serialVersionUID = 7320831300767054723L;

		@Override
		protected boolean removeEldestEntry( LinkedHashMap.Entry<String, Bitmap> eldest ) {
			if ( size() > HARD_CACHE_CAPACITY ) {
				sSoftBitmapCache.put( eldest.getKey(), eldest.getValue() );
				return true;
			} else
				return false;
		}
	};

	private static LruCache<String, Bitmap> sSoftBitmapCache = null;

	public SimpleBitmapCache() {
	    sSoftBitmapCache = new LruCache<String, Bitmap>(SIZE_OF_LRU_CACHE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return getBitmapSize(bitmap);
            }
        };
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public static int getBitmapSize(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= 11) {
            return bitmap.getByteCount();
        } else {
            return bitmap.getRowBytes() * bitmap.getHeight();
        }
  }

	public Bitmap getBitmapFromCache( String url ) {
		synchronized ( sHardBitmapCache ) {
			final Bitmap bitmap = sHardBitmapCache.get( url );
			if ( bitmap != null ) {
				sHardBitmapCache.remove( url );
				sHardBitmapCache.put( url, bitmap );
				return bitmap;
			}
		}

		final Bitmap bitmap = sSoftBitmapCache.get( url );
        if ( bitmap != null ) {
            return bitmap;
        } else {
            sSoftBitmapCache.remove( url );
        }

		return null;
	}

	public void addBitmapToCache( String url, Bitmap bitmap ) {
		if ( bitmap != null ) {
			synchronized ( sHardBitmapCache ) {
				sHardBitmapCache.put( url, bitmap );
			}
		}
	}

	public void clearCache() {
		sHardBitmapCache.clear();
		sSoftBitmapCache.evictAll();
	}

	public void resetPurgeTimer() {
		purgeHandler.removeCallbacks( mPurger );
		purgeHandler.postDelayed( mPurger, DELAY_BEFORE_PURGE );
	}

	private final Runnable mPurger = new Runnable() {

		@Override
		public void run() {
			clearCache();
		}
	};
}
