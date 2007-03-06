// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo.impl;

import net.spy.cache.DiskCache;
import net.spy.photo.ImageCache;
import net.spy.photo.PhotoConfig;
import net.spy.photo.PhotoException;

/**
 * ImageCache that uses local files.
 */
public class LocalImageCacheImpl extends Object implements ImageCache {

	private DiskCache cache=null;

	/**
	 * Get an instance of LocalImageCacheImpl.
	 */
	public LocalImageCacheImpl() {
		super();

		PhotoConfig conf=PhotoConfig.getInstance();
		cache=new DiskCache(conf.get("cache.dir", "/var/tmp/photocache"),
			conf.getInt("cache.dcache.lrusize", 100));
	}

	/**
	 * @see ImageCache
	 */
	public byte[] getImage(String key) throws PhotoException {
		byte[] rv=(byte[])cache.get(key);
		return(rv);
	}

	/**
	 * @see ImageCache
	 */
	public void putImage(String key, byte[] image) throws PhotoException {
		cache.put(key, image);
	}

}

