// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
// arch-tag: 2F1649D7-5D6E-11D9-944D-000A957659CC

package net.spy.photo.struts;

import java.io.IOException;

import java.sql.PreparedStatement;

import java.util.Iterator;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.spy.db.SpyDB;
import net.spy.db.Saver;

import net.spy.photo.Persistent;
import net.spy.photo.PhotoConfig;
import net.spy.photo.PhotoSessionData;
import net.spy.photo.User;
import net.spy.photo.UserFactory;
import net.spy.photo.MutableUser;
import net.spy.photo.PhotoUserException;
import net.spy.photo.NoSuchPhotoUserException;
import net.spy.photo.Profile;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.DynaValidatorForm;

/**
 * Create a user from a profile.
 */
public class NewUserAction extends PhotoAction {

	/**
	 * Get an instance of NewUserAction.
	 */
	public NewUserAction() {
		super();
	}

	/**
	 * Process the request.
	 */
	public ActionForward spyExecute(ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,HttpServletResponse response)
		throws Exception {

		DynaValidatorForm nuf=(DynaValidatorForm)form;

		// Get the profile
		Profile p=new Profile((String)nuf.get("profile"));

		UserFactory uf=UserFactory.getInstance();

		// Verify the user doesn't already exist.
		String username=(String)nuf.get("username");
		try {
			uf.getUser(username);
			throw new ServletException("User " + username + " already exists.");
		} catch(NoSuchPhotoUserException e) {
			// This is supposed to happen
		}

		// Get the new user and fill it with the data from the form
		MutableUser pu=uf.createNew();
		pu.setName(username);
		pu.setPassword((String)nuf.get("password"));
		pu.setRealname((String)nuf.get("realname"));
		pu.setEmail((String)nuf.get("email"));

		// Populate the ACL entries.
		for(Iterator it=p.getACLEntries().iterator(); it.hasNext();) {
			Integer i=(Integer)it.next();
			pu.getACL().addViewEntry(i.intValue());
		}

		// Save the user.
		uf.persist(pu);

		// Get the session data and assign the new credentials
		PhotoSessionData sessionData=getSessionData(request);
		sessionData.setUser(uf.getUser(username));

		// Try to log it.
		SpyDB db=new SpyDB(PhotoConfig.getInstance());
		PreparedStatement pst=db.prepareStatement(
			"insert into user_profile_log"
			+ "(profile_id, wwwuser_id, remote_addr) "
			+ "values(?,?,?)"
			);
		pst.setInt(1, p.getId());
		pst.setInt(2, pu.getId());
		pst.setString(3, request.getRemoteAddr());
		pst.executeUpdate();
		pst.close();
		db.close();

		return(mapping.findForward("next"));
	}

}
