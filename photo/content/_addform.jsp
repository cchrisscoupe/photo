<%@ page import="net.spy.photo.Category" %>
<%@ page import="net.spy.photo.PhotoUtil" %>
<%@ taglib uri='/tlds/struts-logic.tld' prefix='logic' %>
<%@ taglib uri='/tlds/struts-html.tld' prefix='html' %>
<%@ taglib uri='/tlds/struts-bean.tld' prefix='bean' %>
<%@ taglib uri='/tlds/photo.tld' prefix='photo' %>

<%
	// Check to see if there was already an add here.
	Integer idInteger=(Integer)request.getAttribute("net.spy.photo.UploadID");
	String idString=null;
	if(idInteger!=null) {
		idString=idInteger.toString();
	}
%>

<% if(idInteger!=null) { %>
<p>
	Your image has been uploaded.  Its ID is <%= idString %> and you can
	see it <photo:imgLink id="<%= idString %>">here</photo:imgLink> before
	too long.
</p>
<% } %>

<p>

<div class="sectionheader">Add a Photo</div>

<html:form action="upload.do" enctype="multipart/form-data">

	<html:errors/>

	<table border="0" width="100%">

	<tr>
		<td align="left" width="50%">
			<table border="0">
			<tr>
				<td>Category:</td>
				<td>
					<html:select property="category" size="5">
					<photo:getCatList showAddable="true">
						<logic:iterate type="net.spy.photo.Category" id="i" name="catList">
							<html:option value="<%= "" + i.getId() %>">
								<%= i.getName() %></html:option>
						</logic:iterate>
					</photo:getCatList>
					</html:select>
				</td>
			</tr>
			</table>
		</td>

		<td align="right" width="50%">
			<table border="0">
				<tr>
					<td>Date Taken:</td>
					<td>
						<html:text property="taken"/>
					</td>
				</tr>
				<tr>
					<td>Keywords:</td>
					<td><html:text property="keywords"/></td>
				</tr>
				<tr>
					<td>Picture:</td>
					<td><html:file property="picture"/></td>
				</tr>
			</table>
		</td>
	</tr>

	</table>

	<center>
		<table>
			<tr>
			<td align="left">
			Short Description:<br/>
			<html:textarea property="info" cols="60" rows="5"/>
			</tr>
			</td>
		</table>
		<html:submit>Add Image</html:submit>
		<html:reset>Clear</html:reset>
	</center>

</html:form>

</p>
