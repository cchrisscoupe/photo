/*
 * Copyright (c) 1999 Dustin Sallings
 *
 * $Id: PhotoSession.java,v 1.44 2000/12/26 06:21:42 dustin Exp $
 */

package net.spy.photo;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.*;
import java.text.*;
import sun.misc.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.oreilly.servlet.*;

import net.spy.*;
import net.spy.cache.*;
import net.spy.log.*;

// The class
public class PhotoSession extends Object
{ 
	// This kinda stuff is only persistent for a single connection.
	protected String remote_user=null, self_uri=null;
	protected MultipartRequest multi=null;
	protected SpyLog logger=null;
	protected PhotoSecurity security = null;
	protected PhotoServlet photo_servlet = null;
	protected Hashtable groups=null;

	protected String xslt_stylesheet=null;

	protected boolean xmlraw=false;
	protected boolean debug=true;

	protected PhotoAheadFetcher aheadfetcher=null;

	// These are public because they may be used by PhotoHelpers
	public HttpServletRequest request=null;
	public HttpServletResponse response=null;
	public HttpSession session=null;
	public Integer remote_uid=null;

	public PhotoSession(PhotoServlet p,
		HttpServletRequest request,
		HttpServletResponse response) {

		photo_servlet=p;
		this.request=request;
		this.response=response;
		this.session=request.getSession(false);

		// The aheadfetcher
		this.aheadfetcher=p.aheadfetcher;

		logger=p.logger;
		security=p.security;
		xmlraw=false;
	}

	// This gets us back into the servlet engine's log.
	protected void log(String whu) {
		photo_servlet.log(whu);
	}

	// process a request
	public void process() throws ServletException, IOException {

		String func=null, type=null, out=null;;

		try {
			type = request.getContentType();
			if(type != null && type.startsWith("multipart/form-data")) {
				multi = new MultipartRequest(request, "/tmp");
			} else {
				multi = null;
			}

			// Set the self_uri
			self_uri = request.getRequestURI();

			getCreds();

			getStyleSheet();

			// Figure out what they want, default to index.
			if(multi==null) {
				func=request.getParameter("func");

				// Find out if they want raw XML or not...
				String tmp=request.getParameter("xmlraw");
				if(tmp!=null) {
					xmlraw=true;
				}
			} else {
				func=multi.getParameter("func");
			}

			out=dispatchFunction(func);

		} catch(Exception e) {
			log("Error:  " + e + "\nStack:  " + SpyUtil.getStack(e, 1));
			out=createError(e);
			try {
				sendXML(out);
			} catch(Exception e2) {
				log("Compound error:  We also failed to send back the error, "
					+ e2);
				throw new ServletException("Error sending error message:  "
					+ e2);
			}
			out=null;
		}

		// Some things handle their own responses, and return null.
		if(out!=null) {
			send_response(out);
		}
	}

	protected String createError(Exception e) {
		StringBuffer rv=new StringBuffer();

		// XML header
		rv.append("<?xml version=\"1.0\"?>\n");

		rv.append("<exception>\n");
		rv.append("\t<text>\n");
		rv.append("\t\t" + PhotoXSLT.normalize(e.getMessage(), true) + "\n");
		rv.append("\t</text>\n");
		rv.append("\t<exception_class>\n");
		rv.append("\t\t" + PhotoXSLT.normalize(e.getClass().getName(), true)
			+ "\n");
		rv.append("\t</exception_class>\n");
		rv.append("\t<stack>\n");
		String s[]=SpyUtil.split(",", SpyUtil.getStack(e, 1));
		for(int i=0; i<s.length; i++) {
			rv.append("\t\t<stack_entry>\n");
			rv.append("\t\t\t" + PhotoXSLT.normalize(s[i], true) + "\n");
			rv.append("\t\t</stack_entry>\n");
		}
		rv.append("\t</stack>\n");
		rv.append("</exception>\n");

		return(rv.toString());
	}

	protected String dispatchFunction(String func) throws Exception {

		// If there's no function, set the function to index.
		if(func==null) {
			func="index";
		}

		// Lowercase it so that we don't have to keep doing case ignores
		func=func.toLowerCase();
		log(remote_user + " requested " + func);
		String out=null;

		// OK, see what they're doing.
		if(func.equals("search")) {
			out=doFind();
		} else if(func.equals("nextresults")) {
			out=displaySearchResults();
		} else if(func.equals("addimage")) {
			out=doAddPhoto();
		} else if(func.equals("index")) {
			out=doIndex();
		} else if(func.equals("findform")) {
			out=doFindForm();
		} else if(func.equals("addform")) {
			out=doAddForm();
		} else if(func.equals("catview")) {
			out=doCatView();
		} else if(func.equals("setstyle")) {
			out=doSetStyle();
		} else if(func.equals("styleform")) {
			out=doStyleForm();
		} else if(func.equals("getstylesheet")) {
			out=doGetStylesheet();
		} else if(func.equals("display")) {
			out=doDisplay();
		} else if(func.equals("logview")) {
			out=doLogView();
		} else if(func.equals("getimage")) {
			out=showImage();
		} else if(func.equals("credform")) {
			out=showCredForm();
		} else if(func.equals("savesearch")) {
			out=saveSearch();
		} else if(func.equals("setstylesheet")) {
			setStyleSheet();
			out=doIndex();
		} else if(func.equals("setcred")) {
			setCreds();
			out=doIndex();
		} else if(func.equals("setadmin")) {
			setAdmin();
			out=doIndex();
		} else if(func.equals("unsetadmin")) {
			unsetAdmin();
			out=doIndex();
		} else if(func.startsWith("adm")) {
			// Anything that starts with rep is probably reporting.
			try {
				PhotoAdmin adm=new PhotoAdmin(this);
				out=adm.process(func);
			} catch(Exception e) {
				throw new ServletException("Admin Exception:  " + e);
			}
		} else if(func.startsWith("rep")) {
			// Anything that starts with rep is probably reporting.
			try {
				PhotoReporting rep=new PhotoReporting(this);
				out=rep.process(func);
			} catch(Exception e) {
				throw new ServletException("Reporting Exception:  " + e);
			}
		} else {
			throw new ServletException("No known function.");
		}
		return(out);
	}

	protected String saveSearch() throws ServletException {
		String output="";

		try {
			PhotoSearch ps = new PhotoSearch();
			PhotoUser user = security.getUser(remote_user);
			ps.saveSearch(request, user);
			output=tokenize("addsearch_success.inc", new Hashtable());
		} catch(Exception e) {
			Hashtable h = new Hashtable();
			log("Search save failed:  " + e);
			h.put("MESSAGE", e.getMessage());
			output=tokenize("addsearch_fail.inc", h);
		}
		return(output);
	}

	// Set the stylesheet
	protected void setStyleSheet() throws ServletException {
		String ss=request.getParameter("stylesheet");

		// Make sure something was passed in.
		if(ss!=null) {
			// Make sure a session exists
			if(session==null) {
				session=request.getSession(true);
			}

			// Make it available for the session
			session.putValue("photo_stylesheet", ss);
			// Make it immediately available
			xslt_stylesheet=ss;
		}
	}

	// Figure out what stylesheet to use
	protected void getStyleSheet() {
		xslt_stylesheet=null;

		PhotoConfig conf=new PhotoConfig();

		if(session!=null) {
			xslt_stylesheet=(String)session.getValue("photo_stylesheet");
		}
	}

	protected void getCreds() throws ServletException {
		getUid();
		log("Authenticated as " + remote_user);
	}

	public void setCreds () throws ServletException, IOException {
		String username=request.getParameter("username");
		String pass=request.getParameter("password");

		// Make sure we drop administrative privs if any
		unsetAdmin();

		// We don't do anything unless the password is correct.
		if(security.checkPW(username, pass)) {
			// Make sure there is a session.
			if(session==null) {
				session=request.getSession(true);
			}
			// Save the username.
			session.putValue("username", username);
			// Make it valid immediately
			remote_user = username;
			// Set the UID variable.
			getUid();
		}
	}

	// Grab a connection from the pool.
	protected Connection getDBConn() throws Exception {
		SpyDB pdb=new SpyDB(new PhotoConfig(), false);
		return(pdb.getConn());
	}

	// Gotta free the connection
	protected void freeDBConn(Connection conn) {
		SpyDB pdb=new SpyDB(new PhotoConfig(), false);
		pdb.freeDBConn(conn);
	}

	// Get the saved searches.
	protected String showSaved() {
		String out="";
		Connection photo=null;
		SpyCache cache=new SpyCache();
		out=(String)cache.get("saved_searches");
		// If we don't have it cached, grab it and cache it.
		if(out==null) {
			log("saved_searches was not cached, must fetch from the db");
			try {
				out="";
				photo=getDBConn();
				BASE64Decoder base64 = new BASE64Decoder();
				Statement st=photo.createStatement();

				String query = "select * from searches order by name\n";
				ResultSet rs = st.executeQuery(query);
				while(rs.next()) {
					byte data[];
					data=base64.decodeBuffer(rs.getString(4));
					String tmp = new String(data);
					out += "    <item link=\"" + self_uri + "?"
						+ PhotoXSLT.normalize(tmp, true) + "\">"
						+ rs.getString(2) + "</item>\n";
				}
				// Cache for fifteen minutes.
				cache.store("saved_searches", out, 15*60*1000);
			} catch(Exception e) {
				log("Error getting search data, returning none:  " + e);
			} finally {
				if(photo != null) {
					freeDBConn(photo);
				}
			}
		}

		return(out);
	}

	// Find out if the authenticated user can add stuff.
	protected boolean canadd() {
		boolean r=false;

		try{
			PhotoUser p = security.getUser(remote_user);
			r=p.canadd;
		} catch(Exception e) {
			log("Error getting canadd permissions:  " + e);
		}

		return(r);
	}

	// Add an image
	protected String doAddPhoto() throws ServletException {
		String out="", type=null;
		int id=-1;
		Hashtable h = new Hashtable();
		Connection photo=null;

		// Make sure the user can add.
		if(!canadd()) {
			log("User " + remote_user + " has no permission to add.");
			return(tokenize("add_denied.inc", h));
		}

		// We need a short lifetime for whatever page this produces
        long l=new java.util.Date().getTime();
        l+=10000L;
        response.setDateHeader("Expires", l);

		File f;
		f = multi.getFile("picture");

		// Check that it's the right file type.
		type = multi.getContentType("picture");
		log("Type is " + type);
		if( type == null || (! (type.startsWith("image/jpeg"))) ) {
			h.put("FILENAME", multi.getFilesystemName("picture"));
			h.put("FILETYPE", type);
			out=tokenize("add_badfiletype.inc", h);
			try {
				f.delete();
			} catch(Exception e) {
				log("Error deleting file " + f + ": "  + e);
			}
			return(out);
		}

		// OK, things look good, let's try to store our data.
		try {
			FileInputStream in=null;
			String query=null;

			// Get the size from the file.
			int size=(int)f.length();

			// Encode the shit;
			int length=0;
			in = new FileInputStream(f);
			byte data[] = new byte[size];
			// Read in the data
			length=in.read(data);
			// If we didn't read enough data, give up.
			if(length!=size) {
				throw new Exception("Error reading enough data!");
			}
			PhotoImage photo_image=new PhotoImage(data);

			photo=getDBConn();
			photo.setAutoCommit(false);
			query = "insert into album(keywords, descr, cat, taken, size, "
				+ " addedby, ts, width, height, tn_width, tn_height)\n"
				+ "   values(?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0)";
			PreparedStatement st=photo.prepareStatement(query);
			// Toss in the parameters
			if (multi.getParameter("keywords") == null) {
				debug ("keywords null, setting to blank string");
				st.setString(1, "");
			} else {
				st.setString(1, multi.getParameter("keywords"));
			}

			if (multi.getParameter("info") == null) {
				debug ("info/desc null, setting to blank string");
				st.setString(2, "");
			} else {
				st.setString(2, multi.getParameter("info"));
			}

			st.setInt(3, Integer.parseInt(multi.getParameter("category")));
			st.setString(4, multi.getParameter("taken"));
			st.setInt(5, size);
			st.setInt(6, remote_uid.intValue());
			st.setTimestamp(7,
				new java.sql.Timestamp(System.currentTimeMillis()));
			// Set the image width and height in the database.
			st.setInt(8, photo_image.width());
			st.setInt(9, photo_image.height());
			st.executeUpdate();

			query = "select currval('album_id_seq')\n";
			ResultSet rs = st.executeQuery(query);
			rs.next();
			id=rs.getInt(1);

			// Get a helper to store the data.
			PhotoImageHelper photo_helper=new PhotoImageHelper(id);
			photo_helper.storeImage(photo_image);

			// Log that the data was stored in the cache, so that, perhaps,
			// it can be permanently stored later on.
			query = "insert into upload_log (photo_id, wwwuser_id, ts)\n"
				+ "  values(?, ?, ?)\n";
			st=photo.prepareStatement(query);
			st.setInt(1, id);
			st.setInt(2, remote_uid.intValue());
			st.setDate(3, new java.sql.Date(System.currentTimeMillis()));
			st.executeUpdate();

			h.put("ID", ""+id);

			photo.commit();
			out += tokenize("add_success.inc", h);
		} catch(Exception e) {
			log("Error adding new image to database:  " + e);
			try {
				photo.rollback();
				h.put("ERRSTR", e.getMessage());
				out += tokenize("add_dbfailure.inc", h);
			} catch(Exception e2) {
				log("Error rolling back and/or reporting add falure:  " + e2);
			}
		} finally {
			if(photo != null) {
				try {
					photo.setAutoCommit(true);
					freeDBConn(photo);
				} catch(Exception e) {
					log("Error cleaning up database after adding an image: "
						+ e);
				}
			}
			try {
				f.delete();
			} catch(Exception e) {
				log("Error removing temporary file after adding an image:  "
					+ e);
			}
		}

		return(out);
	}

	// Get a list of categories for a select list
	// Public because helpers use it
	public String getCatList(int def) {
		String out="";
		Connection photo=null;
		SpyCache cache=new SpyCache();

		String key="catList_" + def;

		out=(String)cache.get(key);
		// If we don't have it, build it and cache it.
		if(out==null) {
			out="";
			try {
				photo=getDBConn();

				String query = "select * from cat where id in\n"
			  		+ "(select cat from wwwacl where\n"
			  		+ "    userid=? or userid=?)\n"
			  		+ "order by name\n";

				PreparedStatement st = photo.prepareStatement(query);
				st.setInt(1, remote_uid.intValue());
				st.setInt(2, PhotoUtil.getDefaultId());
				ResultSet rs = st.executeQuery();

				while(rs.next()) {
					int id=rs.getInt("id");
					String selected="";
					if(id==def) {
						selected=" selected=\"1\"";
					}
					out += "    <option value=\"" + id + "\"" + selected
						+ ">" + rs.getString("name") + "</option>\n";
				}
				// Cache it for five minutes
				cache.store(key, out, 5*60*1000);
			} catch(Exception e) {
				log("Error getting category list:  " + e);
			} finally {
					if(photo != null) {
						freeDBConn(photo);
					}
			}
			// Cache this for five minutes.
			cache.store(key, out, 5*60*1000);
		}
		return(out);
	}

	// Show the ``login'' form
	protected String showCredForm () throws ServletException {
		return(tokenize("authform.inc", new Hashtable()));
	}

	// Show the style form
	protected String doStyleForm () throws ServletException {
		return(tokenize("presetstyle.inc", new Hashtable()));
	}

	// Get the stylesheet from the cookie, or the default.
	protected String doGetStylesheet () throws ServletException {
		String output = null;
		int i;

		Cookie cookies[] = request.getCookies();

		if(cookies!=null) {
			for(i=0; i<cookies.length && output == null; i++) {
				String s = cookies[i].getName();
				if(s.equalsIgnoreCase("photo_style")) {
					output = cookies[i].getValue();
				}
			}
		}

		if(output == null) {
			output = tokenize("style.css", new Hashtable());
		}

		// This is a little different, so we won't use send_response()
		response.setContentType("text/css");
		java.util.Date d=new java.util.Date();
		long l=d.getTime();
		l+=36000000L;
		response.setDateHeader("Expires", l);
		try {
			PrintWriter out = response.getWriter();
			out.print(output);
			out.close();
		} catch(Exception e) {
			log("Error producing stylesheet output:  " + e);
		}
		// We handled our own response.
		return(null);
	}

	// Set the style cookie from the POST data.
	protected String doSetStyle() throws ServletException {
		String stmp="", font="", bgcolor="", c_text="";
		Hashtable h = new Hashtable();

		stmp = request.getParameter("font");
		if(stmp != null && stmp.length() > 1) {
			font = stmp;
		}

		stmp = request.getParameter("bgcolor");
		if(stmp != null && stmp.length() > 1) {
			bgcolor = stmp;
		}

		c_text = "body,td {font-family: " + font + ", Arial; "
			   + "background-color: " + bgcolor + ";}\n";

		stmp = request.getParameter("d_transform");
		if(stmp != null && stmp.length() > 1) {
			c_text += "blockquote {text-transform: " + stmp + ";};\n";
		}

		stmp = request.getParameter("h_transform");
		if(stmp != null && stmp.length() > 1) {
			c_text += "h1,h2,h3,h4,h5 {text-transform: " + stmp + ";};\n";
		}

		// Create the cookie
		Cookie c = new Cookie("photo_style", URLEncoder.encode(c_text));
		// 30 days of cookie
		c.setMaxAge( (30 * 86400) );
		// Describe why we're doing this.
		c.setComment("Your style preferences for the photo album.");
		// Where we'll be using it.
		c.setPath(self_uri);

		// Add it to the responses.
		response.addCookie(c);

		// Prepare output.
		stmp = "";
		h.put("STYLE", c_text);

		stmp = tokenize("setstyle.inc", h);
		return(stmp);
	}

	// Show the add an image form.
	protected String doAddForm() throws ServletException {
		String output = "";
		Hashtable h = new Hashtable();

		try {
			h.put("CAT_LIST", getCatList(-1));
		} catch(Exception e) {
			h.put("CAT_LIST", "");
		}
		h.put("TODAY", PhotoUtil.getToday());

		// If the user cannot add, make it clear on the add page.
		if(canadd()) {
			// User can add, say nothing.
			h.put("CANNOTADD", "");
		} else {
			// User cannot add, point this out.
			h.put("CANNOTADD", "You do not have the ability to add images");
		}
		output += tokenize("addform.inc", h);
		return(output);
	}

	// Show the search form.
	protected String doFindForm() throws ServletException {
		Hashtable h = new Hashtable();

		try {
			h.put("CAT_LIST", getCatList(-1));
		} catch(Exception e) {
			h.put("CAT_LIST", "");
		}
		sendXML("xml/find.xinc", h);
		return(null);
	}

	// View categories
	protected String doCatView() throws ServletException {
		String catstuff="";
		Hashtable h = new Hashtable();
		Connection photo=null;

		try {
			photo = getDBConn();

			String query = "select name,id,catsum(id) as cs from cat\n"
			  	+ "where id in\n"
			  	+ "  (select cat from wwwacl where\n"
			  	+ "   userid=? or userid=?)\n"
			  	+ " order by cs desc";
			PreparedStatement st = photo.prepareStatement(query);
			st.setInt(1, remote_uid.intValue());
			st.setInt(2, PhotoUtil.getDefaultId());
			ResultSet rs = st.executeQuery();

			while(rs.next()) {
				String t;
				if(rs.getInt(3)==1) {
					t = " image";
				} else {
					t = " images";
				}

				// Out of that, build the XML for the output.
				catstuff += "<cat_view_item>\n";
				catstuff += " <category>" + rs.getString(1) + "</category>\n";
				catstuff += " <cat_n>" + rs.getString(2) + "</cat_n>\n";
				catstuff += " <count>" + rs.getString(3) + "</count>\n";
				catstuff += " <qualifier>" + t + "</qualifier>\n";
				catstuff += " <link>"
					+ PhotoXSLT.normalize(
						self_uri
						+ "?func=search&searchtype=advanced&cat="
						+ rs.getString(2) + "&maxret=5",
						true
					)
					+ "</link>\n";
				catstuff += "</cat_view_item>\n\n";
			}
		} catch(Exception e) {
			log("Error producing category view:  " + e);
		}
		finally { freeDBConn(photo); }

		h.put("CATSTUFF", catstuff);

		sendXML("xml/catview.xinc", h);
		return(null);
	}

	// Display the index page.
	protected String doIndexInc() throws ServletException {
		String output = "";;
		Hashtable h = new Hashtable();

		try {
			h.put("SAVED", showSaved());
		} catch(Exception e) {
			log("Error getting saved search list:  " + e);
			h.put("SAVED", "");
		}
		if(isAdmin()) {
			output += tokenize("admin/index.inc", h);
		} else {
			output += tokenize("index.inc", h);
		}
		return(output);
	}

	// Some basic statistic stuff for the home page.
	protected String getTotalImagesShown() throws Exception {
		String ret=null;
		String key="photo_total_images_shown";
		NumberFormat nf=NumberFormat.getNumberInstance();

		SpyCache cache=new SpyCache();

		ret=(String)cache.get(key);
		if(ret==null) {
			SpyDB db=new SpyDB(new PhotoConfig());
			ResultSet rs=db.executeQuery("select count(*) from photo_log");
			rs.next();
			ret=nf.format(rs.getInt(1));
			// Recalculate every hour
			cache.store(key, ret, 60*60*1000);
		}

		return(ret);
	}

	protected String getTotalImages() throws Exception {
		String ret=null;
		String key="photo_total_images";
		NumberFormat nf=NumberFormat.getNumberInstance();

		SpyCache cache=new SpyCache();

		ret=(String)cache.get(key);
		if(ret==null) {
			SpyDB db=new SpyDB(new PhotoConfig());
			ResultSet rs=db.executeQuery("select count(*) from album");
			rs.next();
			ret=nf.format(rs.getInt(1));
			// Recalculate once a day
			cache.store(key, ret, 86400*1000);
		}
		return(ret);
	}

	// Display the index page.
	protected String doIndex() throws ServletException {
		Hashtable h = new Hashtable();

		// Get the saved searches.
		try {
			h.put("SAVED", showSaved());
		} catch(Exception e) {
			log("Error getting saved search list:  " + e);
			h.put("SAVED", "");
		}

		PhotoConfig pc = new PhotoConfig();
		sendXML(pc.get("xinc_index", "xml/index.xinc"), h);

		return(null);
	}

	protected void sendText(String tmplate, Hashtable h)
		throws ServletException {

		try {
			String txt=tokenize(tmplate, h);
			PrintWriter out=response.getWriter();
			out.print(txt);
			out.close();
		} catch(Exception e) {
			// nothing now
		}
	}

	// Get the global meta data
	protected String getGlobalMeta() {
		StringBuffer sb=new StringBuffer();

		sb.append("<meta_stuff>\n");
		sb.append("  <self_uri>" + self_uri + "</self_uri>\n");
		PhotoUser user = security.getUser(remote_user);
		sb.append(user.toXML());
		// gm+="  <username>" + remote_user + "</username>\n";

		String tmp="";
		// Add some statistics on the index
		try {
			tmp=getTotalImagesShown();
		} catch(Exception e) {
			log("Error gathering global meta data:  " + e);
		}
		sb.append("<total_images_shown>" + tmp + "</total_images_shown>\n");
		try {
			tmp=getTotalImages();
		} catch(Exception e) {
			log("Error gathering global meta data:  " + e);
		}
		sb.append("<total_images>" + tmp + "</total_images>\n");
		sb.append("</meta_stuff>\n");

		return(sb.toString());
	}

	// Send raw XML
	protected void sendXML(String xml) throws Exception  {

		SpyCache cache=new SpyCache();

		if(xmlraw) {
			response.setContentType("text/plain");
		} else {
			response.setContentType("text/html");
		}

		PrintWriter out=response.getWriter();

		// If they want raw XML, get it that way.
		if(xmlraw) {
			out.print(xml);
		} else {
			PhotoXSLT.sendXML(xml, xslt_stylesheet, out);
		}

		out.close();
	}

	// Send (and cache) an XML template.
	protected void sendXML(String tmplate, Hashtable h)
		throws ServletException {
		PhotoConfig conf = new PhotoConfig();
		h.put("GLOBALMETA", getGlobalMeta());
		String xml=null;
		try {
			SpyCache cache=new SpyCache();
			String key="xml_p_" + tmplate + "_" + xslt_stylesheet
				+ "_" + PhotoUtil.myHash(h) + "_" + xmlraw;
			String o=(String)cache.get(key);
			// What the content is whether XML or not.
			if(xmlraw) {
				response.setContentType("text/plain");
			} else {
				response.setContentType("text/html");
			}
			if(o==null) {
				log(key + " was not cached...rebuilding.");
				// If they want raw XML, get it that way.
				if(xmlraw) {
					// Raw XML will just include the parsed template
					o=tokenize(tmplate, h);
				} else {
					StringWriter str=new StringWriter();
					xml=tokenize(tmplate, h);
					PhotoXSLT.sendXML(xml, xslt_stylesheet, str);
					// Get the data
					o=str.toString();
				}
				// ...and cache that for ten minutes...by default
				cache.store(key, o,
					((long)(conf.getInt("xml_cache_timeout", 600)))*1000);
			}
			PrintWriter out=response.getWriter();
			out.print(o);
			out.close();
		} catch(org.apache.xalan.xslt.XSLProcessorException e) {
			log("Error parsing this:\n" + xml);
			throw new ServletException("Error parsing XML:  " + e);
		} catch(Exception e) {
			throw new ServletException("Error sending XML:  " + e);
		}
	}

	// Get the UID
	protected void getUid() throws ServletException {
		try {
			if(session==null) {
				remote_user="guest";
			} else {
				remote_user=(String)session.getValue("username");
				// If we have a session, but no username, add guest.
				if(remote_user==null) {
					remote_user="guest";
					session.putValue("username", "guest");
				}
			}
			PhotoUser p=security.getUser(remote_user);
			remote_uid = p.id;
		} catch(Exception e) {
			throw new ServletException("Unknown user: " + remote_user);
		}
	}

	protected void getGroups() throws Exception {
		if(groups == null) {
			groups = new Hashtable();
			Connection photo=getDBConn();

			try {
				PreparedStatement st=photo.prepareStatement(
					"select * from show_group where username = ?"
				);
				st.setString(1, remote_user);
				ResultSet rs = st.executeQuery();
				while(rs.next()) {
					String groupName=rs.getString("groupname");
					groups.put(groupName, "1");
				}
			} catch (Exception e) {
				log("Error fetching groups:  " + e);
			} finally {
				freeDBConn(photo);
			}
		}
	}

	// Display dispatcher -- can be called from a helper
	public String doDisplay() throws ServletException {
		String out="";
		String id=null;
		String search_id=null;

		id = request.getParameter("id");
		search_id = request.getParameter("search_id");

		// A search displayer thing may use this to add additional
		// information
		Hashtable h=new Hashtable();

		PhotoSearchResult r=null;

		try {
			if(id!=null) {
				r=doDisplayByID(h);
			} else if(search_id!=null) {
				r=doDisplayBySearchId(h);
			} else {
				throw new ServletException("No search id, and no search_id");
			}
		} catch(Exception e) {
			throw new ServletException("Error displaying image:  " + e);
		}

		// Populate the hash with the image parts.
		r.addToHash(h);
		// Generate the XML
		String datachunk=r.showXML(self_uri);

		if(isAdmin()) {
			// Admin needs CATS
			int defcat=r.getCatNum();
			h.put("CATS", getCatList(defcat));
			out=tokenize("admin/display.inc", h);
		} else {
			// out=tokenize("display.inc", h);
			h.put("DATA", datachunk);
			sendXML("xml/display.xinc", h);
			out=null;
		}

		return(out);
	}

	protected PhotoSearchResult doDisplayBySearchId(Hashtable h)
		throws Exception {

		PhotoSearchResults results=null;
		results=(PhotoSearchResults)session.getValue("search_results");
		int which=Integer.parseInt(request.getParameter("search_id"));
		PhotoSearchResult r = results.get(which);

		// Add the PREV and NEXT button stuff, if applicable.
		if(results.nResults() > which+1) {
			h.put("NEXT",
				"<a href=\""
					+ PhotoXSLT.normalize(self_uri
						+ "?func=display&search_id=" + (which+1), true )
					+ "\">&gt;&gt;&gt;</a><br/>");
		} else {
			h.put("NEXT", "");
		}

		if(which>0) {
			h.put("PREV",
				"<a href=\""
				+ PhotoXSLT.normalize(self_uri
					+ "?func=display&search_id=" + (which-1), true )
				+ "\">&lt;&lt;&lt;</a><br/>");
		} else {
			h.put("PREV", "");
		}
		return(r);
	}

	// Find and display images.
	protected PhotoSearchResult doDisplayByID(Hashtable h) throws Exception {
		// Get the image_id.  We know it exists, because you can't get to
		// this function without one...of course, it may not parse.
		int image_id=Integer.parseInt(request.getParameter("id"));

		// Get the data
		PhotoSearchResult r=new PhotoSearchResult();
		r.find(image_id, remote_uid.intValue());

		// These don't apply here, bu they need to be defined.
		h.put("PREV", "");
		h.put("NEXT", "");

		return(r);
	}

	// Send the response text...
	protected void send_response(String text)
	{
		// set content type and other response header fields first
		response.setContentType("text/html");
		try {
			PrintWriter out = response.getWriter();

			String tail=null;
			SpyCache cache=new SpyCache();
			String key="t_tail_" + remote_uid + "." + isAdmin();
			tail=(String)cache.get(key);

			// If we didn't get a tail from the cache, build one.
			if(tail==null) {

				Hashtable ht=new Hashtable();
				if(isAdmin()) {
					ht.put("ADMIN_FLAG",
						" - <a href=\"" + self_uri
							+ "?func=unsetadmin\">Admin mode</a>");
				} else {
					ht.put("ADMIN_FLAG", "");
				}

				tail=tokenize("tail.inc", ht);
				// Store if for fifteen minutes.
				cache.store(key, tail, 15*60*1000);
			}

			// Print out the document and the tail together.
			out.print(text + tail);
			out.close();
		} catch(Exception e) {
			log("Error sending response:  " + e);
		}
	}

	// Display search results
	// This whole thing will fail if there's no session.
	protected String displaySearchResults() throws ServletException {
		if(session==null) {
			throw new ServletException("There's no session!");
		}
		PhotoSearchResults results=
			(PhotoSearchResults)session.getValue("search_results");
		if(results==null) {
			throw new ServletException("There are no search results!");
		}

		// We do the middle first, but, well, so what?
		String middle="";

		// Lock the results so the aheadfetcher can't mess us up once we
		// get going.
		synchronized(results) {
			int i=0;
			// if we have a starting point, let's start there.
			try {
				String startingS=request.getParameter("startfrom");
				if(startingS!=null) {
					int starting=Integer.parseInt(startingS);
					results.set(starting);
				}
			} catch(Exception e) {
				log("Error finding out starting point for search results:  "
					+ e);
			}

			middle="<search_result_row>\n";

			for(i=0; i<results.getMaxRet(); i++) {
				PhotoSearchResult r=results.next();
				if(r!=null) {
					// No, this really doesn't belong here.
					if( (i>0) && ((i) % 2) == 0) {
						middle += "</search_result_row>\n<search_result_row>\n";
					}
					middle += "<search_result>\n";
					middle += r.showXML(self_uri);
					middle += "</search_result>\n";
				}
			}

			middle+="</search_result_row>\n";
		}

		Hashtable h = new Hashtable();
		h.put("TOTAL", "" + results.nResults());
		h.put("SEARCH", (String)session.getValue("encoded_search"));
		h.put("RESULTS", middle);
		h.put("LINKTOMORE", linkToMore(results)); 
		// String output = tokenize("xml/results.xinc", h);

		// Ask the aheadfetcher to prefetch the next five entries.
		aheadfetcher.next(results);

		sendXML("xml/results.xinc", h);
		return(null);
	}

	// Find images.
	protected String doFind() throws ServletException {
		String output = "", middle = "";

		// Make sure there's a real session.
		if(session==null) {
			session=request.getSession(true);
		}

		try {
			PhotoSearch ps = new PhotoSearch();
			PhotoUser user = security.getUser(remote_user);
			PhotoSearchResults results=null;
			// Get the results and put them in the mofo session
			results=ps.performSearch(request, user);
			session.putValue("search_results", results);
			session.putValue("encoded_search",
				ps.encodeSearch(request));
		} catch(Exception e) {
			log("Error performing search:  " + e);
		}

		return(displaySearchResults());
	}

	// Link to more search results
	protected String linkToMore(PhotoSearchResults results) {
		String ret = "";
		int remaining=results.nRemaining();

		if(remaining>0) {
			// Decide how to display the button.  The button will read
			// ``Next x'' and x will either be the maximum search result
			// page size, or the number of remaining elements, whichever is
			// smaller.
			int nextwhu=results.getMaxRet();
			if(remaining<nextwhu) {
				nextwhu=remaining;
			}

			ret += "<linktomore>\n"
				+ "  <startfrom>" + results.current() + "</startfrom>\n"
				+ "  <remaining>" + remaining + "</remaining>\n"
				+ "  <nextpage>"  + nextwhu + "</nextpage>\n"
				+ "</linktomore>\n";
		} else {
			ret += "<linktomore>\n"
				+ "  <remaining>" + 0 + "</remaining>\n"
				+ "</linktomore>\n";
		}
		return(ret);
	}

	// Show an image
	protected String showImage() throws ServletException {

		int which=-1;
		boolean thumbnail=false;
		ServletOutputStream out=null;

		response.setContentType("image/jpeg");
		java.util.Date d=new java.util.Date();
		long l=d.getTime();
		// This is thirty days
		l+=25920000000L;
		response.setDateHeader("Expires", l);

		String s = request.getParameter("photo_id");
		which = Integer.valueOf(s).intValue();

		s=request.getParameter("thumbnail");
		if(s!=null) {
			thumbnail=true;
		}

		try {
			// The new swank image extraction object.
			PhotoImageHelper p = new PhotoImageHelper(which);

			// Need a binary output thingy.
			out = response.getOutputStream();

			// Image data
			PhotoImage image=null;

			if(thumbnail) {
				log("Requesting thumbnail");
				image=p.getThumbnail();
			} else {
				log("Requesting full image");
				image=p.getImage();
			}
			logger.log(new PhotoLogImageEntry(remote_uid.intValue(),
				which, true, request));
			out.write(image.getData());

		} catch(Exception e) {
			throw new ServletException("Error displaying image:  " + e);
		}
		// We handle our own response here, because this is an image.
		return(null);
	}

	protected String doLogView() throws ServletException {
		String view, out="";
		PhotoLogView logview=null;

		try {
			logview=new PhotoLogView(this);
		} catch(Exception e) {
			throw new ServletException("Error displaying logs:  " + e);
		}

		view=request.getParameter("view");
		if(view==null) {
			throw new ServletException("LogView without view");
		}

		if(view.equalsIgnoreCase("viewers")) {
			String which;
			which=request.getParameter("which");
			if(which==null) {
				throw new ServletException("LogView/viewers without which");
			}
			try {
				out=logview.getViewersOf(Integer.valueOf(which));
			} catch(Exception e) {
				throw new ServletException("Error displaying viewers:  " + e);
			}
		}
		return(out);
	}

	// Set administrative privys
	protected void setAdmin() throws ServletException {
		try {
			getGroups();
			if(groups.containsKey("admin")) {
				session.putValue("is_admin", "1");
			}
		} catch(Exception e) {
			log("Error setting admin privs:  " + e);
		}
	}

	// Revoke administrative privys
	protected void unsetAdmin() throws ServletException  {
		if(session!=null) {
			session.removeValue("is_admin");
		}
	}

	// Returns true if the session is an admin session
	public boolean isAdmin() {
		boolean ret=false;
		if(session!=null) {
			String admin= (String)session.getValue("is_admin");
			if(admin!=null) {
				ret=true;
			}
		}
		return(ret);
	}

	protected void debug (String msg) {
		if (debug) {
			log("PhotoSession debug: " + msg);
		}
	}


	// Tokenize a template file and return the tokenized stuff.
	protected String tokenize(String file, Hashtable vars) {
		return(PhotoUtil.tokenize(this, file, vars));
	}
}
