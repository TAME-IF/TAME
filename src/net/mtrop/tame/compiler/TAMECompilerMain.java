/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.List;
import com.blackrook.commons.hash.CaseInsensitiveHash;

import net.mtrop.tame.TAMELogic;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.factory.TAMEJSExporter;
import net.mtrop.tame.factory.TAMEJSExporterOptions;
import net.mtrop.tame.factory.TAMEScriptParseException;
import net.mtrop.tame.factory.TAMEScriptReader;
import net.mtrop.tame.factory.TAMEScriptReaderOptions;

/**
 * The compiler main entry point.
 * @author Matthew Tropiano
 */
public final class TAMECompilerMain 
{
	/** Default out file name. */
	private static final String DEFAULT_OUTFILENAME = "module";
	/** Default out file. */
	private static final String DEFAULT_OUTFILE = DEFAULT_OUTFILENAME + ".out";
	/** Default out for HTML. */
	private static final String DEFAULT_OUTFILE_HTML = DEFAULT_OUTFILENAME + ".html";
	/** Default out for JS. */
	private static final String DEFAULT_OUTFILE_JS = DEFAULT_OUTFILENAME + ".js";
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

	/** Switch - JS export, add wrapper. */
	private static final String SWITCH_JSWRAPPER0 = "--js-wrapper"; 
	private static final String SWITCH_JSWRAPPER1 = "-js"; 

	/** Special options. */
	private static final String SWITCH_JSENGINE = "--js-engine"; 
	private static final String SWITCH_JSNODEENGINE = "--js-engine-node"; 

	private static boolean exportJSEngine = false;
	private static boolean exportJSEngineNode = false;
	
	/** Hash of valid wrapper names. */
	private static final CaseInsensitiveHash VALID_WRAPPER_NAMES = new CaseInsensitiveHash() {{
		put(TAMEJSExporter.WRAPPER_NODEENGINE);
		put(TAMEJSExporter.WRAPPER_ENGINE);
		put(TAMEJSExporter.WRAPPER_MODULE);
		put(TAMEJSExporter.WRAPPER_NODE);
		put(TAMEJSExporter.WRAPPER_BROWSER);
		put(TAMEJSExporter.WRAPPER_HTML);
		put(TAMEJSExporter.WRAPPER_HTML_DEBUG);
	}};
	
	// Scan options.
	private static boolean scanOptions(Options options, JSOptions jsOptions, String[] args)
	{
		final int STATE_INPATH = 0;
		final int STATE_OUTPATH = 1;
		final int STATE_DEFINES = 2;
		final int STATE_SWITCHES = 3;
		final int STATE_JSWRAPPERNAME = 4;
		
		final PrintStream out = System.out;

		int state = STATE_INPATH;
		
		for (int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			
			switch (state)
			{
				default:
				case STATE_INPATH:
				{
					if (arg.equals(SWITCH_JSENGINE))
					{
						exportJSEngine = true;
					}
					else if (arg.equals(SWITCH_JSNODEENGINE))
					{
						exportJSEngineNode = true;
					}
					else if (arg.startsWith("-"))
					{
						state = STATE_SWITCHES;
						i--;
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
						out.println("ERROR: Expected an \"out\" path after switch.");
						return false;
					}
					else
					{
						options.fileOutPath = arg.trim();
						state = STATE_SWITCHES;
					}
					break;
				}
				
				case STATE_JSWRAPPERNAME:
				{
					if (arg.startsWith("-"))
					{
						out.println("ERROR: Expected a wrapper name after switch.");
						return false;
					}
					else
					{
						if (!VALID_WRAPPER_NAMES.contains(arg))
						{
							out.println("ERROR: Expected a valid wrapper name after switch.");
							return false;
						}
						
						jsOptions.wrapperName = arg;
						state = STATE_SWITCHES;
					}
					break;
				}
				
				case STATE_DEFINES:
				{
					if (arg.startsWith("-"))
					{
						state = STATE_SWITCHES;
						i--;
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
					else if (arg.equals(SWITCH_JSWRAPPER0) || arg.equals(SWITCH_JSWRAPPER1))
					{
						options.jsOut = true;
						state = STATE_JSWRAPPERNAME;
					}
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
						out.println("ERROR: Unknown switch: "+arg);
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
		
		out.println("TAME Compiler v"+TAMELogic.getVersion()+" by Matt Tropiano");
		out.println("Usages:");
		out.println("tamec [infile] [switches]");
		out.println("tamec --js-engine");
		out.println("tamec --js-engine-node");
		out.println("[infile]: The input file.");
		out.println();
		out.println("[switches]:");
		out.println("    -o [outfile]         Sets the output file.");
		out.println("    --outfile [outfile]");
		out.println();
		out.println("    -d [defines]         Adds define tokens to the parser.");
		out.println("    --defines [defines]");
		out.println();
		out.println("    -v                   Adds verbose output.");
		out.println("    --verbose");
		out.println();
		out.println("    -n                   Does not optimize blocks. DEBUG ONLY");
		out.println("    --no-optimize");
		out.println();
		out.println("    -js [name]           Export to JS, and optionally declare a wrapper to");
		out.println("    --js-wrapper [name]  use for the JavaScript exporter.");
		out.println();
		out.println("                         browser - Exports an embedded version suitable for");
		out.println("                                   ECMAScript 5 capable browsers (default, if");
		out.println("                                   no name declared).");
		out.println("                         html    - Exports an embedded version suitable for");
		out.println("                                   ECMAScript 5 capable browsers with");
		out.println("                                   interface.");
		out.println("                         node    - Exports an embedded NodeJS program.");
		out.println("                         module  - Exports just module data to feed into a");
		out.println("                                   JS TAME engine.");
		out.println();
		out.println("    --js-engine          Export just TAME's engine to JS.");
		out.println();
		out.println("    --js-engine-node     Export just TAME's engine as a NodeJS library");
		out.println("                         (for 'require').");
	}
	
	// Main entry.
	public static void main(String[] args) 
	{
		final PrintStream out = System.out;
		
		if (args.length == 0)
		{
			printHelp();
			System.exit(0);
		}
		
		Options options = new Options();
		JSOptions jsOptions = new JSOptions();
		
		if (!scanOptions(options, jsOptions, args))
			return;
		
		if (exportJSEngine)
		{
			File outJSFile = new File("TAME-"+TAMELogic.getVersion()+".js");
			try {
				jsOptions.wrapperName = TAMEJSExporter.WRAPPER_ENGINE;
				TAMEJSExporter.export(outJSFile, null, jsOptions);
				out.println("Wrote "+outJSFile.getPath()+" successfully.");
			} catch (IOException e) {
				out.println("ERROR: Could not export JS file: "+outJSFile.getPath());
				out.println(e.getMessage());
				return;
			} catch (SecurityException e) {
				out.println("ERROR: Could not write JS file: "+outJSFile.getPath());
				out.println("Writing the file was denied by the OS.");
				return;
			}
			return;
		}
			
		if (exportJSEngineNode)
		{
			File outJSFile = new File("TAME.js");
			try {
				jsOptions.wrapperName = TAMEJSExporter.WRAPPER_NODEENGINE;
				TAMEJSExporter.export(outJSFile, null, jsOptions);
				out.println("Wrote "+outJSFile.getPath()+" successfully.");
			} catch (IOException e) {
				out.println("ERROR: Could not export JS file: "+outJSFile.getPath());
				out.println(e.getMessage());
				return;
			} catch (SecurityException e) {
				out.println("ERROR: Could not write JS file: "+outJSFile.getPath());
				out.println("Writing the file was denied by the OS.");
				return;
			}
			return;
		}
		
		if (Common.isEmpty(options.fileInPath))
		{
			out.println("ERROR: No input file specified!");
			return;
		}

		File infile = new File(options.fileInPath);
		
		if (!infile.exists())
		{
			out.println("ERROR: Input file not found.");
			return;
		}

		TAMEModule module = null;
		try {
			module = TAMEScriptReader.read(infile, options);
		} catch (TAMEScriptParseException e) {
			out.println("ERROR: "+e.getMessage());
			return;
		} catch (IOException e) {
			out.println("ERROR: Could not read input file: "+infile.getPath());
			return;
		} catch (SecurityException e) {
			out.println("ERROR: Could not read input file: "+infile.getPath());
			out.println("Access to the file was denied.");
			return;
		}

		// Write JS standalone file.
		if (options.jsOut)
		{
			// Fill with default if no outfile specified.
			if (Common.isEmpty(options.fileOutPath))
			{
				if (Common.isEmpty(jsOptions.wrapperName))
					options.fileOutPath = DEFAULT_OUTFILE_JS;
				else if (TAMEJSExporter.WRAPPER_HTML.equalsIgnoreCase(jsOptions.wrapperName) || TAMEJSExporter.WRAPPER_HTML_DEBUG.equalsIgnoreCase(jsOptions.wrapperName))
					options.fileOutPath = DEFAULT_OUTFILE_HTML;
				else
					options.fileOutPath = DEFAULT_OUTFILE_JS;
			}
			
			File outJSFile = new File(options.fileOutPath);
			try {
				TAMEJSExporter.export(outJSFile, module, jsOptions);
				out.println("Wrote "+outJSFile.getPath()+" successfully.");
			} catch (IOException e) {
				out.println("ERROR: Could not export JS file: "+outJSFile.getPath());
				out.println(e.getMessage());
				return;
			} catch (SecurityException e) {
				out.println("ERROR: Could not write JS file: "+outJSFile.getPath());
				out.println("Writing the file was denied by the OS.");
				return;
			}
		}
		else
		{
			// Fill with default if no outfile specified.
			if (Common.isEmpty(options.fileOutPath))
				options.fileOutPath = DEFAULT_OUTFILE;

			// Write serialized file.
			FileOutputStream fos = null;
			File outFile = new File(options.fileOutPath);
			try {
				fos = new FileOutputStream(new File(options.fileOutPath));
				module.writeBytes(fos);
				out.println("Wrote "+outFile.getPath()+" successfully.");
			} catch (IOException e) {
				out.println("ERROR: Could not write output file: "+outFile.getPath());
				return;
			} catch (SecurityException e) {
				out.println("ERROR: Could not write output file: "+outFile.getPath());
				out.println("You may not have permission to write a file there.");
				return;
			} finally {
				Common.close(fos);
			}
		}
		
	}

	private static class JSOptions implements TAMEJSExporterOptions
	{
		private String wrapperName;
		private boolean pathOutputEnabled;
		
		JSOptions()
		{
			wrapperName = null;
		}

		@Override
		public String getWrapperName() 
		{
			return wrapperName;
		}
		
		@Override
		public boolean isPathOutputEnabled() 
		{
			return pathOutputEnabled;
		}

		@Override
		public String getModuleVariableName()
		{
			return null;
		}
		
	}
	
	private static class Options implements TAMEScriptReaderOptions
	{
		private String fileInPath;
		private String fileOutPath;
		private boolean jsOut;
		
		private boolean optimizing;
		private boolean verbose;
		private PrintStream verboseOut;
		private List<String> defineList;
		
		Options()
		{
			fileInPath = null;
			fileOutPath = null;
			jsOut = false;
			
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
