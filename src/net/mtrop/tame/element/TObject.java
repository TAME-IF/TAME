/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockEntryType;

import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * Objects are common elements that players interact with, examine, talk to, eat, take - 
 * you get the idea. Objects can also be added to the inventory of players. Objects
 * in the possession of players take precedence over objects in a room.
 * @author Matthew Tropiano
 */
public class TObject extends TElement implements Inheritable<TObject>
{
	/** The parent object. */
	private TObject parent;
	/** Element's names. */
	protected CaseInsensitiveHash names;
	/** Element's names. */
	protected CaseInsensitiveHash tags;

	private TObject()
	{
		super();
		this.parent = null;
		this.names = new CaseInsensitiveHash(3);
		this.tags = new CaseInsensitiveHash(2);
	}
	
	/**
	 * Creates an empty object.
	 * @param identity the unique identity.
	 */
	public TObject(String identity) 
	{
		this();
		setIdentity(identity);
	}

	/**
	 * Checks if this object handles a particular entry type.
	 * @param type the entry type to check.
	 * @return true if so, false if not.
	 */
	public static boolean isValidEntryType(BlockEntryType type)
	{
		switch (type)
		{
			case INIT:
			case PROCEDURE:
			case ONACTION:
			case ONACTIONWITH:
			case ONACTIONWITHOTHER:
			case ONPLAYERBROWSE:
			case ONROOMBROWSE:
			case ONCONTAINERBROWSE:
			case ONWORLDBROWSE:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void setParent(TObject parent)
	{
		this.parent = parent;
	}
	
	@Override
	public TObject getParent()
	{
		return parent;
	}

	/**
	 * Adds an initial name to this object.
	 * These names are found by the interpreter.  
	 * @param name the name to add.
	 */
	public void addName(String name)
	{
		names.put(name);
	}
	
	/**
	 * Gets the initial names on this object as an iterable structure.
	 * @return the case-insensitive hash containing the names.
	 */
	public Iterable<String> getNameIterable()
	{
		return names;
	}

	/**
	 * Adds an initial tag to this object.
	 * These tags are used by some functions for doing stuff to objects.  
	 * @param tag the tag to add.
	 */
	public void addTag(String tag)
	{
		tags.put(tag);
	}
	
	/**
	 * Gets the initial tags on this object as an iterable structure.
	 * @return an iterable object for iteration.
	 */
	public Iterable<String> getTagIterable()
	{
		return tags;
	}

	@Override
	public Block resolveBlock(BlockEntry blockEntry)
	{
		Block out = getBlock(blockEntry);
		return out != null ? out : (parent != null ? parent.resolveBlock(blockEntry) : null);
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

		tags.clear();
		size = sr.readInt();
		while (size-- > 0)
			tags.put(sr.readString("UTF-8"));

	}

}