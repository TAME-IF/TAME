/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.console;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.blackrook.commons.Common;
import com.blackrook.commons.list.List;

import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.TAMELogic;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.TAMEModuleContext;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.TAMEResponseReader;
import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.exception.ModuleStateException;
import net.mtrop.tame.factory.DefaultReaderOptions;
import net.mtrop.tame.factory.TAMEScriptParseException;
import net.mtrop.tame.factory.TAMEScriptReader;
import net.mtrop.tame.struct.Cue;
import net.mtrop.tame.struct.FormatParser;

/**
 * A console client implementation.
 * @author Matthew Tropiano
 */
public class TAMEConsoleClientMain implements TAMEConstants 
{
	
	private static void printHelp(PrintStream out)
	{
		out.println("TAME Console Client v"+TAMELogic.getVersion()+" by Matt Tropiano (C) 2016");
		out.println("Usage: tame [help | module] <switches> <gameload> (--debug)");
		out.println("[help]:");
		out.println("    -h");
		out.println("    --help");
		out.println("    /?");
		out.println("[module]:");
		out.println("    [binaryfile]");
		out.println("    -s [scriptfile]");
		out.println("    --script [scriptfile]");
		out.println("<switches>:");
		out.println("    -v");
		out.println("    --verbose");
		out.println("    -d  [tokens ...]");
		out.println("    --defines [tokens ...]");
		out.println("<gameload>:");
		out.println("    -l [statefile]");
		out.println("    --load-game [statefile]");
	}

	// Entry point.
	public static void main(String ... args)
	{
		TAMEModule module = null;
		TAMEModuleContext moduleContext = null;
		
		boolean help = false;
		boolean script = false;
		boolean debug = false;
		boolean load = false;
		boolean defines = false;
		boolean nooptimize = false;
		boolean verbose = false;
		String path = null;
		String binpath = null;
		String loadpath = null;
		List<String> defineList = new List<>();

		for (String arg : args)
		{
			if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("--help") || arg.equalsIgnoreCase("/?"))
				help = true;
			else if (arg.equalsIgnoreCase("-s"))
				script = true;
			else if (arg.equalsIgnoreCase("--script"))
				script = true;
			else if (arg.equalsIgnoreCase("-n"))
				nooptimize = true;
			else if (arg.equalsIgnoreCase("--no-optimize"))
				nooptimize = true;
			else if (arg.equalsIgnoreCase("-v"))
				verbose = true;
			else if (arg.equalsIgnoreCase("--verbose"))
				verbose = true;
			else if (arg.equalsIgnoreCase("-l"))
				load = true;
			else if (arg.equalsIgnoreCase("--load-game"))
				load = true;
			else if (arg.equalsIgnoreCase("--debug"))
				debug = true;
			else if (script)
			{
				if (arg.equalsIgnoreCase("-d"))
					defines = true;
				else if (arg.equalsIgnoreCase("--defines"))
					defines = true;
				else
					path = arg;
				script = false;
			}
			else if (defines)
				defineList.add(arg);
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
		
		if (args.length == 0 || help)
		{
			printHelp(System.out);
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
		
		Context context = new Context(module, moduleContext, System.out, System.err, debug);
		
		if (loadpath != null)
		{
			if (!loadGame(moduleContext, loadpath))
				return;
		}
		else
		{
			processResponse(TAMELogic.handleInit(moduleContext, debug), context);
		}
		
		if (!context.quit)
			context.gameLoop();
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
	
	static boolean saveGame(TAMEModuleContext context, String path)
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
			return false;
		} finally {
			Common.close(out);
		}
		
		return true;
	}
	
	static boolean loadGame(TAMEModuleContext context, String path)
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
			return false;
		} finally {
			Common.close(in);
		}
		
		return true;
	}
	
	static void processResponse(TAMEResponse response, Context context)
	{
		if (context.debug)
		{
			for (Cue cue : response.getCues())
			{
				if (cue.getType().equals(CUE_FATAL) || cue.getType().equals(CUE_QUIT))
					context.quit = true;
				context.out.println("[" + cue.getType() + "]:" + Common.withEscChars(cue.getContent()));
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
				context.out.println(currentHandler.textBuffer.toString());
		}
	}
	
	/**
	 * Game context.
	 */
	static class Context
	{
		TAMEModule module;
		TAMEModuleContext context;
		PrintStream out;
		PrintStream err;
		boolean paused;
		boolean quit;
		boolean debug;
		
		Context(TAMEModule module, TAMEModuleContext context, PrintStream out, PrintStream err, boolean debug)
		{
			this.module = module;
			this.context = context;
			this.out = out;
			this.err = err;
			this.debug = debug;

			this.paused = false;
			this.quit = false;
		}
		
		void gameLoop()
		{
			final String COMMAND_SAVE = "!save ";
			final String COMMAND_LOAD = "!load ";
			
			boolean good = true;
			String line;
			while (good && !quit)
			{
				out.print("> ");
				if ((line = Common.getLine()) != null)
				{
					line = line.replaceAll("\\s+", " ").trim();
					if (line.toLowerCase().startsWith(COMMAND_SAVE))
						saveGame(context, line.substring(COMMAND_SAVE.length())+".sav");
					else if (line.toLowerCase().startsWith(COMMAND_LOAD))
						loadGame(context, line.substring(COMMAND_LOAD.length())+".sav");
					else
						processResponse(TAMELogic.handleRequest(context, line, debug), this);
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
				context.out.println(textBuffer.toString());
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
					return false;
				case CUE_TIP:
					context.out.println("(TIP: " + cue.getContent() + ")");
					return true;
				case CUE_INFO:
					context.out.println("INFO: " + cue.getContent());
					return true;
				case CUE_ERROR:
					context.out.println("ERROR: " + cue.getContent());
					return true;
				case CUE_FATAL:
					context.out.println("!!FATAL!! " + cue.getContent());
					context.quit = true;
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
