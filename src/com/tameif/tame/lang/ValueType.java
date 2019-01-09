/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

/**
 * Value type enumeration.
 * @author Matthew Tropiano
 */
public enum ValueType
{
	BOOLEAN,
	INTEGER,
	FLOAT,
	STRING,
	LIST,
	OBJECT,
	CONTAINER,
	PLAYER,
	ROOM,
	WORLD,
	ACTION,
	VARIABLE;
	
	public static final ValueType[] VALUES = values();

}

