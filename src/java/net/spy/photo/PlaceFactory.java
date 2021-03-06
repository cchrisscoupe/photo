// Copyright (c) 2006  Dustin Sallings <dustin@spy.net>

package net.spy.photo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import net.spy.db.Saver;
import net.spy.factory.GenFactoryImpl;
import net.spy.photo.impl.DBPlace;
import net.spy.photo.sp.SelectAllPlaces;
import net.spy.util.CloseUtil;

/**
 * Maker of places.
 */
public class PlaceFactory extends GenFactoryImpl<Place> {

	private static final String CACHE_KEY = "place";
	private static final long CACHE_TIME = 86400000l;

	private static PlaceFactory instance=null;

	protected PlaceFactory() {
		super(CACHE_KEY, CACHE_TIME);
	}

	/**
	 * Get the singleton PlaceFactory instance.
	 */
	public static synchronized PlaceFactory getInstance() {
		if(instance == null) {
			instance=new PlaceFactory();
		}
		return instance;
	}

	@Override
	protected Collection<Place> getInstances() {
		Collection<Place> rv=new ArrayList<Place>();
		SelectAllPlaces db=null;
		try {
			db=new SelectAllPlaces(PhotoConfig.getInstance());
			ResultSet rs=db.executeQuery();
			while(rs.next()) {
				rv.add(new DBPlace(rs));
			}
			rs.close();
		} catch(SQLException e ) {
			throw new RuntimeException("Couldn't load places", e);
		} finally {
			CloseUtil.close(db);
		}
		return rv;
	}

	/**
	 * Get a mutable place.
	 *
	 * @param id the ID of the place
	 * @return the MutablePlace
	 */
	public MutablePlace getMutable(int id) {
		MutablePlace mp=(MutablePlace)getObject(id);
		return mp;
	}

	/**
	 * Create a new place.
	 *
	 * @return the new place
	 * @throws Exception if a place can't be created
	 */
	public MutablePlace create() throws Exception {
		return new DBPlace();
	}

	/**
	 * Persist the MutablePlace.
	 *
	 * @param mp the mutable place
	 * @throws Exception if the item can't be persisted
	 */
	public void persist(MutablePlace mp) throws Exception {
		Saver s=new Saver(PhotoConfig.getInstance());
		s.save(mp);
		recache();
	}
}
