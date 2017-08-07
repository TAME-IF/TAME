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
 * The block entry type.
 * Also describes what the compiler should expect when parsing the block type.
 * @author Matthew Tropiano
 */
public enum BlockEntryType
{
	INIT(0),
	START(0),
	AFTERREQUEST(0),
	ONACTION(1, ArgumentType.ACTION),
	ONACTIONWITH(2, ArgumentType.ACTION, ArgumentType.OBJECT),
	ONACTIONWITHANCESTOR(2, ArgumentType.ACTION, ArgumentType.OBJECT_ANY),
	ONACTIONWITHOTHER(1, ArgumentType.ACTION),
	ONMODALACTION(2, ArgumentType.ACTION, ArgumentType.VALUE),
	ONWORLDBROWSE(0),
	ONROOMBROWSE(0),
	ONPLAYERBROWSE(0),
	ONCONTAINERBROWSE(0),
	ONAMBIGUOUSACTION(0, ArgumentType.ACTION),
	ONBADACTION(0, ArgumentType.ACTION),
	ONINCOMPLETEACTION(0, ArgumentType.ACTION),
	ONUNKNOWNACTION(0, ArgumentType.ACTION),
	ONFAILEDACTION(0, ArgumentType.ACTION),
	ONFORBIDDENACTION(0, ArgumentType.ACTION),
	ONROOMFORBIDDENACTION(0, ArgumentType.ACTION),
	;
	
	/** Array to get around multiple allocations. */
	public static final BlockEntryType[] VALUES = values();

	/** Minimum argument length. */
	private int minimumArgumentLength;
	/** Minimum argument length. */
	private ArgumentType[] argumentTypes;
	
	/**
	 * Constructs the block entry type.
	 * @param minimumArgumentLength the minimum required argument length.
	 * @param argumentTypes the argument types to this block.
	 */
	private BlockEntryType(int minimumArgumentLength, ArgumentType ... argumentTypes)
	{
		this.minimumArgumentLength = minimumArgumentLength;
		this.argumentTypes = argumentTypes;
	}
	
	/**
	 * Gets the minimum arguments that need to be accepted.
	 * @return the minimum length.
	 */
	public int getMinimumArgumentLength()
	{
		return minimumArgumentLength;
	}

	/**
	 * Gets the argument types to this block entry.
	 * @return the argument types to this block entry.
	 */
	public ArgumentType[] getArgumentTypes()
	{
		return argumentTypes;
	}
	
}
