/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame.element.type;

import java.io.IOException;
import java.io.InputStream;

import net.mtrop.tame.element.TElement;

/**
 * Container that just holds objects. It cannot be actioned on.
 * @author Matthew Tropiano
 */
public class TContainer extends TElement 
{
	
	private TContainer()
	{
		super();
	}
	
	/**
	 * Creates an empty container.
	 * @param identity its main identity.
	 */
	public TContainer(String identity) 
	{
		this();
		setIdentity(identity);
	}

	/**
	 * Creates this container from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TContainer create(InputStream in) throws IOException
	{
		TContainer out = new TContainer();
		out.readBytes(in);
		return out;
	}

	
}
