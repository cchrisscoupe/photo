// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: PhotoDimensionsImpl.java,v 1.1 2001/12/28 01:54:13 dustin Exp $

package net.spy.photo;

/**
 * A class that contains only a width and height.
 */
public class PhotoDimensionsImpl implements PhotoDimensions {

	private int width=0;
	private int height=0;

	/**
	 * Get a dimensions set.
	 */
	public PhotoDimensionsImpl(int w, int h) {
		super();
		this.width=w;
		this.height=h;
	}

	/**
	 * String me.
	 */
	public String toString() {
		return("PhotoDimensionsImpl@" + width + "x" + height);
	}

	/**
	 * Get the width of the set.
	 */
	public int getWidth() {
		return(width);
	}
	/**
	 * Get the height of the set.
	 */
	public int getHeight() {
		return(height);
	}

}
