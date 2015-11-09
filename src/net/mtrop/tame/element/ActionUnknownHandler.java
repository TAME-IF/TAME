package net.mtrop.tame.element;

import net.mtrop.tame.lang.Block;

/**
 * Attached to classes that use "Action Failed" blocks.
 * @author Matthew Tropiano
 */
public interface ActionUnknownHandler
{

	/** 
	 * Gets this element's "onUnknownAction" block. 
	 */
	public Block getUnknownActionBlock();

	/** 
	 * Sets this element's "onUnknownAction" block. 
	 */
	public void setUnknownActionBlock(Block block);	

}
