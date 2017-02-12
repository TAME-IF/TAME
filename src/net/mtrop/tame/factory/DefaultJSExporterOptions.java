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
 * The default set of options for exporting a module to JS. 
 * @author Matthew Tropiano
 */
public class DefaultJSExporterOptions implements TAMEJSExporterOptions
{

	@Override
	public String getWrapperName()
	{
		return null;
	}
	
	@Override
	public boolean isPathOutputEnabled() 
	{
		return false;
	}

	@Override
	public String getModuleVariableName()
	{
		return null;
	}

}
