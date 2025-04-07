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

import com.tameif.tame.struct.PreprocessorLexer;

/**
 * An interface that allows the user to resolve a resource by path when the
 * {@link TAMEScriptReader} parses it.
 * @author Matthew Tropiano
 */
public interface TAMEScriptIncluder extends PreprocessorLexer.Includer
{
}
