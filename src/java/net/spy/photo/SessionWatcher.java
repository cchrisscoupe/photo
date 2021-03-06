// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import net.spy.log.Logger;
import net.spy.log.LoggerFactory;

/**
 * Watch session creation and destruction and manage sessionData.
 */
public class SessionWatcher extends net.spy.jwebkit.SessionWatcher {

	/**
	 * Called when a session is created.
	 */
	@Override
	public void sessionCreated(HttpSessionEvent se) {
        HttpSession session=se.getSession();

        // Create the session data.
        PhotoSessionData sessionData=new PhotoSessionData();
        // Set the user
		try {
			UserFactory uf=UserFactory.getInstance();
			sessionData.setUser(uf.getUser("guest"));
		} catch(PhotoUserException e) {
			getLogger().warn("Problem setting user", e);
		}
        // The rest of the stuff will remain null until something comes
        // along with something better.
        // Now, add it to the session.
        session.setAttribute(PhotoSessionData.SES_ATTR, sessionData);

		super.sessionCreated(se);
	}

	/**
	 * Get the total number of sessions currently in this engine.
	 */
	public static int totalSessions() {
		return(listSessions().size());
	}

	private static Logger getStaticLogger() {
		Logger log=LoggerFactory.getLogger(SessionWatcher.class);
		return(log);
	}

	/**
	 * Get the SessionData from each session that represents the given
	 * user.
	 */
	public static Collection<PhotoSessionData> getSessionDataByUser(
		String username) {
		ArrayList<PhotoSessionData> al=new ArrayList<PhotoSessionData>();

		for(HttpSession session : listSessions()) {
			try {
				if(session.getAttribute(PhotoSessionData.SES_ATTR) != null) {
					PhotoSessionData sessionData=(PhotoSessionData)
						session.getAttribute(PhotoSessionData.SES_ATTR);

					assert username != null;
					assert sessionData != null;
					assert sessionData.getUser() != null;
					assert sessionData.getUser().getName() != null;

					if(sessionData.getUser().getName().equals(username)) {
						al.add(sessionData);
					} // Found a match
				} else {
					getStaticLogger().warn(
						"Found a session without a photoSession");
				}
			} catch(IllegalStateException e) {
				getStaticLogger().warn("Found invalid session", e);
				SessionStorage.getInstance().removeSession(session);
			}
		} // Flipping through the sessions

		return(al);
	}

	/**
	 * Return the number of sessions containing the given user.
	 */
	public static int getSessionCountByUser(String username) {
		return(getSessionDataByUser(username).size());
	}

	/**
	 * Get a Collection of HttpSession objects representing all users
	 * currently in the system.
	 */
	public static Collection<HttpSession> listSessions() {
		ArrayList<HttpSession> al=new ArrayList<HttpSession>();

		for(HttpSession session : SessionStorage.getInstance().getSessions()) {
			try {
				PhotoSessionData sessionData=(PhotoSessionData)
					session.getAttribute(PhotoSessionData.SES_ATTR);

				if(sessionData!=null) {
					// Only include sessions that are either authenticated or have
					// displayed images.  This helps avoid having inflated lists
					// from monitoring and what-not.
					if(sessionData.getUser().isInRole(User.AUTHENTICATED)
							|| sessionData.getImagesSeen() > 0) {
						// Add the session to the result List
						al.add(session);
					}
				} else {
					getStaticLogger().warn(
						"Found a session without a photoSession");
				}
			} catch(IllegalStateException e) {
				getStaticLogger().debug("Found invalid session", e);
				SessionStorage.getInstance().removeSession(session);
			}
		}

		return(al);
	}

	/** 
	 * Get the PhotoSessionData for the named session.
	 * @return the session, or null if there is none
	 */
	public static PhotoSessionData getSessionData(String id) {
		PhotoSessionData rv=null;

		HttpSession session=SessionStorage.getInstance().getSession(id);
		if(session != null) {
			rv=(PhotoSessionData)
				session.getAttribute(PhotoSessionData.SES_ATTR);
		}

		return(rv);
	}

}
