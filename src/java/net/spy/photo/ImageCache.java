// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo;

/**
 * This interface is used to represent image cache implementations.
 */
public interface ImageCache {

	/**
	 * Get an image by key.
	 */
	byte[] getImage(String key) throws PhotoException;

	/**
	 * Store an image by key.
	 */
	void putImage(String key, byte[] image) throws PhotoException;

}