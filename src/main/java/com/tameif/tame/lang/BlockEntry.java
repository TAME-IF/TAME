/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.tameif.tame.struct.SerialReader;
import com.tameif.tame.struct.SerialWriter;

/**
 * Describes a single block entry stub for a block table.
 * @author Matthew Tropiano
 */
public class BlockEntry implements Saveable
{
	/** Block entry type. */
	private BlockEntryType entryType;
	/** Matching values. */
	private Value[] values;
	
	private int hash;
	private String entryString;
	
	private BlockEntry()
	{
		this.entryType = null;
		this.values = null;
		this.hash = 0;
		this.entryString = null;
	}
	
	private BlockEntry(BlockEntryType entryType, Value... values)
	{
		this.entryType = entryType;
		this.values = values;
	}
	
	/**
	 * Creates a new BlockEntry.
	 * @param entryType the entry type.
	 * @param values the values to match.
	 * @return a new BlockEntry.
	 */
	public static BlockEntry create(BlockEntryType entryType, Value... values)
	{
		return new BlockEntry(entryType, values);
	}
	
	/**
	 * Creates a new BlockEntry from an input stream.
	 * @param in the input stream to read from.
	 * @return a new BlockEntry.
	 * @throws IOException if a read error occurs.
	 */
	public static BlockEntry create(InputStream in) throws IOException
	{
		BlockEntry out = new BlockEntry();
		out.readBytes(in);
		return out;
	}
	
	/**
	 * Gets the entry type.
	 * @return the entry type.
	 */
	public BlockEntryType getEntryType()
	{
		return entryType;
	}
	
	/**
	 * Gets the values to match.
	 * @return the entry values that need matching.
	 */
	public Value[] getValues()
	{
		return values;
	}
	
	/**
	 * Returns a string representation of this entry.
	 * Useful for export to languages that cannot use objects as lookup keys.
	 * @return a string representation of this entry.
	 */
	public String getEntryString()
	{
		if (entryString != null)
			return entryString;
		
		StringBuilder sb = new StringBuilder();
		sb.append(entryType.name()).append('(');
		for (int i = 0; i < values.length; i++)
		{
			sb.append(values[i].toString());
			if (i < values.length - 1)
				sb.append(",");
		}
		sb.append(')');
		entryString = sb.toString();
		
		return entryString;
	}

	@Override
	public int hashCode()
	{
		if (hash == 0)
			hash = getEntryString().hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof BlockEntry)
			return equals((BlockEntry)obj);
		return super.equals(obj);
	}
	
	/**
	 * Checks if this entry is equal to another entry.
	 * @param entry the entry to compare to.
	 * @return true if equal, false if not.
	 */
	public boolean equals(BlockEntry entry)
	{
		if (!entryType.equals(entry.entryType))
			return false;
		
		if (values.length != entry.values.length)
			return false;
		
		for (int i = 0; i < values.length; i++)
			if (!values[i].equals(entry.values[i]))
				return false;
		
		return true;
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeByte(out, (byte)entryType.ordinal());
		sw.writeInt(out, values.length);
		for (int i = 0; i < values.length; i++)
			values[i].writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		entryType = BlockEntryType.VALUES[sr.readByte(in)];
		values = new Value[sr.readInt(in)];
		for (int i = 0; i < values.length; i++)
			values[i] = Value.read(in);
	}

	/**
	 * Gets the string representation of this as a non-debugging-human-friendly string.
	 * @return an output-friendly string of this entry.
	 */
	public String toFriendlyString() 
	{
		StringBuilder sb = new StringBuilder();
		sb.append(entryType.name()).append('(');
		for (int i = 0; i < values.length; i++)
		{
			if (values[i].isString())
				sb.append('"').append(values[i].asString()).append('"');
			else
				sb.append(values[i].asString());
			if (i < values.length - 1)
				sb.append(", ");
		}
		sb.append(')');
		return sb.toString();
	}
	
	@Override
	public String toString() 
	{
		return getEntryString();
	}
	
}
