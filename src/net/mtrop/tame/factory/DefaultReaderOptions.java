/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
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
 * Default script reader options implementation.
 * @author Matthew Tropiano
 */
public class DefaultReaderOptions implements TAMEScriptReaderOptions
{
	private static final String[] NO_DEFINES = {}; 
	
	private String[] defines;
	private boolean optimizing;
	private boolean verbose;
	private PrintStream verboseOut;

	/**
	 * Creates a set of reader options.
	 */
	public DefaultReaderOptions()
	{
		this.defines = NO_DEFINES;
		this.optimizing = true;
		this.verbose = false;
		this.verboseOut = System.out;
	}
	
	/**
	 * Sets the defines used for compiling. 
	 * @param defines the list of defined tokens.
	 */
	public void setDefines(String ... defines)
	{
		this.defines = defines;
	}
	
	@Override
	public String[] getDefines() 
	{
		return defines;
	}
	
	/**
	 * Gets if this reader optimizes finished blocks.
	 * Default is true.
	 * @param optimizing true if optimizing, false if not.
	 */
	public void setOptimizing(boolean optimizing)
	{
		this.optimizing = optimizing;
	}
	
	@Override
	public boolean isOptimizing()
	{
		return optimizing;
	}

	/**
	 * Gets if this prints what it is emitting or constructing.
	 * Only good for debugging.
	 * Default is false.
	 * @param verbose true if verbose, false if not.
	 */
	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
	}
	
	@Override
	public boolean isVerbose()
	{
		return verbose;
	}

	/**
	 * Sets the output stream to print verbose messages to.
	 * By default, this is {@link System#out}.
	 * @param verboseOut the print stream to print to.
	 */
	public void setVerboseOut(PrintStream verboseOut) 
	{
		this.verboseOut = verboseOut;
	}

	@Override
	public PrintStream getVerboseOut() 
	{
		return verboseOut;
	}
	
}
