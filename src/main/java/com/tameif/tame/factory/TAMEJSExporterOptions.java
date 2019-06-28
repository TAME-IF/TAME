/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.factory;

import java.io.PrintStream;

/**
 * The set of options for exporting a module to JS. 
 * @author Matthew Tropiano
 */
public interface TAMEJSExporterOptions 
{
	/**
	 * Gets the JS variable to use if exporting to just module data.
	 * @return the variable name, or null for default.
	 */
	public String getModuleVariableName();

	/**
	 * Gets the JS Wrapper starting file path to use.
	 * @return the wrapper start path.
	 */
	public String getStartingPath();
	
	/**
	 * @return the stream to output verbose output to, or null for no output.
	 */
	public PrintStream getVerboseStream();
	
}
