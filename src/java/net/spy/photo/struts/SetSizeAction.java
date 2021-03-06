// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo.struts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.spy.photo.PhotoSessionData;
import net.spy.photo.impl.PhotoDimensionsImpl;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Action to set the size of an image.
 */
public class SetSizeAction extends PhotoAction {

	/**
	 * Set the optimal viewing size.
	 */
	@Override
	public ActionForward spyExecute(ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,HttpServletResponse response)
		throws Exception {

		DynaActionForm ssf=(DynaActionForm)form;

		PhotoSessionData sessionData=getSessionData(request);
		String dims=(String)ssf.get("dims");
		sessionData.setOptimalDimensions(new PhotoDimensionsImpl(dims));

		getLogger().debug("Set viewing size to %s", dims);

		addMessage(request, MessageType.success, "Viewing size set to " + dims);

		return(mapping.findForward("next"));
	}

}
