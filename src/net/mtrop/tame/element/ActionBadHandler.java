package net.mtrop.tame.element;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.struct.ActionTable;

/**
 * Attached to classes that use "Bad Action" blocks.
 * @author Matthew Tropiano
 */
public interface ActionBadHandler
{
	/** 
	 * Get this element's bad action table. 
	 */
	public ActionTable getBadActionTable();

	/** 
	 * Gets this element's "onBadAction" block. 
	 */
	public Block getBadActionBlock();

	/** 
	 * Sets this element's "onBadAction" block. 
	 */
	public void setBadActionBlock(Block block);	

}
