/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: PhotoLogFlusher.java,v 1.3 1999/10/10 08:03:54 dustin Exp $
 */

import java.sql.*;
import java.lang.*;
import java.util.*;
import java.io.*;

public class PhotoLogFlusher extends Thread {

	public boolean is_running = false;

	private static Connection db;
	// private static BufferedWriter log_file=null;
	private static PhotoLogger log_object;

	public PhotoLogFlusher(ThreadGroup t) {
		super(t, "log_flusher");
		this.setDaemon(true);
	}

	public synchronized void doFlush() {
		Vector v = log_object.flush();
		Statement st = null;
		try {
			st=db.createStatement();
		} catch(SQLException e) {
			System.err.println("BAD LOG ERRROR!  " + e.getMessage());
		}
		for(int i = 0; i<v.size(); i++) {
			PhotoLogEntry l = null;
			try {
				l = (PhotoLogEntry)v.elementAt(i);
				st.executeUpdate(l.toString());
			} catch(SQLException e) {
				System.err.println("Error writing log:  "
					+ l + e.getMessage());
			}
		}
	}

	public void run() {
		is_running = true;

		try {
			PhotoConfig conf = new PhotoConfig();
			Class.forName(conf.dbDriverName);
			db=DriverManager.getConnection(conf.dbSource,
				conf.dbUser, conf.dbPass);
		} catch(Exception e) {
			System.err.println("Error loading postgres driver for logging: "
				+ e.getMessage());
		}
		log_object = new PhotoLogger();

		// System.out.println("Running thread...");

		for(;;) {
			try {
				// Wait a second before continuing
				sleep(1000);
			} catch(Exception e) {
			} finally {
				doFlush();
			}
		}
	}

	public void finalize() throws Throwable {
		doFlush();
		super.finalize();
	}
}
