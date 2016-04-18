/*
 * 
 */
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
     * Checks if an action is allowed for this room.
     * @param action the action to check for this object.
     * @return true if this action is allowed, false if not.
     * @see #getPermissionType()
     */
    public boolean allowsAction(TAction action);
    
}
