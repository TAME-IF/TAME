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
	 * Gets the JS Wrapper name to use.
	 * @return the wrapper name, or null for no wrapper.
	 */
	public String getWrapperName();
	
	/**
	 * Checks if this enables include path output in JS code.
	 * @return true if so, false if not.
	 */
	public boolean isPathOutputEnabled();
	
}
