/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.lang.Saveable;

/**
 * This is a player-activated action.
 * @author Matthew Tropiano
 */
public class TAction implements Comparable<TAction>, Saveable
{
	/**
	 * The action type.
	 */
	public static enum Type
	{	
		/** Has no targets. */
		GENERAL,
		/** Has one target. */
		TRANSITIVE,
		/** Has up to two targets. */
		DITRANSITIVE,
		/** Has very specific targets. */
		MODAL,
		/** Has an open target. */
		OPEN;
		
		// Get around unnecessary allocations.
		static Type[] VALUES = values();
	}

	/** Element's primary identity. */
	private String identity;

	/** 
	 * Action type.
	 * @see Type
	 */
	private Type type;
	
	/** What is the group of names of this action? */
	private CaseInsensitiveHash names;
	
	/** Does the action employ stricter use? */
	private boolean strict;
	/** Does the action reverse its strict handling? */
	private boolean reversed;
	
	/** 
	 * Additional strings.
	 * <ul> 
	 * <li>For <b>DITRANSITIVE</b>, this is the object separator conjunctions.</li>
	 * <li>For <b>MODAL</b>, these are the valid targets.</li>
	 * <li>For <b>OPEN</b>, these are the local variables that hold the input data (usually just one).</li>
	 * </ul>
	 */
	private CaseInsensitiveHash extraStrings;
	
	// Empty constructor.
	private TAction()
	{
		this.names = new CaseInsensitiveHash();
		this.extraStrings = new CaseInsensitiveHash();
		this.type = Type.GENERAL;
		this.strict = false;
		this.reversed = false;
	}
	
	/**
	 * Makes a blank action.
	 * @param identity this action's identity.
	 * @throws IllegalArgumentException if identity is blank.
	 */
	public TAction(String identity)
	{
		this();
		setIdentity(identity);
	}
	
	/** 
	 * Gets the identity (primary identifier name).
	 * @return this action's unique identity.
	 */
	public String getIdentity()
	{
		return identity;
	}
	
	/**
	 * Sets this action's identity.
	 * @param identity the unique identity.
	 * @throws IllegalArgumentException if identity is blank.
	 */
	private void setIdentity(String identity)
	{
		if (Common.isEmpty(identity))
			throw new IllegalArgumentException("Identity cannot be blank.");
		this.identity = identity;
	}

	/**
	 * Gets the action type.
	 * @return the action type.
	 */
	public Type getType()
	{
		return type;
	}

	/**
	 * Sets the action type.
	 * @param type the action type.
	 * @throws IllegalArgumentException if type is null.
	 */
	public void setType(Type type)
	{
		if (type == null)
			throw new IllegalArgumentException("Type cannot be null.");
		this.type = type;
	}

	/**
	 * Checks if this action is strict. Interpreter (and execution logic) changes if this is set:
	 * <ul>
	 * 	<li>For <b>OPEN</b>, nothing is different.</li>
	 * 	<li>For <b>GENERAL</b>, all input after the accepted verb is considered malformed.</li>
	 * 	<li>For <b>MODAL</b>, all input after the accepted verb and mode is considered malformed.</li>
	 * 	<li>For <b>TRANSITIVE</b>, all input after the accepted verb and object is considered malformed.</li>
	 * 	<li>
	 * 		For <b>DITRANSITIVE</b>, all input after the accepted verb, objects, and conjugates is considered malformed,
	 * 		and the order of the interpreted objects affect what <i>onActionWith</i> blocks are taken.
	 *	</li>
	 * </ul>
	 * @return true if restricted, false if not.
	 */
	public boolean isStrict()
	{
		return strict;
	}
	
	/**
	 * Sets if this action is strict.
	 * @param strict true if strict, false if not.
	 */
	public void setStrict(boolean strict) 
	{
		this.strict = strict;
	}
	
	/**
	 * Checks if this reverses the context of a strict ditransitive action.
	 * @return true if so, false if not.
	 */
	public boolean isReversed() 
	{
		return reversed;
	}
	
	/**
	 * Sets if this reverses the context of a strict ditransitive action.
	 * @param reversed true if so, false if not.
	 */
	public void setReversed(boolean reversed) 
	{
		this.reversed = reversed;
	}
	
	/**
	 * Adds an action name.
	 * This name is a parseable name via the interpreter that resolves to this action.
	 * Extra whitespace between this name are replaced with spaces.
	 * @param name the name to add.
	 */
	public void addName(String name)
	{
		names.put(name.replaceAll("\\s+", " "));
	}

	/**
	 * Checks if this action contains this name, case-insensitively.
	 * Keep in mind extra whitespace between names are replaced with spaces.
	 * @param name the name to check for.
	 * @return true if so, false if not.
	 */
	public boolean containsName(String name)
	{
		return names.contains(name);
	}
	
	/**
	 * Gets the names of this action.
	 * @return an iterable structure for the names.
	 */
	public Iterable<String> getNames()
	{
		return names;
	}

	/**
	 * Adds an extra string.
	 * This parseable string (via interpreter) can be a conjugate or mode, depending on the action type.
	 * Extra whitespace between these words are replaced with spaces.
	 * @param extraString the string to add.
	 */
	public void addExtraStrings(String extraString)
	{
		extraStrings.put(extraString.replaceAll("\\s+", " "));
	}

	/**
	 * Checks if this action contains this extra string, case-insensitively.
	 * Keep in mind extra whitespace between extra strings are replaced with spaces.
	 * @param extraString the string to check for.
	 * @return true if so, false if not.
	 */
	public boolean containsExtraString(String extraString)
	{
		return extraStrings.contains(extraString);
	}
	
	/**
	 * Gets this action's extra strings.
	 * @return an iterable structure for the extra strings.
	 */
	public Iterable<String> getExtraStrings()
	{
		return extraStrings;
	}

	@Override
	public int compareTo(TAction action)
	{
		return identity.compareTo(action.identity);
	}

	@Override
	public int hashCode()
	{
		return identity.hashCode();
	}
	
	@Override
	public boolean equals(Object object)
	{
		if (object instanceof TAction)
			return identity.equals(((TAction)object).identity);
		else 
			return super.equals(object);
	}
	
	/**
	 * Compares actions.
	 * Uses identity.
	 * @param action the other action.
	 * @return true if equal, false if not.
	 */
	public boolean equals(TAction action)
	{
		return identity.equals(action.identity);
	}

	@Override
	public String toString()
	{
		return "TAction ["+identity+"]";
	}
	
	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TAction create(InputStream in) throws IOException
	{
		TAction out = new TAction();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeString(identity, "UTF-8");
		sw.writeByte((byte)type.ordinal());
		
		sw.writeBit(strict);
		sw.writeBit(reversed);
		sw.flushBits();
		
		sw.writeInt(names.size());
		for (String s : names)
			sw.writeString(s.toLowerCase(), "UTF-8");
			
		sw.writeInt(extraStrings.size());
		for (String s : extraStrings)
			sw.writeString(s.toLowerCase(), "UTF-8");
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		names.clear();
		extraStrings.clear();
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		setIdentity(sr.readString("UTF-8"));
		type = Type.VALUES[(int)sr.readByte()];
		
		byte flags = sr.readByte();
		strict = (flags & 0x01) != 0;
		reversed = (flags & 0x02) != 0;
		
		int size;
		
		size = sr.readInt();
		while (size-- > 0)
			names.put(sr.readString("UTF-8"));
		
		size = sr.readInt();
		while (size-- > 0)
			extraStrings.put(sr.readString("UTF-8"));
	}

	@Override
	public byte[] toBytes() throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeBytes(bos);
		return bos.toByteArray();
	}

	@Override
	public void fromBytes(byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readBytes(bis);
		bis.close();
	}

}
