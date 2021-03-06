// Copyright (c) 2002  Dustin Sallings <dustin@spy.net>

package net.spy.photo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.spy.cache.SimpleCache;
import net.spy.db.Savable;
import net.spy.db.SaveContext;
import net.spy.db.SaveException;
import net.spy.db.TransactionListener;
import net.spy.photo.sp.DeleteAllProperties;
import net.spy.photo.sp.GetProperties;
import net.spy.photo.sp.InsertProperty;

/**
 * Persistent configuration stuff that can be changed dynamically.
 */
public class PhotoProperties extends Properties
	implements Savable, TransactionListener {

	private boolean isNew=false;
	private boolean isModified=false;

	/**
	 * Get an instance of PhotoProperties (defaulted by the PhotoConfig).
	 */
	public PhotoProperties() throws SQLException {
		super(PhotoConfig.getInstance());

		init();

		isNew=false;
		isModified=false;
	}
	
	/**
	 * Get the singleton instance of PhotoProperties.
	 */
	public static PhotoProperties getInstance()
		throws SQLException {

		// XXX:  use a real singleton here
		return(new PhotoProperties());
	}

	private void init() throws SQLException {
		SimpleCache sc=SimpleCache.getInstance();

		String key="photo_props";

		@SuppressWarnings("unchecked")
		Map<String, String> m=(Map<String,String>)sc.get(key);
		if(m==null) {
			m=initFromDB();
			sc.store(key, m, 900*1000);
		}

		putAll(m);
	}

	private Map<String, String> initFromDB() throws SQLException {
		Map<String, String> hm=new HashMap<String, String>();
		GetProperties gp=new GetProperties(PhotoConfig.getInstance());

		ResultSet rs=gp.executeQuery();
		while(rs.next()) {
			hm.put(rs.getString("name"), rs.getString("value"));
		}

		rs.close();
		gp.close();
		return(hm);
	}

	/** 
	 * Set a property.
	 * This also sets isModified to true.
	 * 
	 * @param k the key
	 * @param v the value
	 *
	 * @return the old value for that property
	 */
	@Override
	public Object setProperty(String k, String v) {
		isModified=true;
		return(super.setProperty(k, v));
	}

	// Savable implementation
	public boolean isModified() {
		return(isModified);
	}
	public boolean isNew() {
		return(isNew);
	}
	public Collection<Savable> getPreSavables(SaveContext context) {
		return(null);
	}
	public Collection<Savable> getPostSavables(SaveContext context) {
		return(null);
	}
	public void save(Connection conn, SaveContext context)
		throws SaveException, SQLException {

		DeleteAllProperties dap=new DeleteAllProperties(conn);
		dap.executeUpdate();
		dap.close();

		InsertProperty ip=new InsertProperty(conn);

		for(Map.Entry<Object, Object> me : entrySet()) {

			ip.setName((String)me.getKey());
			ip.setValue((String)me.getValue());

			int affected=ip.executeUpdate();
			if(affected!=1) {
				throw new SaveException(
					"Should've inserted a record, but didn't.");
			}
		}

		ip.close();
	}
	// End Savable implementation

	public void transactionCommited() {
		isModified=false;
		isNew=false;
	}

}
