// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: SessionWatcher.java,v 1.4 2002/06/11 00:35:01 dustin Exp $

package net.spy.photo;

import java.util.*;

import javax.servlet.http.*;

import net.spy.photo.*;

/**
 * Watch session creation and destruction and manage sessionData.
 */
public class SessionWatcher extends Object implements HttpSessionListener {

	private static Vector allSessions=new Vector();

	/**
	 * Get an instance of SessionWatcher.
	 */
	public SessionWatcher() {
		super();
	}

	/**
	 * Called when a session is created.
	 */
	public void sessionCreated(HttpSessionEvent se) {
        HttpSession session=se.getSession();

        // Create the session data.
        PhotoSessionData sessionData=new PhotoSessionData();
        // Set the user
        sessionData.setUser(Persistent.security.getUser("guest"));
        // The rest of the stuff will remain null until something comes
        // along with something better.
        // Now, add it to the session.
        session.setAttribute("photoSession", sessionData);

        // OK, now add that to our list.
		synchronized(allSessions) {
			allSessions.addElement(se.getSession());
		}
	}

	/**
	 * Called when a session is destroyed.
	 */
	public void sessionDestroyed(HttpSessionEvent se) {
		synchronized(allSessions) {
			allSessions.removeElement(se.getSession());
		}
	}

	/**
	 * Get the total number of sessions currently in this engine.
	 */
	public static int totalSessions() {
		int rv=0;
		synchronized(allSessions) {
			rv=allSessions.size();
		}
		return(rv);
	}

	/**
	 * Get the SessionData from each session that represents the given
	 * user.
	 */
	public static Enumeration getSessionDataByUser(String username) {
		Vector v=new Vector();

		synchronized(allSessions) {
			for(Enumeration e=allSessions.elements(); e.hasMoreElements(); ) {
				HttpSession session=(HttpSession)e.nextElement();

				if(session.getAttribute("photoSession") != null) {
					PhotoSessionData sessionData=
						(PhotoSessionData)session.getAttribute("photoSession");

					// XXX:  I guess it's theoretically possible for some of
					// this stuff to be null.
					if(sessionData.getUser().getUsername().equals(username)) {
						v.addElement(sessionData);
					} // Found a match
				} else {
					System.err.println(
						"Warning:  Found a session without a photoSession");
				}
			} // Flipping through the sessions
		} // allSession lock

		return(v.elements());
	}

	/**
	 * Return the number of sessions containing the given user.
	 */
	public static int getSessionCountByUser(String username) {
		int rv=0;

		for(Enumeration e=getSessionDataByUser(username);e.hasMoreElements();){
			e.nextElement();
            rv++;
        }

		return(rv);
	}

}
