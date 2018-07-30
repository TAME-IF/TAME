/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.blackrook.commons.Common;

/** 
 * Default includer to use when none specified.
 * This includer can either pull from the classpath, URIs, or files.
 * <ul>
 * <li>Paths that start with {@code classpath:} are parsed as resource paths in the current classpath.</li>
 * <li>
 * 		Else, the path is interpreted as a file path, with the following search order:
 * 		<ul>
 * 			<li>Relative to parent of source stream.</li>
 * 			<li>As is.</li>
 * 		</ul>
 * </li>
 * </ul>
 */
public class DefaultIncluder implements TAMEScriptIncluder
{
	private static final String CLASSPATH_PREFIX = "classpath:";
	
	// cannot be instantiated outside of this class.
	public DefaultIncluder(){}
	
	@Override
	public String getNextIncludeResourceName(String streamName, String path) throws IOException
	{
		if (Common.isWindows() && streamName.contains("\\")) // check for Windows paths.
			streamName = streamName.replace('\\', '/');
		
		String streamParent = null;
		int lidx = -1; 
		if ((lidx = streamName.lastIndexOf('/')) >= 0)
			streamParent = streamName.substring(0, lidx + 1);

		if (path.startsWith(CLASSPATH_PREFIX) || (streamParent != null && streamParent.startsWith(CLASSPATH_PREFIX)))
			return ((streamParent != null ? streamParent : "") + path);
		else
		{
			File f = null;
			if (streamParent != null)
			{
				f = new File(streamParent + path);
				if (f.exists())
					return f.getPath();
				else
					return path;
			}
			else
			{
				return path;
			}
			
		}
		
	}

	@Override
	public InputStream getIncludeResource(String path) throws IOException
	{
		if (path.startsWith(CLASSPATH_PREFIX))
			return Common.openResource(path.substring(CLASSPATH_PREFIX.length()));
		else
			return new FileInputStream(new File(path));
	}
}

