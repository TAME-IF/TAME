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
 * Describes argument types.
 * @author Matthew Tropiano
 */
public enum ArgumentType
{
	/** Argument accepts a single literal value, can be anything. */
	VALUE,
	/** Argument accepts any action. */
	ACTION,
	/** Argument accepts general actions. */
	ACTION_GENERAL,
	/** Argument accepts modal or open actions. */
	ACTION_MODAL_OPEN,
	/** Argument accepts transitive/ditransitive actions. */
	ACTION_TRANSITIVE_DITRANSITIVE,
	/** Argument accepts ditransitive actions. */
	ACTION_DITRANSITIVE,
	/** Argument must accept a player. */
	PLAYER,
	/** Argument must accept a room. */
	ROOM,
	/** Argument must accept an object. */
	OBJECT,
	/** Argument must accept an object or an archetype object. */
	OBJECT_ANY,
	/** Argument must accept a container. */
	CONTAINER,
	/** Argument must accept an object container (player/room/container/world). */
	OBJECT_CONTAINER,
	/** Argument must accept an element (object/player/room/container/world). */
	ELEMENT,
	/** Argument must accept an element (object/player/room/container/world) - can be archetype. */
	ELEMENT_ANY,
	/** Argument must accept a variable. */
	VARIABLE;
}
