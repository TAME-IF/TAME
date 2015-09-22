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
package net.mtrop.tame.struct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Saveable;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * Holds action, object to Block mappings for modal action calls.
 * @author Matthew Tropiano
 */
public class ActionWithTable implements Saveable
{
	/** Action:Mode map. */
	private HashMap<String, HashMap<String, Block>> actionMap;
	
	/**
	 * Creates a new ActionModeTable.
	 */
	public ActionWithTable()
	{
		actionMap = null;
	}
	
	/**
	 * Checks if a command block by action exists.
	 */
	public boolean contains(String actionIdentity, String objectIdentity)
	{
		return get(actionIdentity, objectIdentity) != null;
	}

	/**
	 * Gets command block by action.
	 */
	public Block get(String actionIdentity, String objectIdentity)
	{
		if (actionMap == null) 
			return null;		
		
		HashMap<String, Block> objectHash = actionMap.get(actionIdentity);
		
		if (objectHash == null) 
			return null;
		else
			return objectHash.get(objectIdentity);
	}

	/**
	 * Sets/replaces command block by action.
	 */
	public void add(String actionIdentity, String objectIdentity, Block commandBlock)
	{
		if (actionMap == null)
			actionMap = new HashMap<String, HashMap<String, Block>>(3,3);
		
		HashMap<String, Block> hash = null;
		hash = actionMap.get(actionIdentity);
		if (hash == null) 
		{
			hash = new HashMap<String, Block>(2);
			actionMap.put(actionIdentity, hash);
		}
		hash.put(objectIdentity, commandBlock);
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		// TODO: Finish this.
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		// TODO: Finish this.
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
