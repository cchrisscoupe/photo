// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>
//
// $Id: MetaInfo.java,v 1.1 2002/05/16 04:30:15 dustin Exp $

package net.spy.photo.taglib;

import java.text.*;
import java.sql.*;

import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

import net.spy.*;
import net.spy.cache.*;

import net.spy.photo.*;

/**
 * Meta information about the site.
 */
public class MetaInfo extends PhotoTag {

	/**
	 * Get an instance of MetaInfo.
	 */
	public MetaInfo() {
		super();
	}

	private int getCount(String query) throws Exception {
		String key="photo.metainfo." + query;

		SpyCache cache=new SpyCache();

		int rv=-1;

		Integer rvi=(Integer)cache.get(key);
		if(rvi==null) {
			SpyDB db=new SpyDB(new PhotoConfig());
			ResultSet rs=db.executeQuery(query);
			rs.next();
			rv=rs.getInt(1);
			// Store it for an hour.
			cache.store(key, new Integer(rv), 3600*1000);
			db.close();
		} else {
			rv=rvi.intValue();
		}

		return(rv);
	}

	/**
	 * Get the meta info and shove it into the webpage.
	 */
	public int doStartTag() throws JspException {
		try {
			// get the number formatter.
			NumberFormat nf=NumberFormat.getNumberInstance();

			int totalShown=getCount(
				"select count(*) from photo_logs\n"
					+ " where log_type=get_log_type('ImgView')");
			int totalImages=getCount("select count(*) from album");

			String out="This database contains about "
				+ nf.format(totalImages)
				+ " images and has displayed about "
				+ nf.format(totalShown)
				+ ".";
			if(out!=null) {
				pageContext.getOut().write(out);
			}
		} catch(Exception e) {
			e.printStackTrace();
			throw new JspException("Error sending output.");
		}

		return(EVAL_BODY_INCLUDE);
	}

}