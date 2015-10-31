package net.mtrop.tame.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.interrupt.TAMEInterrupt;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * A set of commands in one block.
 * @author Matthew Tropiano
 */
public class Block implements CallableType, Iterable<Command>, Saveable
{
	/** List of statements. */
	private Queue<Command> statementQueue; 

	/**
	 * Creates a new empty block.
	 */
	public Block()
	{
		this.statementQueue = new Queue<Command>();
	}

	/**
	 * Adds a statement to the block.
	 * @param statement the statement to add.
	 */
	public void add(Command statement)
	{
		statementQueue.add(statement);
	}
	
	@Override
	public Iterator<Command> iterator()
	{
		return statementQueue.iterator();
	}

	/**
	 * @return the amount of statements in the block.
	 */
	public int getCount()
	{
		return statementQueue.size();
	}

	@Override
	public void call(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		for (Command command : this)
		{
			response.trace(request, "CALL %s", command);
			command.call(request, response);
		}
	}
	
	@Override
	public String toString()
	{
		return statementQueue.toString();
	}
	
	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static Block create(InputStream in) throws IOException
	{
		Block out = new Block();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeInt(statementQueue.size());
		for (Command command : this)
			command.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		statementQueue.clear();
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		int size = sr.readInt();
		
		while (size-- > 0)
			add(Command.create(in));
		
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
