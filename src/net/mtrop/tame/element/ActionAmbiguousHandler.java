package net.mtrop.tame.element;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Attached to classes that use "Ambiguous Action" blocks.
 * @author Matthew Tropiano
 */
public interface ActionAmbiguousHandler
{
	/**
	 * Gets the ambiguous action table for specific action handlings.
	 */
	public ActionTable getAmbiguousActionTable();

	/** 
	 * Get this player's default "onAmbiguousAction" block. 
	 */
	public Block getAmbiguousActionBlock();

	/** 
	 * Set this player's default "onAmbiguousAction" block. 
	 */
	public void setAmbiguousActionBlock(Block block);

}
