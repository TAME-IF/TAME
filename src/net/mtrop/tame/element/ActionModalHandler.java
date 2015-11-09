package net.mtrop.tame.element;

import net.mtrop.tame.struct.ActionModeTable;

/**
 * Attached to classes that use "Modal Action" blocks.
 * @author Matthew Tropiano
 */
public interface ActionModalHandler
{
	/** 
	 * Gets the modal action table. 
	 */
	public ActionModeTable getModalActionTable();

}
