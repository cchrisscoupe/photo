// Copyright (c) 2000  Dustin Sallings <dustin@spy.net>

package net.spy.photo.migration;

import net.spy.SpyDB;

import net.spy.photo.PhotoConfig;

public class PhotoMigration02 extends PhotoMigration {

	private void addColumns() throws Exception {
		SpyDB db=new SpyDB(new PhotoConfig());
		try {
			db.executeUpdate("alter table wwwacl add column canview boolean");
			db.executeUpdate("alter table wwwacl alter column canview "
				+ "set default true");
			db.executeUpdate("update wwwacl set canview=true");
		} catch(Exception e) {
			System.err.println("Error adding column:  " + e);
		}
		try {
			db.executeUpdate("alter table wwwacl add column canadd boolean");
			db.executeUpdate("alter table wwwacl alter column canadd "
				+ "set default false");
			// This emulates the ``old'' way, set the canadd flag to true
			// for every category the user has an ACL entry for if the user
			// has a canadd flag set.
			db.executeUpdate("update wwwacl set canadd="
				+ "(select canadd from wwwusers where "
				+ "wwwacl.userid = wwwusers.id)");
		} catch(Exception e) {
			System.err.println("Error adding column:  " + e);
		}
		db.close();
	}

	public void migrate() throws Exception {
		if((hasColumn("wwwacl", "canadd")) && (hasColumn("wwwacl", "canview"))
			) {
			System.err.println("Looks like you've already run this kit.");
		} else {
			// Add the new columns.
			addColumns();
		}
	}

	public static void main(String args[]) throws Exception {
		PhotoMigration02 mig=new PhotoMigration02();
		mig.migrate();
	}
}