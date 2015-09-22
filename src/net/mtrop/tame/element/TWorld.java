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

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Contains immutable World data. 
 * @author Matthew Tropiano
 */
public class TWorld extends TElement
{
	/** Blocks executed on action failure. */
	private ActionTable actionFailTable;

	/** Code block ran upon bad action. */
	private Block badActionBlock;
	/** Code block ran upon default action fail. */
	private Block actionFailBlock;
	/** Code block ran when an action is ambiguous. */
	private Block actionAmbiguityBlock;
	
	/**
	 * Constructs an instance of a game world.
	 */
	public TWorld()
	{
		super("world");

		this.actionFailTable = new ActionTable();

		this.badActionBlock = null;
		this.actionFailBlock = null;
		this.actionAmbiguityBlock = null;
	}

	/** 
	 * Get this module's action fail table. 
	 */
	public ActionTable getActionFailTable()
	{
		return actionFailTable;
	}

	/** 
	 * Get this module's "onBadAction" block. 
	 */
	public Block getBadActionBlock()
	{
		return badActionBlock;
	}

	/** 
	 * Set this module's "onBadAction" block. 
	 */
	public void setBadActionBlock(Block eab)	
	{
		badActionBlock = eab;
	}

	/** 
	 * Get this module's default "onAmbiguousAction" block. 
	 */
	public Block getAmbiguousActionBlock()
	{
		return actionAmbiguityBlock;
	}

	/** 
	 * Set this module's default "onAmbiguousAction" block. 
	 */
	public void setAmbiguousActionBlock(Block eab)
	{
		actionAmbiguityBlock = eab;
	}

	/** 
	 * Get this module's default "onActionFail" block. 
	 */
	public Block getActionFailBlock()
	{
		return actionFailBlock;
	}

	/** 
	 * Set this module's default "onActionFail" block. 
	 */
	public void setActionFailBlock(Block eab)
	{
		actionFailBlock = eab;
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
