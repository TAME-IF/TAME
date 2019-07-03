package com.tameif.tame.struct;

/**
 * Object functions.
 * @author Matthew Tropiano
 */
public final class ValueUtils
{

	/**
	 * Attempts to parse a boolean from a string.
	 * If the string is null, this returns false.
	 * If the string does not equal "true" (case ignored), this returns false.
	 * @param s the input string.
	 * @return the interpreted boolean.
	 */
	public static boolean parseBoolean(String s)
	{
		if (s == null || !s.equalsIgnoreCase("true"))
			return false;
		else
			return true;
	}

	/**
	 * Attempts to parse an int from a string.
	 * If the string is null or the empty string, this returns 0.
	 * @param s the input string.
	 * @return the interpreted integer.
	 */
	public static int parseInt(String s)
	{
		if (s == null)
			return 0;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Attempts to parse a long from a string.
	 * If the string is null or the empty string, this returns 0.
	 * @param s the input string.
	 * @return the interpreted long integer.
	 */
	public static long parseLong(String s)
	{
		if (s == null)
			return 0L;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	/**
	 * Attempts to parse a boolean from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * If the string does not equal "true," this returns false.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted boolean or def if the input string is blank.
	 */
	public static boolean parseBoolean(String s, boolean def)
	{
		if (isStringEmpty(s))
			return def;
		else if (!s.equalsIgnoreCase("true"))
			return false;
		else
			return true;
	}

	/**
	 * Attempts to parse an int from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted integer or def if the input string is blank.
	 */
	public static int parseInt(String s, int def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * Attempts to parse a long from a string.
	 * If the string is null or the empty string, this returns <code>def</code>.
	 * @param s the input string.
	 * @param def the fallback value to return.
	 * @return the interpreted long integer or def if the input string is blank.
	 */
	public static long parseLong(String s, long def)
	{
		if (isStringEmpty(s))
			return def;
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public static boolean isStringEmpty(Object obj)
	{
		if (obj == null)
			return true;
		else
			return ((String)obj).trim().length() == 0;
	}

	/**
	 * Returns the enum instance of a class given class and name, or null if not a valid name.
	 * If value is null, this returns null.
	 * @param <T> the Enum object type.
	 * @param value the value to search for.
	 * @param enumClass the Enum class to inspect.
	 * @return the enum value or null if the target does not exist.
	 */
	public static <T extends Enum<T>> T getEnumInstance(String value, Class<T> enumClass)
	{
		if (value == null)
			return null;
		
		try {
			return Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Escape string.
	 * @param s the input string.
	 * @return the string with escape characters in it.
	 */
	public static String escapeString(String s)
	{
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			switch (c)
			{
				case '\0':
					out.append("\\0");
					break;
				case '\b':
					out.append("\\b");
					break;
				case '\t':
					out.append("\\t");
					break;
				case '\n':
					out.append("\\n");
					break;
				case '\f':
					out.append("\\f");
					break;
				case '\r':
					out.append("\\r");
					break;
				case '\\':
					out.append("\\\\");
					break;
				case '"':
					out.append("\\\"");    					
					break;
				default:
					if (c < 0x0020 || c >= 0x7f)
						out.append("\\u"+String.format("%04x", (int)c));
					else
						out.append(c);
					break;
			}
		}
		
		return out.toString();
	}
	
	
}
