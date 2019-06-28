package com.tameif.tame.struct;

import java.io.PrintStream;

/**
 * String utility libraries.
 * @author Matthew Tropiano
 */
public final class StringUtils
{

	/**
	 * Prints a message out to standard out, word-wrapped
	 * to a set column width (in characters). The width cannot be
	 * 1 or less or this does nothing. This will also turn any whitespace
	 * character it encounters into a single space, regardless of speciality.
	 * @param message the output message.
	 * @param width the width in characters.
	 * @return the ending column for subsequent calls.
	 */
	public static int printWrapped(CharSequence message, int width)
	{
		return printWrapped(System.out, message, width);
	}

	/**
	 * Prints a message out to a PrintStream, word-wrapped
	 * to a set column width (in characters). The width cannot be
	 * 1 or less or this does nothing. This will also turn any whitespace
	 * character it encounters into a single space, regardless of speciality.
	 * @param out the print stream to use. 
	 * @param message the output message.
	 * @param width the width in characters.
	 * @return the ending column for subsequent calls.
	 */
	public static int printWrapped(PrintStream out, CharSequence message, int width)
	{
		return printWrapped(out, message, 0, width);
	}

	/**
	 * Prints a message out to a PrintStream, word-wrapped
	 * to a set column width (in characters). The width cannot be
	 * 1 or less or this does nothing. This will also turn any whitespace
	 * character it encounters into a single space, regardless of speciality.
	 * @param out the print stream to use. 
	 * @param message the output message.
	 * @param startColumn the starting column.
	 * @param width the width in characters.
	 * @return the ending column for subsequent calls.
	 */
	public static int printWrapped(PrintStream out, CharSequence message, int startColumn, int width)
	{
		if (width <= 1) return startColumn;
		
		StringBuilder token = new StringBuilder();
		StringBuilder line = new StringBuilder();
		int ln = startColumn;
		int tok = 0;
		for (int i = 0; i < message.length(); i++)
		{
			char c = message.charAt(i);
			if (c == '\n')
			{
				line.append(token);
				ln += token.length();
				token.delete(0, token.length());
				tok = 0;
				out.println(line.toString());
				line.delete(0, line.length());
				ln = 0;
			}
			else if (Character.isWhitespace(c))
			{
				line.append(token);
				ln += token.length();
				if (ln < width-1)
				{
					line.append(' ');
					ln++;
				}
				token.delete(0, token.length());
				tok = 0;
			}
			else if (c == '-')
			{
				line.append(token);
				ln += token.length();
				line.append('-');
				ln++;
				token.delete(0, token.length());
				tok = 0;
			}
			else if (ln + token.length() + 1 > width-1)
			{
				out.println(line.toString());
				line.delete(0, line.length());
				ln = 0;
				token.append(c);
				tok++;
			}
			else
			{
				token.append(c);
				tok++;
			}
		}
		
		String linestr = line.toString();
		if (line.length() > 0)
			out.print(linestr);
		if (token.length() > 0)
			out.print(token.toString());
		
		return ln + tok;
	}

	/**
	 * Gets a full String representation of a Throwable type,
	 * including a line-by-line breakdown of the stack trace.
	 * @param t the throwable to render into a string.
	 * @return a multi-line string of the exception, similar to the stack dump.
	 */
	public static String getExceptionString(Throwable t)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(t.getClass().getName()+": "+t.getLocalizedMessage());
		sb.append('\n');
		for (StackTraceElement ent : t.getStackTrace())
		{
			sb.append(ent.toString());
			sb.append('\n');
		}
		if (t.getCause() != null)
		{
			sb.append("...Caused by:\n");
			sb.append(getExceptionString(t.getCause()));
		}
		return sb.toString();
	}

}
