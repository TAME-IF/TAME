/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.commons.hash.HashMap;

/**
 * A caching structure for all RegEx patterns.
 * <p>RegExs, when compiled into {@link Pattern}s, can be reused several times.
 * This cache is for automatic building/returning existing patterns by String.
 * <p>All operations are thread-safe.
 * @author Matthew Tropiano
 */
public final class PatternCache
{
	// Can't instantiate.
	private PatternCache() {}
	
	/** The main cache map. */
	private static HashMap<String, Pattern> cacheMap;
	
	static
	{
		cacheMap = new HashMap<>(8);
	}
	
	/**
	 * Gets an existing, compiled pattern or a newly-compiled one for the provided expression.
	 * Also useful for pre-warming oft-used expressions.
	 * @param regex the input RegEx.
	 * @return a newly-compiled {@link Pattern} or an existing one.
	 * @throws PatternSyntaxException if the expression cannot be compiled.
	 */
	public static Pattern get(String regex)
	{
		if (cacheMap.containsKey(regex))
			return cacheMap.get(regex);
		
		Pattern out;
		synchronized (cacheMap)
		{
			// return to threads compiling the same pattern
			if (cacheMap.containsKey(regex))
				return cacheMap.get(regex);
			
			cacheMap.put(regex, out = Pattern.compile(regex));
		}
		
		return out;
	}

}
