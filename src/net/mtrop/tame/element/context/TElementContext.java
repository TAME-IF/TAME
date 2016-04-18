/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.element.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.lang.StateSaveable;
import net.mtrop.tame.lang.Value;
import net.mtrop.tame.lang.ValueHash;

/**
 * Holds contextual information for a {@link TElement}.
 * @author Matthew Tropiano
 */
public abstract class TElementContext<T extends TElement> implements StateSaveable
{
	/** Variable bank. */
	protected ValueHash variables;

	/** Reference to source element. */
	protected T element;

	/**
	 * Creates an element context.
	 * @param element the element reference.
	 */
	protected TElementContext(T element)
	{
		this.element = element;
		this.variables = new ValueHash();
	}

	/**
	 * Gets the element associated with this context.
	 * @return the element that this holds state for.
	 */
	public T getElement()
	{
		return element;
	}

	/**
	 * Reads in a bunch of bytes that represent the current context state.
	 * The context is changed after the call.
	 * @param in the input stream to read from.
	 * @throws IOException if a read error occurs.
	 */
	protected void read(InputStream in) throws IOException
	{
		variables.readBytes(in);
	}

	/**
	 * Writes a bunch of bytes that represent the current context state.
	 * @param out the output stream to write from.
	 * @throws IOException if a write error occurs.
	 */
	protected void write(OutputStream out) throws IOException
	{
		variables.writeBytes(out);
	}

	/**
	 * Sets a value on this context, first searching in local variables.
	 * If it exists in local, it is replaced in local, else, it is persisted.
	 * @param variableName the variable name.
	 * @param value the variable value.
	 */
	public void setValue(String variableName, Value value)
	{
		variables.put(variableName, value);
	}

	/**
	 * Gets a variable's value on this context, first searching in local variables.
	 * @param variableName the variable name.
	 * @return the corresponding value, or a value that represents "false" if no value.
	 */
	public Value getValue(String variableName)
	{
		if (variables.containsKey(variableName))
			return variables.get(variableName);
		else
			return Value.create(false);
	}

	/**
	 * Sets a persistent value on this context.
	 * @param variableName the variable name.
	 * @param value the variable value.
	 */
	public void setPersistantValue(String variableName, Value value)
	{
		variables.put(variableName, value);
	}

	/**
	 * Gets a persistent variable's value on this context.
	 * @param variableName the variable name.
	 * @return the corresponding value, or a value that represents "false" if no value.
	 */
	public Value getPersistantValue(String variableName)
	{
		if (variables.containsKey(variableName))
			return variables.get(variableName);
		return Value.create(false);
	}

	@Override
	public String toString()
	{
		return "Context:" + element;
	}
	
	@Override
	public void writeStateBytes(TAMEModule module, OutputStream out) throws IOException 
	{
		variables.writeBytes(out);
	}

	@Override
	public void readStateBytes(TAMEModule module, InputStream in) throws IOException 
	{
		variables.readBytes(in);
	}

	@Override
	public byte[] toStateBytes(TAMEModule module) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeStateBytes(module, bos);
		return bos.toByteArray();
	}

	@Override
	public void fromStateBytes(TAMEModule module, byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readStateBytes(module, bis);
		bis.close();
	}
	
}
