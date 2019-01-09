/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.List;
import com.tameif.tame.TAMELogic;
import com.tameif.tame.TAMEModule;
import com.tameif.tame.factory.TAMEJSExporter;
import com.tameif.tame.factory.TAMEJSExporterOptions;
import com.tameif.tame.factory.TAMEScriptParseException;
import com.tameif.tame.factory.TAMEScriptReader;
import com.tameif.tame.factory.TAMEScriptReaderOptions;

/**
 * The compiler main entry point.
 * @author Matthew Tropiano
 */
public final class TAMECompilerMain 
{
	/** Default out file name. */
	private static final String DEFAULT_OUTFILENAME = "module";
	/** Default out file. */
	private static final String DEFAULT_OUTFILE = DEFAULT_OUTFILENAME + ".tame";
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
	/** Switch - set input charset. */
	private static final String SWITCH_CHARSET0 = "--charset"; 
	private static final String SWITCH_CHARSET1 = "-c"; 

	/** Switch - JS export, add wrapper. */
	private static final String SWITCH_JSWRAPPER0 = "--js-wrapper"; 
	private static final String SWITCH_JSWRAPPER1 = "-js"; 

	/** Special options. */
	private static final String SWITCH_HELP = "--help"; 
	private static final String SWITCH_HELP2 = "-h"; 
	private static final String SWITCH_VERSION = "--version"; 
	private static final String SWITCH_JSENGINE = "--js-engine"; 
	private static final String SWITCH_JSNODEENGINE = "--js-engine-node"; 
	private static final String SWITCH_JSNODEENGINELIB = "--js-engine-node-lib"; 

	private static boolean printHelp = false;
	private static boolean printVersion = false;
	private static boolean exportJSEngine = false;
	private static boolean exportJSEngineNode = false;
	private static boolean exportJSEngineNodeLib = false;
	
	private static void printVersion(PrintStream out)
	{
		out.println("TAME Compiler v" + TAMELogic.getVersion() + " by Matt Tropiano");
		out.println("Running on: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + ", " + System.getProperty("java.vm.name") + ", v" +System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
	}
	
	private static void printSplash(PrintStream out)
	{
		printVersion(out);
		out.println("Type `tame --help` for help.");
	}

	private static void printHelp(PrintStream out)
	{
		printVersion(out);
		out.println("Usages:");
		out.println("tamec [infile] [switches]");
		out.println("tamec --help");
		out.println("tamec --version");
		out.println("tamec --js-engine");
		out.println("tamec --js-engine-node");
		out.println("tamec --js-engine-node-lib");
		out.println();
		out.println("[infile]: The input file.");
		out.println();
		out.println("[switches]:");
		out.println("    -h                    Print help and quit.");
		out.println("    --help");
		out.println("    --version             Print version and quit.");
		out.println();
		out.println("    -o [outfile]          Sets the output file.");
		out.println("    --outfile [outfile]");
		out.println();
		out.println("    -d [defines]          Adds define tokens to the parser.");
		out.println("    --defines [defines]");
		out.println();
		out.println("    -c [charset]          Sets the charset to use for reading by name.");
		out.println("    --charset [charset]");
		out.println();
		out.println("    -v                    Adds verbose output.");
		out.println("    --verbose");
		out.println();
		out.println("    -n                    Does not optimize blocks. DEBUG ONLY");
		out.println("    --no-optimize");
		out.println();
		out.println("    -js [name]            Export to JS, and optionally declare a wrapper to");
		out.println("    --js-wrapper [name]   use for the JavaScript exporter.");
		out.println();
		out.println("                          html       - Exports an embedded version suitable for");
		out.println("                                       ECMAScript 6 capable browsers with");
		out.println("                                       interface.");
		out.println("                          html-debug - Exports an embedded version suitable for");
		out.println("                                       ECMAScript 6 capable browsers with");
		out.println("                                       interface, in debug mode.");
		out.println("                          node       - Exports an embedded NodeJS program.");
		out.println("                          module     - Exports just module data to feed into a");
		out.println("                                       JS TAME engine.");
		out.println("                          [filename] - A file to use as the starting point for");
		out.println("                                       the exporter.");
		out.println();
		out.println("    --js-engine           Export just TAME's engine to JS.");
		out.println();
		out.println("    --js-engine-node      Export just TAME's engine as a NodeJS program.");
		out.println();
		out.println("    --js-engine-node-lib  Export just TAME's engine as a NodeJS module");
		out.println("                          (for 'require').");
		out.println();
		out.println("The currently assumed input charset, when unspecified, is \""+Charset.defaultCharset().name()+"\".");
	}
	
	// Scan options.
	private static boolean scanOptions(Options options, JSOptions jsOptions, String[] args)
	{
		final int STATE_INPATH = 0;
		final int STATE_OUTPATH = 1;
		final int STATE_DEFINES = 2;
		final int STATE_CHARSET = 3;
		final int STATE_SWITCHES = 4;
		final int STATE_JSWRAPPERNAME = 5;
		
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
					if (arg.equals(SWITCH_HELP))
					{
						printHelp = true;
					}
					else if (arg.equals(SWITCH_HELP2))
					{
						printHelp = true;
					}
					else if (arg.equals(SWITCH_VERSION))
					{
						printVersion = true;
					}
					else if (arg.equals(SWITCH_JSENGINE))
					{
						exportJSEngine = true;
					}
					else if (arg.equals(SWITCH_JSNODEENGINE))
					{
						exportJSEngineNode = true;
					}
					else if (arg.equals(SWITCH_JSNODEENGINELIB))
					{
						exportJSEngineNodeLib = true;
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
				
				case STATE_CHARSET:
				{
					if (arg.startsWith("-"))
					{
						out.println("ERROR: Expected a charset name after switch (try \"UTF-8\" or \""+Charset.defaultCharset().name()+"\").");
						return false;
					}
					else if (!Charset.isSupported(arg.trim()))
					{
						out.println("ERROR: Charset \""+arg+"\" is not supported!");
						return false;
					}
					else
					{
						options.inputCharset = Charset.forName(arg);
						state = STATE_SWITCHES;
					}
					break;
				}
			
				case STATE_SWITCHES:
				{
					if (arg.equals(SWITCH_DEFINE0) || arg.equals(SWITCH_DEFINE1))
						state = STATE_DEFINES;
					else if (arg.equals(SWITCH_CHARSET0) || arg.equals(SWITCH_CHARSET1))
						state = STATE_CHARSET;
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

	// Main entry.
	public static void main(String[] args) 
	{
		final PrintStream out = System.out;
		
		if (args.length == 0)
		{
			printSplash(out);
			System.exit(0);
		}
		
		Options options = new Options();
		JSOptions jsOptions = new JSOptions();
		
		if (!scanOptions(options, jsOptions, args))
			return;
		
		if (printHelp)
		{
			printHelp(out);
			System.exit(0);
		}
		
		if (printVersion)
		{
			printVersion(out);
			System.exit(0);
		}
		
		if (exportJSEngine)
		{
			// Fill with default if no outfile specified.
			if (Common.isEmpty(options.fileOutPath))
				options.fileOutPath = "TAME-"+TAMELogic.getVersion()+".js";

			File outJSFile = new File(options.fileOutPath);
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
			// Fill with default if no outfile specified.
			if (Common.isEmpty(options.fileOutPath))
				options.fileOutPath = "TAME.js";

			File outJSFile = new File(options.fileOutPath);
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
		
		if (exportJSEngineNodeLib)
		{
			// Fill with default if no outfile specified.
			if (Common.isEmpty(options.fileOutPath))
				options.fileOutPath = "TAME.js";

			File outJSFile = new File(options.fileOutPath);
			try {
				jsOptions.wrapperName = TAMEJSExporter.WRAPPER_NODELIBRARY;
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
		private Charset inputCharset;
		
		Options()
		{
			fileInPath = null;
			fileOutPath = null;
			jsOut = false;
			
			optimizing = true;
			verbose = false;
			verboseOut = System.out;
			defineList = new List<>();
			inputCharset = StandardCharsets.UTF_8;
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

		@Override
		public Charset getInputCharset()
		{
			return inputCharset;
		}
		
	}
	
}
