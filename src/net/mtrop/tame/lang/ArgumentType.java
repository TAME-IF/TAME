package net.mtrop.tame.lang;

/**
 * Describes argument types.
 * @author Matthew Tropiano
 */
public enum ArgumentType
{
	/** Argument accepts an action. */
	ACTION,
	/** Argument accepts a single value, can be anything. */
	VALUE,
	/** Argument must accept a player. */
	PLAYER,
	/** Argument must accept a room. */
	ROOM,
	/** Argument must accept an object. */
	OBJECT,
	/** Argument must accept a variable. */
	VARIABLE;
}
