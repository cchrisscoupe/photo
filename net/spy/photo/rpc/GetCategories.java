// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: GetCategories.java,v 1.1 2002/09/17 23:40:47 dustin Exp $

package net.spy.photo.rpc;

import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;

import net.spy.photo.Category;
import net.spy.photo.Persistent;
import net.spy.photo.PhotoException;
import net.spy.photo.PhotoImage;
import net.spy.photo.PhotoLogUploadEntry;
import net.spy.photo.PhotoSaver;
import net.spy.photo.PhotoUser;


/**
 * Get the categories a user has access to.
 */
public class GetCategories extends RPCMethod {

	/**
	 * Get an instance of GetCategories.
	 */
	public GetCategories() {
		super();
	}

	/** 
	 * Get the names of the categories to which the user has add access.
	 * 
	 * @param username username
	 * @param password password
	 * @return Vector of Strings
	 * @throws PhotoException if there's trouble getting the cat list
	 */
	public Vector getAddable(String username, String password)
		throws PhotoException {
		// Authenticate the user
		authenticate(username, password);

		Vector rv=new Vector();

		PhotoUser u=getUser();
		Collection cats=Category.getCatList(u.getId(), Category.ACCESS_WRITE);
		for(Iterator i=cats.iterator(); i.hasNext();) {
			Category c=(Category)i.next();
			rv.addElement(c.getName());
		}

		return(rv);
	}
}

