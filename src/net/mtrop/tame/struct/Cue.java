/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame.struct;


/**
 * A cue is a piece of significant response sent back to the player on
 * a single request-response loop. Many cues can be sent back at once, and it
 * is up to the client how to process them.
 * @author Matthew Tropiano
 */
public class Cue
{	
	/** Type of cue. */
	protected String type;
	/** The content. */
	protected String content;
	
	/** 
	 * Creates a new message cue. 
	 */
	private Cue(String type, String content)
	{
		this.type = type;
		this.content = content;
	}
	
	/**
	 * Creates a new cue with no content.
	 * @param type the cue type.
	 */
	public static Cue create(String type)
	{
		return create(type, 0);
	}
	
	/**
	 * Creates a new cue.
	 * @param type the cue type.
	 * @param content the content.
	 */
	public static Cue create(String type, boolean content)
	{
		return new Cue(type, String.valueOf(content));
	}
	
	/**
	 * Creates a new cue.
	 * @param type the cue type.
	 * @param content the content.
	 */
	public static Cue create(String type, long content)
	{
		return new Cue(type, String.valueOf(content));
	}
	
	/**
	 * Creates a new cue.
	 * @param type the cue type.
	 * @param content the content.
	 */
	public static Cue create(String type, double content)
	{
		return new Cue(type, String.valueOf(content));
	}
	
	/**
	 * Creates a new cue.
	 * @param type the cue type.
	 * @param content the content.
	 */
	public static Cue create(String type, String content)
	{
		return new Cue(type, content);
	}
	
	/** Returns this message cue's type. */
	public String getType()
	{
		return type;
	}
	
	/**
	 * Checks if a cue is a specific type.
	 * @return true if match, false otherwise.
	 */
	public boolean isType(String type)
	{
		return getType().equals(type);
	}
	
	/** Returns this message cue's content. */
	public String getContent()
	{
		return content;
	}

	@Override
	public String toString()
	{
		return "["+type+": \""+content+"\"]";
	}
	
}

