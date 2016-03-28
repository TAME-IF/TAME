/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.element;

import net.mtrop.tame.element.type.TAction;
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
	 * @return the table that handles specific forbidden actions. 
	 */
	public ActionTable getActionForbiddenTable();

	/** 
	 * Get this element's default "onActionForbid" block. 
	 * @return the block that handles non-specific forbidden actions. 
	 */
	public Block getActionForbiddenBlock();

	/** 
	 * Sets this element's default "onActionForbid" block. 
	 * @param block the block that handles non-specific forbidden actions.
	 */
	public void setActionForbiddenBlock(Block block);

	/**
	 * Gets the action permission type.
	 * This determines how to interpret the permission around an action on this object.
	 * @return the permission type to use for permitted actions.
	 */
	public PermissionType getPermissionType();

	/**
	 * Sets the action permission type.
	 * This determines how to interpret the permission around an action on this object.
	 * @param permissionType the permission type to use for permitted actions.
	 */
	public void setPermissionType(PermissionType permissionType);

	/**
	 * Adds an action to the action list to be excluded/restricted.
	 * @param action the action to add.
	 */
	public void addPermissionAction(TAction action);

	/**
	 * Checks if an action is allowed for this room.
	 * @param action the action to check for this object.
	 * @return true if this action is allowed, false if not.
	 * @see #getPermissionType()
	 */
	public boolean allowsAction(TAction action);

}
