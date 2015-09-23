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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.TAMEConstants;
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

	/** Code block ran upon default action fail. */
	private Block actionFailBlock;
	/** Code block ran when an action is ambiguous. */
	private Block actionAmbiguityBlock;
	/** Code block ran upon bad action. */
	private Block badActionBlock;

	/**
	 * Constructs an instance of a game world.
	 */
	public TWorld()
	{
		super();
		setIdentity(TAMEConstants.IDENTITY_CURRENT_WORLD);
		
		this.actionFailTable = new ActionTable();

		this.actionFailBlock = null;
		this.actionAmbiguityBlock = null;
		this.badActionBlock = null;
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

	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TWorld create(InputStream in) throws IOException
	{
		TWorld out = new TWorld();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		super.writeBytes(out);
		
		actionFailTable.writeBytes(out);
		
		sw.writeBit(actionFailBlock != null);
		sw.writeBit(actionAmbiguityBlock != null);
		sw.writeBit(badActionBlock != null);
		sw.flushBits();
		
		if (actionFailBlock != null)
			actionFailBlock.writeBytes(out);
		if (actionAmbiguityBlock != null)
			actionAmbiguityBlock.writeBytes(out);
		if (badActionBlock != null)
			badActionBlock.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		super.readBytes(in);
		actionFailTable = ActionTable.create(in);
		
		byte blockbits = sr.readByte();
		
		if ((blockbits & 0x01) != 0)
			actionFailBlock = Block.create(in);
		if ((blockbits & 0x02) != 0)
			actionAmbiguityBlock = Block.create(in);
		if ((blockbits & 0x04) != 0)
			badActionBlock = Block.create(in);
	}

}
