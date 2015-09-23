/*******************************************************************************
 * Copyright (c) 2009-2013 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
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
 * Holds contextual information for a TAMEElement
 * @author Matthew Tropiano
 */
public abstract class TElementContext<T extends TElement> implements StateSaveable
{
	/** Reference to source element. */
	protected T elementRef;
	/** Variable bank. */
	protected ValueHash variables;

	
	protected TElementContext(T ref)
	{
		elementRef = ref;
		variables = new ValueHash();
	}

	/**
	 * Gets the element associated with this context.
	 */
	public T getElement()
	{
		return elementRef;
	}

	/**
	 * Reads in a bunch of bytes that represent the current context state.
	 */
	protected void read(InputStream in) throws IOException
	{
		variables.readBytes(in);
	}

	/**
	 * Writes a bunch of bytes that represent the current context state.
	 */
	protected void write(OutputStream out) throws IOException
	{
		variables.writeBytes(out);
	}

	/**
	 * Sets a value on this context.
	 * @param variableName the variable name.
	 * @param value the variable value.
	 */
	public void setValue(String variableName, Value value)
	{
		variables.put(variableName, value);
	}

	/**
	 * Gets a variable's value on this context.
	 * @param variableName the variable name.
	 */
	public Value getValue(String variableName)
	{
		if (!containsVariable(variableName))
			setValue(variableName, Value.create(0));
		return variables.get(variableName);
	}
	
	/**
	 * Tests if a variable's value exists on this context.
	 * @param variableName the variable's name.
	 */
	public boolean containsVariable(String variableName)
	{
		return variables.containsKey(variableName);
	}
	
	@Override
	public String toString()
	{
		return "Context:" + elementRef;
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
