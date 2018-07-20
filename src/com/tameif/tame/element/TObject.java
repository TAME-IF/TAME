/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.lang.BlockEntryType;

/**
 * Objects are common elements that players interact with, examine, talk to, eat, take - 
 * you get the idea. Objects can also be added to the inventory of players. Objects
 * in the possession of players take precedence over objects in a room.
 * @author Matthew Tropiano
 */
public class TObject extends TElement
{
	/** Element's names. */
	protected CaseInsensitiveHash names;
	/** Element's determiners (name token affixes). */
	protected CaseInsensitiveHash determiners;
	/** Element's names. */
	protected CaseInsensitiveHash tags;

	private TObject()
	{
		super();
		this.names = new CaseInsensitiveHash(3);
		this.determiners = new CaseInsensitiveHash(2);
		this.tags = new CaseInsensitiveHash(2);
	}
	
	/**
	 * Creates an empty object.
	 * @param identity the unique identity.
	 * @param parent the object's parent object.
	 */
	public TObject(String identity, TObject parent) 
	{
		this();
		setIdentity(identity);
		setParent(parent);
	}

	/**
	 * Creates an empty object.
	 * @param identity the unique identity.
	 */
	public TObject(String identity) 
	{
		this(identity, null);
	}

	@Override
	public final boolean isValidEntryType(BlockEntryType type)
	{
		switch (type)
		{
			case INIT:
			case ONACTION:
			case ONACTIONWITH:
			case ONACTIONWITHANCESTOR:
			case ONACTIONWITHOTHER:
			case ONELEMENTBROWSE:
			case ONWORLDBROWSE:
			case ONPLAYERBROWSE:
			case ONROOMBROWSE:
			case ONCONTAINERBROWSE:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Adds an initial name to this object.
	 * These names are found by the interpreter.  
	 * @param name the name to add.
	 */
	public void addName(String name)
	{
		if (isArchetype())
			throw new ModuleException("Object that are archetypes cannot have names.");
		names.put(name);
	}
	
	/**
	 * Gets the initial names on this object as an iterable structure.
	 * @return the case-insensitive hash containing the names.
	 */
	public Iterable<String> getNames()
	{
		return names;
	}

	/**
	 * Adds a name determiner to this object.
	 * These are prepended to names when they are added/removed in contexts.  
	 * @param determiner the name to add.
	 */
	public void addDeterminer(String determiner)
	{
		if (isArchetype())
			throw new ModuleException("Object that are archetypes cannot have name determiners.");
		determiners.put(determiner);
	}
	
	/**
	 * Gets the name determiners on this object as an iterable structure.
	 * @return the case-insensitive hash containing the determiners.
	 */
	public Iterable<String> getDeterminers()
	{
		return determiners;
	}

	/**
	 * Adds an initial tag to this object.
	 * These tags are used by some functions for doing stuff to objects.  
	 * @param tag the tag to add.
	 */
	public void addTag(String tag)
	{
		if (isArchetype())
			throw new ModuleException("Object that are archetypes cannot have tags.");
		tags.put(tag);
	}
	
	/**
	 * Gets the initial tags on this object as an iterable structure.
	 * @return an iterable object for iteration.
	 */
	public Iterable<String> getTags()
	{
		return tags;
	}

	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TObject create(InputStream in) throws IOException
	{
		TObject out = new TObject();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		super.writeBytes(out);
		
		sw.writeInt(names.size());
		for (String name : names)
			sw.writeString(name.toLowerCase(), "UTF-8");
		sw.writeInt(determiners.size());
		for (String determiner : determiners)
			sw.writeString(determiner.toLowerCase(), "UTF-8");
		sw.writeInt(tags.size());
		for (String tag : tags)
			sw.writeString(tag.toLowerCase(), "UTF-8");
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		super.readBytes(in);
		
		int size;
		
		names.clear();
		size = sr.readInt();
		while (size-- > 0)
			names.put(sr.readString("UTF-8"));

		determiners.clear();
		size = sr.readInt();
		while (size-- > 0)
			determiners.put(sr.readString("UTF-8"));

		tags.clear();
		size = sr.readInt();
		while (size-- > 0)
			tags.put(sr.readString("UTF-8"));

	}

}
