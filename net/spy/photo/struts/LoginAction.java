// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: LoginAction.java,v 1.8 2002/06/22 23:15:06 dustin Exp $

package net.spy.photo.struts;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.*;

import net.spy.photo.*;

/**
 * Validate user credentials and perform a login.
 */
public class LoginAction extends PhotoAction {

	/**
	 * Get an instance of LoginAction.
	 */
	public LoginAction() {
		super();
	}

	/**
	 * Process the request.
	 */
	public ActionForward perform(ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,HttpServletResponse response)
		throws IOException, ServletException {

		ActionForward rv=null;

		LoginForm lf=(LoginForm)form;

		PhotoSessionData sessionData=getSessionData(request);

		PhotoUser user=Persistent.getSecurity().getUser(lf.getUsername());
		if(user==null) {
			throw new ServletException(
				"Your username or password is incorrect.");
		}

		if(user.checkPassword(lf.getPassword())) {
			sessionData.setUser(user);

			PhotoLogEntry ple=new PhotoLogEntry(user.getId(), "Login",request);
			Persistent.getLogger().log(ple);
		} else {
			PhotoLogEntry ple=new PhotoLogEntry(
				user.getId(), "AuthFail", request);
			Persistent.getLogger().log(ple);
			throw new ServletException(
				"Your username or password is incorrect.");
		}

		// Find out of the user wanted to upgrade to admin privs after 
		if(lf.getAdmin()) {
			rv=mapping.findForward("setadmin");
		} else {
			rv=mapping.findForward("success");
		}

		// Go ahead and say it's alright.
		return(rv);
	}

}
