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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for date-related stuff.
 * @author Matthew Tropiano
 */
public final class DateUtils 
{
	// The date format pattern.
	private static final Pattern DATE_FORMAT_PATTERN = Pattern.compile(
		"E+|e+|y+|M+|d+|w+|W+|a+|A+|h+|H+|k+|K+|m+|s+|S+|z+|Z+|X+|'.*'"
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
	
	// E+|e+|y+|M+|d+|W+|u+|a+|A+|h+|H+|k+|K+|m+|s+|S+|z+|Z+|X+|'.*'
	static HashMap<Character, FormatFunction> FORMAT_FUNCS = new HashMap<Character, FormatFunction>()
	{	
		private static final long serialVersionUID = 4313042438938002096L;
		
		// init funcs.
		{
			put('E', (locale, token, date, sb)->
			{
				int val = date.get(Calendar.ERA);
				if (val == GregorianCalendar.BC)
					sb.append(token.length() < 2 ? "B" : "BC");
				else
					sb.append(token.length() < 2 ? "A" : "AD");
			});
			put('e', (locale, token, date, sb)->
			{
				int val = date.get(Calendar.ERA);
				if (val == GregorianCalendar.BC)
					sb.append(token.length() < 2 ? "b" : "bc");
				else
					sb.append(token.length() < 2 ? "a" : "ad");
			});
			put('y', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.YEAR)));
			});
			put('M', (locale, token, date, sb)->
			{
				if (token.length() <= 2)
					sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.MONTH) + 1));
				else
				{
					int month = date.get(Calendar.MONTH);
					sb.append(locale.getMonthsOfYear()[MathUtils.clampValue(token.length() - 3, 0, 1)][month]);
				}
			});
			put('d', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.DAY_OF_MONTH)));
			});
			put('W', (locale, token, date, sb)->
			{
				sb.append(locale.getDaysOfWeek()[MathUtils.clampValue(token.length() - 1, 0, 3)][date.get(Calendar.DAY_OF_WEEK) - 1]);
			});
			put('w', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.DAY_OF_WEEK) - 1));
			});
			put('a', (locale, token, date, sb)->
			{
				int val = date.get(Calendar.HOUR_OF_DAY);
				if (val < 12)
					sb.append(token.length() < 2 ? "a" : "am");
				else
					sb.append(token.length() < 2 ? "p" : "pm");
			});
			put('A', (locale, token, date, sb)->
			{
				int val = date.get(Calendar.HOUR_OF_DAY);
				if (val < 12)
					sb.append(token.length() < 2 ? "A" : "AM");
				else
					sb.append(token.length() < 2 ? "P" : "PM");
			});
			put('h', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.HOUR_OF_DAY)));
			});
			put('H', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.HOUR_OF_DAY) + 1));
			});
			put('k', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.HOUR_OF_DAY) % 12));
			});
			put('K', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", (date.get(Calendar.HOUR_OF_DAY) % 12) + 1));
			});
			put('m', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.MINUTE)));
			});
			put('s', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.SECOND)));
			});
			put('S', (locale, token, date, sb)->
			{
				sb.append(String.format("%0" + token.length() + "d", date.get(Calendar.MILLISECOND)));
			});
			put('z', (locale, token, date, sb)->
			{
				int minuteOffset = date.get(Calendar.ZONE_OFFSET) / (60 * 1000);
				int absMinuteOffset = Math.abs(minuteOffset);
				sb.append(String.format("GMT%c%02d:%02d", (minuteOffset < 0 ? '-' : '+'), absMinuteOffset / 60, absMinuteOffset % 60));
			});
			put('Z', (locale, token, date, sb)->
			{
				int minuteOffset = date.get(Calendar.ZONE_OFFSET) / (60 * 1000);
				int absMinuteOffset = Math.abs(minuteOffset);
				sb.append(String.format("%c%02d%02d", (minuteOffset < 0 ? '-' : '+'), absMinuteOffset / 60, absMinuteOffset % 60));
			});
			put('X', (locale, token, date, sb)->
			{
				int minuteOffset = date.get(Calendar.ZONE_OFFSET) / (60 * 1000);
				int absMinuteOffset = Math.abs(minuteOffset);
				switch (token.length())
				{
					case 1:
						sb.append(String.format("%c%d", (minuteOffset < 0 ? '-' : '+'), absMinuteOffset / 60));
						break;
					case 2:
						sb.append(String.format("%c%02d", (minuteOffset < 0 ? '-' : '+'), absMinuteOffset / 60));
						break;
					case 3:
						sb.append(String.format("%c%02d%02d", (minuteOffset < 0 ? '-' : '+'), absMinuteOffset / 60, absMinuteOffset % 60));
						break;
					case 4:
						sb.append(String.format("%c%02d:%02d", (minuteOffset < 0 ? '-' : '+'), absMinuteOffset / 60, absMinuteOffset % 60));
						break;
					default:
						sb.append(String.format("GMT%c%02d:%02d", (minuteOffset < 0 ? '-' : '+'), absMinuteOffset / 60, absMinuteOffset % 60));
						break;
				}
			});
			put('\'', (locale, token, date, sb)->
			{
				if (token.length() - 2 == 0)
					sb.append('\'');
				else
					sb.append(token.substring(1, token.length() - 1));
			});
		}
	};

	
	/**
	 * Formats a millisecond time into a formatted date string.
	 * <p>Uses repeated patterns of the following letters to output parts of a number that represents a
	 * point in time. The formatting string can output quoted characters as-is if those series of 
	 * characters are enclosed in single-quotes (<kbd>'</kbd>). Unrecognized characters are output as-is.
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
	 * 				<td>e</td>
	 * 				<td>Era Designator (lowercase)</td>
	 * 				<td>Text</td>
	 * 				<td>a; ad</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>E</td>
	 * 				<td>Era Designator (uppercase)</td>
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
	 * 				<td>W</td>
	 * 				<td>Day of Week in Month (Name)</td>
	 * 				<td>Text</td>
	 * 				<td>T; Tu; Tue; Tuesday</td>
	 * 			</tr>
	 * 			<tr>
	 * 				<td>w</td>
	 * 				<td>Day Number of Week (0=Sunday, ... , 6=Saturday)</td>
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
	 * 				<td>AM/PM marker (upper case)</td>
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
	 * @param locale the locale type to use for the names of days and months.
	 * @param time the input time in milliseconds since the epoch.
	 * @param formatString the formatting string.
	 * @return the output string.
	 */
	public static String formatTime(final DateLocale locale, final long time, final String formatString)
	{
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(time);
		Matcher matcher = DATE_FORMAT_PATTERN.matcher(formatString);
				
		int lastEnd = 0; 
		StringBuilder sb = new StringBuilder();
		while (matcher.find())
		{
			int start = matcher.start();
			if (start > lastEnd)
				sb.append(formatString.substring(lastEnd, start));
			
			FormatFunction func;
			char fc = formatString.charAt(start);
			if ((func = FORMAT_FUNCS.get(fc)) != null)
				func.format(locale, matcher.group(), calendar, sb);
			
			lastEnd = matcher.end();
		}
		
		if (lastEnd < formatString.length())
			sb.append(formatString.substring(lastEnd));
		
		return sb.toString();
	}
	
	/**
	 * Format functions.
	 */
	@FunctionalInterface
	private static interface FormatFunction
	{
		void format(DateLocale locale, String token, Calendar date, StringBuilder builder);
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
