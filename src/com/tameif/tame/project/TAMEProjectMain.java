package com.tameif.tame.project;

import java.io.PrintStream;

import com.blackrook.commons.Reflect;
import com.blackrook.commons.linkedlist.Queue;
import com.tameif.tame.TAMELogic;

/**
 * The entry point for the project main.
 * @author Matthew Tropiano
 */
public final class TAMEProjectMain
{
	/** Errors */
	private static final int ERROR_NONE = 0;
	private static final int ERROR_BADOPTIONS = 1;
	private static final int ERROR_BADCOMPILE = 2;
	private static final int ERROR_IOERROR = 3;
	private static final int ERROR_SECURITYERROR = 4;
	private static final int ERROR_NOINPUT = 5;

	/**
	 * Program modes.
	 */
	private enum Mode
	{
		/**
		 * Prints help.
		 */
		HELP
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				printHelp(out);
				return ERROR_NONE;
			};
		},

		/**
		 * Prints version.
		 */
		VERSION
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				printVersion(out);
				return ERROR_NONE;
			};
		},

		/**
		 * Creates projects.
		 */
		CREATE
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				// TODO: Finish this.
				return ERROR_NONE;
			};
		},
		;
		
		public abstract int execute(PrintStream out, Queue<String> args);
	}

	private static void printVersion(PrintStream out)
	{
		out.println("TAME Project v" + TAMELogic.getVersion() + " by Matt Tropiano");
		out.println("Running on: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + ", " + System.getProperty("java.vm.name") + ", v" +System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
	}
	
	private static void printSplash(PrintStream out)
	{
		printVersion(out);
		out.println("Type `tamep help` for help.");
	}

	private static void printHelp(PrintStream out)
	{
		printVersion(out);
		// TODO: Finish this.
	}
	
	// Scan options.
	private static Mode scanMode(Queue<String> args)
	{
		String modeName = args.dequeue().toUpperCase();
		return Reflect.getEnumInstance(modeName, Mode.class);
	}
	
	public static void main(String[] args)
	{
		final PrintStream out = System.out;
		
		if (args.length == 0)
		{
			printSplash(out);
			System.exit(ERROR_NONE);
			return;
		}
		
		Queue<String> argQueue = new Queue<>();
		for (String a : args)
			argQueue.add(a);
		
		Mode mode;
		if ((mode = scanMode(argQueue)) == null)
		{
			out.println("ERROR: Unknown mode.");
			out.println("Type `tamep help` for help.");
			System.exit(ERROR_BADOPTIONS);
			return;
		}
		
		System.exit(mode.execute(out, argQueue));
	}

}
