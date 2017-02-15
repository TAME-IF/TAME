/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame;

import java.io.PrintStream;

import net.mtrop.tame.lang.ArgumentType;

public final class TAMEDoxCommandDump 
{

	public static void main(String[] args) throws Exception
	{
		PrintStream out = new PrintStream(System.out, true, "UTF-8");
		
		for (TAMECommand command : TAMECommand.values())
		{
			if (command.isInternal())
				continue;

			out.println("\t<tamedox:part title=\""+command.name()+"\" group=\""+command.getGrouping()+"\" src=\"included/commands/"+command.name().toLowerCase()+".html\" dest=\"command-"+command.name().toLowerCase()+".html\" />");
		}
		out.close();
	}

}
