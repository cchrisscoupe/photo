<%@ page import="net.spy.photo.CategoryFactory" %>
<%@ taglib uri='http://jakarta.apache.org/struts/tags-logic' prefix='logic' %>
<%@ taglib uri='http://jakarta.apache.org/struts/tags-html' prefix='html' %>
<%@ taglib uri='http://jakarta.apache.org/struts/tags-bean' prefix='bean' %>
<%@ taglib uri='/tlds/photo.tld' prefix='photo' %>

<%
	String lastProfile=(String)request.getAttribute("net.spy.photo.ProfileId");
%>

<% if(lastProfile!=null) { %>

<p>
	Profile created, ID is <code><%= lastProfile %></code>.
	<hr/>
</p>

<% } %>

<p>

<div class="sectionheader">New Profile</div>

<html:form action="/admnewprofile">

	Profile description: <html:text property="name"/><br/>

	<table border="1">

	<tr>
		<th>Name</th>
		<th>Can View</th>
	</tr>

	<logic:iterate id="cat" type="net.spy.photo.Category"
		collection="<%= CategoryFactory.getInstance().getAdminCatList() %>">

		<tr>
			<td><%= cat.getName() %></td>
			<td>
				<html:multibox property="categories" value="<%= "" + cat.getId() %>"/>
			</td>
		</tr>
	</logic:iterate>

	</table>

	<html:submit>Create Profile</html:submit>
</html:form>

</p>
<%-- arch-tag: C9245172-5D6F-11D9-A347-000A957659CC --%>
