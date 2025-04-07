/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.factory;

import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * Default script reader options implementation.
 * @author Matthew Tropiano
 */
public class DefaultReaderOptions implements TAMEScriptReaderOptions
{
	private static final String[] NO_DEFINES = {}; 
	
	private Charset inputCharset;
	private String[] defines;
	private boolean optimizing;
	private PrintStream verboseStream;

	/**
	 * Creates a set of reader options.
	 */
	public DefaultReaderOptions()
	{
		this.defines = NO_DEFINES;
		this.optimizing = true;
		this.verboseStream = null;
		this.inputCharset = Charset.defaultCharset();
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
	 * Sets the output stream to print verbose messages to.
	 * By default, this is <code>null</code>.
	 * @param verboseOut the print stream to print to.
	 */
	public void setVerboseStream(PrintStream verboseOut) 
	{
		this.verboseStream = verboseOut;
	}

	@Override
	public PrintStream getVerboseStream() 
	{
		return verboseStream;
	}

	/**
	 * Sets the charset to use for reading module source.
	 * @param inputCharset the charset to use.
	 */
	public void setInputCharset(Charset inputCharset)
	{
		this.inputCharset = inputCharset;
	}
	
	@Override
	public Charset getInputCharset()
	{
		return inputCharset;
	}
	
}
