/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import com.tameif.tame.TAMEInterrupt;
import com.tameif.tame.TAMERequest;
import com.tameif.tame.TAMEResponse;
import com.tameif.tame.struct.SerialReader;
import com.tameif.tame.struct.SerialWriter;

/**
 * A set of operations in one block.
 * @author Matthew Tropiano
 */
public class Block implements CallableType, Iterable<Operation>, Saveable
{
	/** List of operations. */
	private Queue<Operation> operationQueue; 

	/**
	 * Creates a new empty block.
	 */
	public Block()
	{
		this.operationQueue = new LinkedList<Operation>();
	}

	/**
	 * Adds a statement to the block.
	 * @param statement the statement to add.
	 */
	public void add(Operation statement)
	{
		operationQueue.add(statement);
	}

	@Override
	public Iterator<Operation> iterator()
	{
		return operationQueue.iterator();
	}

	/**
	 * @return the amount of statements in the block.
	 */
	public int getCount()
	{
		return operationQueue.size();
	}

	@Override
	public void execute(TAMERequest request, TAMEResponse response, ValueHash blockLocal) throws TAMEInterrupt
	{
		for (Operation operation : this)
		{
			if (operation.getOperation().isInternal())
				response.trace(request, TraceType.INTERNAL, "CALL %s %s %s", 
					operation.getOperation().toString(), 
					operation.getOperand0() != null ? operation.getOperand0().toString() : "",
					operation.getOperand1() != null ? operation.getOperand1().toString() : ""
				);
			else
				response.trace(request, TraceType.FUNCTION, "CALL %s %s %s", 
					operation.getOperation().toString(), 
					operation.getOperand0() != null ? operation.getOperand0().toString() : "",
					operation.getOperand1() != null ? operation.getOperand1().toString() : ""
				);
			operation.execute(request, response, blockLocal);
		}
	}
	
	@Override
	public String toString()
	{
		return operationQueue.toString();
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
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeInt(out, operationQueue.size());
		for (Operation operation : this)
			operation.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		operationQueue.clear();
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		int size = sr.readInt(in);
		while (size-- > 0)
			add(Operation.create(in));
	}

}
