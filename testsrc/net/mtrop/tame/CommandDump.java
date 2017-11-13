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
import java.util.Arrays;

import net.mtrop.tame.lang.BlockEntryType;

public final class CommandDump 
{

	public static void main(String[] args)
	{
		PrintStream out = System.out;
		
		for (TAMECommand command : TAMECommand.VALUES)
		{
			if (command.isInternal())
				continue;
			
			out.println(command.name() + " Returns: " + command.getReturnType() + " Arguments: " + Arrays.toString(command.getArgumentTypes()));
		}

		out.println("---------------------------");
		
		for (TAMECommand command : TAMECommand.VALUES)
		{
			if (command.isInternal())
				continue;
			
			out.print(command.name() + " ");
		}

		out.println("\n---------------------------");
		
		for (BlockEntryType command : BlockEntryType.VALUES)
		{
			out.print(command.name() + " ");
		}
		
		out.println("\n---------------------------");
		
		out.println("var TCommandFunctions =");
		out.println("[");
		for (TAMECommand command : TAMECommand.VALUES)
		{
			out.println("\t/* " +command.name() + " */");
			out.println("\t{");
			out.println("\t\t\"name\": '"+command.name()+"', ");
			out.println("\t\t\"doCommand\": function(request, response, blockLocal, command)");
			out.println("\t\t{");
			out.println("\t\t\t// TODO: Finish this.");
			out.println("\t\t}");
			out.println("\t},");
			out.println();
		}
		out.println("];");

	}

}
