// Copyright (c) 2001  Dustin Sallings <dustin@spy.net>

package net.spy.photo.filter;

import java.util.Calendar;
import java.util.Date;

/**
 * This filter randomly selects one picture per month from a group and
 * throws the rest away.
 */
public class OnceAMonthFilter extends DateFilter {

	/** 
	 * Truncate the date to the beginning of the month.
	 */
	@Override
	protected Date roundDate(Date d) {
		Calendar cal=Calendar.getInstance();
		cal.setTime(d);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		return(cal.getTime());
	}

}
