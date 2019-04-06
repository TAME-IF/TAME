/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;

import com.blackrook.commons.util.IOUtils;
import com.tameif.tame.factory.TAMEJSExporter;
import com.tameif.tame.factory.TAMEJSExporterOptions;
import com.tameif.tame.factory.TAMEScriptReader;

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
			public String getStartingPath() 
			{
				return "node";
				//return null;
			}

			@Override
			public String getModuleVariableName()
			{
				return null;
			}
			
			@Override
			public PrintStream getVerboseStream()
			{
				return null;
			}
			
		});
		
		if (outProperty != null)
		{
			PrintWriter pw = new PrintWriter(new File(outProperty));
			pw.append(out);
			IOUtils.close(pw);
		}
		else
		{
			System.out.println(out);
		}
	}

}
