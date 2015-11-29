/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.List;

import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.factory.TAMEScriptParseException;
import net.mtrop.tame.factory.TAMEScriptReader;
import net.mtrop.tame.factory.TAMEScriptReaderOptions;

/**
 * The compiler main entry point.
 * @author Matthew Tropiano
 */
public final class TAMECompilerMain 
{
	/** Default out file. */
	private static final String VERSION = "1.0";

	/** Default out file. */
	private static final String DEFAULT_OUTFILE = "module.out";
	/** Switch - don't optimize. */
	private static final String SWITCH_NOOPTIMIZE0 = "--no-optimize"; 
	private static final String SWITCH_NOOPTIMIZE1 = "-n"; 
	/** Switch - set output file. */
	private static final String SWITCH_OUTFILE0 = "--outfile"; 
	private static final String SWITCH_OUTFILE1 = "-o"; 
	/** Switch - set verbose. */
	private static final String SWITCH_VERBOSE0 = "--verbose"; 
	private static final String SWITCH_VERBOSE1 = "-v"; 
	/** Switch - add defines. */
	private static final String SWITCH_DEFINE0 = "--defines"; 
	private static final String SWITCH_DEFINE1 = "-d"; 
	
	// Scan options.
	private static boolean scanOptions(Options options, String[] args)
	{
		final int STATE_INPATH = 0;
		final int STATE_OUTPATH = 1;
		final int STATE_DEFINES = 2;
		final int STATE_SWITCHES = 3;
		
		int state = STATE_INPATH;
		
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			
			switch (state)
			{
				default:
				case STATE_INPATH:
				{
					if (arg.startsWith("-"))
					{
						state = STATE_SWITCHES;
						i++;
						continue;
					}
					else
					{
						options.fileInPath = arg;
					}
					break;
				}
				
				case STATE_OUTPATH:
				{
					if (arg.startsWith("-"))
					{
						System.out.println("ERROR: Expected an \"out\" path after switch.");
						return false;
					}
					else
					{
						options.fileOutPath = arg;
					}
					break;
				}
				
				case STATE_DEFINES:
				{
					if (arg.startsWith("-"))
					{
						state = STATE_SWITCHES;
						i++;
						continue;
					}
					else
					{
						options.defineList.add(arg);
					}
					break;
				}
			
				case STATE_SWITCHES:
				{
					if (arg.equals(SWITCH_DEFINE0) || arg.equals(SWITCH_DEFINE1))
						state = STATE_DEFINES;
					else if (arg.equals(SWITCH_OUTFILE0) || arg.equals(SWITCH_OUTFILE1))
						state = STATE_OUTPATH;
					else if (arg.equals(SWITCH_NOOPTIMIZE0) || arg.equals(SWITCH_NOOPTIMIZE1))
					{
						options.optimizing = false;
						state = STATE_INPATH;
					}
					else if (arg.equals(SWITCH_VERBOSE0) || arg.equals(SWITCH_VERBOSE1))
					{
						options.verbose = true;
						state = STATE_INPATH;
					}
					else
					{
						System.out.println("ERROR: Internal error.");
						return false;
					}
					break;
				}
				
			}
			
		}
		
		return true;
	}

	public static void printHelp()
	{
		PrintStream out = System.out;
		
		out.println("TAME Compiler v"+VERSION+" by Matt Tropiano");
		out.println("tamec [infile] [switches]");
		out.println("[switches]:");
		out.println("    -o [outfile]           Sets the output file.");
		out.println("    --outfile [outfile]");
		out.println();
		out.println("    -d [defines]           Adds define tokens to the parser.");
		out.println("    --defines [defines]");
		out.println();
		out.println("    -v                     Adds verbose output.");
		out.println("    --verbose");
		out.println();
		out.println("    -n                     Does not optimize blocks. DEBUG ONLY");
		out.println("    --no-optimize");
		
	}
	
	// Main entry.
	public static void main(String[] args) 
	{
		if (args.length == 0)
		{
			printHelp();
			System.exit(0);
		}
		
		Options options = new Options();
		
		if (!scanOptions(options, args))
			return;
		
		if (Common.isEmpty(options.fileInPath))
		{
			System.out.println("ERROR: No input file specified!");
			return;
		}

		if (Common.isEmpty(options.fileOutPath))
		{
			System.out.println("ERROR: No output file specified!");
			return;
		}
		
		File infile = new File(options.fileInPath);
		
		if (!infile.exists())
		{
			System.out.println("ERROR: Input file not found.");
			return;
		}

		TAMEModule module = null;
		try {
			module = TAMEScriptReader.read(infile, options);
		} catch (TAMEScriptParseException e) {
			System.out.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.out.println("ERROR: Could not read input file: "+infile.getPath());
			return;
		} catch (SecurityException e) {
			System.out.println("ERROR: Could not read input file: "+infile.getPath());
			System.out.println("Access to the file was denied.");
			return;
		}

		FileOutputStream fos = null;
		File outFile = new File(options.fileOutPath);
		try {
			fos = new FileOutputStream(new File(options.fileOutPath));
			module.writeBytes(fos);
		} catch (IOException e) {
			System.out.println("ERROR: Could not write output file: "+outFile.getPath());
			return;
		} catch (SecurityException e) {
			System.out.println("ERROR: Could not write output file: "+outFile.getPath());
			System.out.println("You may not have permission to write a file there.");
			return;
		} finally {
			Common.close(fos);
		}
		
		System.out.println("Wrote "+outFile.getPath()+" successfully.");
	}
	
	private static class Options implements TAMEScriptReaderOptions
	{
		private String fileInPath;
		private String fileOutPath;
		
		private boolean optimizing;
		private boolean verbose;
		private PrintStream verboseOut;
		private List<String> defineList;
		
		Options()
		{
			fileInPath = null;
			fileOutPath = DEFAULT_OUTFILE;
			optimizing = true;
			verbose = false;
			verboseOut = System.out;
			defineList = new List<>();
		}
		
		@Override
		public String[] getDefines() 
		{
			String[] out = new String[defineList.size()];
			defineList.toArray(out);
			return out;
		}
		
		@Override
		public PrintStream getVerboseOut() 
		{
			return verboseOut;
		}
		
		@Override
		public boolean isVerbose() 
		{
			return verbose;
		}
		
		@Override
		public boolean isOptimizing() 
		{
			return optimizing;
		}
		
	}
	
}
