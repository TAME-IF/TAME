package net.mtrop.tame.element;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Attached to classes that use "Action Failed" blocks.
 * @author Matthew Tropiano
 */
public interface ActionFailedHandler
{

	/** 
	 * Get this element's action failure table. 
	 */
	public ActionTable getActionFailedTable();

	/** 
	 * Get this element's default "onFailedAction" block. 
	 */
	public Block getActionFailedBlock();

	/** 
	 * Set this element's default "onFailedAction" block. 
	 */
	public void setActionFailedBlock(Block block);

}
