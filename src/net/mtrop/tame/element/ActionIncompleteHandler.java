package net.mtrop.tame.element;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Attached to classes that use "Action Incomplete" blocks, 
 * called when a (di)transitive, modal, or open block does not have a target.
 * @author Matthew Tropiano
 */
public interface ActionIncompleteHandler
{
	/** 
	 * Get this element's action incomplete table for specific actions. 
	 */
	public ActionTable getActionIncompleteTable();

	/** 
	 * Gets this element's "onIncompleteAction" block. 
	 */
	public Block getActionIncompleteBlock();

	/** 
	 * Sets this element's "onIncompleteAction" block. 
	 */
	public void setActionIncompleteBlock(Block block);	

}
