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
package net.mtrop.tame.element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionModeTable;
import net.mtrop.tame.struct.ActionTable;

/**
 * Contains immutable World data. 
 * @author Matthew Tropiano
 */
public class TWorld extends TActionableElement 
	implements ActionFailedHandler, ActionModalHandler, ActionUnknownHandler, ActionAmbiguousHandler, ActionIncompleteHandler, ActionBadHandler
{
	/** Blocks executed on action failure. */
	private ActionTable actionFailedTable;
	/** Table used for modal actions. */
	private ActionModeTable modalActionTable;
	/** Blocks ran when an action is ambiguous. */
	private ActionTable actionAmbiguousTable;
	/** Blocks ran when an action is incomplete. */
	protected ActionTable actionIncompleteTable;
	/** Blocks ran when an action is valid, but a predicate isn't. */
	protected ActionTable badActionTable;

	/** Code block ran after each request. */
	private Block afterRequestBlock;
	/** Code block ran upon default action fail. */
	private Block actionFailBlock;
	/** Code block ran when an action is ambiguous. */
	private Block actionAmbiguityBlock;
	/** Code block ran upon incomplete action. */
	protected Block actionIncompleteBlock;
	/** Code block ran when an action is valid, but a predicate isn't. */
	protected Block badActionBlock;
	/** Code block ran upon unknown action. */
	private Block unknownActionBlock;

	/**
	 * Constructs an instance of a game world.
	 */
	public TWorld()
	{
		super(TAMEConstants.IDENTITY_CURRENT_WORLD);
		
		this.actionFailedTable = new ActionTable();
		this.modalActionTable = new ActionModeTable();
		this.actionAmbiguousTable = new ActionTable();
		this.actionIncompleteTable = new ActionTable();
		this.badActionTable = new ActionTable();

		this.afterRequestBlock = null;
		this.actionFailBlock = null;
		this.actionAmbiguityBlock = null;
		this.actionIncompleteBlock = null;
		this.badActionBlock = null;
		this.unknownActionBlock = null;
	}

	@Override
	public ActionTable getActionFailedTable()
	{
		return actionFailedTable;
	}

	@Override
	public ActionModeTable getModalActionTable()
	{
		return modalActionTable;
	}

	@Override
	public ActionTable getAmbiguousActionTable()
	{
		return actionAmbiguousTable;
	}
	
	@Override
	public Block getUnknownActionBlock()
	{
		return unknownActionBlock;
	}

	@Override
	public void setUnknownActionBlock(Block eab)	
	{
		unknownActionBlock = eab;
	}

	@Override
	public Block getAmbiguousActionBlock()
	{
		return actionAmbiguityBlock;
	}

	@Override
	public void setAmbiguousActionBlock(Block eab)
	{
		actionAmbiguityBlock = eab;
	}

	@Override
	public Block getActionFailedBlock()
	{
		return actionFailBlock;
	}

	@Override
	public void setActionFailedBlock(Block eab)
	{
		actionFailBlock = eab;
	}

	@Override
	public ActionTable getActionIncompleteTable() 
	{
		return actionIncompleteTable;
	}

	@Override
	public Block getActionIncompleteBlock() 
	{
		return actionIncompleteBlock;
	}

	@Override
	public void setActionIncompleteBlock(Block block) 
	{
		actionIncompleteBlock = block;
	}

	@Override
	public ActionTable getBadActionTable()
	{
		return badActionTable;
	}

	@Override
	public Block getBadActionBlock() 
	{
		return badActionBlock;
	}

	@Override
	public void setBadActionBlock(Block block) 
	{
		this.badActionBlock = block;
	}

	/**
	 * Gets the block executed after each user request.
	 */
	public Block getAfterRequestBlock() 
	{
		return afterRequestBlock;
	}
	
	/**
	 * Sets the block executed after each user request.
	 */
	public void setAfterRequestBlock(Block afterRequestBlock) 
	{
		this.afterRequestBlock = afterRequestBlock;
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
		
		actionFailedTable.writeBytes(out);
		modalActionTable.writeBytes(out);
		actionAmbiguousTable.writeBytes(out);
		actionIncompleteTable.writeBytes(out);
		badActionTable.writeBytes(out);
		
		sw.writeBit(afterRequestBlock != null);
		sw.writeBit(actionFailBlock != null);
		sw.writeBit(actionAmbiguityBlock != null);
		sw.writeBit(actionIncompleteBlock != null);
		sw.writeBit(badActionBlock != null);
		sw.writeBit(unknownActionBlock != null);
		sw.flushBits();
		
		if (afterRequestBlock != null)
			afterRequestBlock.writeBytes(out);
		if (actionFailBlock != null)
			actionFailBlock.writeBytes(out);
		if (actionAmbiguityBlock != null)
			actionAmbiguityBlock.writeBytes(out);
		if (actionIncompleteBlock != null)
			actionIncompleteBlock.writeBytes(out);
		if (badActionBlock != null)
			badActionBlock.writeBytes(out);
		if (unknownActionBlock != null)
			unknownActionBlock.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		super.readBytes(in);
		
		actionFailedTable = ActionTable.create(in);
		modalActionTable = ActionModeTable.create(in);
		actionAmbiguousTable = ActionTable.create(in);
		actionIncompleteTable = ActionTable.create(in);
		badActionTable = ActionTable.create(in);

		byte blockbits = sr.readByte();
		
		if ((blockbits & 0x01) != 0)
			afterRequestBlock = Block.create(in);
		if ((blockbits & 0x02) != 0)
			actionFailBlock = Block.create(in);
		if ((blockbits & 0x04) != 0)
			actionAmbiguityBlock = Block.create(in);
		if ((blockbits & 0x08) != 0)
			actionIncompleteBlock = Block.create(in);
		if ((blockbits & 0x10) != 0)
			badActionBlock = Block.create(in);
		if ((blockbits & 0x20) != 0)
			unknownActionBlock = Block.create(in);
	}

}
