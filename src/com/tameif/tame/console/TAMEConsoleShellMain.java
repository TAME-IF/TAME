/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.list.List;
import com.tameif.tame.TAMEConstants;
import com.tameif.tame.TAMELogic;
import com.tameif.tame.TAMEModule;
import com.tameif.tame.TAMEModuleContext;
import com.tameif.tame.TAMEResponse;
import com.tameif.tame.TAMEResponseReader;
import com.tameif.tame.element.context.TElementContext;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.exception.ModuleStateException;
import com.tameif.tame.factory.DefaultReaderOptions;
import com.tameif.tame.factory.TAMEScriptParseException;
import com.tameif.tame.factory.TAMEScriptReader;
import com.tameif.tame.lang.Cue;
import com.tameif.tame.lang.FormatParser;
import com.tameif.tame.lang.TraceType;

/**
 * A console client implementation.
 * @author Matthew Tropiano
 */
public class TAMEConsoleShellMain implements TAMEConstants 
{
	
	private static void printVersion(PrintStream out)
	{
		out.println("TAME Console Shell v" + TAMELogic.getVersion() + " by Matt Tropiano");
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
		out.println("Usage: tame [help | module] <switches> <gameload> <other>");
		out.println("[help]:");
		out.println("    -h                       Print help and quit.");
		out.println("    --help");
		out.println("    --version                Print version and quit.");
		out.println();
		out.println("[module]:");
		out.println("    [binaryfile]             The compiled module to run.");
		out.println();
		out.println("<switches>:");
		out.println("    -s                       The input file is an uncompiled script.");
		out.println("    --script");
		out.println("    -v                       Verbose script compiler output.");
		out.println("    --verbose");
		out.println("    -d [tokens ...]          Defines predefined preprocessor tokens.");
		out.println("    --defines [tokens ...]");
		out.println();
		out.println("<gameload>:");
		out.println("    -l [statefile]           Loads a save state.");
		out.println("    --load-game [statefile]");
		out.println();
		out.println("<other>:");
		out.println("    --debug                  Show received cues.");
		out.println("    --inspect                Enable inspector.");
		out.println("    --trace [type ...]       If debug, also show trace cues (no types = all).");
		out.println();
		out.println("                             interpreter - trace INTERPRETER scanning.");
		out.println("                             context     - trace CONTEXT changes.");
		out.println("                             entry       - trace ENTRY point calls.");
		out.println("                             control     - trace CONTROL statement flow.");
		out.println("                             function    - trace FUNCTION calls.");
		out.println("                             value       - trace VALUE set/clear, local or");
		out.println("                                           context.");
	}

	// Entry point.
	public static void main(String ... args)
	{
		TAMEModule module = null;
		TAMEModuleContext moduleContext = null;
		
		boolean help = false;
		boolean version = false;
		boolean debug = false;
		boolean inspector = false;
		boolean load = false;
		boolean nooptimize = false;
		boolean verbose = false;

		// scan state
		boolean script = false;
		boolean trace = false;
		boolean defines = false;

		String path = null;
		String binpath = null;
		String loadpath = null;
		List<String> defineList = new List<>();
		Hash<TraceType> traceTypes = null;

		for (String arg : args)
		{
			if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("--help"))
			{
				help = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("--version"))
			{
				version = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("-s") || arg.equalsIgnoreCase("--script"))
			{
				script = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("-n") || arg.equalsIgnoreCase("--no-optimize"))
			{
				nooptimize = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("-v") || arg.equalsIgnoreCase("--verbose"))
			{
				verbose = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("-l") || arg.equalsIgnoreCase("--load-game"))
			{
				load = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("--debug"))
			{
				debug = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("--inspect"))
			{
				inspector = true;
				trace = false;
				defines = false;
			}
			else if (arg.equalsIgnoreCase("--trace"))
			{
				trace = true;
				traceTypes = new Hash<>();
				defines = false;
			}
			else if (defines)
			{
				defineList.add(arg);
			}
			else if (trace)
			{
				TraceType t;
				if ((t = Reflect.getEnumInstance(arg.toUpperCase(), TraceType.class)) != null)
					traceTypes.put(t);
			}
			else if (script)
			{
				path = arg;
				script = false;
			}
			else if (load)
			{
				loadpath = arg;
				load = false;
			}
			else
			{
				binpath = arg;
			}
		}
		
		if (args.length == 0)
		{
			printSplash(System.out);
			System.exit(0);
		}
		
		if (help)
		{
			printHelp(System.out);
			System.exit(0);
		}
		
		if (version)
		{
			printVersion(System.out);
			System.exit(0);
		}

		if (script)
		{
			System.out.println("ERROR: No module script file specified!");
			System.exit(1);
		}
		else if (!Common.isEmpty(path))
		{
			module = parseScript(path, verbose, !nooptimize, defineList); 
			if (module == null)
				System.exit(4);
		}
		else
		{
			if (Common.isEmpty(binpath))
			{
				System.out.println("ERROR: No module file specified!");
				System.exit(2);
			}
			else
			{
				module = readBinary(binpath); 
			}
		}
		
		if (module == null)
		{
			System.out.println("ERROR: No module!");
			System.exit(3);
		}
		
		moduleContext = new TAMEModuleContext(module);
		
		Context context = new Context(moduleContext, System.out, debug, inspector, trace);
		TraceType[] traceList;
		
		// no types specified = trace all.
		if (traceTypes != null)
		{
			if (traceTypes.isEmpty())
			{
				traceList = TraceType.VALUES;
			}
			else
			{
				traceList = new TraceType[traceTypes.size()];
				traceTypes.toArray(traceList);
			}
		}
		else
			traceList = new TraceType[0];

		if (loadpath != null)
		{
			if (!loadGame(moduleContext, loadpath))
				return;
		}
		else
		{
			context.out.println();
			processResponse(TAMELogic.handleInit(moduleContext, traceList), context);
			context.out.println();
		}
		
		if (!context.quit)
			context.gameLoop(traceList);
	}
	
	static TAMEModule parseScript(String path, boolean verbose, boolean optimizing, List<String> defines)
	{
		DefaultReaderOptions opts = new DefaultReaderOptions();
		opts.setVerbose(verbose);
		opts.setOptimizing(optimizing);
		String[] defineArray = new String[defines.size()];
		defines.toArray(defineArray);
		opts.setDefines(defineArray);
		
		File file = new File(path);
		if (!file.exists())
		{
			System.out.println("ERROR: Module "+path+" does not exist!");
			return null;
		}
		
		TAMEModule out = null;
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			out = TAMEScriptReader.read(file, opts);
		} catch (TAMEScriptParseException e) {
			System.out.println("ERROR: "+e.getMessage());
			return null;
		} catch (SecurityException e) {
			System.out.println("ERROR: Could not read from "+file.getPath()+". Access denied.");
			return null;
		} catch (IOException e) {
			System.out.println("ERROR: Could not read from "+file.getPath());
			System.out.println(Common.getExceptionString(e));
			return null;
		} finally {
			Common.close(in);
		}
		
		return out;
	}
	
	static TAMEModule readBinary(String path)
	{
		File file = new File(path);
		if (!file.exists())
		{
			System.out.println("ERROR: Module "+path+" does not exist!");
			return null;
		}
		
		TAMEModule out = null;
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			out = TAMEModule.create(in);
		} catch (ModuleException e) {
			System.out.println("ERROR: "+file.getPath()+" is not a TAME module.");
			return null;
		} catch (SecurityException e) {
			System.out.println("ERROR: Could not read from "+file.getPath()+". Access denied.");
			return null;
		} catch (IOException e) {
			System.out.println("ERROR: Could not read from "+file.getPath());
			System.out.println(Common.getExceptionString(e));
			return null;
		} finally {
			Common.close(in);
		}
		
		return out;
	}
	
	private static boolean saveGame(TAMEModuleContext context, String path)
	{
		File file = new File(path);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			context.writeBytes(out);
		} catch (ModuleException e) {
			System.out.println("ERROR: Could not write "+file.getPath()+". " + e.getMessage());
			return false;
		} catch (SecurityException e) {
			System.out.println("ERROR: Could not write "+file.getPath()+". Access denied.");
			return false;
		} catch (IOException e) {
			System.out.println("ERROR: Could not write "+file.getPath());
			e.printStackTrace(System.err);
			return false;
		} finally {
			Common.close(out);
		}
		
		return true;
	}
	
	private static boolean loadGame(TAMEModuleContext context, String path)
	{
		File file = new File(path);
		if (!file.exists())
		{
			System.out.println("ERROR: Save file "+path+" does not exist!");
			return false;
		}
		
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			context.readBytes(in);
		} catch (ModuleStateException e) {
			System.out.println("ERROR: Could not read from "+file.getPath()+". " + e.getMessage());
			return false;
		} catch (SecurityException e) {
			System.out.println("ERROR: Could not read from "+file.getPath()+". Access denied.");
			return false;
		} catch (IOException e) {
			System.out.println("ERROR: Could not read from "+file.getPath());
			e.printStackTrace(System.err);
			return false;
		} finally {
			Common.close(in);
		}
		
		return true;
	}
	
	private static void processResponse(TAMEResponse response, Context context)
	{
		if (context.debug)
		{
			context.out.println("Interpret time: "+(response.getInterpretNanos()/1000000.0)+" ms");
			context.out.println("Request time: "+(response.getRequestNanos()/1000000.0)+" ms");
			context.out.println("Operations: "+response.getOperationsExecuted());
			context.out.println("Cues: "+response.getCues().size());
			context.out.println();
			for (Cue cue : response.getCues())
			{
				if (cue.getType().equals(CUE_FATAL) || cue.getType().equals(CUE_QUIT))
					context.quit = true;
				context.out.println("[" + cue.getType() + "] " + Common.withEscChars(cue.getContent()));
			}
		}
		else
		{
			CueHandler currentHandler = new CueHandler(response, context);
			while (!context.quit && currentHandler.read())
			{
				if (context.paused)
				{
					context.out.print("(Continue) ");
					Common.getLine();
					context.paused = false;
				}
			}

			if (currentHandler.textBuffer.length() > 0)
			{
				if (Common.isWindows())
					Common.printWrapped(context.out, currentHandler.textBuffer.toString(), 80); // windows terminal
				else
					context.out.println(currentHandler.textBuffer.toString());
			}
		}
	}
	
	private static void inspect(Context context, String inspectString)
	{
		String[] split = inspectString.split("\\.", 2);
		if (split.length < 1 || Common.isEmpty(split[0]))
		{
			context.out.println("?> Must specify a variable.");
		}
		else if (split.length < 2 || Common.isEmpty(split[1]))
		{
			TElementContext<?> ec = null;
			
			if (split[0].equalsIgnoreCase("world"))
				ec = context.context.getWorldContext();
			else
				ec = context.context.getContextByIdentity(split[0]);

			if (ec == null)
			{
				context.out.println("?> Context \""+split[0]+"\" not found.");
			}
			else
			{
				Iterator<String> it = ec.values();
				while (it.hasNext())
				{
					String v = it.next();
					context.out.println("?> "+split[0]+"."+v+" = "+ec.getValue(v));
				}
			}
		}
		else
		{
			TElementContext<?> ec = null;
			
			if (split[0].equalsIgnoreCase("world"))
				ec = context.context.getWorldContext();
			else
				ec = context.context.getContextByIdentity(split[0]);
			
			if (ec == null)
				context.out.println("?> Context \""+split[0]+"\" not found.");
			else
				context.out.println("?> "+split[0]+"."+split[1]+" = "+ec.getValue(split[1]));
			
		}
		
		context.out.println();
	}

	/**
	 * Game context.
	 */
	private static class Context
	{
		TAMEModuleContext context;
		PrintStream out;
		boolean paused;
		boolean quit;
		boolean debug;
		boolean inspector;
		
		Context(TAMEModuleContext context, PrintStream out, boolean debug, boolean inspector, boolean trace)
		{
			this.context = context;
			this.out = out;
			this.debug = debug;
			this.inspector = inspector;

			this.paused = false;
			this.quit = false;
		}
		
		void gameLoop(TraceType[] traceList)
		{
			final String COMMAND_SAVE = "!save ";
			final String COMMAND_LOAD = "!load ";
			final String COMMAND_QUIT = "!quit";
			final String INSPECTOR_PREFIX = "?";
			
			boolean good = true;
			String line;
			while (good && !quit)
			{
				out.print("] ");
				if ((line = Common.getLine()) != null)
				{
					out.println();
					line = line.replaceAll("\\s+", " ").trim();
					if (inspector && line.startsWith(INSPECTOR_PREFIX))
						inspect(this, line.substring(INSPECTOR_PREFIX.length()));
					else if (line.toLowerCase().startsWith(COMMAND_SAVE))
						saveGame(context, line.substring(COMMAND_SAVE.length())+".sav");
					else if (line.toLowerCase().startsWith(COMMAND_LOAD))
						loadGame(context, line.substring(COMMAND_LOAD.length())+".sav");
					else if (line.toLowerCase().startsWith(COMMAND_QUIT))
						good = false;
					else
					{
						processResponse(TAMELogic.handleRequest(context, line, traceList), this);
						out.println();
					}
				}
				else
					good = false;
			}
		}
		
	}
	
	/**
	 * Cue handler.
	 */
	static class CueHandler extends TAMEResponseReader
	{
		ConsoleFormatter formatter;
		StringBuilder textBuffer;
		Context context;
		int lastColumn;

		CueHandler(TAMEResponse response, Context context) 
		{
			super(response);
			this.context = context;
			this.textBuffer = new StringBuilder();
			this.formatter = new ConsoleFormatter(textBuffer);
		}

		@Override
		public boolean handleCue(Cue cue) 
		{
			if (!cue.getType().equals(CUE_TEXT) && !cue.getType().equals(CUE_TEXTF) && textBuffer.length() > 0)
			{
				if (Common.isWindows())
					lastColumn = Common.printWrapped(context.out, textBuffer.toString(), lastColumn, 80); // windows terminal
				else
					context.out.print(textBuffer.toString());
				textBuffer.delete(0, textBuffer.length());
			}
			
			switch (cue.getType())
			{
				default:
				case CUE_TRACE:
					return true;
				case CUE_QUIT:
					context.quit = true;
					return false;
				case CUE_WAIT:
					Common.sleep(Common.parseLong(cue.getContent()));
					return true;
				case CUE_TEXT:
					textBuffer.append(cue.getContent());
					return true;
				case CUE_TEXTF:
					formatter.parse(cue.getContent());
					return true;
				case CUE_PAUSE:
					context.paused = true;
					lastColumn = 0;
					return false;
				case CUE_ERROR:
					context.out.println("ERROR: " + cue.getContent());
					lastColumn = 0;
					return true;
				case CUE_FATAL:
					context.out.println("!!FATAL!! " + cue.getContent());
					context.quit = true;
					lastColumn = 0;
					return false;
			}
		}
		
	}
	
	/**
	 * Format handler.
	 */
	static class ConsoleFormatter extends FormatParser
	{
		StringBuilder textBuffer;
		
		public ConsoleFormatter(StringBuilder textBuffer) 
		{
			this.textBuffer = textBuffer;
		}
		
		@Override
		public void startTag(String tagName) 
		{
			// Nothing.
		}
		
		@Override
		public void sendText(String text) 
		{
			textBuffer.append(text);
		}
		
		@Override
		public void endTag(String tagName) 
		{
			// Nothing.
		}
		
	}
	
}
