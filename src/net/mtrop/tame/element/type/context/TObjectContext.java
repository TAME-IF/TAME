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
package net.mtrop.tame.element.type.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.type.TObject;

/**
 * Object context.
 * @author Matthew Tropiano
 */
public class TObjectContext extends TElementContext<TObject>
{
	/** Element's names. */
	protected CaseInsensitiveHash currentObjectNames;
	
	/**
	 * Creates an object context. 
	 */
	public TObjectContext(TObject ref)
	{
		super(ref);
		currentObjectNames = new CaseInsensitiveHash(2);
	}

	/** 
	 * Adds a name.
	 */
	public void addName(String name) 
	{
		currentObjectNames.put(name);
	}

	/** 
	 * Removes a name. 
	 */
	public void removeName(String name) 
	{
		currentObjectNames.remove(name);
	}

	/**
	 * Returns true if this contains a particular name.
	 */
	public boolean containsName(String name)
	{
		return currentObjectNames.contains(name);
	}

	@Override
	public void writeStateBytes(TAMEModule module, OutputStream out) throws IOException 
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		super.writeStateBytes(module, out);
		sw.writeInt(currentObjectNames.size());
		for (String name : currentObjectNames)
			sw.writeString(name.toLowerCase(), "UTF-8");
	}

	@Override
	public void readStateBytes(TAMEModule module, InputStream in) throws IOException 
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		super.readStateBytes(module, in);
		int size = sr.readInt();
		while (size-- > 0)
			addName(sr.readString("UTF-8"));
	}

}
