package net.mtrop.tame.element;

import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.PermissionType;
import net.mtrop.tame.struct.ActionTable;

/**
 * Attached to classes that use "Action Forbidden" blocks.
 * @author Matthew Tropiano
 */
public interface ActionForbiddenHandler
{

	/** 
	 * Get this element's action forbid table for specific actions. 
	 */
	public ActionTable getActionForbiddenTable();

	/** 
	 * Get this element's default "onActionForbid" block. 
	 */
	public Block getActionForbiddenBlock();

	/** 
	 * Sets this element's default "onActionForbid" block. 
	 */
	public void setActionForbiddenBlock(Block block);

	/**
	 * Gets the action permission type.
	 */
	public PermissionType getPermissionType();

	/**
	 * Sets the action permission type.
	 */
	public void setPermissionType(PermissionType permissionType);

	/**
	 * Adds an action to the action list to be excluded/restricted.
	 */
	public void addPermittedAction(TAction action);

	/**
	 * Returns if an action is allowed for this room.
	 */
	public boolean allowsAction(TAction action);

}
