// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: AddCheck.java,v 1.2 2002/07/10 03:38:09 dustin Exp $

package net.spy.photo.taglib;

import javax.servlet.jsp.JspException;

/**
 * Conditional based on a user's canAdd flag.
 */
public class AddCheck extends ConditionalTag {

	/**
	 * Get an instance of AddCheck.
	 */
	public AddCheck() {
		super();
	}

	/**
	 * If the user can add and negate is false, process the body.
	 */
	public int doStartTag() throws JspException {
		// Find out if the user can add.
		boolean canAdd=getSessionData().getUser().canAdd();
		// Get the return value based on this truth.
		int rv=getReturnValue(canAdd);

		return(rv);
	}

}