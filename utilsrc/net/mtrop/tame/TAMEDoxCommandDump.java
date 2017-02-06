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
		
		out.println("<?xml encoding=\"UTF-8\"?>");
		out.println();

		out.println("<!-- TEMPLATES -->");
		out.println("<!-- <tamedox:commandreturntype type=\"VALUE TYPE HERE\">REASON GOES HERE</tamedox:commandreturntype> -->");
		out.println();
		
		for (TAMECommand command : TAMECommand.values())
		{
			if (command.isInternal())
				continue;

			out.println("<!-- Command: "+command.name()+" -->");
			out.println("<tamedox:command name=\""+command.name()+"\" group=\"UNNAMED\" sort=\"0\"/>");
			if (command.getArgumentTypes() != null)
			{
				out.println("\t<tamedox:commandargs>");
				for (ArgumentType at : command.getArgumentTypes())
					out.println("\t\t<tamedox:commandarg type=\""+at.name()+"\"><!-- PURPOSE GOES HERE --></tamedox:commandarg>");
				out.println("\t</tamedox:commandargs>");
				if (command.getReturnType() != null)
				{
					out.println("\t<tamedox:commandreturntypes>");
					out.println("\t\t<!--ADD SHIT HERE -->");
					out.println("\t</tamedox:commandreturntypes>");
				}
				out.println("\t<tamedox:commanddocs src=\"static/command-docs-"+command.name().toLowerCase()+".html\"/>");
				out.println("\t<tamedox:commandexample script=\"scripts/examples/command-"+command.name().toLowerCase()+"\"/>");
				out.println("\t<tamedox:commandtech src=\"static/command-docs-"+command.name().toLowerCase()+"-tech.html\"/>");
			}
			out.println("</tamedox:command>");
			out.println();
		}
	
		out.close();
	}

}
