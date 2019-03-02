/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.util;

import java.util.regex.Pattern;

/**
 * Utility class for date-related stuff.
 * @author Matthew Tropiano
 */
public final class DateUtils 
{
	// The date format pattern.
	private static final Pattern DATE_FORMAT_PATTERN = Pattern.compile(
		"G+|y+|M+|d+|E+|u+|a+|A+|h+|H+|k+|K+|m+|s+|S+|z+|Z+|X+|'.*'"
	);
	
	/**
	 * The default locale for names of parts of the calendar. 
	 */
	public static final DateLocale DEFAULT_LOCALE = new DateLocale()
	{
		private final String[][] MONTHS = new String[][] {
			{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"},
			{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"},
		};
		
		private final String[][] DAYS = new String[][] {
			{"S", "M", "T", "W", "T", "F", "S"},
			{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"},
			{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
			{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"},
		};
		
		@Override
		public String[][] getMonthsOfYear() {
			return MONTHS;
		}
		
		@Override
		public String[][] getDaysOfWeek() {
			return DAYS;
		}
		
	};
	
	/**
	 * Formats a millisecond time into a formatted date string.
	 * <p>Uses repeated patterns of the following letters to output parts of a number that represents a
	 * point in time. The formatting string can output quoted characters as-is if those series of 
	 * characters are enclosed in single-quotes (<kbd>'</kbd>).
	 * <p>"Text" types print full names at 4 or more of the same letter (or may max out at less letters), 
	 * "number" types print a minimum of the amount of digits represented by the amount of letters, padding 
	 * the rest of the digits with zeroes, "month" types print numbers at 2 or less letters, text at 3 or more.  
	 * <table>
	 * 		<thead>
	 * 			<tr>
	 * 				<th>Letter</th>
	 * 				<th>Part of Date</th>
	 * 				<th>Type</th>
	 * 				<th>Examples</th>
	 * 			</tr>
	 * 		</thead>
	 *		<tbody>
	 * 			<tr>
	 * 				<td>G</td>
	 * 				<td>Era Designator</td>
	 * 				<td>Text</td>
	 * 				<td>A; AD</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>y</td>
	 * 				<td>Year</td>
	 * 				<td>Number</td>
	 * 				<td>09; 2009</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>M</td>
	 * 				<td>Month in Year</td>
	 * 				<td>Month</td>
	 * 				<td>09; Sep; September</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>d</td>
	 * 				<td>Day in Month</td>
	 * 				<td>Number</td>
	 * 				<td>5; 05</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>E</td>
	 * 				<td>Day of Week in Month (Name)</td>
	 * 				<td>Text</td>
	 * 				<td>T; Tu; Tue; Tuesday</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>u</td>
	 * 				<td>Day Number of Week (1=Monday, ... , 7=Sunday)</td>
	 * 				<td>Number</td>
	 * 				<td>2</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>a</td>
	 * 				<td>am/pm marker (lower case)</td>
	 * 				<td>Text</td>
	 * 				<td>a; am</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>A</td>
	 * 				<td>am/pm marker (upper case)</td>
	 * 				<td>Text</td>
	 * 				<td>A; AM</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>h</td>
	 * 				<td>Hour in Day (0-23)</td>
	 * 				<td>Number</td>
	 * 				<td>8; 08</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>H</td>
	 * 				<td>Hour in Day (1-24)</td>
	 * 				<td>Number</td>
	 * 				<td>9; 09</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>k</td>
	 * 				<td>Hour in Day (AM/PM) (0-11)</td>
	 * 				<td>Number</td>
	 * 				<td>8; 08</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>K</td>
	 * 				<td>Hour in Day (AM/PM) (1-12)</td>
	 * 				<td>Number</td>
	 * 				<td>9; 09</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>m</td>
	 * 				<td>Minute in Hour</td>
	 * 				<td>Number</td>
	 * 				<td>3; 03</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>s</td>
	 * 				<td>Seconds in Minute</td>
	 * 				<td>Number</td>
	 * 				<td>3; 03</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>S</td>
	 * 				<td>Milliseconds in Second</td>
	 * 				<td>Number</td>
	 * 				<td>567</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>z</td>
	 * 				<td>Time Zone</td>
	 * 				<td>Text</td>
	 * 				<td>GMT-08:00</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>Z</td>
	 * 				<td>RFC 822 Time Zone</td>
	 * 				<td>Text</td>
	 * 				<td>-0800</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>X</td>
	 * 				<td>Variable Length Time Zone</td>
	 * 				<td>Text</td>
	 * 				<td>-8; -08; -0800; -08:00; GMT-08:00</td>
	 * 			</tr>
	 *		</tbody>
	 * </table>
	 * <p>
	 * @param time the input time in milliseconds since the epoch.
	 * @param formatString the formatting string.
	 * @param utc if true, interpret as UTC.
	 * @return the output string.
	 */
	public static String formatTime(DateLocale locale, long time, String formatString, boolean utc)
	{
		StringBuilder sb = new StringBuilder();
		// TODO: Finish this.
		return sb.toString();
	}
	
	/**
	 * The locale interface for the output text for the formatting of dates.
	 */
	public static interface DateLocale
	{
		/**
		 * Gets the array lookup for the names of the days of the week.
		 * Must always return an array of dimensions [4][7].
		 * @return the array. 
		 */
		String[][] getDaysOfWeek(); 

		/**
		 * Gets the array lookup for the names of the months of the year.
		 * Must always return an array of dimensions [2][12].
		 * @return the array. 
		 */
		String[][] getMonthsOfYear(); 
	}
	
}
