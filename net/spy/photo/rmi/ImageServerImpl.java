// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ImageServerImpl.java,v 1.1 2002/06/16 08:09:14 dustin Exp $

package net.spy.photo.rmi;

import java.rmi.Naming;
import java.rmi.RemoteException;

import net.spy.photo.*;

/**
 * Implementation of ImageServer as an RMI client.
 */
public class ImageServerImpl extends Object implements ImageServer {

	private static RemoteImageServer server = null;

	/**
	 * Get an instance of ImageServerImpl.
	 */
	public ImageServerImpl() {
		super();
	}

	/**
	 * @see ImageServer
	 */
	public PhotoImage getImage(int image_id, PhotoDimensions dim)
		throws PhotoException {

		PhotoImage rv=null;

		ensureConnected();

		try {
			rv=server.getImage(image_id, dim);
		} catch(RemoteException e) {
			throw new PhotoException("Error saving image.", e);
		}

		return(rv);
	}

	/**
	 * @see ImageServer
	 */
	public PhotoImage getThumbnail(int image_id) throws PhotoException {
		PhotoConfig conf=new PhotoConfig();
		PhotoDimensions pdim=
			new PhotoDimensionsImpl(conf.get("thumbnail_size"));
		return(getImage(image_id, pdim));
	}

	/**
	 * @see ImageServer
	 */
	public void storeImage(int image_id, PhotoImage image)
		throws PhotoException {

		ensureConnected();
		log("Storing image " + image_id);
		try {
			server.storeImage(image_id, image);
		} catch(RemoteException e) {
			throw new PhotoException("Error saving image", e);
		}
		log("Stored image " + image_id);
	}

	// Make sure the connection to the remote server is maintained.
	private static synchronized void ensureConnected() throws PhotoException {
		boolean needconn=true;

		try {
			if(server.ping()) {
				needconn=false;
			}
		} catch(Exception e) {
			// Need a new connection
		}

		if(needconn) {
			PhotoConfig conf=new PhotoConfig();
			String serverPath=conf.get("imageserver");
			log("Connecting to RemoteImageServer at " + serverPath);
			try {
				server=(RemoteImageServer)Naming.lookup(serverPath);
			} catch(Exception e) {
				throw new PhotoException(
					"Error getting RemoteImageServer from " + serverPath, e);
			}
		}
	}

	// log a message.
	private static void log(String msg) {
		System.err.println(msg);
	}

}
