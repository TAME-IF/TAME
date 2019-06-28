/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.tameif.tame.TAMEModule;
import com.tameif.tame.element.TElement;
import com.tameif.tame.lang.StateSaveable;
import com.tameif.tame.lang.Value;
import com.tameif.tame.lang.ValueHash;

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
	 * Sets a variable's value on this context.
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
	 * @return the corresponding value, or a value that represents "false" if no value.
	 */
	public Value getValue(String variableName)
	{
		if (variables.containsKey(variableName))
			return variables.get(variableName);
		return Value.create(false);
	}

	/**
	 * Returns an iterator of this context's value names.
	 * @return an iterator of each set value.
	 */
	public Iterator<String> values()
	{
		return variables.names().iterator();
	}
	
	/**
	 * Clears a variable's value on this context.
	 * @param variableName the variable name.
	 */
	public void clearValue(String variableName)
	{
		if (variables.containsKey(variableName))
			variables.remove(variableName);
	}

	@Override
	public String toString()
	{
		return "Context:" + element;
	}
	
	@Override
	public void writeStateBytes(TAMEModule module, AtomicLong referenceCounter, Map<Object, Long> referenceSet, OutputStream out) throws IOException 
	{
		variables.writeReferentialBytes(referenceCounter, referenceSet, out);
	}

	@Override
	public void readStateBytes(TAMEModule module, Map<Long, Value> referenceMap, InputStream in) throws IOException 
	{
		variables.readReferentialBytes(referenceMap, in);
	}

}
