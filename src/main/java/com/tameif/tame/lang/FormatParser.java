/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

import java.util.Deque;
import java.util.LinkedList;

/**
 * The formatting parser for formatted text.
 * Formatted text uses a format like UBB that uses [tag] and [/] to surround text in order to style it.
 * In order to escape the bracket characters ('[' and ']'), add them twice (eg. "[[" outputs as "[").
 * The protected methods are called at tag boundaries as they are read.
 * @author Matthew Tropiano
 */
public abstract class FormatParser
{
	/** The current string builder during parse. */
	private StringBuilder builder;
	/** The current tag stack during parse. */
	private Deque<String> tagStack;
	
	/**
	 * Creates a new text formatting parser.
	 */
	public FormatParser()
	{
		this.builder = new StringBuilder();
		this.tagStack = new LinkedList<>();
	}
	
	/**
	 * Parses a string and calls the tag methods
	 * @param sequence the input text to parse.
	 */
	public void parse(CharSequence sequence)
	{
		final int STATE_TEXT = 0;
		final int STATE_TAG_MAYBE = 1;
		final int STATE_TAG = 2;
		final int STATE_TAG_END_MAYBE = 3;
		
		int state = STATE_TEXT;
		
		int len = sequence.length(), i = 0;
		
		while (i < len)
		{
			char c = sequence.charAt(i);
			
			switch (state)
			{
				case STATE_TEXT:
				{
					if (c == '[')
						state = STATE_TAG_MAYBE;
					else
						builder.append(c);
				}
				break;

				case STATE_TAG_MAYBE:
				{
					if (c == '[')
					{
						state = STATE_TEXT;
						builder.append(c);
					}
					else
					{
						state = STATE_TAG;
						emitText();
						i--;
					}
				}
				break;
				
				case STATE_TAG:
				{
					if (c == ']')
						state = STATE_TAG_END_MAYBE;
					else
						builder.append(c);
				}
				break;
				
				case STATE_TAG_END_MAYBE:
				{
					if (c == ']')
					{
						state = STATE_TAG;
						builder.append(c);
					}
					else
					{
						state = STATE_TEXT;
						emitTag();
						i--;
					}
				}
				break;
			}
			
			i++;
		}
		
		if (state == STATE_TAG_END_MAYBE)
			emitTag();
		
		emitText();
		while (!tagStack.isEmpty())
			endTag(tagStack.pollFirst());
	}
	
	private void emitText()
	{
		if (builder.length() == 0)
			return;
		
		sendText(builder.toString());
		builder.delete(0, builder.length());
	}

	private void emitTag()
	{
		if (builder.length() == 0)
			return;

		String tag = builder.toString();
		builder.delete(0, builder.length());
		
		if (tag.equals("/"))
		{
			if (tagStack.isEmpty())
				return;
			endTag(tagStack.pollFirst());
		}
		else
		{
			tagStack.push(tag);
			startTag(tag);
		}
	}

	
	/**
	 * Called at the start of a tag.
	 * @param tagName the name of the started tag.
	 */
	public abstract void startTag(String tagName);

	/**
	 * Called when a chunk of text needs to be output (before/in/after tags).
	 * @param text the output text.
	 */
	public abstract void sendText(String text);

	/**
	 * Called at the end of a tag.
	 * @param tagName the name of the ended tag.
	 */
	public abstract void endTag(String tagName);

}
