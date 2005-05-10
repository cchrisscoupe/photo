<%@ page import="net.spy.photo.Category" %>
<%@ page import="net.spy.photo.CategoryFactory" %>
<%@ taglib uri='/tlds/struts-logic.tld' prefix='logic' %>
<%@ taglib uri='/tlds/struts-html.tld' prefix='html' %>
<%@ taglib uri='/tlds/struts-bean.tld' prefix='bean' %>
<%@ taglib uri='/tlds/photo.tld' prefix='photo' %>

<p>

<div class="sectionheader">Category Administration</div>

<html:form action="/admcatedit">
	<html:select property="catId">
		<html:option value="-1">New Category</html:option>
		<logic:iterate id="cat" type="net.spy.photo.Category"
			collection="<%= CategoryFactory.getInstance().getAdminCatList() %>">

			<html:option value="<%= "" + cat.getId() %>"><%= cat.getName() %>
			</html:option>
		</logic:iterate>
	</html:select>

	<html:submit>Edit</html:submit>
</html:form>
<%-- arch-tag: C6C09115-5D6F-11D9-BE9B-000A957659CC --%>