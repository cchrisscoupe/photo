/*
 * Copyright (c) 2000  Dustin Sallings <dustin@spy.net>
 *
 * $Id: PhotoSearchResults.java,v 1.10 2001/12/28 04:45:30 dustin Exp $
 */

package net.spy.photo;

import java.util.*;
import java.io.Serializable;

import javax.servlet.*;
import javax.servlet.http.*;

import net.spy.*;

/**
 * Represents results from a search.
 */
public class PhotoSearchResults extends Object implements Serializable {
	private Vector _results=null;
	private int _current=0;
	private String self_uri=null;
	private int maxret=5;

	private PhotoDimensions maxSize=null;

	/**
	 * Get a search results object for the given URI.
	 */
	public PhotoSearchResults(String self_uri) {
		super();
		_results=new Vector();
		this.self_uri=self_uri;
	}

	/**
	 * Add a search result to the list.
	 */
	public void add(PhotoSearchResult r) {
		// Set the result id
		r.setId(_results.size());
		// Tell it the size we want the images
		r.setMaxSize(maxSize);
		// Now add it
		_results.addElement(r);
	}

	/**
	 * Set the maximum image size to be represented.
	 */
	public void setMaxSize(PhotoDimensions maxSize) {
		this.maxSize=maxSize;
	}

	/**
	 * Add a photo ID to the result list.
	 */
	public void add(Integer what) {
		_results.addElement(what);
	}

	/**
	 * Get the URI this result was built from.
	 */
	public String getURI() {
		return(self_uri);
	}

	/**
	 * Set the search result we're lookin' at.
	 */
	public void set(int to) {
		if(to>_results.size()) {
			_current=_results.size();
		} else {
			_current=to;
		}
	}

	/**
	 * Set the maximum number of results for size.
	 */
	public void setMaxRet(int maxret) {
		this.maxret=maxret;
	}

	/**
	 * Get the requested maximum number of results per page.
	 */
	public int getMaxRet() {
		return(maxret);
	}

	/**
	 * String representation of the search results.
	 */
	public String toString() {
		return("Photo search results:\n" + _results);
	}

	/**
	 * Get the current entry.
	 */
	public PhotoSearchResult get() {
		return(get(_current));
	}

	/**
	 * Get the entry at the given location.
	 */
	public PhotoSearchResult get(int which) {
		PhotoSearchResult ret=null;
		Object o=_results.elementAt(which);
		// We hope that it's a PhotoSearchResult, but an Integer will do.
		try {
			ret=(PhotoSearchResult)o;
		} catch(ClassCastException e) {
			try {
				Integer i=(Integer)o;
				ret=new PhotoSearchResult(i.intValue(), which);
				// Add the max dimensions so it'll scale.
				ret.setMaxSize(maxSize);
			} catch(Exception e2) {
				System.err.println("Error getting result "
					+ which + ":  " + e2);
			}
		}
		return(ret);
	}

	/**
	 * Get the next result, or null if we're done
	 */
	public PhotoSearchResult next() {
		PhotoSearchResult r=null;

		if(_current<_results.size()) {
			r=get(_current);
			_current++;
		}
		return(r);
	}

	/**
	 * Get the previous result, or null if we're at the beginning
	 */
	public PhotoSearchResult prev() {
		PhotoSearchResult r=null;

		if(_current>0) {
			_current--;
			r=get(_current);
		}
		return(r);
	}

	/**
	 * Find out how many results total are in this result set.
	 */
	public int nResults() {
		return(_results.size());
	}

	/**
	 * Find out how many results are remaining.
	 */
	public int nRemaining() {
		return(_results.size()-_current);
	}

	/**
	 * Find out which one we're on.
	 */
	public int current() {
		return(_current);
	}
}
