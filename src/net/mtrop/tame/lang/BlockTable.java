package net.mtrop.tame.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

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
	 * @param block the block that is associated with the entry.
	 */
	public Block get(BlockEntry blockEntry)
	{
		return blockMap.get(blockEntry);
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeInt(blockMap.size());
		for (ObjectPair<BlockEntry, Block> entry : blockMap)
		{
			entry.getKey().writeBytes(out);
			entry.getValue().writeBytes(out);
		}

	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		blockMap.clear();
		int size = sr.readInt();
		while (size-- > 0)
		{
			add(BlockEntry.create(in), Block.create(in));
		}
		
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
