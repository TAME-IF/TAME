/*
 * 
 */
package net.mtrop.tame.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

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
	
	private BlockEntry()
	{
		this.entryType = null;
		this.values = null;
		this.hash = 0;
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

	@Override
	public int hashCode()
	{
		if (hash == 0)
		{
			int out = entryType.ordinal();
			for (int i = 0; i < values.length; i++)
				out += 31 * values[i].hashCode();
			hash = out;
		}
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
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeByte((byte)entryType.ordinal());
		sw.writeInt(values.length);
		for (int i = 0; i < values.length; i++)
			values[i].writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		entryType = BlockEntryType.VALUES[sr.readByte()];
		values = new Value[sr.readInt()];
		for (int i = 0; i < values.length; i++)
			values[i] = Value.create(in);
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
		StringBuilder sb = new StringBuilder();
		sb.append(entryType.name()).append('(');
		for (int i = 0; i < values.length; i++)
		{
			sb.append(values[i].toString());
			if (i < values.length - 1)
				sb.append(", ");
		}
		sb.append(')');
		return sb.toString();
	}
	
}
