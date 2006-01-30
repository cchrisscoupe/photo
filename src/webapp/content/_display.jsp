<%@ page import="net.spy.photo.PhotoImageData" %>
<%@ page import="net.spy.photo.PhotoDimensions" %>
<%@ page import="net.spy.photo.PhotoRegion" %>
<%@ page import="net.spy.photo.PhotoUtil" %>
<%@ page import="net.spy.photo.Keyword" %>
<%@ page import="net.spy.photo.Comment" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ taglib uri='http://jakarta.apache.org/struts/tags-logic' prefix='logic' %>
<%@ taglib uri='http://jakarta.apache.org/struts/tags-html' prefix='html' %>
<%@ taglib uri='/tlds/photo.tld' prefix='photo' %>

<html:xhtml/>

<photo:javascript url="/js/annimg.js"/>
<photo:javascript url="/js/catedit.js"/>

<%
	PhotoImageData image=(PhotoImageData)request.getAttribute("image");
	// Need to know the new dims for the rest of the page
	float scaleFactor=PhotoUtil.getScaleFactor(image.getDimensions(),
		(PhotoDimensions)request.getAttribute("displayDims"));

	String searchIdS=(String)request.getParameter("search_id");
%>

<% if(searchIdS == null) { %>
	<div class="displayBrief"><c:out value="${image.descr}"/></div>
<% } else { %>
	<table width="100%">
		<tr valign="top">
			<td align="left" width="30%">
					<photo:imgLink id="0" relative="prev"
						searchId='<%= "" + searchIdS %>'>
						<img src="<c:url value='/images/prev.png'/>" alt="previous"/>
					</photo:imgLink>
			</td>

			<td align="center">
				<div class="displayBrief"><c:out value="${image.descr}"/></div>
			</td>

			<td align="right" width="30%">
					<photo:link url='<%= "/display.do?search_id=" + searchIdS %>'>
						<img src="<c:url value='/images/pause.png'/>" alt="pause"/>
					</photo:link>
					<photo:link url='<%= "/refreshDisplay.do?search_id=" + searchIdS %>'>
						<img src="<c:url value='/images/play.png'/>" alt="slideshow"/>
					</photo:link>
					<photo:imgLink id="<%= 0 %>" relative="next"
							searchId="<%= "" + searchIdS %>">
						<img src="<c:url value='/images/next.png'/>" alt="next"/>
					</photo:imgLink>
			</td>
		</tr>
	</table>
<% } %>

	<div id="imgDisplay">
		<photo:imgSrc id='<%= image.getId() %>' showOptimal="true"
			class="annotated"
			usemap="#annotationMap"/>
	</div>

	<c:set var="imgId"><%= image.getId() %></c:set>

	<div>
		<b>Category</b>: <q><span id="imgCat"><c:out
			value="${image.catName}"/></span></q>&nbsp;<b>Keywords</b>:
			<i><span id="imgKeywords"><c:forEach
				var="kw" items="${image.keywords}"> <c:out value="${kw.keyword}"
					/></c:forEach></span></i><br/>
		<b>Size</b>:  <c:out value="${image.dimensions}"/>
			(<c:out value="${image.size}"/> bytes)<br />
		<b>Taken</b>:  <span id="imgTaken"><c:out value="${image.taken}"/></span>
		<b>Added</b>:
			<c:out value="${image.timestamp}"/>
		by <c:out value="${image.addedBy.realName}"/><br />
		<b>Info</b>:
		<div id="imgDescr" class="imgDescr"><c:out value="${image.descr}"/></div>
		<b><span style="display: none" id="metalabel" id="metalabel">Meta</span>
			<a id="metalink" href="#">Get Meta Data</a>
			<img src="<c:url value='/images/indicator.gif'/>"
				alt="indicator" id="metaindicator" style="display: none"/>
			</b>
		<div id="meta" style="display: none">
			<table class="metadata">
				<thead>
					<tr>
						<th>Attribute</th><th>Value</th>
					</tr>
				</thead>
				<tbody id="metadata">
					<tr>
						<td></td><td></td>
					</tr>
				</tbody>
			</table>
		</div>
		<logic:present role="admin">
			<script type="text/javascript">
				new Ajax.InPlaceEditor('imgDescr',
					'<c:url value="/ajax/photo/descr?imgId=${imgId}"/>', {rows: 10, cols: 80});
				new Ajax.InPlaceEditor('imgKeywords',
					'<c:url value="/ajax/photo/keywords?imgId=${imgId}"/>');
				new Ajax.InPlaceEditor('imgTaken',
					'<c:url value="/ajax/photo/taken?imgId=${imgId}"/>');
				setupCategoryEditor('imgCat',
					'<c:url value="/ajax/photo/cat?imgId=${imgId}"/>',
					'<c:url value="/"/>');
			</script>
		</logic:present>

	</div>

<logic:present role="authenticated">
	[<photo:link url="/annotateForm.do" id="<%= String.valueOf(image.getId()) %>">
			Annotate
	</photo:link>] | 
	[<photo:link url="/logView.do" id="<%= String.valueOf(image.getId()) %>">
			Who's seen this?
	</photo:link>] | 
	[<photo:link url="/addToGallery.do" id="<%= String.valueOf(image.getId()) %>">
		Add to Gallery
	</photo:link>] | 
</logic:present>
[<photo:imgLink id='<%= String.valueOf(image.getId()) %>'>
	Linkable image
</photo:imgLink>] |
[<photo:link url='<%= "/PhotoServlet/" + image.getId() + ".jpg?id=" + image.getId() %>'>Full Size Image</photo:link>]
<%--
[<photo:imgLink id="<%= "" + image.getId() %>">Full Size Image</photo:imgLink>]
--%>

<div id="ratings">
	<h2>Rating</h2>

	<div id="yourvote">
		<c:if test="${not empty myrating}">
			On <fmt:formatDate value="${myrating.timestamp}"
				pattern="EEE, d MMM yyyy HH:mm"/>, you rated this image
			<c:out value="${myrating.vote}"/>/5.
		</c:if>
	</div>
	<div>
		<span id="avgvote">
			Average rating is <fmt:formatNumber value="${image.votes.average}"/>
			with <fmt:formatNumber value="${image.votes.size}"/> votes.
		</span>
		<span id="stars"></span>
		<img src="<c:url value='/images/indicator.gif'/>"
			alt="indicator" id="rateindicator" style="display: none"/>
	</div>
</div>

<div id="comments" class="comments">

	<h2>Comments</h2>

	<div id="commentanchor"></div>

	<logic:iterate id="comment"
		type="net.spy.photo.Comment"
		collection="<%= Comment.getCommentsForPhoto(image.getId()) %>">

		<div class="comments">
			<div class="commentheader">
				At <%= comment.getTimestamp() %>,
					<%= comment.getUser().getRealname() %> said the following:
			</div>
			<div class="commentbody">
				<%= comment.getNote() %>
			</div>
		</div>
	</logic:iterate>

</div>

<div>

	<script type="text/javascript">
		var rateAvg='<c:out value="${image.votes.average}"/>';
		var rateSize='<c:out value="${image.votes.size}"/>';
		var baseUrl='<c:url value="/"/>';
		var imgid='<c:out value="${image.id}"/>';
		var exifUrl='<c:url value="/ajax/exif/${image.id}"/>';
		var starSrcs=new Array();
		// <![CDATA[

		function calculateStarSrcs() {
			starSrcs=new Array();
			for(var i=0; i<5; i++) {
				var starProto=$('starempty');
				if((i+1) <= rateAvg) {
					starProto=$('starfull');
				} else if((i+1) - rateAvg <= 0.5) {
					starProto=$('starhalf');
				}
				starSrcs.push(starProto.src);
			}
		}

		function createStarsForShow() {
			calculateStarSrcs();
			clearThing($('stars'));
			$A(starSrcs).each(function(src, i) {
				var newStar=document.createElement("img");
				newStar.src=src;
				newStar.id='star' + i;
				$('stars').appendChild(newStar);
				});
		}

		// Load the meta data
		function loadMeta() {
			Element.show("metaindicator");
			new Ajax.Request(exifUrl, {
				method: 'get',
				onComplete: function(req) {
					Element.hide("metaindicator");
				},
				onFailure: function(req) {
					alert("Failed to save vote.");
				},
				onSuccess: function(req) {
					var xml=req.responseXML;
					Element.show("metalabel");
					Element.show("meta");
					Element.hide("metalink");
					clearThing($('metadata'));

					var d=$('metadata');

					var tags=xml.getElementsByTagName("tag");
					for(var i=0; i<tags.length; i++) {
						var tag=tags[i];
						var tr=document.createElement("tr");

						var td1=document.createElement("td");
						td1.appendChild(document.createTextNode(
							getElementText(tag, "key", 0)));
						var td2=document.createElement("td");
						td2.appendChild(document.createTextNode(
							getElementText(tag, "value", 0)));

						tr.appendChild(td1);
						tr.appendChild(td2);

						d.appendChild(tr);
					}

				}
				});
		}

		// Set up the rating bar
		Event.observe(window, 'load', createStarsForShow, false);

		// Set up the meta loader
		Event.observe(window, 'load', function() {
			$('metalink').onclick=function() {
				loadMeta();
				return false;
			};
			}, false);
		// ]]>
	</script>

	<logic:present role="authenticated">

	<script type="text/javascript">
		// <![CDATA[

		function rateImage(rating) {
			Element.show("rateindicator");
			new Ajax.Request(baseUrl + 'ajax/photo/vote', {
				method: 'post',
				postBody: $H({imgId: imgid, vote: rating}).toQueryString(),
				onComplete: function(req) {
					Element.hide("rateindicator");
				},
				onFailure: function(req) {
					alert("Failed to save vote.");
				},
				onSuccess: function(req) {
					clearThing($('yourvote'));
					$('yourvote').appendChild(document.createTextNode("You just voted "
						+ rating + "/5"));
					// Get the json response and update the UI with the new ratings
					var json=eval('(' + req.responseText + ')');
					rateAvg=json.avg;
					calculateStarSrcs();
					restoreStars();
					clearThing($('avgvote'));
					$('avgvote').appendChild(document.createTextNode("Average vote is "
						+ rateAvg + " of " + json.size + " votes"));
				}
				});
		}

		function submitComment() {
			var comment=$F("comment");
			if(comment == "") {
				alert("You'll need to actually type in a comment to post it.");
				return(false);
			}
			Form.disable("commentForm");
			$("comment").value="";

			Element.show("addindicator");
			var postBody=$H({imgId: imgid, comment: comment}).toQueryString();
			new Ajax.Request(baseUrl + 'ajax/photo/comment', {
				method: 'post',
				postBody: postBody,
				onComplete: function(req) {
					Form.enable("commentForm");
					Element.hide("addindicator");
				},
				onFailure: function(req) {
					alert("Failed to add comment.");
					},
				onSuccess: function(req) {
					var c=document.createElement("div");
					h=document.createElement("div");
					h.className="commentheader";
					h.appendChild(document.createTextNode("You added"));
					b=document.createElement("div");
					b.className="commentbody";
					b.appendChild(document.createTextNode(comment));

					c.className="comments";
					c.appendChild(h);
					c.appendChild(b);
					$("comments").appendChild(c);
					}
				});
		}

		function bindStarCall(f, v) {
			var i=v + '';
			return function() { f(i); return(false); };
		}

		function hoverStars(max) {
			var empty=$('starempty').src;
			var partial=$('starpartial').src;
			$A(starSrcs).each(function(s, i) {
				var s=empty;
				if(i <= max) {
					s=partial;
				}
				$('star' + i).src=s;
				});
		}

		function restoreStars() {
			$A(starSrcs).each(function(src, i) { $('star' + i).src=src; });
		}

		function createStarsForEdit() {
			calculateStarSrcs();
			clearThing($('stars'));
			$A(starSrcs).each(function(src, i) {
				var newStar=document.createElement("img");
				newStar.src=starSrcs[i];
				newStar.id='star' + i;
				newStar.onclick = bindStarCall(rateImage, i+1);
				newStar.onmouseover = bindStarCall(hoverStars, i);
				newStar.onmouseout = restoreStars;
				$('stars').appendChild(newStar);
				});
		}

		// Set up the rating bar
		Event.observe(window, 'load', createStarsForEdit, false);

		// ]]>
	</script>

		<fmt:message key="display.comment"/><br/>
		<html:form action="/addcomment" onsubmit="submitComment(); return false;"
			styleId="commentForm">
			<div>
				<html:textarea styleId="comment" property="comment" cols="50" rows="2"/>
			</div>
			<div>
				<html:submit>Add Comment</html:submit>
				<input type="hidden" name="imageId" value="<%= image.getId() %>"/>
			<img src="<c:url value='/images/indicator.gif'/>"
				alt="indicator" id="addindicator" style="display: none"/>
			</div>
		</html:form>
	</logic:present>
</div>
<map id="annotationMap" name="annotationMap">
	<logic:iterate id="region"
		type="net.spy.photo.AnnotatedRegion"
		collection="<%= image.getAnnotations() %>">

		<%
			PhotoRegion scaledRegion=PhotoUtil.scaleRegion(region, scaleFactor);
			int rx1=scaledRegion.getX();
			int ry1=scaledRegion.getY();
			int rx2=scaledRegion.getWidth() + rx1;
			int ry2=scaledRegion.getHeight() + ry1;

			String coords=rx1 + "," + ry1 + "," + rx2 + "," + ry2;
			String keywords="";
			for(Keyword k : region.getKeywords()) {
				keywords += k.getKeyword() + " ";
			}
		%>

		<c:set var="u">
			<c:url value="/search.do">
				<c:param name="what"><%= keywords %></c:param>
				<c:param name="field" value="keywords"/>
				<c:param name="fieldjoin" value="and"/>
				<c:param name="sdirection" value="desc"/>
			</c:url>
		</c:set>

		<area alt="" title="<%= region.getTitle() %>"
			href="<c:out value='${u}'/>" shape="rect" coords="<%= coords %>"/>

	</logic:iterate>
</map>
<div style="display: none;">
	<img src="<c:url value='/images/star_empty.gif'/>" alt="star" id="starempty"/>
	<img src="<c:url value='/images/star_partial.gif'/>" alt="star" id="starpartial"/>
	<img src="<c:url value='/images/star_full.gif'/>" alt="star" id="starfull"/>
	<img src="<c:url value='/images/star_half.gif'/>" alt="star" id="starhalf"/>
</div>
<%-- arch-tag: AC919514-5D6F-11D9-ACF8-000A957659CC --%>
