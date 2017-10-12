/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.lang;

/**
 * The type of set of actions attributed to action permissions on players or rooms.
 * @author Matthew Tropiano
 */
public enum PermissionType
{
	FORBID,
	ALLOW;
	
	public static final PermissionType[] VALUES = values();
}

