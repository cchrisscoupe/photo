// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: ImgSrcTag.java,v 1.1 2002/06/18 06:34:50 dustin Exp $

package net.spy.photo.taglib;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import net.spy.photo.PhotoUtil;

/**
 * Taglib to link to an image.
 */
public class ImgSrcTag extends PhotoTag {

	private String url=null;
	private String border=null;
	private String width=null;
	private String height=null;
	private String alt=null;

	/**
	 * Get an instance of ImageLink.
	 */
	public ImgSrcTag() {
		super();
		release();
	}

	/**
	 * Set the relative URL to which to link.
	 */
	public void setUrl(String url) {
		this.url=url;
	}

	/**
	 * Set a border setting for this image.
	 */
	public void setBorder(String border) {
		this.border=border;
	}

	/**
	 * Set the width of this image.
	 */
	public void setWidth(String width) {
		this.width=width;
	}

	/**
	 * Set the height of this image.
	 */
	public void setHeight(String height) {
		this.height=height;
	}

	/**
	 * Set the alt text for this image link.
	 */
	public void setAlt(String alt) {
		this.alt=alt;
	}

	/**
	 * Start link.
	 */
	public int doStartTag() throws JspException {

		StringBuffer sb=new StringBuffer();
		sb.append("<img src=\"");

		HttpServletRequest req=(HttpServletRequest)pageContext.getRequest();
		sb.append(PhotoUtil.getRelativeUri(req, url));
		sb.append("\"");

		if(border!=null) {
			sb.append(" border=\"");
			sb.append(border);
			sb.append("\""); 
		}

		if(width!=null) {
			sb.append(" width=\"");
			sb.append(width);
			sb.append("\""); 
		}

		if(height!=null) {
			sb.append(" height=\"");
			sb.append(height);
			sb.append("\""); 
		}

		if(alt!=null) {
			sb.append(" alt=\"");
			sb.append(alt);
			sb.append("\""); 
		}

		sb.append("/>");

		try {
			pageContext.getOut().write(sb.toString());
		} catch(Exception e) {
			e.printStackTrace();
			throw new JspException("Error sending output:  " + e);
		}

		return(EVAL_BODY_INCLUDE);
	}

	/**
	 * Reset all values.
	 */
	public void release() {
		url=null;
	}

}
