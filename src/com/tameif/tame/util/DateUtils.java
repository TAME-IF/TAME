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

import java.text.SimpleDateFormat;
import java.util.regex.Pattern;

/**
 * Utility class for date-related stuff.
 * @author Matthew Tropiano
 */
public final class DateUtils 
{
	// The date format pattern.
	private static final Pattern DATE_FORMAT_PATTERN = Pattern.compile(
		"G+|y+|M+|d+|F+|E+|a+|H+|k+|K+|h+|m+|s+|S+|z+|Z+|'.*'"
	);
	
	// The default locale for names of parts of the calendar. 
	private static final DateLocale DEFAULT_LOCALE = new DateLocale()
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
	 * <p>Similar to {@link SimpleDateFormat}, this outputs a formatted date.
	 * <p>Uses repeated patterns of the following letters to output parts of a number that represents a
	 * point in time. The formatting string can output quoted characters as-is if those series of 
	 * characters are enclosed in single-quotes (<kbd>'</kbd>).
	 * <p>"Text" types print full names at 4 or more of the same letter, "number" types print a minimum of the amount
	 * of digits represented by the amount of letters, padding the rest of the digits with zeroes, "month" types
	 * print numbers at 2 or less letters, text at 3 or more.  
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
	 * 				<td>F</td>
	 * 				<td>Day in Month</td>
	 * 				<td>Number</td>
	 * 				<td>5; 05</td>
	 * 			</tr>
	 * 			<!-- FINISH THIS -->
	 *		</tbody>
	 * </table>
	 * <p>
	 * @param time the input time in milliseconds since the epoch.
	 * @param formatString the formatting string.
	 * @param utc if true, interpret as UTC.
	 * @return the output string.
	 */
	public static String formatTime(long time, String formatString, boolean utc)
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
