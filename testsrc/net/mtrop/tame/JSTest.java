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

import java.io.File;
import java.io.PrintWriter;

import com.blackrook.commons.Common;

import net.mtrop.tame.factory.TAMEJSExporter;
import net.mtrop.tame.factory.TAMEJSExporterOptions;
import net.mtrop.tame.factory.TAMEScriptReader;

/**
 * A test JS exporter.
 * @author Matthew Tropiano
 */
public final class JSTest 
{

	public static void main(String[] args) throws Exception
	{
		String inProperty = System.getProperty("jstest.infile.path");
		String outProperty = System.getProperty("jstest.outfile.path");
		String out = TAMEJSExporter.exportToString(TAMEScriptReader.read(new File(inProperty)), new TAMEJSExporterOptions()
		{
			@Override
			public boolean isPathOutputEnabled() 
			{
				return true;
			}
			
			@Override
			public String getWrapperName() 
			{
				return "node";
				//return null;
			}

			@Override
			public String getModuleVariableName()
			{
				return null;
			}
		});
		
		if (outProperty != null)
		{
			PrintWriter pw = new PrintWriter(new File(outProperty));
			pw.append(out);
			Common.close(pw);
		}
		else
		{
			System.out.println(out);
		}
	}

}
