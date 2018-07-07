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

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * Entry point descriptor for function calls on TAME elements.
 * @author Matthew Tropiano
 */
public class FunctionEntry implements Saveable 
{
	private static final String[] NO_ARGS = new String[0];
	
	/** Block-local variables, popped in reverse off stack. */
	private String[] arguments;
	/** The operation block to execute. */
	private Block block;

	private FunctionEntry()
	{
		this.arguments = NO_ARGS;
		this.block = null;
	}
	
	private FunctionEntry(Block block, String ... arguments)
	{
		this.block = block;
		this.arguments = arguments;
	}

	/**
	 * Creates a new FunctionEntry.
	 * @param block the function block.
	 * @param arguments the function argument names.
	 * @return a new FunctionEntry.
	 */
	public static FunctionEntry create(Block block, String ... arguments)
	{
		FunctionEntry out = new FunctionEntry();
		out.block = block;
		out.arguments = arguments;
		return out;
	}
	
	/**
	 * Creates a new FunctionEntry, null block (MUST BE SET LATER).
	 * @param arguments the function argument names.
	 * @return a new FunctionEntry.
	 */
	public static FunctionEntry create(String ... arguments)
	{
		FunctionEntry out = new FunctionEntry();
		out.block = null;
		out.arguments = arguments;
		return out;
	}
	
	/**
	 * Creates a new FunctionEntry from an input stream.
	 * @param in the input stream to read from.
	 * @return a new BloFunctionEntryckEntry.
	 * @throws IOException if a read error occurs.
	 */
	public static FunctionEntry create(InputStream in) throws IOException
	{
		FunctionEntry out = new FunctionEntry();
		out.readBytes(in);
		return out;
	}
	
	/**
	 * Gets the argument names for this function.
	 * @return an array of argument names, in the order declared.
	 */
	public String[] getArguments() 
	{
		return arguments;
	}
	
	/**
	 * Sets the function block on this entry.
	 * @param block the block to set.
	 */
	public void setBlock(Block block) 
	{
		this.block = block;
	}
	
	/**
	 * @return The function's block.
	 */
	public Block getBlock() 
	{
		return block;
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeInt(arguments.length);
		for (String arg : arguments)
			sw.writeString(arg, "UTF-8");
		block.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		int argCount = sr.readInt();
		int i = 0;
		arguments = new String[argCount];
		while (argCount-- > 0)
			arguments[i++] = sr.readString("UTF-8");
		block = Block.create(in);
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
