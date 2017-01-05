/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.factory;

import java.io.PrintStream;

/**
 * An interface for reader options. 
 * These influence reader/compiler behavior.
 * @author Matthew Tropiano
 */
public interface TAMEScriptReaderOptions 
{
	/**
	 * Gets what to predefine in the preprocessor.
	 * This can affect what gets compiled and what doesn't.
	 * Must not return null.
	 * @return a list of defined tokens.
	 */
	public String[] getDefines();
	
	/**
	 * Gets if this reader optimizes finished blocks.
	 * @return true if so, false if not.
	 */
	public boolean isOptimizing();
	
	/**
	 * Gets if this prints what it is emitting or constructing.
	 * Only good for debugging.
	 * @return true if so, false if not.
	 */
	public boolean isVerbose();
	
	/**
	 * Gets the output stream to print verbose messages to.
	 * @return the print stream to use.
	 */
	public PrintStream getVerboseOut();
	
}
