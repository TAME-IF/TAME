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

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface that allows the user to resolve a resource by path when the
 * {@link TAMEScriptReader} parses it.
 * @author Matthew Tropiano
 */
public interface TAMEScriptIncluder 
{
	/**
	 * Returns a path when the parser needs the next name of a resource.
	 * By default, this attempts to resolve the new path of the next resource (based on parent information).
	 * @param streamName the current name of the stream. This includer may use this to procure a relative path.
	 * @param path the stream path.
	 * @return an open {@link InputStream} for the requested resource, or null if not found.
	 * @throws IOException if the included resource cannot be read or the stream cannot be opened.
	 */
	public String getNextIncludeResourceName(String streamName, String path) throws IOException;
		
	/**
	 * Returns an open {@link InputStream} for a path when the parser needs a resource.
	 * By default, this attempts to open a file at the provided path.
	 * @param path the stream path.
	 * @return an open {@link InputStream} for the requested resource, or null if not found.
	 * @throws IOException if the included resource cannot be read or the stream cannot be opened.
	 */
	public InputStream getIncludeResource(String path) throws IOException;
		
}
