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
package net.mtrop.tame.element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionWithTable;

import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * Objects are common elements that players interact with, examine, talk to, eat, take - 
 * you get the idea. Objects can also be added to the inventory of players. Objects
 * in the possession of players take precedence over objects in a room.
 * @author Matthew Tropiano
 */
public class TObject extends TElement
{
	/** Table used for ditransitive actions. */
	protected ActionWithTable actionWithTable;
	/** Element's names. */
	protected CaseInsensitiveHash names;
	/** Code block ran upon "seeing" the object. */
	protected Block roomBrowseBlock;
	/** Code block ran upon "seeing" the object in your inventory. */
	protected Block playerBrowseBlock;

	public TObject(String id)
	{
		super(id);
		this.names = new CaseInsensitiveHash(3);
		this.actionWithTable = new ActionWithTable();
		this.roomBrowseBlock = new Block();
		this.playerBrowseBlock = new Block();
	}
	
	/**
	 * Gets the initial names on this object.
	 */
	public CaseInsensitiveHash getNames()
	{
		return names;
	}
	
	/** 
	 * Gets the "action with table." 
	 */
	public ActionWithTable getActionWithTable()
	{
		return actionWithTable;
	}
	
	/** 
	 * Get the "browsing in possession of a room" action block.
	 */
	public Block getRoomBrowseBlock()
	{
		return roomBrowseBlock;
	}
	
	/** 
	 * Set the "browsing in possession of a room" action block.
	 */
	public void setRoomBrowseBlock(Block eab)
	{
		roomBrowseBlock = eab;
	}
	
	/** 
	 * Get the "browsing in possession of a player" action block. 
	 */
	public Block getPlayerBrowseBlock()
	{
		return playerBrowseBlock;
	}
	
	/** 
	 * Set the "browsing in possession of a player" action block. 
	 */
	public void setPlayerBrowseBlock(Block eab)
	{
		playerBrowseBlock = eab;
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		super.writeBytes(out);
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		// TODO: Finish this.
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		super.readBytes(in);
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
