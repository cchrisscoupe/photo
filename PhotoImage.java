/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: PhotoImage.java,v 1.4 1999/09/29 07:37:23 dustin Exp $
 */

import java.io.*;
import java.sql.*;
import java.util.*;
import sun.misc.*;

import com.javaexchange.dbConnectionBroker.*;

// The class
public class PhotoImage extends PhotoHelper
{ 
	RHash rhash;
	Vector image_data;
	boolean wascached;

	private void getRhash() {
		// Get an rhash to cache images and shite.
		try {
			rhash = new RHash("//dhcp-104/RObjectServer");
		} catch(Exception e) {
			rhash = null;
		}
	}

	public PhotoImage() throws Exception {
		super();
		image_data = null;
		getRhash();
	}

	public PhotoImage(DbConnectionBroker db, RHash r) {
		super(db);
		image_data = null;
		rhash=r;
	}

	// Find out if the last fetch was cached.
	public boolean wasCached() {
		return(wascached);
	}

	// Show an image
	public Vector fetchImage(int id) throws Exception {

		String query, key;
		BASE64Decoder base64 = new BASE64Decoder();
		image_data = new Vector();

		image_data = null;

		key = "photo_" + id;

		if(rhash!=null) {
			image_data = (Vector)rhash.get(key);
			wascached=true;
		} else {
			log("No rhash for image cache, must use database directly");
			wascached=false;
		}

		if(image_data==null) {

			image_data = new Vector();
			Connection photo;
			try {
				photo=getDBConn();
			} catch(Exception e) {
				throw new Exception("Can't get database connection: "
					+ e.getMessage());
			}
			query = "select * from image_store where id = " + id +
				" order by line";

			System.out.print("Doing query:  " + query + "\n");
		
			try {

				Statement st = photo.createStatement();
				ResultSet rs = st.executeQuery(query);

				log("Getting image " + id + " from database.");
				wascached=false;

				while(rs.next()) {
					byte data[];
					data=base64.decodeBuffer(rs.getString(3));
					image_data.addElement(data);
				}
				if(rhash != null) {
					log("Storing " + key + " in RHash");
					rhash.put(key, image_data);
				} else {
					log("No RHash, can't cache data.");
				}
			} catch(Exception e) {
				throw new Exception("Problem getting image: " +
					e.getMessage());
			}
			finally { freeDBConn(photo); }
		} else {
			log("Got image " + id + " from RHash.");
		}

		return(image_data);
	}
}
