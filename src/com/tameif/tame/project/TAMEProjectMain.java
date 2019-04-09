package com.tameif.tame.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Properties;

import com.blackrook.commons.Reflect;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.util.ArrayUtils;
import com.blackrook.commons.util.FileUtils;
import com.blackrook.commons.util.IOUtils;
import com.blackrook.commons.util.OSUtils;
import com.blackrook.commons.util.ObjectUtils;
import com.blackrook.commons.util.ValueUtils;
import com.tameif.tame.TAMELogic;
import com.tameif.tame.factory.TAMEJSExporterOptions;
import com.tameif.tame.factory.TAMEScriptReaderOptions;

/**
 * The entry point for the project main.
 * @author Matthew Tropiano
 */
public final class TAMEProjectMain
{
	public static final String[] TEXT_FILE_TYPES = 
	{
		"css",
		"htm",
		"html",
		"js",
		"txt",
	};
	
	/** System property - Template Path. */
	private static final String SYSTEM_PROPERTY_TEMPLATE_PATH = "tame.project.template.path";

	/** Template Directory */
	private static final String TEMPLATE_PATH = System.getProperty(SYSTEM_PROPERTY_TEMPLATE_PATH);
	
	/** Replace Key - Current Year */
	private static final String REPLACEKEY_CURRENT_YEAR = "CURRENT_YEAR";
	/** Replace Key - Current Date */
	private static final String REPLACEKEY_CURRENT_DATE = "CURRENT_DATE";
	/** Replace Key - System Charset */
	private static final String REPLACEKEY_SYSTEM_CHARSET = "SYSTEM_CHARSET";
	/** Replace Key - Compiler Version */
	private static final String REPLACEKEY_COMPILER_VERSION = "COMPILER_VERSION";

	/** Project Property - Distribution Output */
	private static final String PROJECT_PROPERTY_DIST = "tame.project.dist";
	/** Project Property - Distribution Output Module */
	private static final String PROJECT_PROPERTY_DIST_MODULE = "tame.project.dist.module";
	/** Project Property - Distribution Output Web Directory */
	private static final String PROJECT_PROPERTY_DIST_WEB = "tame.project.dist.web";
	/** Project Property - Distribution Output Web Directory Zipped */
	private static final String PROJECT_PROPERTY_DIST_WEB_ZIP = "tame.project.dist.web.zip";

	/** Project Property - Charset. */
	private static final String PROJECT_PROPERTY_CHARSET = "tame.project.charset";
	/** Project Property - TAMEScript Entry Point. */
	private static final String PROJECT_PROPERTY_SRC_MAIN = "tame.project.src.main";
	/** Project Property - TAMEScript Web Starting Index. */
	private static final String PROJECT_PROPERTY_HTML_INDEX = "tame.project.html.index";
	/** Project Property - TAMEScript Web Static Assets. */
	private static final String PROJECT_PROPERTY_HTML_ASSETS = "tame.project.html.assets";

	/** Project Property - TAMEScript Defines. */
	private static final String PROJECT_PROPERTY_DEFINES = "tame.project.defines";

	/** Project Property - No Optimize. */
	private static final String PROJECT_PROPERTY_NOOPTIMIZE = "tame.project.nooptimize";
	/** Project Property - Verbose Output. */
	private static final String PROJECT_PROPERTY_VERBOSE = "tame.project.verbose";

	/** Switch CREATE - Use template. */
	private static final String SWITCH_CREATE_TEMPLATE0 = "--template";
	/** Switch CREATE - Use template. */
	private static final String SWITCH_CREATE_TEMPLATE1 = "-t";
	/** Switch CREATE - Make Git Ignore. */
	private static final String SWITCH_CREATE_GIT = "--git";
	
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
				if (args.isEmpty())
				{
					out.println("ERROR: Missing project directory.");
					return ERROR_BADOPTIONS;
				}
				
				File projectDir = new File(args.dequeue());
				if (projectDir.exists())
				{
					out.println("ERROR: Target directory already exists: " + projectDir.getPath());
					return ERROR_IOERROR;
				}
				else if (!FileUtils.createPath(projectDir.getPath()))
				{
					out.println("ERROR: Could not create directory for project: " + projectDir);
					return ERROR_IOERROR;
				}
				
				boolean addGitIgnore = false;
				String webTemplateName = "standard";
				
				final int STATE_START = 0;
				final int STATE_TEMPLATE = 1;
				int state = STATE_START;
				while (!args.isEmpty())
				{
					String arg = args.dequeue();
					switch (state)
					{
						case STATE_START:
						{
							if (arg.equals(SWITCH_CREATE_TEMPLATE0) || arg.equals(SWITCH_CREATE_TEMPLATE1))
								state = STATE_TEMPLATE;
							else if (arg.equals(SWITCH_CREATE_GIT))
								addGitIgnore = true;
							else
							{
								out.println("ERROR: Unknown switch: " + arg);
								return ERROR_BADOPTIONS;
							}
						}
						break;
						
						case STATE_TEMPLATE:
						{
							webTemplateName = args.dequeue();
							state = STATE_START;
						}
						break;
					}
				}
				if (state == STATE_TEMPLATE)
				{
					out.println("ERROR: Expected template name after template switch.");
					return ERROR_BADOPTIONS;
				}

				File webTemplateDirectory = new File(TEMPLATE_PATH + File.separator + webTemplateName);
				if (!webTemplateDirectory.isDirectory())
				{
					out.println("ERROR: Could not find template: " + webTemplateDirectory.getPath());
					return ERROR_IOERROR;
				}
				
				// TODO: Finish this.
				return ERROR_NONE;
			};
			
			@Override
			public void help(PrintStream out) 
			{
				out.println("Usage: tamep create [directory] [switches]");
				out.println("Creates a new project in a directory.");
				out.println();
				out.println("[directory]:");
				out.println("    The directory to create with the project data.");
				out.println();
				out.println("[switches]:");
				out.println();
				out.println("    --template [name]    If specified, changes what template to use for");
				out.println("    -t                   the project.");
				out.println();
				out.println("    --git                Adds a .gitignore file to the project.");
				out.println();
			}
			
			@Override
			public String description() 
			{
				return "Creates/sets up a new project.";
			}
			
		},

		/**
		 * Updates TAME components.
		 */
		UPDATE
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				// TODO: Finish this.
				return ERROR_NONE;
			};
			
			@Override
			public void help(PrintStream out) 
			{
				out.println("Usage: tamep update [component]");
				// TODO: Finish this.
			}
			
			@Override
			public String description() 
			{
				return "Updates parts of the project.";
			}
			
		},

		/**
		 * Cleans up the compiled project, deleting the contents of the distribution folder.
		 */
		CLEAN
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				// TODO: Finish this.
				return ERROR_NONE;
			};
			
			@Override
			public void help(PrintStream out) 
			{
				out.println("Usage: tamep clean");
				// TODO: Finish this.
			}
			
			@Override
			public String description() 
			{
				return "Deletes the compiled project files.";
			}
			
		},

		/**
		 * Compiles the project.
		 */
		COMPILE
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				// TODO: Finish this.
				return ERROR_NONE;
			};
			
			@Override
			public void help(PrintStream out) 
			{
				out.println("Usage: tamep compile [switches]");
				// TODO: Finish this.
			}
			
			@Override
			public String description() 
			{
				return "Compiles the project.";
			}
			
		},

		/**
		 * Compiles and releases the project.
		 * Zips up the compiled web assets.
		 */
		RELEASE
		{
			public int execute(PrintStream out, Queue<String> args)
			{
				// TODO: Finish this.
				return ERROR_NONE;
			};
			
			@Override
			public void help(PrintStream out) 
			{
				out.println("Usage: tamep release");
				// TODO: Finish this.
			}
			
			@Override
			public String description() 
			{
				return "Releases the project, creating a Zip of the web assets.";
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

	private static void copyToDirectory(File sourceDirectory, File destinationDirectory) throws IOException
	{
		if (!FileUtils.createPath(destinationDirectory.getPath()))
			throw new IOException("Could not create directory: " + destinationDirectory.getPath());

		for (File f : sourceDirectory.listFiles())
		{
			File destinationFile = new File(destinationDirectory.getPath() + File.separator + f.getName());
			if (f.isDirectory())
				copyToDirectory(f, destinationFile);
			else 
			{
				// if text file, export() instead of copy.
				if (ArrayUtils.indexOf(FileUtils.getFileExtension(f).toLowerCase(), TEXT_FILE_TYPES) >= 0)
				{
					try (
						Reader src = IOUtils.openTextFile(f); 
						Writer dest = new PrintWriter(new FileOutputStream(destinationFile))
					)
					{
						export(src, dest);
					}
				}
				else
				{
					try (
						InputStream src = new BufferedInputStream(new FileInputStream(f)); 
						OutputStream dest = new FileOutputStream(destinationFile)
					)
					{
						IOUtils.relay(src, dest, 16384);
					}
				}
			}
		}
	}
	
	private static void deleteDirectory(File directory, boolean deleteTop) throws IOException
	{
		for (File f : directory.listFiles())
		{
			if (f.isDirectory())
				deleteDirectory(f, true);
			else if (!f.delete())
				throw new IOException("Could not delete file: " + f.getPath());
		}
		if (deleteTop && !directory.delete())
			throw new IOException("Could not delete directory: " + directory.getPath());
	}

	private static void export(Reader reader, Writer writer) throws IOException
	{
		final int STATE_INIT = 0;
		final int STATE_TAG_START = 1;
		final int STATE_TAG = 2;
		final int STATE_TAG_END = 3;
		int state = STATE_INIT;
		StringBuilder tag = new StringBuilder();
		char c;
		while ((c = (char)reader.read()) >= 0) switch (state)
		{
			case STATE_INIT:
			{
				if (c == '{')
					state = STATE_TAG_START;
				else
					writer.append(c);
			}
			break;

			case STATE_TAG_START:
			{
				if (c == '{')
					state = STATE_TAG;
				else
				{
					state = STATE_INIT;
					writer.append('{');
					writer.append(c);
				}
			}
			break;

			case STATE_TAG:
			{
				if (c == '}')
				{
					state = STATE_TAG_END;
				}
				else
				{
					tag.append(c);
				}
			}
			break;
			
			case STATE_TAG_END:
			{
				if (c == '}')
				{
					state = STATE_INIT;
					String tagName = tag.toString();
					switch (tagName)
					{
						case REPLACEKEY_CURRENT_YEAR:
							// TODO: Finish.
							break;
						case REPLACEKEY_CURRENT_DATE:
							// TODO: Finish.
							break;
						case REPLACEKEY_SYSTEM_CHARSET:
							// TODO: Finish.
							break;
						case REPLACEKEY_COMPILER_VERSION:
							// TODO: Finish.
							break;
						default:
							tag.append("{{");
							tag.append(c);
							tag.append("}}");
							break;
					}
					tag.delete(0, tag.length());
				}
				else
				{
					state = STATE_TAG;
					tag.append('}');
					tag.append(c);
				}
			}
			break;
		}
	}
	
	/**
	 * Calls UglifyJS with a STDIN stream and STDOUT.
	 * @param inputData the file input.
	 * @param outputData the file output.
	 * @throws IOException If a read/write error occurs.
	 * @throws SecurityException if read/write has no permission to continue.
	 * @throws InterruptedException if this thread is interrupted waiting for the process to finish.
	 */
	private static void uglify(InputStream inputData, OutputStream outputData) throws IOException, InterruptedException
	{
		Process proc;
		if (OSUtils.isWindows())
		{
			proc = Runtime.getRuntime().exec(new String[] {
				"cmd", "/c", "uglifyjs", "-c", "-m", "--comments"
			});
		}
		else
		{
			proc = Runtime.getRuntime().exec(new String[] {
				"/bin/bash", "-c", "uglifyjs", "-c", "-m", "--comments"	
			});
		}

		IOUtils.relay(inputData, proc.getOutputStream(), 16384);
		IOUtils.relay(proc.getInputStream(), outputData, 16384);
		proc.waitFor();
	}

	/**
	 * Checks if UglifyJS is present.
	 * @return true if so, false if not, null on error.
	 */
	private static Boolean hasUglify()
	{
		Process proc;
		if (OSUtils.isWindows())
		{
			try {
				(proc = Runtime.getRuntime().exec(new String[] {
					"cmd", "/c", "where /q uglifyjs"	
				})).waitFor();
			} catch (SecurityException | IOException | InterruptedException e) {
				return null;
			}
		}
		else
		{
			try {
				(proc = Runtime.getRuntime().exec(new String[] {
					"/bin/bash", "-c", "which uglifyjs"	
				})).waitFor();
			} catch (SecurityException | IOException | InterruptedException e) {
				return null;
			}
		}
		return proc.exitValue() == 0;
	}
	
	@FunctionalInterface
	private static interface PropertyConverter<T>
	{
		T convert(String input);
	}
	
	/**
	 * Compiler options.
	 */
	private static final class Options implements TAMEScriptReaderOptions, TAMEJSExporterOptions
	{
		private String outPath; 
		private String outModulePath; 
		private String outWebDirectory; 
		private String outWebZip; 

		private Charset charset;
		private String scriptPath;
		private String webStartingFile;
		private String webAssetsDirectory;

		private String[] defines;
		private boolean optimizing;
		private PrintStream verboseOut;
		
		private Options(Properties properties)
		{
			this.outPath = convertProperty(properties, PROJECT_PROPERTY_DIST, null, (input)->
				input
			);
			this.outModulePath = convertProperty(properties, PROJECT_PROPERTY_DIST_MODULE, null, (input)->
				input
			);
			this.outWebDirectory = convertProperty(properties, PROJECT_PROPERTY_DIST_WEB, null, (input)->
				input
			);
			this.outWebZip = convertProperty(properties, PROJECT_PROPERTY_DIST_WEB_ZIP, null, (input)->
				input
			);
			
			this.charset = convertProperty(properties, PROJECT_PROPERTY_CHARSET, Charset.defaultCharset().displayName(), (input)->
				Charset.forName(input)
			);
			this.scriptPath = convertProperty(properties, PROJECT_PROPERTY_SRC_MAIN, null, (input)->
				input
			);
			this.webStartingFile = convertProperty(properties, PROJECT_PROPERTY_HTML_INDEX, null, (input)->
				input
			);
			this.webAssetsDirectory = convertProperty(properties, PROJECT_PROPERTY_HTML_ASSETS, null, (input)->
				input
			);
			
			this.defines = convertProperty(properties, PROJECT_PROPERTY_DEFINES, "", (input)->
				ObjectUtils.isEmpty(input) ? new String[]{} : input.split("\\,\\s+")
			);
			this.optimizing = convertProperty(properties, PROJECT_PROPERTY_NOOPTIMIZE, "false", (input)->
				!ValueUtils.parseBoolean(input)
			);
			this.verboseOut = convertProperty(properties, PROJECT_PROPERTY_VERBOSE, "false", (input)->
				ValueUtils.parseBoolean(input) ? System.out : null
			);
		}
		
		private static <T> T convertProperty(Properties properties, String key, String defValue, PropertyConverter<T> converter)
		{
			String value = properties.getProperty(key);
			String convertable = ObjectUtils.isEmpty(value) ? defValue : value;
			return converter.convert(convertable);
		}
		
		public String getOutPath() 
		{
			return outPath;
		}
		
		public String getOutModulePath()
		{
			return outModulePath;
		}
		
		public String getOutWebDirectory() 
		{
			return outWebDirectory;
		}
		
		public String getOutWebZip()
		{
			return outWebZip;
		} 
		
		public String getScriptPath()
		{
			return scriptPath;
		}
		
		@Override
		public Charset getInputCharset()
		{
			return charset;
		}

		@Override
		public String[] getDefines()
		{
			return defines;
		}

		@Override
		public boolean isOptimizing()
		{
			return optimizing;
		}

		@Override
		public String getModuleVariableName() 
		{
			return "EmbeddedData";
		}

		@Override
		public String getStartingPath() 
		{
			return webStartingFile;
		}
		
		public String getAssetsPath()
		{
			return webAssetsDirectory;
		}

		@Override
		public PrintStream getVerboseStream() 
		{
			return verboseOut;
		}
		
	}
	
}
