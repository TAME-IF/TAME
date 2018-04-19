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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.AbstractMap;
import com.blackrook.commons.AbstractSet;

/**
 * Describes an object that can be saved/loaded but preserves value references.
 * @author Matthew Tropiano
 */
public interface ReferenceSaveable
{
	/**
	 * Exports this object to bytes, preserving up value references from a map.
	 * @param referenceSet the reference set to use for "seen" value references.
	 * @param out the output stream to write to.
	 * @throws IOException if a write problem occurs.
	 */
	public void writeReferentialBytes(AbstractSet<Long> referenceSet, OutputStream out) throws IOException;
	
	/**
	 * Imports this object from bytes, looking up value references in a map.
	 * @param referenceMap the reference map to use for "seen" value references.
	 * @param in the input stream to read from.
	 * @throws IOException if a read problem occurs.
	 */
	public void readReferentialBytes(AbstractMap<Long, Value> referenceMap, InputStream in) throws IOException;
	
	/**
	 * Gets this object's representation as bytes, preserving up value references from a map.
	 * @param referenceSet the reference set to use for "seen" value references.
	 * @return the byte array of state bytes.
	 * @throws IOException if a write problem occurs.
	 */
	public byte[] toReferentialBytes(AbstractSet<Long> referenceSet) throws IOException;
	
	/**
	 * Reads this object's representation from a byte array, looking up value references in a map.
	 * @param referenceMap the reference map to use for "seen" value references.
	 * @param data the data to read.
	 * @throws IOException if a read problem occurs.
	 */
	public void fromReferentialBytes(AbstractMap<Long, Value> referenceMap, byte[] data) throws IOException;
	
}
