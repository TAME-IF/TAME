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
		 * Prints help for a command.
		 */
		HELP
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				Mode mode = null;
				if (!args.isEmpty())
					mode = Reflect.getEnumInstance(args.dequeue().toUpperCase(), Mode.class);
				printVersion(out);
				out.println();
				if (mode == null)
					HELP.help(out);
				else
					mode.help(out);
				return ERROR_NONE;
			};
			
			@Override
			public void help(PrintStream out)
			{
				out.println("Usage: tamep help [mode]");
				out.println("Prints help for a specific mode. Valid values for [mode] include:");
				out.println();
				for (Mode m : Mode.values())
					out.printf("%-9s %s\n", m.name().toLowerCase(), m.description());
			}
			
			@Override
			public String description() 
			{
				return "Prints help for a specific mode.";
			}
			
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
			
			@Override
			public void help(PrintStream out)
			{
				out.println("Usage: tamep version");
				out.println("Prints just the version splash for TAME Project.");
				out.println("This should line up with TAME's current version.");
			}

			@Override
			public String description() 
			{
				return "Prints the version splash for TAME Project.";
			}
			
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
			
			@Override
			public void help(PrintStream out) 
			{
				// TODO Finish this.
			}
			
			@Override
			public String description() 
			{
				return "Creates/sets up a new project.";
			}
			
		},
		;
		
		/**
		 * Executes this program mode.
		 * @param out the output stream for console output.
		 * @param args the queue of command arguments.
		 * @return the exit code for the program.
		 */
		public abstract int execute(PrintStream out, Queue<String> args);
		
		/**
		 * Prints help for this mode.
		 * @param out the output stream for console output.
		 */
		public abstract void help(PrintStream out);
		
		/**
		 * @return a short description of this mode.
		 */
		public abstract String description();
		
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
