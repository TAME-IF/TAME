/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.element;

import net.mtrop.tame.lang.PermissionType;

/**
 * Interface for objects that can forbid actions.
 * @author Matthew Tropiano
 */
public interface ForbiddenHandler
{
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
	 * @return an iterable list of the allowed actions.
	 */
	public Iterable<String> getPermissionActions();

	/**
     * Checks if an action is allowed for this room.
     * @param action the action to check for this object.
     * @return true if this action is allowed, false if not.
     * @see #getPermissionType()
     */
    public boolean allowsAction(TAction action);
    
}
