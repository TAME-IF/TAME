/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.struct;

import com.blackrook.commons.linkedlist.Stack;

/**
 * The formatting parser for formatted text.
 * Formatted text uses a format like UBB that uses [tag] and [/] to surround text in order to style it.
 * In order to escape the bracket characters ('[' and ']'), add them twice (eg. "[[" outputs as "[").
 * The protected methods are called at tag boundaries as they are read.
 * @author Matthew Tropiano
 */
public abstract class TextFormattingParser
{
	/** The current tag stack during parse. */
	private Stack<String> tagStack;
	
	/**
	 * Creates a new text formatting parser.
	 */
	public TextFormattingParser()
	{
		this.tagStack = new Stack<>();
	}
	
	/**
	 * Parses a string and calls the tag methods
	 * @param text the input text to parse.
	 */
	public void parse(String text)
	{
		// TODO: Finish this.
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
