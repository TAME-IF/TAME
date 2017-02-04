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

	public static void main(String[] args)
	{
		PrintStream out = System.out;
		
		for (TAMECommand command : TAMECommand.values())
		{
			if (command.isInternal())
				continue;
			
			out.println("<tamedox:command>");
			out.println("\t<tamedox:commandname>"+command.name()+"</tamedox:commandname>");
			if (command.getArgumentTypes() != null)
			{
				out.println("\t<tamedox:commandargs>");
				for (ArgumentType at : command.getArgumentTypes())
					out.println("\t\t<tamedox:commandarg>"+at.name()+"</tamedox:commandarg>");
				out.println("\t</tamedox:commandargs>");
				out.println("\t<tamedox:commandreturn>"+command.getReturnType()+"</tamedox:commandreturn>");

				out.println("\t<tamedox:commanddocs>");
				out.println("\t\t<!--ADD SHIT HERE -->");
				out.println("\t</tamedox:commanddocs>");
				out.println("\t<tamedox:commandexample script=\"scripts/examples/command-"+command.name().toLowerCase()+".tame\"/>");
				out.println("\t<tamedox:commandtechnoweenie>");
				out.println("\t\t<!--ADD SHIT HERE -->");
				out.println("\t</tamedox:commandtechnoweenie>");
			}
			out.println("</tamedox:command>");
		}

	}

}
