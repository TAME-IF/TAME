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
package net.mtrop.tame.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.struct.Value;
import net.mtrop.tame.struct.ValueHash;
import net.mtrop.tame.world.TElement;

/**
 * Holds contextual information for a TAMEElement
 * @author Matthew Tropiano
 */
public abstract class TElementContext<T extends TElement>
{
	/** Reference to source element. */
	protected T elementRef;
	/** Variable bank. */
	protected ValueHash variables;

	public TElementContext(T ref)
	{
		elementRef = ref;
		variables = new ValueHash();
	}

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
		return "Context " + elementRef;
	}
	
}
