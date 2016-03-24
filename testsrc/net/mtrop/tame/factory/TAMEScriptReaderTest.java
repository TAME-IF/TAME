/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.factory;

import java.io.File;

import com.blackrook.commons.Common;

import net.mtrop.tame.TAMEModule;

public final class TAMEScriptReaderTest 
{
	public static void main(String[] args) throws Exception
	{
		String res = "./scripts/test.tsc";
		try {
			TAMEModule module = TAMEScriptReader.read(new File(res));
		} catch (TAMEScriptParseException e) {
			System.err.println(e.getMessage());
		}
		Common.noop();
	}
}
