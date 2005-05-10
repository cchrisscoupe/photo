// Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
// arch-tag: 50F82D92-5D6E-11D9-889A-000A957659CC

package net.spy.photo.taglib;

import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.VariableInfo;

/**
 * Variables that get set to hold meta info.
 */
public class MetaInfoExtraInfo extends TagExtraInfo {

	/** 
	 * Get the variable info.
	 */
	public VariableInfo[] getVariableInfo(TagData data) {
		return(new VariableInfo[] {
			new VariableInfo(
				"metaShown", "java.lang.Integer", true, VariableInfo.NESTED),
			new VariableInfo(
				"metaImages", "java.lang.Integer", true, VariableInfo.NESTED)});
	}

}