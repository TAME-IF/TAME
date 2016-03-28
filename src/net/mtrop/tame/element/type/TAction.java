/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.element.type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.lang.Saveable;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

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
	
	/** 
	 * Addtional strings. 
	 * For DITRANSITIVE, this is the object separator conjunctions.
	 * For MODAL, these are the valid targets.
	 */
	private CaseInsensitiveHash extraStrings;
	
	// Empty constructor.
	private TAction()
	{
		this.names = new CaseInsensitiveHash();
		this.extraStrings = new CaseInsensitiveHash();
		this.type = Type.GENERAL;
	}
	
	/**
	 * Makes a blank action.
	 * @param identity this action's identity.
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
	 */
	public void setType(Type type)
	{
		this.type = type;
	}

	/**
	 * Gets the names of this action.
	 * @return the case-insensitive hash containing the names.
	 */
	public CaseInsensitiveHash getNames()
	{
		return names;
	}

	/**
	 * Gets this action's extra strings.
	 * @return the case-insensitive hash containing the names.
	 */
	public CaseInsensitiveHash getExtraStrings()
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
		this.type = Type.values()[(int)sr.readByte()];
		
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
