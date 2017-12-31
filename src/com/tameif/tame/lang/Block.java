/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
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
import java.util.Iterator;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.TAMEInterrupt;
import com.tameif.tame.TAMERequest;
import com.tameif.tame.TAMEResponse;

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
	public void execute(TAMERequest request, TAMEResponse response, ValueHash blockLocal) throws TAMEInterrupt
	{
		response.trace(request, "Start block.");
		for (Command command : this)
		{
			response.trace(request, "CALL %s", command);
			command.execute(request, response, blockLocal);
		}
		response.trace(request, "End block.");
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