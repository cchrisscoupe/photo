// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: AddToGalleryAction.java,v 1.3 2002/07/02 07:03:41 dustin Exp $

package net.spy.photo.struts;

import java.io.*;
import java.util.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.apache.struts.action.*;

import net.spy.SpyDB;

import net.spy.photo.*;

/**
 * Action to add an image to a gallery.
 *
 * If a new gallery is not currently being created, a new gallery will be
 * created.
 */
public class AddToGalleryAction extends PhotoAction {

	/**
	 * Get an instance of AddToGalleryAction.
	 */
	public AddToGalleryAction() {
		super();
	}

	/**
	 * Perform the action.
	 */
	public ActionForward perform(ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,HttpServletResponse response)
		throws IOException, ServletException {

		AddToGalleryForm atgf=(AddToGalleryForm)form;
		if(atgf==null) {
			// If we didn't get one, make one here...this is ugly, but
			// whatever.
			atgf=new AddToGalleryForm();
			atgf.setId(Integer.parseInt(request.getParameter("id")));
			ActionErrors e=atgf.validate(mapping, request);
			if(e!=null && e.size()>0) {
				throw new ServletException("Form is broken.");
			}
		}

		PhotoSessionData sessionData=getSessionData(request);
		PhotoUser user=sessionData.getUser();

		HttpSession session=request.getSession(false);

		Gallery g=(Gallery)session.getAttribute("newGallery");
		if(g==null) {
			log("Creating new gallery.");
			// If we don't have a gallery yet, get a new one.
			g=new Gallery(user);
			// Add it to the session.
			session.setAttribute("newGallery", g);
		}

		// Add the new image
		try {
			g.addImage(atgf.getId());
		} catch(Exception e) {
			throw new ServletException("Error adding new image", e);
		}

		return(mapping.findForward("success"));
	}

}
