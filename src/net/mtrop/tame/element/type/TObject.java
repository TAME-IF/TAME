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
import java.io.OutputStream;

import net.mtrop.tame.element.TActionableElement;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionTable;
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
public class TObject extends TActionableElement
{
	/** Table used for ditransitive actions. */
	protected ActionWithTable actionWithTable;
	/** Table used for ditransitive with other actions. */
	protected ActionTable actionWithOtherTable;
	
	/** Element's names. */
	protected CaseInsensitiveHash names;
	/** Code block ran upon browsing a room with this object in it. */
	protected Block roomBrowseBlock;
	/** Code block ran upon browsing a player with this object in it. */
	protected Block playerBrowseBlock;
	/** Code block ran upon browsing a container with this object in it. */
	protected Block containerBrowseBlock;

	private TObject()
	{
		super();
		this.names = new CaseInsensitiveHash(3);
		this.actionWithTable = new ActionWithTable();
		this.actionWithOtherTable = new ActionTable();
		
		this.roomBrowseBlock = null;
		this.playerBrowseBlock = null;
		this.containerBrowseBlock = null;
	}
	
	/**
	 * Creates an empty object.
	 * @param identity its main identity.
	 */
	public TObject(String identity) 
	{
		this();
		setIdentity(identity);
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
	 * Gets the block to call if used with another object not handled by "action with."
	 */
	public ActionTable getActionWithOtherTable() 
	{
		return actionWithOtherTable;
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
	public void setRoomBrowseBlock(Block block)
	{
		roomBrowseBlock = block;
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
	public void setPlayerBrowseBlock(Block block)
	{
		playerBrowseBlock = block;
	}
	
	/** 
	 * Get the "browsing in possession of a container" action block. 
	 */
	public Block getContainerBrowseBlock()
	{
		return containerBrowseBlock;
	}
	
	/** 
	 * Set the "browsing in possession of a container" action block. 
	 */
	public void setContainerBrowseBlock(Block block)
	{
		containerBrowseBlock = block;
	}
	
	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TObject create(InputStream in) throws IOException
	{
		TObject out = new TObject();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		super.writeBytes(out);
		actionWithTable.writeBytes(out);
		actionWithOtherTable.writeBytes(out);
		
		sw.writeInt(names.size());
		for (String name : names)
			sw.writeString(name.toLowerCase(), "UTF-8");
			
		sw.writeBit(roomBrowseBlock != null);
		sw.writeBit(playerBrowseBlock != null);
		sw.writeBit(containerBrowseBlock != null);
		sw.flushBits();

		if (roomBrowseBlock != null)
			roomBrowseBlock.writeBytes(out);
		if (playerBrowseBlock != null)
			playerBrowseBlock.writeBytes(out);
		if (containerBrowseBlock != null)
			containerBrowseBlock.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		super.readBytes(in);
		actionWithTable = ActionWithTable.create(in);
		actionWithOtherTable = ActionTable.create(in);
		
		names.clear();
		int size = sr.readInt();
		while (size-- > 0)
			names.put(sr.readString("UTF-8"));

		byte blockbits = sr.readByte();
		
		if ((blockbits & 0x01) != 0)
			roomBrowseBlock = Block.create(in);
		if ((blockbits & 0x02) != 0)
			playerBrowseBlock = Block.create(in);
		if ((blockbits & 0x04) != 0)
			containerBrowseBlock = Block.create(in);
	}

}