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
 * The block entry type.
 * Also describes what the compiler should expect when parsing the block type.
 * @author Matthew Tropiano
 */
public enum BlockEntryType
{
	// Lifecycle.
	INIT(0),
	START(0),
	AFTERSUCCESSFULCOMMAND(0),
	AFTERFAILEDCOMMAND(0),
	AFTEREVERYCOMMAND(0),
	
	// Good interpret.
	ONACTION(1, ArgumentType.ACTION),
	ONMODALACTION(2, ArgumentType.ACTION, ArgumentType.VALUE),
	ONACTIONWITH(2, ArgumentType.ACTION, ArgumentType.OBJECT),
	ONACTIONWITHANCESTOR(2, ArgumentType.ACTION, ArgumentType.OBJECT_ANY),
	ONACTIONWITHOTHER(1, ArgumentType.ACTION),
	ONUNHANDLEDACTION(0, ArgumentType.ACTION),

	// Bad interpret.
	ONUNKNOWNCOMMAND(0),
	ONAMBIGUOUSCOMMAND(0, ArgumentType.ACTION),
	ONINCOMPLETECOMMAND(0, ArgumentType.ACTION),
	ONMALFORMEDCOMMAND(0, ArgumentType.ACTION),
	
	// Function-specific.
	ONELEMENTBROWSE(1, ArgumentType.OBJECT_CONTAINER_ANY),
	ONWORLDBROWSE(0),
	ONROOMBROWSE(0),
	ONPLAYERBROWSE(0),
	ONCONTAINERBROWSE(0),
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
