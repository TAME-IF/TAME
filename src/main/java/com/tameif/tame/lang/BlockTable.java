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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.tameif.tame.struct.SerialReader;
import com.tameif.tame.struct.SerialWriter;

/**
 * The table of block entry to executable block.
 * @author Matthew Tropiano
 */
public class BlockTable implements Saveable
{
	/** Block map. */
	private HashMap<BlockEntry, Block> blockMap;
	
	/**
	 * Creates an empty block table.
	 */
	public BlockTable()
	{
		this.blockMap = new HashMap<>(4, 1.0f);
	}
	
	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static BlockTable create(InputStream in) throws IOException
	{
		BlockTable out = new BlockTable();
		out.readBytes(in);
		return out;
	}

	/**
	 * Adds a block to this block table.
	 * @param blockEntry the block entry descriptor.
	 * @param block the block that is associated with the entry.
	 */
	public void add(BlockEntry blockEntry, Block block)
	{
		blockMap.put(blockEntry, block);
	}

	/**
	 * Gets a matching block for this block entry.
	 * @param blockEntry the block entry descriptor.
	 * @return the associated block or null if not found.
	 */
	public Block get(BlockEntry blockEntry)
	{
		return blockMap.get(blockEntry);
	}
	
	/**
	 * @return an iterable structure for all entries in this table.
	 */
	public Iterable<Map.Entry<BlockEntry, Block>> getEntries()
	{
		return blockMap.entrySet();
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeInt(out, blockMap.size());
		for (Map.Entry<BlockEntry, Block> entry : blockMap.entrySet())
		{
			entry.getKey().writeBytes(out);
			entry.getValue().writeBytes(out);
		}
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		blockMap.clear();
		int size = sr.readInt(in);
		while (size-- > 0)
			add(BlockEntry.create(in), Block.create(in));
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
